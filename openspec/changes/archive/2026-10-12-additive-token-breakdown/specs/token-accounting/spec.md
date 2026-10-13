## MODIFIED Requirements

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

### Requirement: Limite da medição
O aplicativo MUST explicar que tokens reportados pela CLI não equivalem automaticamente a saldo, cobrança ou consumo de assinatura.

#### Scenario: Consulta do total
- **WHEN** o usuário consulta como os tokens são medidos
- **THEN** a interface distingue contagem de tokens de cobrança e mostra a origem dos dados

#### Scenario: Leitura no painel principal
- **WHEN** o usuário visualiza o total de tokens da CLI
- **THEN** o painel indica que essa contagem não é valor cobrado nem saldo da assinatura
