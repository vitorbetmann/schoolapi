# README template (used in Step 10)

Modeled on github.com/vitorbetmann/clean-resmapi. One `README.md` with two complete halves.

## Rules
- The top has the title, then the two anchor links exactly like this (blank line between them):
  ```markdown
  - [🇨🇦 English Description](#english-description)

  - [🇧🇷 Descrição em Português](#descrição-em-português)
  ```
- Then `---`, then `## 🇨🇦English Description` with the full English text.
- Then `---`, then `## 🇧🇷Descrição em Português` with the full Portuguese text.
- Both halves use the **same headings in the same order and level**, translated. Code blocks,
  commands, JSON, identifiers, resource names and URLs are byte-identical in both halves.
- Mermaid diagrams: same diagram in both halves. Node labels in English are fine; translate them
  only if it helps.
- Portuguese is pt-BR, technical and direct. Keep AWS service names in English (DynamoDB, not "Dínamo").
- Check: `grep '^##' README.md` must list each heading twice (EN then PT) in matching order.

## Section order (EN / PT)
Only sections the assignment asks for. Don't add others (ADR-012).

| # | English | Português | Brief requirement it covers |
|---|---|---|---|
| 1 | English Description (what it is, which challenge) | Descrição em Português | — (intro) |
| 2 | Demo video | Vídeo de demonstração | Deliverable: video (the group adds the link) |
| 3 | Cloud model | Modelo de cloud | "Explicação do modelo de cloud escolhido" |
| 4 | Architecture (Mermaid diagram, components, data model) | Arquitetura | "Arquitetura da solução", "componentes envolvidos" |
| 5 | Serverless functions (responsibility, trigger, input/output, permissions) | Funções serverless | "Documentação das funções criadas" |
| 6 | Endpoint (`POST /avaliacao`, request/response examples) | Endpoint | Entry endpoint from the brief |
| 7 | Security and access governance | Segurança e governança de acesso | "Configurações de segurança", "governança de acesso" |
| 8 | Monitoring | Monitoramento | "Configuração do monitoramento" |
| 9 | Deploy (manual and automated) | Deploy (manual e automatizado) | "Instruções de deploy", "deploy automatizado" |
