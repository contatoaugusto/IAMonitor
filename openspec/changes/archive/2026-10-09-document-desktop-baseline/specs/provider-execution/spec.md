# Spec Delta

## Purpose

Iniciar uma execução nova da CLI escolhida na pasta do projeto e interpretar seus eventos sem acessar credenciais diretamente nem interceptar sessões já abertas.

## ADDED Requirements

### Requirement: Seleção da IA antes da execução
O aplicativo MUST permitir escolher Codex, Claude Code, Gemini CLI, Cursor Agent ou GitHub Copilot CLI antes de iniciar uma sessão.

#### Scenario: Escolha da ferramenta
- **WHEN** o usuário escolhe uma IA no formulário de nova sessão
- **THEN** o formulário apresenta instruções de preparo e limites de medição específicos dessa CLI

### Requirement: Execução pela CLI autenticada
O aplicativo MUST iniciar uma nova execução da CLI na pasta escolhida e usar a autenticação que a própria CLI já mantém no ambiente do usuário.

#### Scenario: Pedido enviado
- **WHEN** o usuário inicia uma sessão com a CLI instalada e autenticada
- **THEN** o pedido é enviado ao novo processo da CLI com a pasta do projeto como diretório de trabalho

### Requirement: Proveniência dos eventos
O aplicativo MUST obter operações e uso de tokens apenas dos eventos emitidos pelo processo que iniciou e MUST informar quando a CLI não fornece contagem.

#### Scenario: CLI sem uso estruturado
- **WHEN** os eventos de uma execução não contêm contagem de tokens
- **THEN** o painel informa indisponibilidade em vez de apresentar zero como medição
