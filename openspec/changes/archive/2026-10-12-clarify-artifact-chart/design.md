# Desenho técnico

## Context

Ver `proposal.md`. `WorkbenchMetrics` já fornece o catálogo completo e as estimativas por categoria. `WorkbenchCharts.Artifacts` desenha o painel atual; `Dashboard` possui o detalhe selecionado e `DetailsDialog` carrega texto fora da thread de eventos.

## Goals / Non-Goals

**Objetivos:** manter as oito categorias à vista no tamanho padrão da bancada, com fonte legível e valores explícitos; abrir o inventário exato de arquivos da categoria escolhida.

**Fora do escopo:** atribuir tokens reportados pela CLI a arquivos, alterar os totais da sessão, a classificação ou o PDF.

## Decisions

1. `WorkbenchCharts.Artifacts` usará uma lista gráfica em duas colunas, com altura estável por categoria e rolagem apenas se faltar espaço. Cada item terá nome completo, quantidade de arquivos, estimativa local e barra proporcional. A alternativa de ampliar a pintura atual foi descartada porque ela não preserva legibilidade quando o painel encolhe.
2. O gráfico emitirá a categoria selecionada para `Dashboard`, que abrirá `DetailsDialog` usando um retrato da sessão. O texto virá de `SourceDetails`, filtrado por igualdade da categoria de `Store.Artifact`; reutilizar a chave genérica de origem misturaria, por exemplo, Spec e OpenSpec.
3. O detalhe apresentará caminho, contagem e estimativa individual sem inferir que o arquivo foi enviado por inteiro ao modelo. A alternativa de somar estimativas ao total reportado pela CLI é incorreta e não será aplicada.

## Risks / Trade-offs

- Painéis menores podem exigir rolagem → tamanho padrão mostra o catálogo inteiro; rolagem mantém acesso em janelas estreitas.
- Categorias futuras podem ter nomes maiores → a lista mantém largura do viewport e permite ajuste visual sem truncar dados no detalhe.
