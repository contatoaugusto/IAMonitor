# Proposal

## Why

O cartão de cada ciclo ainda concentra entrada, cache e saída em uma barra empilhada. Essa apresentação dificulta perceber quanto cada medida representa do total, especialmente durante uma apresentação de sessões.

## What Changes

- Cada ciclo com uso reportado exibe uma barra horizontal independente para cada medida observada, com valor absoluto e percentual do total do ciclo.
- Cache aparece como parte da entrada; raciocínio, quando reportado, aparece como parte da saída. O painel deixa explícito que essas parcelas não são somadas de novo ao total.
- Ciclos pendentes ou sem contagem continuam sem barras preenchidas e sem atribuição de zero tokens.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `presentation-and-export`: detalhar a decomposição visual individual dentro de cada cartão de ciclo.

## Impact

`WorkbenchCharts` e sua prévia/teste de interface, texto do README e contrato de apresentação. Sem mudança na persistência, na contabilidade de tokens ou no PDF.
