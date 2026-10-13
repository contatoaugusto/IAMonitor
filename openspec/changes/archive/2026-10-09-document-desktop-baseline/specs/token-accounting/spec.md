# Spec Delta

## Purpose

Apresentar uma contagem de tokens auditável para a atividade, distinguindo entrada, saída, cache e lacunas dos eventos informados pela CLI.

## ADDED Requirements

### Requirement: Total reportado pela CLI
O aplicativo MUST mostrar o total informado pela CLI para a execução e decompor esse total em entrada, saída e eventual parcela sem detalhamento.

#### Scenario: Total coerente
- **WHEN** a CLI informa 1.200 tokens de entrada, 450 de saída e 1.650 no total
- **THEN** o painel apresenta 1.650 tokens e a conta 1.200 + 450 = 1.650

#### Scenario: Parcela sem detalhamento
- **WHEN** o total informado supera a soma de entrada e saída
- **THEN** a diferença é exibida como sem detalhamento sem alterar o total informado

### Requirement: Cache e raciocínio não duplicados
O aplicativo MUST tratar cache como parte da entrada e raciocínio informado como parte da saída, sem somá-los novamente ao total.

#### Scenario: Entrada com cache
- **WHEN** a CLI informa entrada e cache
- **THEN** o cache é identificado como subconjunto da entrada e não acrescenta tokens ao total

### Requirement: Estado da contagem
O aplicativo MUST distinguir contagem final, parcial, incompleta, indisponível e divergente conforme os eventos e o estado da execução.

#### Scenario: Resultado sem uso
- **WHEN** pelo menos um resultado da execução não informa uso
- **THEN** o total conhecido é apresentado como mínimo incompleto

#### Scenario: Dados incompatíveis
- **WHEN** as parcelas informadas não podem compor um total coerente
- **THEN** o painel não apresenta um número final confiável

### Requirement: Limite da medição
O aplicativo MUST explicar que tokens reportados pela CLI não equivalem automaticamente a saldo, cobrança ou consumo de assinatura.

#### Scenario: Consulta do total
- **WHEN** o usuário consulta como os tokens são medidos
- **THEN** a interface distingue contagem de tokens de cobrança e mostra a origem dos dados
