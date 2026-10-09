/**
 * Nuvio Pro social server: friends by code, recommendations with replies, and friends' activity
 * ("watching now" / recently finished).
 *
 * Identity is the user's Nuvio account. The app sends its Nuvio access token, which is checked
 * against Nuvio's auth server and never stored (only its SHA-256, for a short cache). Each Nuvio
 * profile is its own person, picked with the `X-Nuvio-Profile` header.
 */

interface Env {
  DB: D1Database;
  SUPABASE_URL: string;
  SUPABASE_ANON_KEY: string;
  /** Local development only (`.dev.vars`): accepts `Bearer dev:<account>` without a Nuvio call. */
  DEV_FAKE_AUTH?: string;
}

// Minimal D1 typings, so the worker builds without @cloudflare/workers-types.
interface D1Result<T> { results: T[] }
interface D1PreparedStatement {
  bind(...values: unknown[]): D1PreparedStatement;
  first<T = Record<string, unknown>>(): Promise<T | null>;
  all<T = Record<string, unknown>>(): Promise<D1Result<T>>;
  run(): Promise<unknown>;
}
interface D1Database {
  prepare(query: string): D1PreparedStatement;
  batch(statements: D1PreparedStatement[]): Promise<unknown[]>;
}

const SESSION_TTL_MS = 10 * 60 * 1000;
/** A "watching" row counts as live while heartbeats keep arriving within this window. */
const WATCHING_LIVE_MS = 15 * 60 * 1000;
/** Heartbeats for the same title within this window update one row instead of adding another. */
const WATCHING_MERGE_MS = 6 * 60 * 60 * 1000;
const FEED_WINDOW_MS = 14 * 24 * 60 * 60 * 1000;
const ACTIVITY_RETENTION_MS = 30 * 24 * 60 * 60 * 1000;
const RECOMMENDATION_RETENTION_MS = 180 * 24 * 60 * 60 * 1000;
const NOTIFICATION_RETENTION_MS = 60 * 24 * 60 * 60 * 1000;
const MAX_PROFILES = 8;
const MAX_RECOMMENDATIONS_PER_DAY = 60;
const REACTIONS = new Set(["love", "laugh", "fire", "like", "dislike", "watched"]);
const FRIEND_CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";

class HttpError extends Error {
  constructor(readonly status: number, readonly code: string) {
    super(code);
  }
}

interface Caller {
  account: string;
  profile: number;
  personId: string;
}

interface PersonRow {
  id: string;
  name: string;
  avatar: string | null;
  friend_code: string;
  sharing: number;
}

interface ActivityRow {
  id: number;
  person_id: string;
  kind: string;
  content_type: string;
  content_id: string;
  title: string;
  poster: string | null;
  season: number | null;
  episode: number | null;
  episode_title: string | null;
  progress: number | null;
  created_at: number;
  updated_at: number;
}

interface RecommendationRow {
  id: string;
  from_id: string;
  to_id: string;
  content_type: string;
  content_id: string;
  title: string;
  poster: string | null;
  note: string | null;
  reaction: string | null;
  reply: string | null;
  created_at: number;
  replied_at: number | null;
  seen_at: number | null;
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    try {
      return await route(request, env);
    } catch (error) {
      if (error instanceof HttpError) return json({ error: error.code }, error.status);
      console.error(error);
      return json({ error: "internal" }, 500);
    }
  },

  async scheduled(_event: unknown, env: Env): Promise<void> {
    const now = Date.now();
    await env.DB.batch([
      env.DB.prepare("DELETE FROM sessions WHERE expires_at < ?").bind(now),
      env.DB.prepare("DELETE FROM activity WHERE updated_at < ?").bind(now - ACTIVITY_RETENTION_MS),
      env.DB.prepare("DELETE FROM recommendations WHERE created_at < ?").bind(now - RECOMMENDATION_RETENTION_MS),
      env.DB.prepare("DELETE FROM notifications WHERE created_at < ?").bind(now - NOTIFICATION_RETENTION_MS),
    ]);
  },
};

async function route(request: Request, env: Env): Promise<Response> {
  const url = new URL(request.url);
  const path = url.pathname.replace(/\/+$/, "");
  const method = request.method;

  if (path === "" || path === "/v1/health") return json({ ok: true });

  const caller = await authenticate(request, env);
  const body = method === "POST" || method === "PUT" ? await readBody(request) : {};

  if (path === "/v1/me" && method === "GET") return json(await me(env, caller));
  if (path === "/v1/me" && method === "PUT") return json(await updateMe(env, caller, body));

  if (path === "/v1/friends" && method === "GET") return json(await friends(env, caller));
  if (path === "/v1/friends/add" && method === "POST") return json(await addFriend(env, caller, body));
  if (path === "/v1/friends/accept" && method === "POST") return json(await acceptFriend(env, caller, body));
  if (path === "/v1/friends/decline" && method === "POST") return json(await dropRequest(env, personIdOf(body), caller.personId));
  if (path === "/v1/friends/cancel" && method === "POST") return json(await dropRequest(env, caller.personId, personIdOf(body)));
  if (path === "/v1/friends/remove" && method === "POST") return json(await removeFriend(env, caller, body));

  if (path === "/v1/recommendations" && method === "GET") return json(await recommendations(env, caller));
  if (path === "/v1/recommendations" && method === "POST") return json(await recommend(env, caller, body));
  const recMatch = path.match(/^\/v1\/recommendations\/([A-Za-z0-9-]+)\/(respond|seen)$/);
  if (recMatch && method === "POST") {
    return json(
      recMatch[2] === "respond"
        ? await respond(env, caller, recMatch[1], body)
        : await markSeen(env, caller, recMatch[1]),
    );
  }

  if (path === "/v1/activity" && method === "POST") return json(await recordActivity(env, caller, body));
  if (path === "/v1/feed" && method === "GET") return json(await feed(env, caller));
  if (path === "/v1/notifications" && method === "GET") return json(await notifications(env, caller));

  throw new HttpError(404, "not_found");
}

// ------------------------------------------------------------------------------------------
// Identity
// ------------------------------------------------------------------------------------------

async function authenticate(request: Request, env: Env): Promise<Caller> {
  const header = request.headers.get("Authorization") ?? "";
  const token = header.startsWith("Bearer ") ? header.slice(7).trim() : "";
  if (!token) throw new HttpError(401, "missing_token");

  const profileHeader = request.headers.get("X-Nuvio-Profile");
  const profile = profileHeader == null ? 1 : Number.parseInt(profileHeader, 10);
  if (!Number.isInteger(profile) || profile < 1 || profile > MAX_PROFILES) {
    throw new HttpError(400, "bad_profile");
  }

  const account = await verifyAccount(env, token);
  return {
    account,
    profile,
    personId: profile === 1 ? `nuvio:${account}` : `nuvio:${account}:${profile}`,
  };
}

async function verifyAccount(env: Env, token: string): Promise<string> {
  if (env.DEV_FAKE_AUTH === "1" && token.startsWith("dev:")) return token.slice(4);

  const now = Date.now();
  const tokenHash = await sha256(token);
  const cached = await env.DB
    .prepare("SELECT account FROM sessions WHERE token_hash = ? AND expires_at > ?")
    .bind(tokenHash, now)
    .first<{ account: string }>();
  if (cached) return cached.account;

  const response = await fetch(`${env.SUPABASE_URL}/auth/v1/user`, {
    headers: { apikey: env.SUPABASE_ANON_KEY, Authorization: `Bearer ${token}` },
  });
  if (response.status === 401 || response.status === 403) throw new HttpError(401, "invalid_token");
  if (!response.ok) throw new HttpError(503, "auth_unavailable");
  const user = (await response.json()) as { id?: string; is_anonymous?: boolean };
  if (!user.id) throw new HttpError(401, "invalid_token");

  await env.DB
    .prepare("INSERT OR REPLACE INTO sessions (token_hash, account, expires_at) VALUES (?, ?, ?)")
    .bind(tokenHash, user.id, now + SESSION_TTL_MS)
    .run();
  return user.id;
}

/** The caller's person row, created with a fresh friend code on first use. */
async function ensurePerson(env: Env, caller: Caller): Promise<PersonRow> {
  const existing = await env.DB
    .prepare("SELECT id, name, avatar, friend_code, sharing FROM people WHERE id = ?")
    .bind(caller.personId)
    .first<PersonRow>();
  if (existing) return existing;

  const now = Date.now();
  for (let attempt = 0; attempt < 5; attempt++) {
    const code = randomFriendCode();
    try {
      await env.DB
        .prepare(
          `INSERT INTO people (id, account, profile, friend_code, created_at, updated_at)
           VALUES (?, ?, ?, ?, ?, ?)`,
        )
        .bind(caller.personId, caller.account, caller.profile, code, now, now)
        .run();
      return { id: caller.personId, name: "", avatar: null, friend_code: code, sharing: 1 };
    } catch (error) {
      // A friend-code collision retries with a new code; a race on the same person reads it back.
      const raced = await env.DB
        .prepare("SELECT id, name, avatar, friend_code, sharing FROM people WHERE id = ?")
        .bind(caller.personId)
        .first<PersonRow>();
      if (raced) return raced;
    }
  }
  throw new HttpError(500, "friend_code_unavailable");
}

// ------------------------------------------------------------------------------------------
// Me
// ------------------------------------------------------------------------------------------

async function me(env: Env, caller: Caller) {
  const person = await ensurePerson(env, caller);
  const counts = await env.DB
    .prepare(
      `SELECT
         (SELECT COUNT(*) FROM friend_requests WHERE to_id = ?1) AS incoming,
         (SELECT COUNT(*) FROM recommendations WHERE to_id = ?1 AND seen_at IS NULL) AS unseen`,
    )
    .bind(caller.personId)
    .first<{ incoming: number; unseen: number }>();
  return {
    id: person.id,
    name: person.name,
    avatar: person.avatar,
    friendCode: formatFriendCode(person.friend_code),
    sharing: person.sharing,
    incomingRequests: counts?.incoming ?? 0,
    unseenRecommendations: counts?.unseen ?? 0,
  };
}

async function updateMe(env: Env, caller: Caller, body: Record<string, unknown>) {
  await ensurePerson(env, caller);
  const name = optionalText(body.name, 40);
  const avatar = optionalUrl(body.avatar);
  const sharing = body.sharing === undefined ? undefined : body.sharing === 0 || body.sharing === 1 ? body.sharing : null;
  if (sharing === null) throw new HttpError(400, "bad_sharing");

  await env.DB
    .prepare(
      `UPDATE people SET
         name = COALESCE(?, name),
         avatar = CASE WHEN ? THEN ? ELSE avatar END,
         sharing = COALESCE(?, sharing),
         updated_at = ?
       WHERE id = ?`,
    )
    .bind(name ?? null, body.avatar !== undefined ? 1 : 0, avatar ?? null, sharing ?? null, Date.now(), caller.personId)
    .run();
  return me(env, caller);
}

// ------------------------------------------------------------------------------------------
// Friends
// ------------------------------------------------------------------------------------------

async function friends(env: Env, caller: Caller) {
  await ensurePerson(env, caller);
  const now = Date.now();
  const [friendRows, incoming, outgoing, live] = await Promise.all([
    env.DB
      .prepare(
        `SELECT p.id, p.name, p.avatar, p.sharing, f.created_at AS since
         FROM friendships f JOIN people p ON p.id = f.friend_id
         WHERE f.person_id = ? ORDER BY p.name COLLATE NOCASE`,
      )
      .bind(caller.personId)
      .all<PersonRow & { since: number }>(),
    env.DB
      .prepare(
        `SELECT p.id, p.name, p.avatar, r.created_at AS since
         FROM friend_requests r JOIN people p ON p.id = r.from_id
         WHERE r.to_id = ? ORDER BY r.created_at DESC`,
      )
      .bind(caller.personId)
      .all<PersonRow & { since: number }>(),
    env.DB
      .prepare(
        `SELECT p.id, p.name, p.avatar, r.created_at AS since
         FROM friend_requests r JOIN people p ON p.id = r.to_id
         WHERE r.from_id = ? ORDER BY r.created_at DESC`,
      )
      .bind(caller.personId)
      .all<PersonRow & { since: number }>(),
    env.DB
      .prepare(
        `SELECT a.* FROM activity a
         JOIN friendships f ON f.friend_id = a.person_id AND f.person_id = ?
         JOIN people p ON p.id = a.person_id AND p.sharing = 1
         WHERE a.kind = 'watching' AND a.updated_at > ?
         ORDER BY a.updated_at DESC`,
      )
      .bind(caller.personId, now - WATCHING_LIVE_MS)
      .all<ActivityRow>(),
  ]);

  const watchingByPerson = new Map<string, ActivityRow>();
  for (const row of live.results) {
    if (!watchingByPerson.has(row.person_id)) watchingByPerson.set(row.person_id, row);
  }

  return {
    friends: friendRows.results.map((row) => ({
      ...publicPerson(row),
      since: row.since,
      watchingNow: watchingByPerson.has(row.id) ? activityJson(watchingByPerson.get(row.id)!) : null,
    })),
    incoming: incoming.results.map((row) => ({ ...publicPerson(row), since: row.since })),
    outgoing: outgoing.results.map((row) => ({ ...publicPerson(row), since: row.since })),
  };
}

async function addFriend(env: Env, caller: Caller, body: Record<string, unknown>) {
  await ensurePerson(env, caller);
  const code = normalizeFriendCode(body.code);
  if (!code) throw new HttpError(400, "bad_code");

  const target = await env.DB
    .prepare("SELECT id, name, avatar FROM people WHERE friend_code = ?")
    .bind(code)
    .first<PersonRow>();
  if (!target) throw new HttpError(404, "code_not_found");
  if (target.id === caller.personId) throw new HttpError(400, "own_code");

  if (await areFriends(env, caller.personId, target.id)) {
    return { status: "friends", person: publicPerson(target) };
  }

  // They already asked us: adding their code accepts it.
  const reverse = await env.DB
    .prepare("SELECT 1 FROM friend_requests WHERE from_id = ? AND to_id = ?")
    .bind(target.id, caller.personId)
    .first();
  if (reverse) {
    await befriend(env, caller.personId, target.id);
    await notify(env, target.id, "friend_accepted", caller.personId);
    return { status: "friends", person: publicPerson(target) };
  }

  const pending = await env.DB
    .prepare("SELECT 1 FROM friend_requests WHERE from_id = ? AND to_id = ?")
    .bind(caller.personId, target.id)
    .first();
  if (!pending) {
    await env.DB
      .prepare("INSERT INTO friend_requests (from_id, to_id, created_at) VALUES (?, ?, ?)")
      .bind(caller.personId, target.id, Date.now())
      .run();
    await notify(env, target.id, "friend_request", caller.personId);
  }
  return { status: "requested", person: publicPerson(target) };
}

async function acceptFriend(env: Env, caller: Caller, body: Record<string, unknown>) {
  const from = personIdOf(body);
  const request = await env.DB
    .prepare("SELECT 1 FROM friend_requests WHERE from_id = ? AND to_id = ?")
    .bind(from, caller.personId)
    .first();
  if (!request) throw new HttpError(404, "request_not_found");
  await befriend(env, caller.personId, from);
  await notify(env, from, "friend_accepted", caller.personId);
  return { status: "friends" };
}

async function dropRequest(env: Env, from: string, to: string) {
  await env.DB.prepare("DELETE FROM friend_requests WHERE from_id = ? AND to_id = ?").bind(from, to).run();
  return { ok: true };
}

async function removeFriend(env: Env, caller: Caller, body: Record<string, unknown>) {
  const other = personIdOf(body);
  await env.DB.batch([
    env.DB.prepare("DELETE FROM friendships WHERE person_id = ? AND friend_id = ?").bind(caller.personId, other),
    env.DB.prepare("DELETE FROM friendships WHERE person_id = ? AND friend_id = ?").bind(other, caller.personId),
  ]);
  return { ok: true };
}

async function befriend(env: Env, a: string, b: string) {
  const now = Date.now();
  await env.DB.batch([
    env.DB.prepare("INSERT OR IGNORE INTO friendships (person_id, friend_id, created_at) VALUES (?, ?, ?)").bind(a, b, now),
    env.DB.prepare("INSERT OR IGNORE INTO friendships (person_id, friend_id, created_at) VALUES (?, ?, ?)").bind(b, a, now),
    env.DB.prepare("DELETE FROM friend_requests WHERE (from_id = ? AND to_id = ?) OR (from_id = ? AND to_id = ?)").bind(a, b, b, a),
  ]);
}

async function areFriends(env: Env, a: string, b: string): Promise<boolean> {
  const row = await env.DB
    .prepare("SELECT 1 FROM friendships WHERE person_id = ? AND friend_id = ?")
    .bind(a, b)
    .first();
  return row != null;
}

// ------------------------------------------------------------------------------------------
// Recommendations
// ------------------------------------------------------------------------------------------

async function recommendations(env: Env, caller: Caller) {
  const [received, sent] = await Promise.all([
    env.DB
      .prepare(
        `SELECT r.*, p.name AS person_name, p.avatar AS person_avatar
         FROM recommendations r JOIN people p ON p.id = r.from_id
         WHERE r.to_id = ? ORDER BY r.created_at DESC LIMIT 100`,
      )
      .bind(caller.personId)
      .all<RecommendationRow & { person_name: string; person_avatar: string | null }>(),
    env.DB
      .prepare(
        `SELECT r.*, p.name AS person_name, p.avatar AS person_avatar
         FROM recommendations r JOIN people p ON p.id = r.to_id
         WHERE r.from_id = ? ORDER BY r.created_at DESC LIMIT 100`,
      )
      .bind(caller.personId)
      .all<RecommendationRow & { person_name: string; person_avatar: string | null }>(),
  ]);
  const toJson = (row: RecommendationRow & { person_name: string; person_avatar: string | null }, otherId: string) => ({
    id: row.id,
    person: { id: otherId, name: row.person_name, avatar: row.person_avatar },
    contentType: row.content_type,
    contentId: row.content_id,
    title: row.title,
    poster: row.poster,
    note: row.note,
    reaction: row.reaction,
    reply: row.reply,
    createdAt: row.created_at,
    repliedAt: row.replied_at,
    seen: row.seen_at != null,
  });
  return {
    received: received.results.map((row) => toJson(row, row.from_id)),
    sent: sent.results.map((row) => toJson(row, row.to_id)),
  };
}

async function recommend(env: Env, caller: Caller, body: Record<string, unknown>) {
  await ensurePerson(env, caller);
  const recipients = Array.isArray(body.to) ? [...new Set(body.to.filter((id): id is string => typeof id === "string"))] : [];
  if (recipients.length === 0 || recipients.length > 20) throw new HttpError(400, "bad_recipients");
  const content = contentOf(body);
  const note = optionalText(body.note, 280) ?? null;

  const now = Date.now();
  const sentToday = await env.DB
    .prepare("SELECT COUNT(*) AS n FROM recommendations WHERE from_id = ? AND created_at > ?")
    .bind(caller.personId, now - 24 * 60 * 60 * 1000)
    .first<{ n: number }>();
  if ((sentToday?.n ?? 0) + recipients.length > MAX_RECOMMENDATIONS_PER_DAY) {
    throw new HttpError(429, "too_many_recommendations");
  }

  const friendIds = new Set(
    (
      await env.DB
        .prepare("SELECT friend_id FROM friendships WHERE person_id = ?")
        .bind(caller.personId)
        .all<{ friend_id: string }>()
    ).results.map((row) => row.friend_id),
  );
  if (recipients.some((id) => !friendIds.has(id))) throw new HttpError(403, "not_friends");

  await env.DB.batch(
    recipients.map((to) =>
      env.DB
        .prepare(
          `INSERT INTO recommendations (id, from_id, to_id, content_type, content_id, title, poster, note, created_at)
           VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`,
        )
        .bind(crypto.randomUUID(), caller.personId, to, content.type, content.id, content.title, content.poster, note, now),
    ).concat(
      recipients.map((to) =>
        notificationStatement(env, to, "recommendation", caller.personId, {
          contentType: content.type,
          contentId: content.id,
          title: content.title,
          poster: content.poster,
          text: note,
        }),
      ),
    ),
  );
  return { sent: recipients.length };
}

async function respond(env: Env, caller: Caller, id: string, body: Record<string, unknown>) {
  const reaction = body.reaction == null ? null : String(body.reaction);
  if (reaction != null && !REACTIONS.has(reaction)) throw new HttpError(400, "bad_reaction");
  const reply = optionalText(body.reply, 280) ?? null;
  if (reaction == null && reply == null) throw new HttpError(400, "empty_response");

  const now = Date.now();
  const result = await env.DB
    .prepare(
      `UPDATE recommendations
       SET reaction = COALESCE(?, reaction), reply = COALESCE(?, reply), replied_at = ?, seen_at = COALESCE(seen_at, ?)
       WHERE id = ? AND to_id = ?
       RETURNING from_id, content_type, content_id, title, poster`,
    )
    .bind(reaction, reply, now, now, id, caller.personId)
    .first<Pick<RecommendationRow, "from_id" | "content_type" | "content_id" | "title" | "poster">>();
  if (!result) throw new HttpError(404, "recommendation_not_found");
  await notify(env, result.from_id, "recommendation_reply", caller.personId, {
    contentType: result.content_type,
    contentId: result.content_id,
    title: result.title,
    poster: result.poster,
    // "reaction|reply": the app renders the reaction as its emoji.
    text: [reaction ?? "", reply ?? ""].join("|"),
  });
  return { ok: true };
}

async function markSeen(env: Env, caller: Caller, id: string) {
  await env.DB
    .prepare("UPDATE recommendations SET seen_at = ? WHERE id = ? AND to_id = ? AND seen_at IS NULL")
    .bind(Date.now(), id, caller.personId)
    .run();
  return { ok: true };
}

// ------------------------------------------------------------------------------------------
// Activity
// ------------------------------------------------------------------------------------------

async function recordActivity(env: Env, caller: Caller, body: Record<string, unknown>) {
  const person = await ensurePerson(env, caller);
  // Sharing off: nothing is stored at all, not merely hidden.
  if (person.sharing === 0) return { stored: false };

  const kind = body.kind;
  if (kind !== "watching" && kind !== "finished") throw new HttpError(400, "bad_kind");
  const content = contentOf(body);
  const season = optionalInt(body.season);
  const episode = optionalInt(body.episode);
  const episodeTitle = optionalText(body.episodeTitle, 200) ?? null;
  const progress = typeof body.progress === "number" && body.progress >= 0 && body.progress <= 1 ? body.progress : null;
  const now = Date.now();

  const open = await env.DB
    .prepare(
      `SELECT id FROM activity
       WHERE person_id = ? AND kind = 'watching' AND content_id = ?
         AND COALESCE(season, -1) = ? AND COALESCE(episode, -1) = ? AND updated_at > ?
       ORDER BY updated_at DESC LIMIT 1`,
    )
    .bind(caller.personId, content.id, season ?? -1, episode ?? -1, now - WATCHING_MERGE_MS)
    .first<{ id: number }>();

  if (open) {
    await env.DB
      .prepare("UPDATE activity SET kind = ?, progress = COALESCE(?, progress), poster = COALESCE(?, poster), updated_at = ? WHERE id = ?")
      .bind(kind, kind === "finished" ? 1 : progress, content.poster, now, open.id)
      .run();
  } else {
    await env.DB
      .prepare(
        `INSERT INTO activity (person_id, kind, content_type, content_id, title, poster, season, episode, episode_title, progress, created_at, updated_at)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      )
      .bind(
        caller.personId, kind, content.type, content.id, content.title, content.poster,
        season, episode, episodeTitle, kind === "finished" ? 1 : progress, now, now,
      )
      .run();
  }
  return { stored: true };
}

async function feed(env: Env, caller: Caller) {
  const now = Date.now();
  const rows = await env.DB
    .prepare(
      `SELECT a.*, p.name AS person_name, p.avatar AS person_avatar
       FROM activity a
       JOIN friendships f ON f.friend_id = a.person_id AND f.person_id = ?
       JOIN people p ON p.id = a.person_id AND p.sharing = 1
       WHERE a.updated_at > ?
       ORDER BY a.updated_at DESC LIMIT 80`,
    )
    .bind(caller.personId, now - FEED_WINDOW_MS)
    .all<ActivityRow & { person_name: string; person_avatar: string | null }>();
  return {
    items: rows.results.map((row) => ({
      ...activityJson(row),
      person: { id: row.person_id, name: row.person_name, avatar: row.person_avatar },
      live: row.kind === "watching" && row.updated_at > now - WATCHING_LIVE_MS,
    })),
  };
}

// ------------------------------------------------------------------------------------------
// Notifications
// ------------------------------------------------------------------------------------------

interface NotificationExtra {
  contentType?: string | null;
  contentId?: string | null;
  title?: string | null;
  poster?: string | null;
  text?: string | null;
}

function notificationStatement(env: Env, personId: string, kind: string, actorId: string, extra: NotificationExtra = {}) {
  return env.DB
    .prepare(
      `INSERT INTO notifications (person_id, kind, actor_id, content_type, content_id, title, poster, text, created_at)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`,
    )
    .bind(
      personId, kind, actorId, extra.contentType ?? null, extra.contentId ?? null,
      extra.title ?? null, extra.poster ?? null, extra.text ?? null, Date.now(),
    );
}

async function notify(env: Env, personId: string, kind: string, actorId: string, extra: NotificationExtra = {}) {
  await notificationStatement(env, personId, kind, actorId, extra).run();
}

/**
 * The latest notices plus the counts behind the app's badges, in one call so the app can poll
 * cheaply. The app keeps its own read state, keyed by notice id.
 */
async function notifications(env: Env, caller: Caller) {
  const [rows, counts] = await Promise.all([
    env.DB
      .prepare(
        `SELECT n.*, p.name AS person_name, p.avatar AS person_avatar
         FROM notifications n LEFT JOIN people p ON p.id = n.actor_id
         WHERE n.person_id = ? ORDER BY n.id DESC LIMIT 50`,
      )
      .bind(caller.personId)
      .all<{
        id: number; kind: string; actor_id: string; content_type: string | null; content_id: string | null;
        title: string | null; poster: string | null; text: string | null; created_at: number;
        person_name: string | null; person_avatar: string | null;
      }>(),
    env.DB
      .prepare(
        `SELECT
           (SELECT COUNT(*) FROM friend_requests WHERE to_id = ?1) AS incoming,
           (SELECT COUNT(*) FROM recommendations WHERE to_id = ?1 AND seen_at IS NULL) AS unseen`,
      )
      .bind(caller.personId)
      .first<{ incoming: number; unseen: number }>(),
  ]);
  return {
    items: rows.results.map((row) => ({
      id: row.id,
      kind: row.kind,
      person: { id: row.actor_id, name: row.person_name ?? "", avatar: row.person_avatar },
      contentType: row.content_type,
      contentId: row.content_id,
      title: row.title,
      poster: row.poster,
      text: row.text,
      createdAt: row.created_at,
    })),
    incomingRequests: counts?.incoming ?? 0,
    unseenRecommendations: counts?.unseen ?? 0,
  };
}

// ------------------------------------------------------------------------------------------
// Helpers
// ------------------------------------------------------------------------------------------

function activityJson(row: ActivityRow) {
  return {
    id: row.id,
    kind: row.kind,
    contentType: row.content_type,
    contentId: row.content_id,
    title: row.title,
    poster: row.poster,
    season: row.season,
    episode: row.episode,
    episodeTitle: row.episode_title,
    progress: row.progress,
    startedAt: row.created_at,
    updatedAt: row.updated_at,
  };
}

function publicPerson(row: Pick<PersonRow, "id" | "name" | "avatar">) {
  return { id: row.id, name: row.name, avatar: row.avatar };
}

function contentOf(body: Record<string, unknown>) {
  const type = optionalText(body.contentType, 20);
  const id = optionalText(body.contentId, 200);
  const title = optionalText(body.title, 200);
  if (!type || !id || !title) throw new HttpError(400, "bad_content");
  return { type, id, title, poster: optionalUrl(body.poster) ?? null };
}

function personIdOf(body: Record<string, unknown>): string {
  const id = body.personId;
  if (typeof id !== "string" || !id.startsWith("nuvio:") || id.length > 120) throw new HttpError(400, "bad_person");
  return id;
}

function optionalText(value: unknown, maxLength: number): string | undefined {
  if (value == null) return undefined;
  if (typeof value !== "string") throw new HttpError(400, "bad_text");
  const trimmed = value.trim();
  if (trimmed.length === 0) return undefined;
  return trimmed.slice(0, maxLength);
}

function optionalUrl(value: unknown): string | undefined {
  const text = optionalText(value, 600);
  if (text === undefined) return undefined;
  return /^https:\/\//i.test(text) ? text : undefined;
}

function optionalInt(value: unknown): number | null {
  return typeof value === "number" && Number.isInteger(value) && value >= 0 && value < 10000 ? value : null;
}

function randomFriendCode(): string {
  const bytes = crypto.getRandomValues(new Uint8Array(8));
  return Array.from(bytes, (byte) => FRIEND_CODE_ALPHABET[byte % FRIEND_CODE_ALPHABET.length]).join("");
}

function formatFriendCode(code: string): string {
  return `${code.slice(0, 4)}-${code.slice(4)}`;
}

function normalizeFriendCode(value: unknown): string | null {
  if (typeof value !== "string") return null;
  const code = value.toUpperCase().replace(/[^A-Z0-9]/g, "");
  if (code.length !== 8) return null;
  return [...code].every((char) => FRIEND_CODE_ALPHABET.includes(char)) ? code : null;
}

async function readBody(request: Request): Promise<Record<string, unknown>> {
  const text = await request.text();
  if (!text) return {};
  if (text.length > 16_384) throw new HttpError(413, "body_too_large");
  try {
    const parsed = JSON.parse(text);
    if (parsed && typeof parsed === "object" && !Array.isArray(parsed)) return parsed as Record<string, unknown>;
  } catch {
    // Falls through to the error below.
  }
  throw new HttpError(400, "bad_json");
}

async function sha256(value: string): Promise<string> {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value));
  return Array.from(new Uint8Array(digest), (byte) => byte.toString(16).padStart(2, "0")).join("");
}

function json(data: unknown, status = 200): Response {
  return new Response(JSON.stringify(data), {
    status,
    headers: { "Content-Type": "application/json; charset=utf-8", "Cache-Control": "no-store" },
  });
}
