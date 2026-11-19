# Architecture Decision Records

Short format: **Decision · Why · Alternatives considered · Consequences.**
Add a new ADR instead of editing an accepted one.

---

## ADR-001 — Cloud provider: AWS
**Decision:** AWS, single region (`us-east-1`).
**Why:**
- Every requirement has a managed, serverless-native service that needs no glue: API Gateway
  (endpoint), Lambda (functions), DynamoDB (data), DynamoDB Streams (event trigger), SNS (email),
  EventBridge Scheduler (weekly cron), S3 (reports), CloudWatch (monitoring), KMS/IAM/CloudTrail
  (security and governance).
- **Credits:** the always-free tier covers this project's volume (1M Lambda requests/month,
  25 GB DynamoDB, 1,000 SNS emails/month), so running the project should cost cents. AWS Academy Learner Lab
  credits are also common in FIAP courses.
- Most course material, tutorials and examples for serverless Java target AWS, which means
  less time lost to tooling.
- `us-east-1` has every service and the lowest prices.
**Alternatives:** Azure Functions (good Java support, but email needs Communication Services
setup and Durable/Timer concepts add explanation overhead); GCP Cloud Functions (Java support
is weaker, and Cloud Scheduler + Pub/Sub + Firestore is equivalent but less common in the course).
**Consequences:** vendor lock-in to AWS event formats, which we contain by keeping logic out of handlers.

## ADR-002 — Language/runtime: Java 21, plain handlers (no framework)
**Decision:** Java 21 (`java21` managed runtime, `arm64`), handlers implement
`RequestHandler<I,O>` directly. Small hand-written services, AWS SDK v2, Powertools for Java.
**Why:**
- ADJT is a Java track, so evaluators expect Java.
- **Cold start:** a plain handler JAR is ~10–15 MB and starts in roughly 1–2 s. Spring adds
  several seconds.
- **Single Responsibility is easier to show:** each function is a few readable classes. No DI
  container hides the wiring, which helps in the code walkthrough.
- `arm64` (Graviton) is ~20% cheaper per GB-s with equal or better performance.
**Alternatives:** Quarkus (great native-image cold starts, but GraalVM builds are slow/fragile in
CI and add a concept to explain); Spring Cloud Function (familiar, but heavy cold starts and more
memory = more cost).
**Consequences:** manual wiring (constructor injection by hand); acceptable at this size.

## ADR-003 — IaC and deploy tool: AWS SAM
**Decision:** One SAM template defines the whole stack. CI runs `sam build && sam deploy`.
**Why:** SAM is CloudFormation with serverless shorthand. The whole environment (including
security config) is versioned code that evaluators can read. `sam local invoke` gives local
testing for free. Also native support for event filters, policy templates and API keys.
**Alternatives:** Terraform (multi-cloud, but more boilerplate for Lambda packaging and a state
backend to manage); CDK (powerful, but adds a second language/abstraction to explain); Serverless
Framework (now requires an account/licence for v4).
**Consequences:** CloudFormation limits/rollbacks; fine for this scope.

## ADR-004 — Database: DynamoDB (on-demand)
**Decision:** One table `Feedbacks`, PK `id`; GSI `by-day` (PK `dia`, SK `submittedAt`); Streams on.
**Why:** It's serverless (no instance running when idle, so no cost and no VPC). It's pay per
request, and encryption, PITR and IAM fine-grained access are built in. **Streams** let us trigger
notifications from the data change itself, which decouples the receiver from the notifier (SRP).
The GSI by day turns the weekly report into 7 cheap `Query` calls instead of a full `Scan`.
**Alternatives:** RDS/Aurora Serverless v2 (SQL is handy for reports but it needs a VPC, has a minimum
cost when idle, and Lambda in a VPC adds complexity); DocumentDB (not serverless).
**Consequences:** aggregations are done in code (fine for weekly volumes).

## ADR-005 — Function boundaries (Single Responsibility)
**Decision:** Three functions:
1. `submit-feedback` — validate + classify + persist.
2. `notify-critical-feedback` — notify admins of a critical feedback.
3. `generate-weekly-report` — aggregate the week, store and send the report.
**Why:** Each has one reason to change and one trigger type (HTTP, data stream, schedule). Each also
has its own IAM permissions: the receiver can't send email, and the notifier can't read the table. The
receiver never waits for the email, so a failing email provider can't break feedback intake.
**Alternative rejected:** the receiver calling SNS directly. That's simpler, but it mixes two
responsibilities, couples latency/failure, and needs broader permissions.


## ADR-006 — Notifications: SNS email subscriptions
**Decision:** SNS topics `critical-feedback`, `weekly-report`, `ops-alerts` with email subscriptions.
**Why:** It takes zero setup beyond confirming a link. SES needs domain/email verification and starts in
sandbox mode (and is often blocked in Learner Lab). SNS fans out to several admins and is free up to
1,000 emails/month. It's also KMS-encryptable.
**Alternatives:** SES, Slack/Chime webhook.
**Consequences:** plain-text emails with SNS footer. That's acceptable for the challenge.

## ADR-007 — Urgency classification
**Decision:** score (`nota`) `0–3 → CRITICAL`, `4–6 → MEDIUM`, `7–10 → LOW`. Only `CRITICAL` notifies.
**Why:** The brief requires an "urgência" field but doesn't define it. A score-based rule is
deterministic, explainable and testable. Thresholds are constants in `Urgency.fromScore`
(could become env vars).
**Alternatives:** keyword/sentiment analysis (Comprehend), rejected as unrequired cost.

## ADR-008 — AWS Academy Learner Lab compatibility
**Decision:** The template accepts an optional `LambdaRoleArn` parameter. When set, all functions use
that role (e.g. `LabRole`) instead of generated least-privilege roles.
**Why:** Learner Lab forbids creating IAM roles/OIDC providers. With this parameter the same code
deploys in both environments.
**Consequences:** In Learner Lab you lose per-function least privilege and GitHub OIDC. Say so in the
README and point to the generated policies in the template as the intended production setup.

## ADR-009 — API protection: API key + usage plan
**Decision:** `x-api-key` required, usage plan throttling, JSON-schema request validation at the gateway.
**Why:** The endpoint is public by nature (students). The API key + throttling limits abuse and cost.
Gateway validation rejects malformed input before Lambda is billed.
**Alternatives:** Cognito user pools (real student auth, which is the right production answer but out of scope
for the brief); IAM auth (not suitable for browsers); WAF (extra ~$5/month+).

## ADR-010 — CI/CD: GitHub Actions with OIDC
**Decision:** `deploy.yml` runs the tests and deploys `main` assuming a role through OIDC, with a
protected `prod` environment.
**Why:** No long-lived AWS keys stored in GitHub (security + governance). The repo is public anyway
(a deliverable), so Actions minutes are free.
**Alternatives:** CodePipeline (all-AWS, but costs and more setup); storing access keys as secrets (rejected:
key leakage risk).

## ADR-011 — Language policy and code style
**Decision:** All code and technical docs are in English. The README is bilingual in one file
(English first, then a full Portuguese mirror). Code follows the Clean Architecture layout of
github.com/vitorbetmann/clean-resmapi: `entities`, `usecases` (+ `interfaces` and `exceptions`)
and `infrastructure`.
**Why:** English code is the industry norm and matches the reference repo. A bilingual README
serves the FIAP evaluators (Portuguese) and a portfolio audience (English) without maintaining two
files that drift apart. The same layout maps cleanly to serverless: use cases are framework-free,
so the Lambda handler is just another "controller".
**Exceptions:** the external contract `POST /avaliacao` `{descricao, nota}` keeps the brief's names,
mapped to English with `@JsonProperty` at the DTO boundary. Email and weekly-report text is pt-BR
because the admins and evaluators read Portuguese; it lives in one class (`PtBrMessages`).
**Consequences:** every README change must touch both halves (enforced by CLAUDE.md rule 9 and
`/review-step`).

## ADR-012 — Scope: only what the assignment requires
**Decision:** Build only what `docs/BRIEF.md` requires or the rubric evaluates. Extras are left out
even when they are good practice: Postman collection, coverage reports, CI test workflow, X-Ray
tracing, SnapStart, WAF, S3 versioning/lifecycle, extra docs files and video tooling.
**Why:** The group's call. It keeps the scope small and the explanation focused on the graded items.
**Consequences:** anything new needs a requirement to point to, or the group's explicit OK.

## ADR-013 — Day attribute: `day`, São Paulo calendar date
**Decision:** The `by-day` GSI partition key is the attribute `day` (not `dia` as written in ADR-004).
Its value is the `America/Sao_Paulo` calendar date of `submittedAt` (`yyyy-MM-dd`); `submittedAt`
itself stays UTC ISO-8601.
**Why:** ADR-011 makes every DynamoDB attribute English. Using the São Paulo date means a feedback sent
at 22:30 in Brazil counts on that day in the weekly report, which is what the Brazilian admins expect.
**Consequences:** supersedes the attribute name in ADR-004; everything else in ADR-004 still holds.

## ADR-014 — DynamoDB feedback gateway lives in `shared`
**Decision:** `DynamoFeedbackGatewayImpl` and its item mapping (`FeedbackItem`) live in
`shared/…/infrastructure/dynamo`. The SNS and S3 gateways stay in their function modules.
**Why:** Two functions use the same table (`submit-feedback` writes, `generate-weekly-report` reads by day),
and a function module can't depend on another. One mapping keeps the attribute names in one place.
**Consequences:** `shared` depends on the DynamoDB Enhanced Client (with the lightweight URL-connection
HTTP client). Entities and use cases still have no AWS imports. Each function's IAM policy still limits
what it can do with the table (write-only vs read-only).

## ADR-015 — Build: Maven builds the Lambda JARs, SAM skips building
**Decision:** `mvn verify` builds each function's shaded JAR. In `infra/template.yaml` each function's
`CodeUri` points at that JAR and has `Metadata: SkipBuild: True`, so `sam build` only copies the
template and `sam deploy` uploads the JARs. The flow is `mvn -q verify` → `sam build` → `sam deploy`.
**Why:** SAM's Maven builder copies each function folder to a scratch directory, where the parent POM
(`../../pom.xml`) and the `shared` sibling module don't resolve. Building from the root with Maven keeps one
build (the same one the tests run), and the shaded JAR keeps the Log4j2 plugin cache that Powertools needs.
**Alternatives:** SAM's built-in Maven builder (breaks on the multi-module layout without installing
`shared` into `~/.m2` first); `BuildMethod: makefile` (an extra Makefile per function to explain).
**Consequences:** `sam build`/`sam deploy` need a prior `mvn package`/`verify`, so CI runs Maven first.

## ADR-016 — Access governance and audit: Identity Center permission sets + CloudTrail
**Decision:** The three access profiles (`FeedbackAdmins`, `FeedbackDevelopers`, `FeedbackReadOnly`) are
IAM Identity Center groups with permission sets assigned to the account, defined in a separate one-time template
`infra/bootstrap/access-governance.yaml` that an administrator deploys. MFA is enforced in the Identity Center
settings (console). The app stack (`infra/template.yaml`) adds one CloudTrail trail (management events,
multi-region, log file validation) writing to a private, TLS-only, SSE-S3 bucket that is retained if the stack is deleted.
**Why:** People sign in through Identity Center (no IAM users), so the profiles must live there to be used, and MFA
belongs at the identity provider. Keeping them out of the app stack means the CI deploy role (Step 9) never needs
`sso-admin`/`identitystore` rights. SSE-S3 for the trail avoids an extra key-policy statement and KMS request costs;
the first trail's management events are free.
**Alternatives:** IAM groups with an `aws:MultiFactorAuthPresent` condition in the app stack (they would stay empty
because nobody uses IAM users); encrypting the trail with the CMK (more key policy, KMS cost).
**Consequences:** Group membership is managed in the console or with `aws identitystore create-group-membership`.
Learner Lab can't create these (ADR-008), so the trail is conditional on `CreateRoles` and the bootstrap template is
skipped there. SAM still attaches the AWS-managed `AWSLambdaBasicExecutionRole` (and `AWSLambdaDynamoDBExecutionRole`
for the stream trigger), whose resources are `*`; every statement we write is resource-scoped.

## ADR-017 — CD roles: environment-scoped OIDC trust + CloudFormation execution role
**Decision:** `infra/bootstrap/github-oidc.yaml` (deployed once by an admin) creates the GitHub OIDC provider,
`github-deploy-schoolapi` and `cfn-exec-schoolapi`. The deploy role trusts only tokens whose subject is
`repo:<owner>/<repo>:environment:prod`; the `prod` GitHub environment allows only the `main` branch and needs a
reviewer's approval. The deploy role can only manage change sets on the `schoolapi` stack, upload to the SAM
artifact bucket and pass `cfn-exec-schoolapi` to CloudFormation; that execution role (PowerUser + IAM on
`role/schoolapi-*`) is what actually changes resources. Role ARNs live in GitHub repository variables
(`AWS_DEPLOY_ROLE_ARN`, `AWS_CFN_EXEC_ROLE_ARN`), not in the repo.
**Why:** A job that uses an environment gets an `environment:` subject instead of `ref:`, so trusting the
environment (whose rules enforce the branch and the approval) is the precise condition. Splitting the deploy and
execution roles means a leaked workflow token can't change resources outside CloudFormation. The bootstrap role names
don't start with `schoolapi-`, so neither the execution role nor `FeedbackAdmins` can edit them.
**Alternatives:** One deploy role with all resource permissions (simpler, broader); trusting `ref:refs/heads/main`
(doesn't match once the job uses an environment).
**Consequences:** The app stack is now associated with `cfn-exec-schoolapi`; later manual deploys keep using it.
Learner Lab can't create OIDC providers or roles (ADR-008), so there you deploy manually with lab credentials.
