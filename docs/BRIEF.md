# Tech Challenge — Fase 4 (resumo do enunciado)

**Tema:** Cloud Computing, Serverless e Deploy em nuvem. Plataforma de feedback de aulas:
estudantes avaliam, administradores recebem notificações de itens críticos e relatórios.

## Requisitos
- Ambiente de nuvem configurado, com segurança para dados de clientes e governança de acesso.
- Componentes de suporte configurados (banco de dados etc.).
- Deploy automatizado dos componentes atualizáveis (ex.: funções).
- Aplicação monitorada.
- Notificações automáticas aos administradores para problemas críticos.
- Relatório semanal dos feedbacks, com média de avaliações.

## Regras
- Obrigatório serverless e cloud.
- Mínimo de 2 funções serverless, respeitando Responsabilidade Única
  (a separação correta de serviços/responsabilidades é avaliada).

## Entregáveis
- Repositório aberto com o código-fonte.
- Vídeo demonstrando a aplicação funcionando, funções serverless ativas e configurações.

## Avaliação
- Explicação do modelo de cloud escolhido e componentes.
- Funcionamento correto.
- Qualidade do código, com documentação.
- Descrição do projeto: arquitetura, instruções de deploy, configuração do monitoramento,
  documentação das funções.
- Configuração do ambiente de nuvem e funções serverless, com explicação do modelo e da segurança.

## Contratos
`POST /avaliacao`
```json
{ "descricao": "string", "nota": 0 }
```
`nota`: inteiro de 0 a 10.

**E-mail de urgência:** descrição, urgência, data de envio.

**Relatório semanal:** descrição, urgência, data de envio, quantidade de avaliações por dia,
quantidade de avaliações por urgência (e média das avaliações, pelos requisitos).
