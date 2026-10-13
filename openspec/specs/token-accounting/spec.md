# token-accounting Specification

## Purpose
Apresentar uma contagem de tokens auditável para a atividade, distinguindo entrada, saída, cache e lacunas dos eventos informados pela CLI.

## Requirements

### Requirement: Total reportado pela CLI
O aplicativo MUST mostrar o total informado pela CLI para a execução e decompor esse total em parcelas exclusivas: entrada sem cache, cache, saída e eventual parcela sem detalhamento. A entrada total informada MUST ser preservada como soma das duas parcelas de entrada.

#### Scenario: Total coerente
- **WHEN** a CLI informa 1.200 tokens de entrada, 300 de cache, 450 de saída e 1.650 no total
- **THEN** o painel apresenta 1.650 tokens e a conta 900 de entrada sem cache + 300 de cache + 450 de saída = 1.650

#### Scenario: Parcela sem detalhamento
- **WHEN** o total informado supera a soma de entrada e saída
- **THEN** a diferença é exibida como sem detalhamento sem alterar o total informado

#### Scenario: Entrada majoritariamente em cache
- **WHEN** a CLI informa 4.709.555 de entrada, 4.635.904 de cache, 15.200 de saída e 4.724.755 no total
- **THEN** a decomposição mostra 73.651 de entrada sem cache + 4.635.904 de cache + 15.200 de saída = 4.724.755, sem acrescentar novamente a entrada total

### Requirement: Cache e raciocínio não duplicados
O aplicativo MUST tratar cache como parte da entrada e raciocínio informado como parte da saída, sem somá-los novamente ao total.

#### Scenario: Entrada com cache
- **WHEN** a CLI informa entrada e cache
- **THEN** o cache é subtraído da entrada total para obter a entrada sem cache, e essas duas parcelas de entrada podem ser somadas sem duplicação

#### Scenario: Saída com raciocínio
- **WHEN** a CLI informa saída e raciocínio
- **THEN** o raciocínio permanece dentro da saída e não aparece como parcela adicional da soma do total

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

#### Scenario: Leitura no painel principal
- **WHEN** o usuário visualiza o total de tokens da CLI
- **THEN** o painel indica que essa contagem não é valor cobrado nem saldo da assinatura

### Requirement: Separação entre operação e cota da conta
O aplicativo MUST distinguir tokens reportados para a operação de percentuais de cota da conta e MUST descrever a variação como movimento observado na conta.

#### Scenario: Leituras comparáveis
- **WHEN** leituras antes e depois do Codex têm o mesmo limite, duração e instante de renovação
- **THEN** o painel mostra os dois percentuais e a diferença em pontos percentuais, avisando que outras atividades da conta podem contribuir

#### Scenario: Janela renovada ou leitura ausente
- **WHEN** a janela muda ou uma leitura falha
- **THEN** a variação aparece como indisponível, sem subtrair percentuais incompatíveis
