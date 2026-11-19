#!/usr/bin/env bash
# Generates the weekly report on demand for the 7 days before the reference date.
# Usage: scripts/invoke-report.sh [YYYY-MM-DD]   (default: today in Sao Paulo; STACK_NAME defaults to schoolapi)
set -euo pipefail

STACK_NAME="${STACK_NAME:-schoolapi}"
REFERENCE_DATE="${1:-$(TZ=America/Sao_Paulo date +%F)}"
FUNCTION_NAME="$(aws cloudformation describe-stacks --stack-name "$STACK_NAME" \
  --query "Stacks[0].Outputs[?OutputKey=='GenerateWeeklyReportFunctionName'].OutputValue" --output text)"

OUTPUT="$(mktemp)"
trap 'rm -f "$OUTPUT"' EXIT

FUNCTION_ERROR="$(aws lambda invoke --function-name "$FUNCTION_NAME" \
  --cli-binary-format raw-in-base64-out \
  --payload "{\"referenceDate\":\"$REFERENCE_DATE\"}" \
  --query 'FunctionError' --output text "$OUTPUT")"

cat "$OUTPUT"
echo
if [[ "$FUNCTION_ERROR" != "None" ]]; then
  echo "Report generation failed: $FUNCTION_ERROR" >&2
  exit 1
fi
