#!/usr/bin/env bash

set -euo pipefail

version=""
changelog_file=""
extra_note=""

usage() {
    echo "Usage: $0 --version <version> --changelog <file> [--note <extra iOS note>]" >&2
    echo "  --version    Release version, e.g. 0.4.21-b1" >&2
    echo "  --changelog  Path to a markdown file with the '### Upstream (NuvioMedia)' / '### Pro' sections" >&2
    echo "  --note       Optional extra '> **Note for iOS users:**' line, appended after the standing one" >&2
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --version)
            version="${2:-}"
            shift 2
            ;;
        --changelog)
            changelog_file="${2:-}"
            shift 2
            ;;
        --note)
            extra_note="${2:-}"
            shift 2
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

if [[ -z "$version" || -z "$changelog_file" ]]; then
    usage
    exit 1
fi

if [[ ! -f "$changelog_file" ]]; then
    echo "Changelog file not found: ${changelog_file}" >&2
    exit 1
fi

cat <<EOF
# Nuvio Pro ${version}

**AltStore/SideStore source:**
\`\`\`
https://raw.githubusercontent.com/albertovinaroz/NuvioPro/pro/store-pro.json
\`\`\`

## Changelog

EOF

cat "$changelog_file"

cat <<'EOF'

> **Note for iOS users:** since Nuvio embeds heavy media frameworks, sideloading tools like AltStore, SideStore, or Sideloadly have to manually re-sign thousands of internal components with your Apple ID certificate. If the installation progress bar looks stuck, don't close it — give it a few minutes to finish signing in the background.
EOF

if [[ -n "$extra_note" ]]; then
    printf '\n> **Note for iOS users:** %s\n' "$extra_note"
fi
