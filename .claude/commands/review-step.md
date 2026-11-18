---
description: Review the current/last step against acceptance criteria and the FIAP rubric
---
Review the most recently ticked (or in-progress) step from `docs/PROGRESS.md`:

1. Go through each acceptance criterion in `docs/PLAN.md`. Mark ✅ / ❌ with evidence: test output,
   file/line, or a command I should run.
2. Check the code against `CLAUDE.md` rules: SRP (handler only adapts), no hard-coded ARNs/emails,
   least-privilege policies, log retention, encryption, no student text in INFO logs, tests present.
   Language policy: no Portuguese identifiers or logs outside the `POST /avaliacao` DTO; email/report
   text is pt-BR and lives only in `PtBrMessages`.
   Scope: flag anything not required by `docs/BRIEF.md` (ADR-012) and suggest removing it.
   If `README.md` changed, both halves must have matching headings (`grep '^##' README.md`) and the
   same content.
3. Map what this step contributes to the rubric tags `[ARQ] [FUNC] [COD] [DOC] [SEC]` and point out
   anything an evaluator could challenge.
4. Give a short list of fixes, by priority. Don't apply them unless I say so.
