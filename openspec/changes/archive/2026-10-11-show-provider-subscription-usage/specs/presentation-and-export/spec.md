## ADDED Requirements

### Requirement: Quadro de assinatura na sessão selecionada
O aplicativo MUST apresentar o provedor, o estado da execução, os tokens da operação e o estado da cota no painel principal.

#### Scenario: Provedor sem cota estruturada
- **WHEN** a sessão selecionada usa Claude, Gemini, Cursor ou Copilot e o fluxo não informa cota verificável
- **THEN** o quadro mostra a cota como indisponível e oferece acesso à consulta oficial, sem exibir saldo presumido

#### Scenario: Sem sessão selecionada
- **WHEN** nenhuma sessão está selecionada
- **THEN** o quadro solicita a seleção de uma sessão e não mostra leituras antigas
