## Why

O gráfico de uso atual desenha vários resultados dentro de uma única área, dificultando a leitura individual de cada evento. O gráfico de artefatos omite categorias sem arquivos identificados, o que impede comparar visualmente as possibilidades da sessão.

## What Changes

- Apresentar um cartão gráfico independente para cada ciclo/resultado de uso recente, com identificação, estado, entrada, cache, saída e total quando disponíveis.
- Manter no gráfico de artefatos todas as categorias suportadas, inclusive as não identificadas na sessão, com barra vazia e indicação de zero arquivos.
- Reconhecer arquivos de comandos na classificação local de artefatos e manter os mesmos tipos no PDF.

## Capacidades afetadas

- `presentation-and-export`: gráficos de uso individuais na bancada.
- `artifact-observation`: catálogo completo de categorias e classificação de comandos.
