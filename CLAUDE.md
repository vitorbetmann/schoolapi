# CLAUDE.md — SchoolAPI (FIAP ADJT · Fase 4 · Tech Challenge)

You are helping a student group build and deliver the Phase 4 Tech Challenge.
Work **one step at a time** from `docs/PLAN.md`. Never jump ahead.

## The challenge in one paragraph
A cloud-hosted platform where students submit course feedback (`POST /avaliacao`
with `descricao` + `nota` 0–10). Critical feedback triggers an automatic email to
admins. A weekly report (average score, count per day, count per urgency) is
generated automatically. It **must** be serverless, run in the cloud, have **at least
two serverless functions with Single Responsibility**, automated deploy, monitoring,
security configuration and access governance. Deliverables: public repo + demo video (the group records the video itself; Claude doesn't handle it).
Full brief: `docs/BRIEF.md`.

## Fixed decisions (see `docs/adr/DECISIONS.md` for justifications — do not change without a new ADR)
| Area | Choice |
|---|---|
| Cloud | AWS (single region, `us-east-1` by default) |
| Language | Java 21, plain `RequestHandler` classes (no Spring/Quarkus) |
| IaC + deploy | AWS SAM (`infra/template.yaml`) |
| CI/CD | GitHub Actions → `sam build && sam deploy` via OIDC role |
| API | API Gateway REST API, API-key + usage plan |
| Data | DynamoDB (on-demand, KMS encryption, Streams enabled) |
| Notifications | SNS topic `critical-feedback` with email subscriptions |
| Schedule | EventBridge Scheduler, weekly cron |
| Reports storage | S3 bucket (private, encrypted, versioned) |
| Observability | Powertools for AWS Lambda (Java): structured logs, metrics; CloudWatch alarms + dashboard |

## Architecture (Single Responsibility per function)
```
Student ──POST /avaliacao──► API Gateway ──► [fn] submit-feedback ──► DynamoDB (Feedbacks)
                                                                           │ Stream (INSERT, urgency=CRITICAL)
                                                                           ▼
                                                              [fn] notify-critical-feedback ──► SNS critical-feedback ──► admin email
EventBridge Scheduler (weekly) ──► [fn] generate-weekly-report ──► reads DynamoDB (GSI by day)
                                                         ├─► S3 reports/YYYY-Www.json
                                                         └─► SNS weekly-report ──► admin email
CloudWatch: logs, metrics, alarms ──► SNS ops-alerts
```
- `submit-feedback`: validate input, classify urgency, persist. Nothing else.
- `notify-critical-feedback`: format + publish the urgent notice. Nothing else.
- `generate-weekly-report`: aggregate last 7 days, persist report, publish summary. Nothing else.

## Domain rules
- **External API contract (fixed by the brief):**
  `POST /avaliacao` with body `{"descricao": string, "nota": int}`. The request DTO maps
  `descricao → description` and `nota → score` with `@JsonProperty`. Everything behind it is English.
- `score` integer 0–10, `description` non-blank, max 1000 chars → else HTTP 400.
- Urgency: `0–3 → CRITICAL`, `4–6 → MEDIUM`, `7–10 → LOW` (assumption, ADR-007).
- `Feedback` fields: `id`, `description`, `score`, `urgency`, `submittedAt`, `day`.
- Critical-feedback email contains: description, urgency, submission date.
- Weekly report contains: each feedback's description/urgency/submission date, average score,
  count per day, count per urgency.
- **Email and report text is in pt-BR** (the admins and evaluators are Brazilian). Labels:
  `Descrição`, `Urgência`, `Data de envio`, `Média das avaliações`, `Avaliações por dia`,
  `Avaliações por urgência`. Urgency is shown as `Crítica` / `Média` / `Baixa`, while the enum
  and stored values stay `CRITICAL` / `MEDIUM` / `LOW`. Example subject:
  `[Urgente] Novo feedback crítico recebido`, `Relatório semanal de feedbacks — 2026-W47`.
  Keep these strings in one class (`shared/…/usecases/messages/PtBrMessages`) so translations
  live in one place.
- All timestamps stored in UTC ISO-8601; dates in emails/reports rendered in `America/Sao_Paulo`.
- Response of `POST /avaliacao`: `201 {"id", "urgency", "submittedAt"}`; `400 {"errors": [...]}`.

## Repo layout
```
shared/                 entities + use cases + gateway interfaces (plain Java, unit-tested)
functions/submit-feedback/
functions/notify-critical-feedback/
functions/generate-weekly-report/
infra/template.yaml     SAM template (all AWS resources)
infra/samconfig.toml
.github/workflows/      deploy.yml (test + deploy on push to main)
scripts/                smoke-test.sh, seed-feedbacks.sh, invoke-report.sh
docs/                   PLAN, PROGRESS, BRIEF, ADRs, README template
```

## Code style (mirrors github.com/vitorbetmann/clean-resmapi)
Clean Architecture, all in English. Base package `com.vitorbetmann.schoolapi` (same convention as `com.vitorbetmann.cleanresmapi`). Project and repo name: `schoolapi`; the `Project` tag value is `schoolapi`.
```
shared/        …/entities/          Feedback, Urgency, WeeklyReport (records/enums, invariants in factories)
               …/usecases/          SubmitFeedback, NotifyCriticalFeedback, GenerateWeeklyReport
                                    (one public `execute(...)` method each, constructor injection)
               …/usecases/interfaces/  FeedbackGateway, NotificationGateway, ReportStorageGateway
               …/usecases/exceptions/  InvalidFeedbackException, …
functions/<fn> …/infrastructure/    <Fn>Handler (AWS adapter only), Dynamo/Sns/S3 *GatewayImpl,
                                    dtos/ (request/response records with fromDomain()/toDomain())
```
- A handler does: parse event → call one use case → map result/exception to the response. No logic.
- Wiring is manual in a static initializer (no DI container). This keeps cold starts low.
- Use `var` for obvious local types and `this.` for fields, and keep exception messages specific
  (`"Feedback score out of range: " + score`).
- Self-explanatory names over comments. Javadoc only on public use cases and gateways.
- Tests mirror packages: unit tests for entities/use cases with fake gateways (JUnit 5 + Mockito + AssertJ),
  plus a handler test per function using sample events from `events/`.

## Working rules
1. At the start of a session read `docs/PROGRESS.md` to find the current step.
2. Implement only the current step. Finish its **acceptance criteria** before marking done.
3. After each step: run `mvn -q verify` (and `sam validate --lint` once infra exists),
   tick the step in `docs/PROGRESS.md`, add a short "what changed" note, suggest a commit
   message (Conventional Commits). Don't commit unless asked.
4. Business logic lives in `shared/` or in plain classes, not in handlers → unit-testable
   without AWS. Handlers only adapt events ↔ services.
5. Every resource in the SAM template gets: least-privilege policy, `Project` tag,
   log retention (14 days), encryption where available.
6. Never hard-code account IDs, emails, ARNs or secrets. Use SAM parameters / env vars.
7. **Never run `sam deploy`, `aws ... delete`, or anything that spends cloud credits
   without explicit confirmation.** Credits are limited.
8. **Language policy (ADR-011):** all code is in English: identifiers, log messages, metric
   names, DynamoDB attributes, SAM resource names, commit messages and all `docs/`.
   Two exceptions: the external API contract above (defined by the brief), and the text of the
   emails and the weekly report, which is pt-BR (`PtBrMessages`).
9. **README is bilingual in one file**, like the reference repo: title, then two anchor links
   (`🇨🇦 English Description` / `🇧🇷 Descrição em Português`), the full English section, `---`,
   then a full Portuguese mirror with **the same headings in the same order** (translated).
   Whenever you change one half, update the other half in the same edit. Code blocks,
   commands and identifiers stay identical in both halves.
10. **Build only what the assignment requires** (`docs/BRIEF.md`). Don't add extras, even good
    practices, unless a requirement or rubric item needs them (no Postman collections, coverage
    reports, CI test workflows, tracing, WAF, extra docs, etc.). If you think something
    unrequired is worth adding, ask first.

## Commands
- `mvn -q verify` — build + unit tests
- `sam validate --lint -t infra/template.yaml`
- `sam build -t infra/template.yaml`
- `sam local invoke SubmitFeedbackFunction -e events/post-avaliacao.json`
- `sam deploy --config-file infra/samconfig.toml` (**ask first**)

## Slash commands in this project
- `/next-step` — implement the next unchecked step from the plan
- `/step <n>` — show or implement a specific step
- `/review-step` — check the current step against its acceptance criteria + rubric
- `/status` — summarize progress and what's left for delivery
