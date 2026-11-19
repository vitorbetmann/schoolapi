#!/usr/bin/env bash
# Writes 30 sample feedbacks spread over the 7 days before today (Sao Paulo time) straight into
# DynamoDB, so the weekly report has data without waiting a week. The API always stamps "now",
# which is why this goes to the table directly. The 3 critical items also trigger
# notify-critical-feedback (3 emails).
# Usage: scripts/seed-feedbacks.sh   (uses the current AWS profile; STACK_NAME defaults to schoolapi)
set -euo pipefail

STACK_NAME="${STACK_NAME:-schoolapi}"
TABLE_NAME="$(aws cloudformation describe-stacks --stack-name "$STACK_NAME" \
  --query "Stacks[0].Outputs[?OutputKey=='FeedbacksTableName'].OutputValue" --output text)"

TODAY="$(TZ=America/Sao_Paulo date +%F)"

days_before() {
  if date -d "$TODAY" +%F >/dev/null 2>&1; then
    date -d "$TODAY - $1 day" +%F
  else
    date -j -v-"$1"d -f %F "$TODAY" +%F
  fi
}

urgency_of() {
  if (( $1 <= 3 )); then echo CRITICAL; elif (( $1 <= 6 )); then echo MEDIUM; else echo LOW; fi
}

# Per day (7 days ago .. yesterday): how many feedbacks. Total 30.
PER_DAY=(5 4 6 3 5 4 3)
# 3 critical, 10 medium, 17 low.
SCORES=(9 8 2 7 10 5 6 8 9 4 7 1 8 6 9 10 5 7 8 4 9 3 6 7 8 5 9 6 10 4)
# Comments per urgency, so the text always matches the score's tone.
CRITICAL_DESCRIPTIONS=(
  "Não consegui acompanhar a explicação, a aula foi confusa do início ao fim."
  "O áudio da gravação estava cortando e perdi metade do conteúdo."
  "Os exercícios cobraram assuntos que não foram explicados."
)
MEDIUM_DESCRIPTIONS=(
  "Ritmo um pouco rápido demais."
  "Faltaram exemplos do mundo real."
  "Conteúdo bom, mas os slides estavam desatualizados."
  "A parte prática foi curta, gostaria de mais exercícios."
  "Explicação razoável, alguns pontos ficaram vagos."
)
LOW_DESCRIPTIONS=(
  "Aula muito boa, exemplos claros."
  "Conteúdo bem organizado."
  "Gostei dos exercícios práticos."
  "Excelente aula, professor muito didático."
  "Ótima explicação, consegui aplicar no trabalho."
  "Material de apoio muito completo."
)
critical_count=0
medium_count=0
low_count=0

description_for() {
  case "$1" in
    CRITICAL)
      description="${CRITICAL_DESCRIPTIONS[critical_count % ${#CRITICAL_DESCRIPTIONS[@]}]}"
      critical_count=$(( critical_count + 1 )) ;;
    MEDIUM)
      description="${MEDIUM_DESCRIPTIONS[medium_count % ${#MEDIUM_DESCRIPTIONS[@]}]}"
      medium_count=$(( medium_count + 1 )) ;;
    *)
      description="${LOW_DESCRIPTIONS[low_count % ${#LOW_DESCRIPTIONS[@]}]}"
      low_count=$(( low_count + 1 )) ;;
  esac
}

WORK_DIR="$(mktemp -d)"
trap 'rm -rf "$WORK_DIR"' EXIT

items=()
index=0
for offset in 7 6 5 4 3 2 1; do
  day="$(days_before "$offset")"
  for (( slot = 0; slot < PER_DAY[7 - offset]; slot++ )); do
    score="${SCORES[index]}"
    hour=$(( 12 + slot * 2 ))
    submitted_at="$(printf '%sT%02d:%02d:00.000Z' "$day" "$hour" $(( (index * 7) % 60 )))"
    id="$(uuidgen | tr '[:upper:]' '[:lower:]')"
    urgency="$(urgency_of "$score")"
    description_for "$urgency"
    items+=("{\"PutRequest\":{\"Item\":{\"id\":{\"S\":\"$id\"},\"description\":{\"S\":\"$description\"},\"score\":{\"N\":\"$score\"},\"urgency\":{\"S\":\"$urgency\"},\"submittedAt\":{\"S\":\"$submitted_at\"},\"day\":{\"S\":\"$day\"}}}}")
    index=$(( index + 1 ))
  done
done

write_batch() {
  local file="$WORK_DIR/batch.json" joined
  joined="$(IFS=,; echo "$*")"
  printf '{"%s":[%s]}' "$TABLE_NAME" "$joined" > "$file"
  local unprocessed
  unprocessed="$(aws dynamodb batch-write-item --request-items "file://$file" \
    --query 'length(UnprocessedItems)' --output text)"
  if [[ "$unprocessed" != "0" ]]; then
    echo "Some items were not written (throttled); run the script again" >&2
    exit 1
  fi
}

write_batch "${items[@]:0:25}"
write_batch "${items[@]:25}"

echo "Seeded ${#items[@]} feedbacks into $TABLE_NAME from $(days_before 7) to $(days_before 1)"
