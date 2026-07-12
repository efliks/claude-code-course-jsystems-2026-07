#!/usr/bin/env bash
# Publish course slide decks to the DevPowers site repo.
# Junctions/symlinks do not work across two git repos (git tracks the link,
# not content) - this explicit copy is the sync mechanism. Run after any
# deck change.
#
# Usage:
#   ./publish-slides.sh          # copy only
#   ./publish-slides.sh --push   # copy + commit + push DevPowers (deploys to production)

set -euo pipefail

slides_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "$slides_dir/../.." && pwd)"

if [[ -n "${DEVPOWERS_REPO:-}" && -d "$DEVPOWERS_REPO" ]]; then
  dev_repo="$DEVPOWERS_REPO"
elif [[ -d "$HOME/dev/DevPowers" ]]; then
  dev_repo="$HOME/dev/DevPowers"
elif [[ -d "/mnt/c/Users/BiuroEdukey/DEV/Projects/DevPowers" ]]; then
  dev_repo="/mnt/c/Users/BiuroEdukey/DEV/Projects/DevPowers"
else
  echo "Error: DevPowers repo not found. Tried:" >&2
  echo "  \$DEVPOWERS_REPO=${DEVPOWERS_REPO:-<unset>}" >&2
  echo "  $HOME/dev/DevPowers" >&2
  echo "  /mnt/c/Users/BiuroEdukey/DEV/Projects/DevPowers" >&2
  exit 1
fi

dst="$dev_repo/szkolenia/claude-code-jsystems"
mkdir -p "$dst"

copied=()
for n in 1 2 3; do
  from="$slides_dir/day-$n.html"
  if [[ -f "$from" ]]; then
    to="$dst/Prezentacja_Dzien$n.html"
    sed 's|\.\./survey/dashboard/index\.html|ankieta.html|g' "$from" > "$to"
    copied+=("Prezentacja_Dzien$n.html")
  fi
done

dash="$repo_root/course-materials/survey/dashboard/index.html"
if [[ -f "$dash" ]]; then
  cp "$dash" "$dst/ankieta.html"
  copied+=("ankieta.html")
fi

echo "Copied to ${dst}: $(IFS=', '; echo "${copied[*]}")"

if [[ "${1:-}" == "--push" ]]; then
  pushd "$dev_repo" >/dev/null
  git add "szkolenia/claude-code-jsystems"
  staged=$(git diff --cached --name-only)
  if [[ -n "$staged" ]]; then
    git commit -m "Szkolenia: Claude Code JSystems - sync slajdow z repo kursu"
    git push
    echo "DevPowers committed and pushed (production deploy)."
  else
    echo "No changes to publish - DevPowers already up to date."
  fi
  popd >/dev/null
else
  echo "NEXT: review the diff in the DevPowers repo and commit, or rerun with --push to auto commit+push."
fi
