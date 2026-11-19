# Progress

Claude: read this first every session. Tick a step only when all its acceptance criteria pass.
Add a one-line note per step (date, what changed, anything left over).

- [x] Step 0 — Accounts and local tools
- [x] Step 1 — Repo scaffold
- [x] Step 2 — Domain (shared), test-first
- [x] Step 3 — Function 1: submit-feedback
- [ ] Step 4 — Infrastructure base + first deploy
- [ ] Step 5 — Function 2: notify-critical-feedback
- [ ] Step 6 — Function 3: generate-weekly-report
- [ ] Step 7 — Security and access governance
- [ ] Step 8 — Monitoring
- [ ] Step 9 — Automated deploy (CD)
- [ ] Step 10 — README (English + Portuguese)

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
