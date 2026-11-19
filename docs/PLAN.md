# Implementation plan — step by step

Each step is small enough for one Claude Code session. Do them in order.
**Rubric** maps each step to the challenge's evaluation criteria:
`[ARQ]` cloud model and components explained · `[FUNC]` app works · `[COD]` code quality and docs ·
`[DOC]` project description (architecture, deploy, monitoring, functions) · `[SEC]` cloud and security configuration.

---

## Step 0 — Accounts and local tools (manual, ~1h)
**Goal:** everyone in the group can build locally and one person can deploy.
- Install JDK 21, Maven 3.9+, Docker, AWS CLI v2, AWS SAM CLI, Git.
- AWS account: enable MFA on root, create an IAM Identity Center user (or IAM user) for
  each member. Never use root keys.
  - *AWS Academy Learner Lab?* See ADR-008: you can't create IAM roles there, so set the
    `LambdaRoleArn` parameter to the lab's `LabRole` and skip OIDC (deploy from a laptop).
- Create an **AWS Budget** ($5, alert at 50/80/100%), since the brief stresses limited credits.
- Create a public GitHub repo, push this project.

**Acceptance:** `java -version` = 21, `sam --version`, `aws sts get-caller-identity` all work.
Budget alert exists. Rubric: `[SEC]`

## Step 1 — Repo scaffold
**Goal:** Maven multi-module build is green with no logic yet.
- Parent `pom.xml` (already present): Java 21, JUnit 5, AssertJ, AWS Lambda core/events,
  Jackson, Powertools (logging/metrics), AWS SDK v2 BOM.
- Modules: `shared`, `functions/submit-feedback`, `functions/notify-critical-feedback`,
  `functions/generate-weekly-report`. Each function module builds a shaded JAR (maven-shade-plugin).
- `events/` folder with sample events (API Gateway POST, DynamoDB stream INSERT, Scheduler event).

**Acceptance:** `mvn -q verify` passes. Rubric: `[COD]`

## Step 2 — Domain (`shared/`), test-first
**Goal:** all business rules are pure Java and covered by unit tests.
Packages and naming follow the "Code style" section of `CLAUDE.md`.
- `entities/Feedback` record (`id`, `description`, `score`, `urgency`, `submittedAt`, `day`) with a
  `Feedback.create(description, score, clock)` factory that validates and classifies.
- `entities/Urgency` enum with `Urgency.fromScore(int)` (0–3 CRITICAL, 4–6 MEDIUM, 7–10 LOW).
- `usecases/exceptions/InvalidFeedbackException` carrying the list of violations
  (score range, description blank/too long, missing body).
- `entities/WeeklyReport` + `WeeklyReport.from(List<Feedback>, period)` (average to 2 decimals,
  count per day incl. days with zero, count per urgency incl. zero, list of items).
- `usecases/interfaces/`: `FeedbackGateway`, `NotificationGateway`, `ReportStorageGateway`.
- `usecases/messages/PtBrMessages`: all pt-BR email/report strings and urgency labels (see `CLAUDE.md`).
- `JsonMapper` util (Jackson + JavaTimeModule, ISO dates).

**Acceptance:** unit tests pass, including boundary tests for scores 0,3,4,6,7,10,-1,11.
Rubric: `[COD]` `[FUNC]`

## Step 3 — Function 1: `submit-feedback`
**Goal:** a handler that turns `POST /avaliacao` into a stored feedback.
- `SubmitFeedbackHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>`.
- Use case `SubmitFeedback.execute(description, score)` → `Feedback` (in `shared`).
- `infrastructure/dtos/SubmitFeedbackRequest` (`@JsonProperty("descricao") description`,
  `@JsonProperty("nota") score`) and `SubmitFeedbackResponse.fromDomain(feedback)`.
- `infrastructure/DynamoFeedbackGatewayImpl` (SDK v2 Enhanced Client), table name from env
  `TABLE_NAME`. Client created once (static), outside the handler.
- Responses: `201` with `{id, urgency, submittedAt}`; `400` RFC 7807 problem with the violations;
  `500` generic problem (no stack traces leaked).
- Powertools `@Logging` (with correlation id). Never log the description at INFO
  (it's student data), so log only id/score/urgency.
- Unit tests: use case with a fake gateway, handler with `events/post-avaliacao.json`.

**Acceptance:** tests pass; `sam local invoke` with `events/post-avaliacao.json` returns 201
(against DynamoDB Local or after Step 4). Rubric: `[FUNC]` `[COD]`

## Step 4 — Infrastructure base + first deploy
**Goal:** the API is live in AWS.
- `infra/template.yaml` (SAM):
  - `Globals`: runtime `java21`, arch `arm64`, memory 512, timeout 15, env `POWERTOOLS_SERVICE_NAME`.
  - `FeedbackKey` (KMS CMK) with alias, used by DynamoDB, SNS, S3.
  - `FeedbacksTable`: PK `id` (S); GSI `by-day` PK `day` (S, yyyy-MM-dd, ADR-013) SK `submittedAt` (S);
    on-demand; SSE-KMS; PITR on; Stream `NEW_IMAGE`.
  - `FeedbackApi` (`AWS::Serverless::Api`): stage `prod`, `ApiKeyRequired: true`, usage plan
    (rate 10 rps, burst 20, quota 5000/day), access logs to CloudWatch, request validator +
    JSON-schema model for the body.
  - `SubmitFeedbackFunction`: event `POST /avaliacao`, policy `DynamoDBWritePolicy` + `kms:Decrypt` on
    `FeedbackKey` via DynamoDB only (needed by the customer-managed key).
  - Explicit `AWS::Logs::LogGroup` per function, 14-day retention.
  - Parameters: `LambdaRoleArn` (optional, for Learner Lab), `Stage`. (`AdminEmail` is added in Step 5,
    where SNS uses it.)
  - Outputs: API URL, API key id, table name.
- `infra/samconfig.toml` with stack name `schoolapi`.
- `scripts/smoke-test.sh` (curl with `x-api-key`): one valid, one invalid, one critical.

**Acceptance:** `sam validate --lint` clean → **ask user** → `sam deploy --guided` →
smoke test returns 201/400; item visible in DynamoDB. Rubric: `[FUNC]` `[SEC]` `[ARQ]`

## Step 5 — Function 2: `notify-critical-feedback`
**Goal:** critical feedback emails admins within seconds.
- SNS topic `critical-feedback` (KMS-encrypted) + email subscription from `AdminEmail`
  (the admin must click the confirmation link).
- `NotifyCriticalFeedbackHandler implements RequestHandler<DynamodbEvent, StreamsEventResponse>`
  - Uses `ReportBatchItemFailures` (partial batch response).
  - Use case `NotifyCriticalFeedback.execute(feedback)` builds the pt-BR message from `PtBrMessages`
    (subject + body with Descrição, Urgência, Data de envio as `dd/MM/yyyy HH:mm` America/Sao_Paulo) and calls
    `NotificationGateway`. Pure and unit-tested.
  - `infrastructure/SnsNotificationGatewayImpl`. The handler maps stream `NewImage` → `Feedback`.
- Event source mapping on the table stream with a **filter** `eventName = INSERT` and
  `urgency = CRITICAL` → the function is only invoked (and billed) for critical items.
- On-failure destination: SQS DLQ `notify-critical-feedback-dlq`; `MaximumRetryAttempts: 3`.
- Policy: `SNSPublishMessagePolicy` on that topic + KMS use. Nothing else.

**Acceptance:** POST with `"nota": 2` → email arrives; `"nota": 8` → no invocation (check the
CloudWatch metric). Unit tests for the use case (including the pt-BR text) + handler. Rubric: `[FUNC]` `[ARQ]`

## Step 6 — Function 3: `generate-weekly-report`
**Goal:** weekly report generated automatically and on demand.
- S3 bucket `reports`: Block Public Access, SSE-KMS, `enforce TLS` bucket policy.
- SNS topic `weekly-report` + email subscription.
- `GenerateWeeklyReportHandler` (input: Scheduler event, or `{"referenceDate":"2026-11-16"}` for
  manual runs, so the report can be shown without waiting a week).
  - Use case `GenerateWeeklyReport.execute(referenceDate)`: `FeedbackGateway.findByDay(...)` for each of
    the last 7 days → `WeeklyReport.from(...)` → `ReportStorageGateway.save(...)` →
    `NotificationGateway.publish(...)`.
  - Infrastructure: `DynamoFeedbackGatewayImpl.findByDay` (GSI `by-day`, paginated),
    `S3ReportStorageGatewayImpl`, `ReportRenderer` (JSON stored + plain-text summary emailed, in pt-BR).
  - Write `reports/YYYY/Www.json` to S3; publish the summary to SNS with the S3 key.
- `AWS::Scheduler::Schedule`: `cron(0 8 ? * MON *)` with timezone `America/Sao_Paulo` (08:00 São Paulo = 11:00 UTC).
- Policy: `DynamoDBReadPolicy` (table + GSI), `S3WritePolicy` (bucket), SNS publish (topic).
- `scripts/invoke-report.sh` to trigger it manually.

**Acceptance:** seed ~30 feedbacks across 7 days (`scripts/seed-feedbacks.sh`), invoke → S3 object
and email contain average, per-day counts, per-urgency counts. Rubric: `[FUNC]` `[ARQ]`

## Step 7 — Security and access governance
**Goal:** cover the requirement "segurança relacionada aos dados de clientes e governança de acesso".
- Review every function role: least privilege, no `*` resources (`sam validate --lint`).
- Data protection: KMS CMK with key policy limited to account + function roles; TLS-only
  policies on S3/SNS; DynamoDB PITR; no student text in logs; API Gateway throttling.
- Access governance: IAM groups/permission sets → `FeedbackAdmins` (full stack),
  `FeedbackDevelopers` (deploy via CI only + read logs), `FeedbackReadOnly`. MFA required.
- CloudTrail (management events, 1 trail) + resource tags `Project`, `Owner`, `Env`.

**Acceptance:** each control exists in the template (or is a documented console setting) and is
listed in the README's security section with the requirement it meets. Rubric: `[SEC]` `[DOC]`

## Step 8 — Monitoring
**Goal:** cover the requirement "aplicação monitorada".
- Powertools Metrics: `FeedbackReceived` (dim `urgency`), `NotificationSent`, `ReportGenerated`.
- CloudWatch alarms → SNS `ops-alerts`: Lambda `Errors > 0` (each fn), API `5XXError`,
  DLQ `ApproximateNumberOfMessagesVisible > 0`.
- CloudWatch dashboard (in the template): invocations, errors, duration, API requests/errors,
  feedbacks by urgency, DLQ depth.

**Acceptance:** dashboard shows data after the smoke test; forcing an error (e.g. bad payload
to the stream fn via test event) triggers an alarm email. Rubric: `[FUNC]` `[DOC]`

## Step 9 — Automated deploy (CD)
**Goal:** cover the requirement "deploy automatizado dos componentes atualizáveis".
- `infra/bootstrap/github-oidc.yaml`: OIDC provider + `GitHubDeployRole` limited to
  `repo:<org>/<repo>:ref:refs/heads/main` (one-time manual deploy).
- `.github/workflows/deploy.yml`: on push to `main` → setup Java 21 → `mvn -B verify` →
  `sam build` → `sam deploy --no-confirm-changeset --no-fail-on-empty-changeset` using
  `aws-actions/configure-aws-credentials` (OIDC, no stored keys).
- GitHub environment `prod` with required reviewer (access governance).
- Learner Lab fallback: documented `scripts/deploy.sh` using temporary credentials.

**Acceptance:** change a log message → merge to `main` → pipeline deploys → new code visible in
the Lambda console. Rubric: `[SEC]` `[FUNC]` `[DOC]`

## Step 10 — README (English + Portuguese)
**Goal:** cover exactly what the rubric asks for in "Descrição do projeto" and the cloud explanation.
Everything lives in `README.md`, so there are no separate docs to keep in sync.
- Bilingual in one file (see `CLAUDE.md` rule 9 and `docs/README-TEMPLATE.md`), with only these sections:
  demo video link, cloud model, architecture (Mermaid diagram + components + data model),
  serverless functions, endpoint, security and access governance, monitoring, deploy instructions.
- Javadoc on public use cases and gateways (rubric: "qualidade do código, com documentação").

**Acceptance:** a person outside the group can deploy from either half of the README alone; both
halves have identical heading structure (`grep '^##' README.md` shows matching pairs).
Rubric: `[DOC]` `[COD]` `[ARQ]` `[SEC]`
