# SchoolAPI — Serverless Course Feedback on AWS

- [🇨🇦 English Description](#english-description)

- [🇧🇷 Descrição em Português](#descrição-em-português)

---

## 🇨🇦English Description

Serverless platform built for the **Tech Challenge — Phase 04** of the FIAP postgraduate program (Pós Tech,
Arquitetura e Desenvolvimento Java). Students rate classes through `POST /avaliacao`, administrators are emailed
automatically about critical feedback, and a weekly report with the average score is generated on a schedule.

> 🚧 Work in progress. The remaining sections are written in **Step 10** of `docs/PLAN.md`.

## Demo video

_Link to be added by the group._

## Security and access governance

Covers the requirement *"segurança relacionada aos dados de clientes e governança de acesso"*. Every control is in
`infra/template.yaml` (app stack) or `infra/bootstrap/access-governance.yaml` (access stack), except the ones marked
**console**.

**Customer data protection**

| Control | Where | Requirement it meets |
|---|---|---|
| Customer-managed KMS key `alias/schoolapi`, yearly rotation | `FeedbackKey` | Encryption at rest under our own key |
| Feedbacks table, logs, both SNS topics and the reports bucket encrypted with that key | `FeedbacksTable`, `*LogGroup`, `*Topic`, `ReportsBucket` | Student data encrypted at rest everywhere it is stored |
| TLS only: requests without HTTPS are denied | `ReportsBucketPolicy`, `*TopicTlsOnlyPolicy`, `AuditTrailBucketPolicy` | Encryption in transit |
| Point-in-time recovery (35 days) | `FeedbacksTable` | Data can be restored after a mistake |
| Private reports bucket: Block Public Access, owner-enforced, versioned | `ReportsBucket` | Reports never public |
| API key + usage plan (10 rps, burst 20, 5000/day) + JSON-schema validation at the gateway | `FeedbackApi` | Limits abuse; bad input never reaches the function |
| Least-privilege role per function: `PutItem` only (submit), `Query` on the `by-day` index only (report), `Publish` on its own topic, KMS use only through DynamoDB/SNS/S3 (`kms:ViaService`) | function `Policies` | Each function can only do its one job |
| Student text never logged (only id, score, urgency); access logs have no body | handlers, `AccessLogSetting` | Customer data stays out of logs |
| Logs kept 14 days | `*LogGroup` | Data minimization |

**Access governance**

| Control | Where | Requirement it meets |
|---|---|---|
| `FeedbackAdmins`: deploy and operate the stack, read data | access stack (Identity Center) | Separation of duties |
| `FeedbackDevelopers`: stack config, function config, logs and metrics; no data, no deploy (deploys go through CI) | access stack (Identity Center) | Separation of duties |
| `FeedbackReadOnly`: metrics, alarms, dashboards and stack status only | access stack (Identity Center) | Separation of duties |
| MFA required at every sign-in (Identity Center → Settings → Authentication) | **console** | Strong authentication |
| No root access keys, root MFA on; people sign in through Identity Center only | **console** | Strong authentication |
| CloudTrail trail `schoolapi-audit`: management events, all regions, log file validation, retained private bucket | `AuditTrail`, `AuditTrailBucket` | Audit of who did what |
| Tags `Project`, `Owner`, `Env` on every resource | `infra/samconfig.toml` (stack tags) | Ownership and cost tracking |

Deploy the access stack once, as an administrator:

```bash
aws sso-admin list-instances --query 'Instances[0].[InstanceArn,IdentityStoreId]' --output text
aws cloudformation deploy --template-file infra/bootstrap/access-governance.yaml --stack-name schoolapi-access-governance --parameter-overrides InstanceArn=<instance-arn> IdentityStoreId=<identity-store-id> --tags Project=schoolapi Owner=vitorbetmann Env=prod
```

Then add people to the groups (console, or `aws identitystore create-group-membership`).

> SAM attaches the AWS-managed `AWSLambdaBasicExecutionRole` (and `AWSLambdaDynamoDBExecutionRole` for the stream
> trigger), whose resources are `*`; every statement written in the template is resource-scoped. In AWS Academy
> Learner Lab, IAM roles, Identity Center and CloudTrail can't be created: with `LambdaRoleArn` set, every function
> uses `LabRole` and the trail is skipped. The generated roles above are the intended production setup (ADR-008, ADR-016).

---

## 🇧🇷Descrição em Português

Plataforma serverless desenvolvida para o **Tech Challenge — Fase 04** da pós-graduação FIAP (Pós Tech,
Arquitetura e Desenvolvimento Java). Estudantes avaliam aulas via `POST /avaliacao`, administradores recebem
e-mails automáticos sobre feedbacks críticos e um relatório semanal com a média das avaliações é gerado de forma
agendada.

> 🚧 Em construção. As demais seções serão escritas no **Step 10** do `docs/PLAN.md`.

## Vídeo de demonstração

_Link a ser adicionado pelo grupo._

## Segurança e governança de acesso

Atende ao requisito *"segurança relacionada aos dados de clientes e governança de acesso"*. Todo controle está em
`infra/template.yaml` (stack da aplicação) ou `infra/bootstrap/access-governance.yaml` (stack de acesso), exceto os
marcados como **console**.

**Proteção dos dados dos clientes**

| Controle | Onde | Requisito atendido |
|---|---|---|
| Chave KMS gerenciada pelo cliente `alias/schoolapi`, rotação anual | `FeedbackKey` | Criptografia em repouso com chave própria |
| Tabela de feedbacks, logs, os dois tópicos SNS e o bucket de relatórios criptografados com essa chave | `FeedbacksTable`, `*LogGroup`, `*Topic`, `ReportsBucket` | Dados dos estudantes criptografados em todo lugar onde são armazenados |
| Somente TLS: requisições sem HTTPS são negadas | `ReportsBucketPolicy`, `*TopicTlsOnlyPolicy`, `AuditTrailBucketPolicy` | Criptografia em trânsito |
| Recuperação point-in-time (35 dias) | `FeedbacksTable` | Dados podem ser restaurados após um erro |
| Bucket de relatórios privado: Block Public Access, owner-enforced, versionado | `ReportsBucket` | Relatórios nunca públicos |
| API key + usage plan (10 rps, burst 20, 5000/dia) + validação JSON-schema no gateway | `FeedbackApi` | Limita abuso; entrada inválida nunca chega à função |
| Role de menor privilégio por função: só `PutItem` (submit), só `Query` no índice `by-day` (relatório), `Publish` só no próprio tópico, uso do KMS só via DynamoDB/SNS/S3 (`kms:ViaService`) | `Policies` de cada função | Cada função só consegue fazer o seu único trabalho |
| Texto dos estudantes nunca vai para os logs (só id, nota, urgência); access logs sem body | handlers, `AccessLogSetting` | Dados dos clientes fora dos logs |
| Logs mantidos por 14 dias | `*LogGroup` | Minimização de dados |

**Governança de acesso**

| Controle | Onde | Requisito atendido |
|---|---|---|
| `FeedbackAdmins`: faz deploy e opera a stack, lê os dados | stack de acesso (Identity Center) | Segregação de funções |
| `FeedbackDevelopers`: configuração da stack e das funções, logs e métricas; sem dados, sem deploy (deploy só via CI) | stack de acesso (Identity Center) | Segregação de funções |
| `FeedbackReadOnly`: só métricas, alarmes, dashboards e status da stack | stack de acesso (Identity Center) | Segregação de funções |
| MFA exigido em todo login (Identity Center → Settings → Authentication) | **console** | Autenticação forte |
| Sem access keys de root, MFA no root; pessoas entram só pelo Identity Center | **console** | Autenticação forte |
| Trail do CloudTrail `schoolapi-audit`: eventos de gerenciamento, todas as regiões, validação dos arquivos de log, bucket privado retido | `AuditTrail`, `AuditTrailBucket` | Auditoria de quem fez o quê |
| Tags `Project`, `Owner`, `Env` em todos os recursos | `infra/samconfig.toml` (tags da stack) | Responsável e rastreio de custos |

Faça o deploy da stack de acesso uma vez, como administrador:

```bash
aws sso-admin list-instances --query 'Instances[0].[InstanceArn,IdentityStoreId]' --output text
aws cloudformation deploy --template-file infra/bootstrap/access-governance.yaml --stack-name schoolapi-access-governance --parameter-overrides InstanceArn=<instance-arn> IdentityStoreId=<identity-store-id> --tags Project=schoolapi Owner=vitorbetmann Env=prod
```

Depois adicione as pessoas aos grupos (console, ou `aws identitystore create-group-membership`).

> O SAM anexa as policies gerenciadas pela AWS `AWSLambdaBasicExecutionRole` (e `AWSLambdaDynamoDBExecutionRole`
> para o gatilho do stream), cujos recursos são `*`; toda statement escrita no template tem recurso específico. No AWS
> Academy Learner Lab não é possível criar roles IAM, Identity Center nem CloudTrail: com `LambdaRoleArn` definido,
> todas as funções usam a `LabRole` e o trail é omitido. As roles geradas acima são a configuração pensada para
> produção (ADR-008, ADR-016).
