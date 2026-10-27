#!/usr/bin/env sh
# Generates any missing local secrets into .env without printing them.
# Existing values (including ANTHROPIC_API_KEY / OPENAI_API_KEY / DEEPGRAM_API_KEY) are kept.
set -eu
cd "$(dirname "$0")/.."
touch .env
# Make sure appended lines never glue onto an existing last line.
[ -s .env ] && [ "$(tail -c1 .env | od -An -c | tr -d ' ')" != "\n" ] && printf '
' >> .env
chmod 600 .env 2>/dev/null || true

gen() { openssl rand -base64 48 | tr -d '\n/+=' | cut -c1-48; }

ensure() {
  if ! grep -q "^$1=" .env; then
    printf '%s=%s\n' "$1" "$2" >> .env
    echo "generated $1"
  fi
}

ensure POSTGRES_PASSWORD "$(gen)"
ensure REDIS_PASSWORD "$(gen)"
ensure JWT_SECRET "$(gen)$(gen)"
ensure INTERNAL_JWT_SECRET "$(gen)$(gen)"
ensure ADMIN_PASSWORD "Lb!$(gen | cut -c1-20)9a"
ensure SESSION_SECRET "$(gen)"
echo "Secrets are in .env (git-ignored). The admin password is ADMIN_PASSWORD in that file."
