# Proposta: leitura dos artefatos identificados

## Why

O gráfico atual comprime oito categorias em barras estreitas, abrevia nomes e usa fontes pequenas. Isso dificulta ler a quantidade de arquivos, a estimativa local e quais arquivos compõem cada categoria.

## What Changes

- Exibir nome completo, contagem de arquivos e estimativa local de tokens em linhas legíveis para cada categoria, inclusive as categorias com zero.
- Permitir abrir, a partir de cada categoria, a lista exata de arquivos identificados nela e suas estimativas individuais.
- Diferenciar visualmente as estimativas de arquivos da contagem de tokens reportada pela CLI.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `artifact-observation`: tornar o gráfico da bancada legível e permitir inspecionar os arquivos de cada categoria.

## Impact

Interface Swing da bancada, detalhe de artefatos, testes de apresentação e README. Sem alteração no cálculo dos tokens reportados, na persistência ou no PDF.
