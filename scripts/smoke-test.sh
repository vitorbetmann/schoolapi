#!/usr/bin/env bash
# Sends one valid, one invalid and one critical feedback to the deployed API.
# Usage: scripts/smoke-test.sh   (uses the current AWS profile; STACK_NAME defaults to schoolapi)
set -euo pipefail

STACK_NAME="${STACK_NAME:-schoolapi}"

stack_output() {
  aws cloudformation describe-stacks --stack-name "$STACK_NAME" \
    --query "Stacks[0].Outputs[?OutputKey=='$1'].OutputValue" --output text
}

API_URL="$(stack_output ApiUrl)"
API_KEY="$(aws apigateway get-api-key --api-key "$(stack_output ApiKeyId)" --include-value \
  --query value --output text)"

failures=0

check() {
  local name="$1" expected="$2" body="$3"
  local response status
  response="$(curl -sS -w $'\n%{http_code}' -X POST "$API_URL" \
    -H "Content-Type: application/json" -H "x-api-key: $API_KEY" -d "$body")"
  status="${response##*$'\n'}"
  if [[ "$status" == "$expected" ]]; then
    echo "PASS $name -> $status ${response%$'\n'*}"
  else
    echo "FAIL $name -> expected $expected, got $status ${response%$'\n'*}"
    failures=$((failures + 1))
  fi
}

check "valid" 201 '{"descricao":"Aula muito boa, exemplos claros.","nota":8}'
check "invalid" 400 '{"descricao":"Nota fora do intervalo.","nota":11}'
check "critical" 201 '{"descricao":"A aula ficou confusa e os exemplos não funcionaram.","nota":2}'

if (( failures > 0 )); then
  echo "$failures check(s) failed"
  exit 1
fi
echo "All checks passed"
