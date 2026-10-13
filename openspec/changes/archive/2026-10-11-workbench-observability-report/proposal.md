## Why

A tela atual mistura execução, simulação, assinatura, tabelas e números com peso semelhante. Isso dificulta acompanhar uma tarefa em vídeo e entender de onde veio cada medida. O PDF também apresenta principalmente texto e listas, sem uma leitura visual das fases e dos fluxos observados.

## What Changes

- Organizar a tela principal como bancada de observação: estado e operação atual, total reportado, decomposição, gráfico dos ciclos de uso e gráfico das estimativas por tipo de artefato.
- Mover controles secundários, simulação e tabelas extensas para uma área de exploração, preservando acesso ao registro completo.
- Produzir PDF paginado com resumo, gráficos vetoriais, fluxo lógico, ciclos medidos, artefatos e cronologia dos eventos.
- Identificar em cada visual a origem e a limitação da medida; a estimativa local de artefatos não representa consumo real atribuído pela CLI.

## Capacidades afetadas

- `presentation-and-export`: bancada principal e relatório analítico.
- `token-accounting`: apresentação gráfica coerente das parcelas e lacunas.
- `artifact-observation`: agrupamento das estimativas locais por categoria.
