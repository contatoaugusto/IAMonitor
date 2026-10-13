## ADDED Requirements

### Requirement: Explicação do processamento local e remoto
O aplicativo MUST explicar, em linguagem acessível, o papel do IAMonitor, da CLI local, das ferramentas e do modelo remoto, sem alegar envio automático de todos os arquivos.

#### Scenario: Antes de executar
- **WHEN** o usuário abre Nova sessão e pede a explicação do fluxo
- **THEN** vê a sequência de preparação local, interação remota e execução de ferramentas, com o limite de observação do IAMonitor

#### Scenario: Trilha de dados
- **WHEN** o usuário abre Dados e envio de uma sessão
- **THEN** a trilha distingue pedido entregue à CLI, evidência local, contexto possivelmente transmitido e corpo HTTPS indisponível

#### Scenario: Provedor diferente de Codex
- **WHEN** outra CLI é escolhida
- **THEN** a explicação não atribui comportamento exclusivo do Codex a esse provedor
