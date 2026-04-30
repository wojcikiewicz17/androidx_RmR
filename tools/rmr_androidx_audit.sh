#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(git rev-parse --show-toplevel)"
cd "$ROOT_DIR"

REPORT_DIR="reports"
REPORT_FILE="$REPORT_DIR/rmr_androidx_audit.txt"
mkdir -p "$REPORT_DIR"

{
  echo "RMR AndroidX Audit Report"
  echo "Generated at: $(date -u +"%Y-%m-%dT%H:%M:%SZ")"
  echo
  echo "== git diff --stat =="
  git diff --stat
  echo
  echo "== untracked --stat =="
  git status --short
  echo
  echo "== lockfiles =="
  find . -type f \( -name "yarn.lock" -o -name "package-lock.json" -o -name "pnpm-lock.yaml" -o -name "gradle.lockfile" -o -name "Cargo.lock" \) | sort
  echo
  echo "== yarn.lock hash (kotlin-js-store) =="
  if [[ -f kotlin-js-store/yarn.lock ]]; then
    sha256sum kotlin-js-store/yarn.lock
  else
    echo "kotlin-js-store/yarn.lock not found"
  fi
  echo
  echo "== Kotlin/JS impacted modules (from changed paths heuristic) =="
  CHANGED_PATHS="$( (git diff --name-only; git ls-files --others --exclude-standard) | sort -u )"
  if [[ -z "$CHANGED_PATHS" ]]; then
    echo "No local changes"
  else
    echo "$CHANGED_PATHS" | grep -Ei '(kotlin-js-store|kotlin|js|compose)' || echo "No Kotlin/JS-related changed paths detected"
  fi
} > "$REPORT_FILE"

echo "Report generated: $REPORT_FILE"
