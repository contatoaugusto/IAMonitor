# Spec Delta

## Purpose

Explicar como cada ferramenta determina tokens e permitir comparar um texto digitado sem confundir simulação local com uso real de uma sessão.

## ADDED Requirements

### Requirement: Metodologia selecionável por ferramenta
O aplicativo MUST oferecer no guia de tokens uma aba que permita selecionar as cinco ferramentas e mostre o método, a dependência de modelo, os limites e uma fonte oficial de cada uma.

#### Scenario: Seleção de Claude
- **WHEN** o usuário seleciona Claude Code na aba
- **THEN** a tela explica que a contagem exata depende do modelo e indica o endpoint oficial de contagem da Anthropic

#### Scenario: Seleção de Cursor ou Copilot
- **WHEN** o usuário seleciona Cursor Agent ou GitHub Copilot CLI
- **THEN** a tela explica que o tokenizer depende do modelo subjacente e que não há um número universal da ferramenta

### Requirement: Simulação de texto isolado
O aplicativo MUST calcular e exibir uma linha para cada uma das cinco ferramentas a partir do texto digitado, com valor e método claramente identificados.

#### Scenario: Texto preenchido
- **WHEN** o usuário digita texto e aciona Calcular tokens
- **THEN** a tela mostra a contagem local `O200K_BASE` para OpenAI e aproximações identificadas para as demais ferramentas

#### Scenario: Texto vazio
- **WHEN** o usuário tenta calcular um texto vazio ou somente espaços
- **THEN** a tela pede que ele digite texto e não apresenta números como se fossem uma medição

### Requirement: Isolamento da simulação
O aplicativo MUST manter o texto e os resultados do simulador fora da medição da sessão e MUST realizar o cálculo sem enviar o texto pela rede.

#### Scenario: Calcular em uma sessão aberta
- **WHEN** o usuário calcula um texto no guia de uma sessão
- **THEN** o total reportado pela CLI e os dados armazenados da sessão permanecem inalterados
