<div align="center">

  <img src="https://nuvio.tv/assets/nuvio-app-logo-wordmark.webp" alt="Nuvio" width="320" />

  <h1>Nuvio Pro</h1>

  <p>
    An unofficial fork of <a href="https://github.com/NuvioMedia/NuvioMobile">Nuvio Mobile</a> that keeps pace with
    upstream and adds features and platform polish on top of it.
    <br /><br />
    Bring your own sources. Nuvio turns them into a library with artwork, ratings, subtitles, and your place saved on every screen.
  </p>

  <p>
    <a href="https://github.com/albertovinaroz/NuvioPro/releases/latest">Releases</a> ·
    <a href="https://github.com/NuvioMedia/NuvioMobile">Upstream project</a> ·
    <a href="https://nuvio.tv">nuvio.tv</a> ·
    <a href="https://nuvio.tv/support">Support Nuvio</a>
  </p>

</div>

> **Unofficial.** This fork is not affiliated with or supported by the Nuvio team. Please report bugs you find here to
> **this** repository, not upstream — unless you can reproduce them on an official build too.

---

## Install

This repo publishes two separate iOS channels — pick one:

| Channel | What it is |
|---|---|
| **Nuvio Pro** | Upstream Nuvio plus everything in [What Pro adds](#what-pro-adds) below. |
| **Nuvio Unmodified** | A plain, unmodified build of NuvioMedia's own source — no fork changes, just a ready-to-sideload IPA kept in sync with their releases. |

### iOS — AltStore / SideStore

Add whichever source(s) you want:

```
https://raw.githubusercontent.com/albertovinaroz/NuvioPro/pro/store-pro.json
```
```
https://raw.githubusercontent.com/albertovinaroz/NuvioPro/pro/store-unmodified.json
```

Or grab an IPA directly from [the latest release](https://github.com/albertovinaroz/NuvioPro/releases/latest) —
Pro and Unmodified builds are tagged and released separately, named `nuvio-<version>-pro-release.ipa` and
`nuvio-<version>-unmodified-release.ipa`.

> This fork is iOS-focused — there's no Android build published here. For Android, use
> [official Nuvio Mobile](https://github.com/NuvioMedia/NuvioMobile) or
> [luqmanfadlli's Enhanced fork](https://github.com/luqmanfadlli/NuvioMobile-Enhanced).

---

## What Pro adds

Everything below is added on top of upstream Nuvio Mobile.

### Home

| Feature | Where | Default |
|---|---|---|
| **Hero trailer autoplay** — trailers play in the hero carousel instead of static artwork. The carousel stops auto-advancing while a trailer plays, so it only moves when you swipe. | Settings → Layout → Home Layout → **Hero Trailer Playback** | Off |
| **Trailer start delay** — how long the artwork holds before the trailer starts, `Instant` to 10 s. Appears once trailer playback is on. | Settings → Layout → Home Layout → **Trailer Start Delay** | Instant |
| **Hero style** — `Full-bleed` (artwork spans the screen), `Card` (rounded, inset), or `Poster` (taller, HBO Max–style layout with the title and details grouped to the left). | Settings → Layout → Home Layout → **Hero Style** | Full-bleed |
| **Home notifications & downloads bar** — a bell (opens the notification feed) and a downloads shortcut float above the hero in every style; hide either one independently for a cleaner look. | Settings → Layout → Home Layout → **Notifications icon** / **Downloads icon** | Both on |
| **Top 10 rank cards & catalog view mode** — a wide card with a big rank numeral for any catalog, plus a per-catalog Portrait/Landscape view mode that also applies to regular cards. | Settings → Layout → Home Layout → expand a catalog | Off |
| **Long-press to add to a list** — long-press the hero's add-to-library button to add or remove the item from specific custom lists instead of just toggling the default. | Hero → add-to-library button | — |
| **Dynamic background** — tints the home screen with a gradient pulled from the featured artwork's colours. | Settings → Layout → **Dynamic background color** | Off |
| **Catalog accent underline** — accent rule under each catalog row heading. | Settings → Layout → **Catalog accent underline** | Off |

### Notifications

| Feature | Where | Default |
|---|---|---|
| **In-app notification feed** — every episode/premiere alert lands in a running list instead of only firing a system notification; mark items read, remove one, or clear the whole feed. | Bell icon above the Home hero | — |
| **Test entries in the feed** — Send Test Notification also drops a preview card into the feed, so you can see how it looks without waiting for a real release. | Settings → Notifications → **Send Test Notification** | — |

### Player

| Feature | Where | Default |
|---|---|---|
| **Tap-to-seek on the timeline** — tap anywhere on the progress bar to jump there. | — | Always on |
| **Volume Boost** — volume can be boosted past 100% | Swipe up all the way past 100% | Always on |
| **Stream Quality Chooser** — quality indicator and the ability to choose quality on HLS streams whenever available. | Player screen overlay | Best quality supported by hardware |
| **Info Button** — playback info button showing the currently playing video and audio details. | Player screen overlay | — |
| **Swipe to Seek toggle** — turn off horizontal swipe-to-seek to prevent accidental seeking, while keeping the up/down brightness and volume swipes. | Settings → Playback → **Swipe to Seek** (under Touch Gestures) | On |
| **Hardware keyboard shortcuts** — <kbd>Space</kbd> play/pause, <kbd>←</kbd> / <kbd>→</kbd> seek 10 s, <kbd>Esc</kbd> leave the player. Inert while a panel is open or the controls are locked. | — | Always on |
| **Adjustable subtitle transparency** | Settings → Playback → Subtitle Rendering → **Background Color** | — |
| **Auto-show subtitles on rewind or mute** — when subtitles are off, they switch on while you rewind or while muted/volume-zero, then hide again once playback catches up or you unmute. Two independent toggles. | Settings → Playback → **Auto-Show Subtitles on Rewind** / **on Mute** | Both on |

### Live TV

Upstream Nuvio has no Live TV. This fork adds the whole feature.

| Feature | Where | Default |
|---|---|---|
| **M3U playlists** | Settings → Integrations → Live TV → **Playlists** | — |
| **Xtream** — connect with a server URL, username and password. | Settings → Integrations → Live TV → **Providers** → Xtream | Not configured |
| **Stalker Portal** — connect with a portal URL and MAC address; login details optional. | Settings → Integrations → Live TV → **Providers** → Stalker Portal | Not configured |
| **Show Live TV in navigation** — the tab appears once at least one source is configured. | Settings → Integrations → Live TV → **Show Live TV in navigation** | On |

### Profiles

| Feature | Where | Default |
|---|---|---|
| **Profile Insights** — activity, library and taste breakdowns for the active profile, in Overview and Taste sections. | Settings → **Profile** | Always available |
| **Custom profile background** — point a profile at any `http(s)` image URL. | Edit Profile → **Choose Profile Background** → Custom → **Custom background URL** | None |
| **Animated profile switch** — tapping a profile glides its avatar to the center of the screen into the loading transition, instead of a hard cut. | Profile selection screen | Always on |
| **Branded launch intro** — a quick wordmark reveal plays while the app resolves your session on cold start, replacing the old loading spinner. | App launch | Always on |
| **Smooth theme color transitions** — the Nuvio wordmark crossfades between Supporter+ color themes instead of cutting abruptly when a profile uses a different one. | — | Always on |
| **Haptic feedback on profile selection** — a light vibration on tap and again when the transition into the app starts. | Profile selection screen | Always on |

### Details & discovery

| Feature | Where | Default |
|---|---|---|
| **More Like This → View All** — the recommendation rail's header opens the full list as a paged grid that keeps loading as you scroll, instead of stopping at one page. | — | Always on when the rail has more to show |
| **Budget and revenue** — added to the details block for movies. | Shown with Settings → Layout → Detail Page → **Details** | — |
| **Real IMDb episode ratings** — episode cards show the actual IMDb rating (via OMDb) instead of TMDB's vote average, falling back to TMDB per episode when IMDb has none. | Settings → Integrations → TMDB Enrichment → **Episode ratings** | On |
| **Your own OMDb API key** — add a free key so episode ratings draw from your own quota instead of sharing one with every other user. | Settings → Integrations → **OMDb** | Uses a shared key if left empty |
| **Random Episode** — Play random episode for series. | 3 dots next to play button → **random icon** | — |
| **Include watched episodes toggle** — Include watched episodes in random playback. | Settings → Playback → **Include watched episodes in random playback**| Off |
| **Season-complete celebration** — a haptic tap and a toast when marking the last unwatched episode of a season as watched. | Episode watched-status sheet | Always on |

### Library

| Feature | Where | Default |
|---|---|---|
| **Library Calendar** — full-screen view of upcoming release dates for ongoing series in the library. | Library screen → **calendar icon** | — |

### Downloads

| Feature | Where | Default |
|---|---|---|
| **Wi-Fi-only downloads** — downloads wait for Wi-Fi unless you allow mobile data. The switch sits at the top of the Downloads screen, not in Settings. | Downloads screen → **Allow mobile data** | Off (Wi-Fi only) |
| **Custom download location** — pick any folder to save downloads to (a folder you grant via Storage Access Framework on Android, or via the Files app on iOS) instead of the app's private storage. | Downloads screen → **gear icon** → **Download Location** | Internal storage |
| **Background downloads (iOS)** — a download keeps transferring after you leave the app, instead of stalling the moment it's backgrounded. Doesn't survive a full force-quit. | — | Always on |
| **Download progress notification (iOS)** — live progress shown in a Lock Screen/Dynamic Island Live Activity while a download is running. | — | Always on |
| **Download-completed notification (iOS)** — a local notification confirming a download finished, backing up the Live Activity for updates iOS drops while backgrounded. | — | Always on |

### Tracking

| Feature | Where | Default |
|---|---|---|
| **Sign in with a code** — device-code sign-in for **Trakt** and **SIMKL**, for when the browser redirect won't come back, especially when installed within LiveContainer. Shows a code to enter on any other device. | Settings → Tracking → provider card → **Connect with code** | — |

> **WeTrakr** now has a preview card in Settings → Tracking. WeTrakr doesn't have a public API yet, so there's nothing to connect to — actual scrobbling support ships once they open it up.

### iOS look and feel

| Feature | Where | Default |
|---|---|---|
| **Experimental Picture in Picture** — Metal-based render pipeline that slides into PiP without reopening the stream. It changes the core video output, so treat it as experimental. | Settings → Playback → **Experimental Picture in Picture** | Off |
| **Morphed Liquid Glass tab bar** — shrinks to a compact pill, with native drag-across-tabs and the system glass highlight (requires an **iPhone on iOS 26 or newer**). | Settings → Layout → **Liquid Glass tab bar** | Morphed |
| **Skia graphics engine** — rebuilt graphics engine for animated artwork rendering with shared codecs and bounded memory. Fixes a crash on large animated collections; supports animated avatars and badges. | — | — |
| **Bundled CJK font** — fixes Chinese subtitle rendering. | — | — |
| **Native-style Settings rows** — plain icon and chevron, no colored chips or section labels, matching the stock iOS/WhatsApp settings list look. | Settings | Always on |
| **Pill-shaped search bars** — every search field (Settings, Search, Live TV, Player live channels, Cloud Library) uses a fully rounded pill, matching iOS conventions. | — | Always on |
| **Liquid Glass mute button** — mutes the hero trailer without leaving Home, in the app's Liquid Glass style. | Top-right of the hero, while a trailer is playing | — |
| **Filled/outlined tab bar icons** — Home, Search, Library and Live TV glyphs swap between filled and outlined artwork based on selection instead of only changing tint. | — | Always on |

---

## Build from source

```bash
git clone https://github.com/albertovinaroz/NuvioPro.git
cd NuvioPro
git checkout pro
```

### Android

Requires Android Studio and the Android SDK.

```bash
./gradlew :androidApp:assembleFullDebug        # sideload flavour
./gradlew :androidApp:assemblePlaystoreDebug   # store flavour
```

### iOS

Requires macOS and Xcode.

```bash
env NUVIO_IOS_DISTRIBUTION=full xcodebuild \
  -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Debug \
  -sdk iphonesimulator \
  -derivedDataPath build/ios-derived-full-simulator \
  CODE_SIGNING_ALLOWED=NO \
  build
```

The shared app is built with Kotlin Multiplatform and Compose Multiplatform.

## Staying in sync

This fork tracks NuvioMedia's `cmp-rewrite` and merges upstream releases as they land, and periodically merges
[luqmanfadlli's own Enhanced fork](https://github.com/luqmanfadlli/NuvioMobile-Enhanced) — another active fork of the
same upstream — to share improvements both ways.

```bash
git remote add upstream https://github.com/NuvioMedia/NuvioMobile.git
git fetch upstream
git merge upstream/cmp-rewrite
```

---

## Credits

Nuvio is built by [NuvioMedia](https://github.com/NuvioMedia) — all credit for the app itself belongs to them and its
contributors. This repository only adds to their work. If you enjoy Nuvio, [support the upstream project](https://nuvio.tv/support).

Thanks also to [luqmanfadlli](https://github.com/luqmanfadlli/NuvioMobile-Enhanced) for maintaining a sibling fork
(Nuvio Enhanced) we regularly trade improvements with.

## License

[GNU General Public License v3.0](./LICENSE) — same as upstream.
