# Desenho

## Contexto

`WorkbenchCharts.CycleCard` recebe `EventLogic.Usage` e hoje desenha uma barra empilhada. A lista rolável e a ordem dos ciclos já atendem ao contrato. `Usage.total` é o denominador reportado; cache integra entrada e raciocínio integra saída.

## Objetivos e limites

- Permitir comparação direta de cada medida com o total do próprio ciclo, mantendo o cartão compacto na visão principal.
- Preservar a distinção entre uso ausente e uso zero.
- A contabilidade, a persistência e o PDF ficam fora desta alteração visual.

## Decisões

- Trocar a barra empilhada por linhas horizontais rotuladas, todas na mesma escala do total do ciclo. O valor em tokens e o percentual ficam ao lado de cada barra. Isso torna cada medida localizável sem depender de legenda ou ordem de cores. Alternativa descartada: manter a barra única com legenda, pois ainda exige reconstruir visualmente cada parcela.
- Entrada, cache, saída e outros aparecem como linhas separadas. Cache terá recuo e rótulo de subconjunto; raciocínio terá recuo sob saída somente quando informado. O cabeçalho esclarece que entrada mais saída mais outros formam o total e que subconjuntos não são somados. Alternativa descartada: separar entrada sem cache de cache, pois o usuário pediu explicitamente a medida de entrada reportada.
- O componente terá altura calculada pelo número de linhas e continuará na lista rolável. Barras vazias representam valor zero somente quando há um resultado de uso reportado. Cartões sem resultado conservam o aviso textual.

## Riscos e compensações

- Mais linhas elevam a altura do cartão e podem exigir rolagem durante a execução. → Usar linhas compactas, manter o ciclo mais recente no topo e conferir a prévia no tamanho da janela principal.
- Percentuais de subconjuntos não são aditivos. → Marcar cache e raciocínio como contidos e explicar a soma no painel.
