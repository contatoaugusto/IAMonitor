# Proposal

## Why

O gráfico mostra Entrada e Cache como barras independentes, embora Cache já esteja incluído na Entrada. Isso sugere uma soma acima do total e confunde contagem observada com gasto da assinatura.

## What Changes

- A decomposição visual e a equação do total usarão parcelas que podem ser somadas uma única vez: Entrada sem cache, Cache, Saída e eventual diferença sem detalhamento.
- A Entrada total reportada continuará preservada como dado da CLI, mas não aparecerá como parcela adicional no gráfico.
- Raciocínio, quando reportado, permanecerá identificado como parte da Saída, sem barra somável própria.
- O painel indicará que tokens observados não são um valor cobrado nem um saldo de assinatura.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `token-accounting`: apresentar a conta do total com parcelas exclusivas e preservar a origem do valor reportado.
- `presentation-and-export`: usar a mesma conta no gráfico principal e nas superfícies que exibem a equação, sem sugerir custo financeiro.

## Impact

`TokenAccounting`, `WorkbenchCharts`, texto do painel e guia, testes de contabilidade e prévias, README e especificações. Sem mudança em eventos, armazenamento, consulta de cota ou preços.
