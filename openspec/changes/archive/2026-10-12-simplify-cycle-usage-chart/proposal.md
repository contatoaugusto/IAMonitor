# Proposal

## Why

As linhas do gráfico de tokens ficaram pequenas e carregadas de percentuais e explicações, enquanto três cartões acima repetem as mesmas medidas e reduzem o espaço disponível. O usuário precisa localizar o valor de cada medida rapidamente.

## What Changes

- O gráfico de cada ciclo mostrará barras e números de tokens maiores, com apenas o nome e o valor de cada medida.
- Percentuais, horário, texto auxiliar e parcelas zeradas opcionais sairão do cartão. O estado sem contagem continuará explícito.
- Os três cartões redundantes de Entrada, Saída e Cache sairão do topo, mantendo o total reportado e a decomposição por ciclo.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `presentation-and-export`: simplificar e ampliar a leitura do consumo por ciclo na visão principal.

## Impact

Interface Swing em `Dashboard` e `WorkbenchCharts`, prévias e testes de interface, README e contrato de apresentação. Nenhuma alteração nas contagens persistidas ou no PDF.
