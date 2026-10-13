# Spec Delta

## Purpose

Permitir explorar um cenário hipotético de economia ao retirar origens locais identificadas, mantendo a medição original da CLI como referência.

## ADDED Requirements

### Requirement: Alternância das origens
O aplicativo MUST permitir incluir ou desconsiderar Skills, Agents, Specs e Hooks na simulação da sessão.

#### Scenario: Origem desconsiderada
- **WHEN** o usuário desativa uma origem disponível
- **THEN** o cenário recalcula o total aproximado e mantém o total reportado inalterado

### Requirement: Limite do desconto
O aplicativo MUST descontar cada arquivo identificado uma única vez e MUST limitar o desconto à entrada sem cache.

#### Scenario: Estimativa superior à entrada elegível
- **WHEN** a estimativa dos arquivos retirados supera a entrada sem cache
- **THEN** o desconto simulado é limitado à entrada elegível

### Requirement: Natureza hipotética
O aplicativo MUST informar que a economia simulada não é medição de tokens realmente evitados nem previsão de cobrança.

#### Scenario: Consulta da simulação
- **WHEN** o usuário vê o total simulado
- **THEN** a interface o identifica como cenário aproximado baseado em arquivos locais
