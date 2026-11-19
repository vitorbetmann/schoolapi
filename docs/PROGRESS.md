# Progress

Claude: read this first every session. Tick a step only when all its acceptance criteria pass.
Add a one-line note per step (date, what changed, anything left over).

- [x] Step 0 — Accounts and local tools
- [x] Step 1 — Repo scaffold
- [x] Step 2 — Domain (shared), test-first
- [x] Step 3 — Function 1: submit-feedback
- [x] Step 4 — Infrastructure base + first deploy
- [x] Step 5 — Function 2: notify-critical-feedback
- [x] Step 6 — Function 3: generate-weekly-report
- [x] Step 7 — Security and access governance
- [x] Step 8 — Monitoring
- [x] Step 9 — Automated deploy (CD)
- [x] Step 10 — README (English + Portuguese)

## Environment facts (fill in during Step 0)
- AWS account type: personal (IAM Identity Center user with `AdministratorAccess`, CLI profile `schoolapi`)
- Region: us-east-1
- Admin email(s) for notifications:
- GitHub repo: https://github.com/vitorbetmann/schoolapi (public)

## Notes
- 2026-11-18 · Step 0 (partial): local tools ready (JDK 21 via `JAVA_HOME`, Maven 3.10.1,
  AWS CLI 2.41.1, SAM CLI 1.172.1, Docker 29.8.0, Git 2.53.0). Group is one person.
  Pending: AWS account activation (card verification failing), root MFA, $5 budget,
  Identity Center user + `aws configure sso`. Must be done before Step 4. (Public GitHub repo: done.)
- 2026-11-18 · Step 0 (done): personal AWS account active (card verified after the bank cleared the
  US$1 authorization). Root MFA on, no root access keys. Budget `schoolapi-budget` $5/month with
  alerts at 50/80/100% actual. IAM Identity Center user with `AdministratorAccess`; CLI via
  `aws configure sso`, profile `schoolapi`, region us-east-1. `java -version` 21, `sam --version`,
  `aws sts get-caller-identity` (assumed SSO role, not root) all verified.
- 2026-11-18 · Step 1: modules `shared` + 3 function modules (English names) enabled, each
  function builds a shaded JAR; versions bumped (SDK 2.59.4, Powertools 2.11.0, JUnit 6.1.3…);
  shared test deps in the parent; `events/` with API Gateway POST, DynamoDB stream INSERT and
  Scheduler samples. `mvn -q verify` green. Open: ADR-004/PLAN say GSI key `dia`, but ADR-011
  says English attributes, so the sample event uses `day`. Needs a new ADR or PLAN fix before Step 4.
- 2026-11-18 · Step 2: `Urgency`, `Feedback.create` (collects all violations into
  `InvalidFeedbackException`), `ReportPeriod` (7 days before a reference date, ISO-week label),
  `WeeklyReport.from` (2-decimal average, empty when no feedback; per-day/per-urgency counts incl.
  zeros), 3 gateway interfaces, `PtBrMessages`, `JsonMapper`. 45 unit tests green. `day` is the
  São Paulo calendar date of `submittedAt` (UTC). "Missing body" violation is left to the Step 3 handler.
- 2026-11-18 · Step 3: `SubmitFeedback` use case; `SubmitFeedbackHandler` (201 / 400 problem+json with
  `errors` / 500 without details), request/response DTOs, `DynamoFeedbackGatewayImpl` + `FeedbackItem`
  in `shared` (ADR-014), attribute `day` (ADR-013, PLAN updated). Powertools logging via the functional
  `PowertoolsLogging.withLogging` (no AspectJ needed), JSON logs with API Gateway correlation id, no
  description in logs. 57 tests green. Left over: `sam local invoke` needs the SAM template → check in Step 4.
- 2026-11-19 · Step 4: `infra/template.yaml` (KMS key `alias/schoolapi`, `schoolapi-feedbacks` table with
  `by-day` GSI, PITR, stream `NEW_IMAGE`; REST API `prod` with API key, usage plan 10/20 rps + 5000/day,
  JSON-schema validation whose 400 uses our `{"errors": [...]}` shape, JSON access logs; `submit-feedback`
  function; KMS-encrypted log groups, 14 days), `infra/samconfig.toml`, `scripts/smoke-test.sh`, ADR-015
  (Maven builds JARs, SAM `SkipBuild`). Deployed to us-east-1; smoke test 201/400/201, no key → 403,
  items in DynamoDB, `sam local invoke` → 201. Changes vs PLAN: the function also needs `kms:Decrypt` on
  the key (scoped with `kms:ViaService` DynamoDB) because the table uses a customer-managed key;
  `AdminEmail` moves to Step 5 (unused params fail `--lint`; pass it with `--parameter-overrides`, never
  in `samconfig.toml`); an API Gateway account CloudWatch role was added (needed for access logs,
  region-wide setting). New API keys take a few minutes to start working.
- 2026-11-19 · Step 5: `NotifyCriticalFeedback` use case (publishes only for `CRITICAL`), pt-BR body in
  `PtBrMessages.criticalFeedbackMessage`, `FeedbackItem.fromAttributes` (stream image → domain, one mapping),
  `NotifyCriticalFeedbackHandler` (partial batch response by sequence number), `SnsNotificationGatewayImpl`.
  Template: `AdminEmail` parameter, KMS-encrypted topic `schoolapi-critical-feedback` + email subscription,
  SQS DLQ (SSE), stream trigger filtered on `INSERT` + `urgency = CRITICAL`, 3 retries, policy = SNS publish
  + KMS via DynamoDB/SNS (+ SAM's stream-read and DLQ send). Test helper `aws-lambda-java-tests` loads
  `events/`. 64 tests green. Deployed: score 8 → no invocation, scores 2 and 1 → `noticeSent=true`, email
  delivered after the subscription was confirmed. `AdminEmail` is passed once with `--parameter-overrides`
  (SAM reuses it on later deploys); it is not in the repo.
- 2026-11-19 · Step 6: `GenerateWeeklyReport` use case (7 × `findByDay` →
  `WeeklyReport.from` → save → publish, returns `GeneratedWeeklyReport`), `PtBrMessages.weeklyReportMessage`
  (period, comma average, totals, per-day/per-urgency counts, every feedback, S3 location).
  `GenerateWeeklyReportHandler` is a `RequestStreamHandler` (JSON via `JsonMapper`; no `referenceDate` → today in
  São Paulo), `S3ReportStorageGatewayImpl` (`reports/YYYY/Www.json`, English JSON via `WeeklyReportDocument`), a copy of
  `SnsNotificationGatewayImpl` (ADR-014 unchanged). Template: private bucket (SSE-KMS + bucket key, versioned,
  Block Public Access, owner-enforced, TLS-only policy), KMS topic `schoolapi-weekly-report` + email subscription,
  function + log group, `ScheduleV2` `cron(0 8 ? * MON *)` in `America/Sao_Paulo`. Changes vs PLAN: `s3:PutObject` on
  `reports/*` instead of `S3WritePolicy` (which also grants ACL/lifecycle writes); an explicit `WeeklyReportScheduleRole`
  because SAM skips creating the Scheduler role when `RoleArn` is an `!If`. Scripts `seed-feedbacks.sh` (30 items
  straight into DynamoDB, 3 critical → 3 critical emails) and `invoke-report.sh`. 74 tests green, lint clean.
  Deployed: schedule enabled (`America/Sao_Paulo`), bucket blocks public access; seed → 3 critical notices sent;
  invoke → `reports/2026/W46.json` (SSE-KMS, 30 items, average 6.67, per-day 5/4/6/3/5/4/3, per-urgency 3/10/17) and
  the pt-BR report email delivered after confirming the new subscription (first invoke ran before confirmation).
- 2026-11-19 · Step 7: least privilege tightened (submit = `dynamodb:PutItem` on the table, report = `dynamodb:Query` on
  `index/by-day`, replacing SAM's broader `DynamoDBWritePolicy`/`DynamoDBReadPolicy`; checked with samtranslator, every
  written statement is resource-scoped, only SAM's AWS-managed execution policies use `*`). `*TopicTlsOnlyPolicy` (both SNS
  topics deny publish without TLS). CloudTrail `schoolapi-audit` (management events, multi-region, log file validation)
  into a retained private SSE-S3 TLS-only bucket, conditional on `CreateRoles`. Stack tags `Project`/`Owner`/`Env` in
  `samconfig.toml`. New `infra/bootstrap/access-governance.yaml`: Identity Center groups + permission sets
  `FeedbackAdmins` (PowerUser + IAM on `role/schoolapi-*`), `FeedbackDevelopers` (stack/function config, logs, metrics;
  no data, no deploy), `FeedbackReadOnly` (metrics/dashboards/stack status), assigned to the account. MFA and group
  membership are documented console settings. ADR-016. README security section in both halves. 74 tests green,
  both templates lint clean. Deployed: SNS needs one topic per policy statement (first deploy rolled back; the retained audit bucket had to be emptied and deleted before redeploying), so there is one TLS-only policy per topic. Smoke test 201/400/201, critical notice sent, report generated with the Query-only role, trail logging, 4 permission sets (3 new + existing AdministratorAccess), Env/Owner/Project tags on resources. Open: `seed-feedbacks.sh` comment/urgency mismatch (separate fix).
- 2026-11-19 · Fix: `seed-feedbacks.sh` picks each comment from a list for the score's urgency (critical / medium / low),
  so a positive comment is never marked critical. Same scores, days and counts (3/10/17). Checked with a fake `aws`
  under macOS bash 3.2.
- 2026-11-19 · Step 8: Powertools Metrics without AspectJ (`MetricsFactory` instance injected into each handler, flushed
  in `finally`): `FeedbackReceived` (dimension `urgency`), `NotificationSent` (count actually sent), `ReportGenerated`;
  namespace `SchoolAPI` via `POWERTOOLS_METRICS_NAMESPACE`. Template: KMS-encrypted TLS-only `schoolapi-ops-alerts` topic
  (publish allowed only for `cloudwatch.amazonaws.com` from `alarm:schoolapi-*`) + email subscription, key policy
  statement letting CloudWatch use the CMK for that topic, 5 alarms (`Errors > 0` per function, API `5XXError`, DLQ
  messages; alarm + OK actions), dashboard `schoolapi-operations` (7 widgets) and `DashboardUrl` output. README Monitoring
  section in both halves. 77 tests green, lint clean. Deployed: smoke test and report OK, all metrics listed; forced
  failure (`referenceDate: invalid`) → `schoolapi-generate-weekly-report-errors` in ALARM within ~1 minute, SNS action
  succeeded. Learned: Powertools `addDimension` adds a separate dimension set (`[Service]`, `[urgency]`), so the
  dashboard queries `FeedbackReceived` by `urgency` alone. New alarms also email once on their first move to OK.
- 2026-11-19 · Step 9: `infra/bootstrap/github-oidc.yaml` (stack `schoolapi-github-oidc`): GitHub OIDC provider,
  `github-deploy-schoolapi` (change sets on `schoolapi`, SAM artifact uploads, `iam:PassRole` to the execution role only)
  and `cfn-exec-schoolapi` (PowerUser + IAM on `role/schoolapi-*`, used by CloudFormation), ADR-017.
  `.github/workflows/deploy.yml`: push to `main` / manual → `mvn -B verify` → `sam build` → `sam deploy --role-arn`, in
  the `prod` environment (required reviewer, `main` only), role ARNs in repository variables. README Deploy section
  (manual + automated + Learner Lab note) in both halves. Learned: this repo's OIDC subject uses GitHub's immutable format
  `repo:owner@id/repo@id:environment:prod`, so the first run was denied until the trust policy matched it. Accepted: a push
  changing the report log line (`durationMs`) deployed through the pipeline and the new line showed in CloudWatch. Also
  fixed: `generate-weekly-report` had no `log4j2.xml` since Step 6, so it logged plain text instead of Powertools JSON.
  No `scripts/deploy.sh` (personal account; the README covers the Learner Lab manual deploy).
- 2026-11-19 · Step 10: README complete in both halves, in the template's order: Cloud model (public cloud, serverless
  FaaS + managed services, why, shared responsibility, cost), Architecture (Mermaid diagram, components table, DynamoDB
  data model, Clean Architecture layout), Serverless functions (responsibility, trigger, input/output, permissions,
  metric, handler per function; pt-BR email example), Endpoint (request, 201/400/403/429/500, curl). Manual deploy is
  generic (clone + any admin credentials). Headings pair up and all 11 code blocks are identical in both halves. Javadoc
  added to `GeneratedWeeklyReport` (use cases and gateways already had it). Open: record the demo video and add its link
  (reset demo data the same day).
