#!/usr/bin/env bash

set -euo pipefail

from_ref=""
to_ref="HEAD"
repository="${GITHUB_REPOSITORY:-}"
offline=false
exclude_commits="${RELEASE_NOTES_EXCLUDE_COMMITS:-}"

usage() {
    echo "Usage: $0 --from <commit> [--to <commit>] [--repository <owner/repo>] [--exclude <hashes>] [--offline]" >&2
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --from)
            from_ref="${2:-}"
            shift 2
            ;;
        --to)
            to_ref="${2:-}"
            shift 2
            ;;
        --repository)
            repository="${2:-}"
            shift 2
            ;;
        --exclude)
            exclude_commits="${exclude_commits} ${2:-}"
            shift 2
            ;;
        --offline)
            offline=true
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            usage
            exit 1
            ;;
    esac
done

if [[ -z "$from_ref" ]]; then
    usage
    exit 1
fi

git cat-file -e "${from_ref}^{commit}" 2>/dev/null || {
    echo "Unknown starting commit: ${from_ref}" >&2
    exit 1
}
git cat-file -e "${to_ref}^{commit}" 2>/dev/null || {
    echo "Unknown ending commit: ${to_ref}" >&2
    exit 1
}

is_excluded_hash() {
    local commit="$1"
    local excluded
    for excluded in ${exclude_commits//,/ }; do
        [[ -n "$excluded" ]] || continue
        if [[ "$commit" == "$excluded"* ]]; then
            return 0
        fi
    done
    return 1
}

is_release_note() {
    local subject_lower
    local version_bump_pattern='^(bump([[:space:]].*)?version|version[[:space:]]+bump)([[:space:]].*)?$'
    local cleanup_pattern='^cleanup([[:space:][:punct:]].*)?$'
    local conventional_noise_pattern='^(build|chore|ci|docs|style|test)(\([^)]*\))?:'
    subject_lower="$(printf '%s' "$1" | tr '[:upper:]' '[:lower:]')"

    [[ "$subject_lower" != *"[skip release notes]"* ]] || return 1
    [[ ! "$subject_lower" =~ $version_bump_pattern ]] || return 1
    [[ ! "$subject_lower" =~ $cleanup_pattern ]] || return 1
    [[ ! "$subject_lower" =~ $conventional_noise_pattern ]] || return 1
    return 0
}

resolve_username() {
    local commit="$1"
    local author_name="$2"
    local author_email="$3"
    local username=""

    if [[ "$author_email" =~ ^[0-9]+\+([^@]+)@users\.noreply\.github\.com$ ]]; then
        username="${BASH_REMATCH[1]}"
    elif [[ "$author_email" =~ ^([^@]+)@users\.noreply\.github\.com$ ]]; then
        username="${BASH_REMATCH[1]}"
    elif [[ "$offline" == false && -n "$repository" && -n "${GH_TOKEN:-}" ]] && command -v gh >/dev/null 2>&1; then
        username="$(gh api "repos/${repository}/commits/${commit}" --jq '.author.login // empty' 2>/dev/null || true)"
        if [[ -z "$username" ]]; then
            username="$(
                gh api \
                    -H 'Accept: application/vnd.github+json' \
                    "repos/${repository}/commits/${commit}/pulls" \
                    --jq '.[0].user.login // empty' \
                    2>/dev/null \
                    || true
            )"
        fi
    fi

    if [[ -z "$username" ]]; then
        username="$(printf '%s' "$author_name" | tr -cd '[:alnum:]_-')"
    fi
    printf '%s' "${username:-unknown}"
}

resolve_pull_request() {
    local commit="$1"

    if [[ "$offline" == true || -z "$repository" || -z "${GH_TOKEN:-}" ]] || ! command -v gh >/dev/null 2>&1; then
        return
    fi

    gh api \
        -H 'Accept: application/vnd.github+json' \
        "repos/${repository}/commits/${commit}/pulls" \
        --jq '[.[] | select(.merged_at != null)][0] | if . == null then empty else [.number, .title, (.user.login // "unknown")] | @tsv end' \
        2>/dev/null \
        || true
}

# Buckets each bullet into one of our three release-notes sections. Username (a GitHub login when
# resolvable, otherwise a sanitized author name) is the primary signal; author name/email are a
# fallback for commits authored before a GitHub login can be resolved (e.g. offline mode).
classify_author() {
    local username_lower author_name_lower author_email_lower
    username_lower="$(printf '%s' "${1:-}" | tr '[:upper:]' '[:lower:]')"
    author_name_lower="$(printf '%s' "${2:-}" | tr '[:upper:]' '[:lower:]')"
    author_email_lower="$(printf '%s' "${3:-}" | tr '[:upper:]' '[:lower:]')"

    case "$username_lower" in
        albertovinaroz) echo "pro"; return ;;
        luqmanfadlli) echo "luqman"; return ;;
    esac
    if [[ "$author_email_lower" == albertovinaroz*@* ]]; then
        echo "pro"; return
    fi
    if [[ "$author_name_lower" == "luqman fadlli" || "$author_email_lower" == luqman.fadlli@* ]]; then
        echo "luqman"; return
    fi
    echo "nuviomedia"
}

seen_subjects=$'\n'
seen_pull_requests=$'\n'
separator=$'\x1f'
notes_nuviomedia=""
notes_luqman=""
notes_pro=""

append_note() {
    local bucket="$1"
    local line="$2"
    case "$bucket" in
        pro) notes_pro+="${line}"$'\n' ;;
        luqman) notes_luqman+="${line}"$'\n' ;;
        *) notes_nuviomedia+="${line}"$'\n' ;;
    esac
}

while IFS="$separator" read -r commit short_hash subject author_name author_email; do
    [[ -n "$commit" ]] || continue
    is_excluded_hash "$commit" && continue

    pull_request="$(resolve_pull_request "$commit")"
    if [[ -n "$pull_request" ]]; then
        pull_request_number=""
        pull_request_title=""
        pull_request_username=""
        IFS=$'\t' read -r pull_request_number pull_request_title pull_request_username <<< "$pull_request"
        if [[ -n "$pull_request_number" ]]; then
            [[ "$seen_pull_requests" != *$'\n'"$pull_request_number"$'\n'* ]] || continue
            seen_pull_requests+="${pull_request_number}"$'\n'
            is_release_note "$pull_request_title" || continue
            display_pull_request_title="$(printf '%s' "$pull_request_title" | sed -E 's/[[:space:]]+$//')"
            bucket="$(classify_author "$pull_request_username" "$author_name" "$author_email")"
            append_note "$bucket" "$(printf -- '- [%s (#%s)](https://github.com/%s/pull/%s) @%s  ' \
                "$display_pull_request_title" \
                "$pull_request_number" \
                "$repository" \
                "$pull_request_number" \
                "${pull_request_username:-unknown}")"
            continue
        fi
    fi

    is_release_note "$subject" || continue

    normalized_subject="$(printf '%s' "$subject" | tr '[:upper:]' '[:lower:]' | sed -E 's/[[:space:]]+/ /g; s/[[:space:].]+$//')"
    [[ "$seen_subjects" != *$'\n'"$normalized_subject"$'\n'* ]] || continue
    seen_subjects+="${normalized_subject}"$'\n'

    display_subject="$(printf '%s' "$subject" | sed -E 's/[[:space:]]+$//; s/\.$//')"
    username="$(resolve_username "$commit" "$author_name" "$author_email")"
    bucket="$(classify_author "$username" "$author_name" "$author_email")"
    append_note "$bucket" "$(printf -- '- %s @%s  ' "$display_subject" "$username")"
done < <(
    git log "${from_ref}..${to_ref}" --no-merges \
        --format="%H${separator}%h${separator}%s${separator}%an${separator}%ae"
)

print_section() {
    local title="$1"
    local body="$2"
    [[ -n "$body" ]] || return 0
    printf '### %s\n\n' "$title"
    printf '%s' "$body"
    printf '\n'
}

print_section "Upstream (NuvioMedia)" "$notes_nuviomedia"
print_section "Upstream (luqmanfadlli's fork)" "$notes_luqman"
print_section "Pro" "$notes_pro"
