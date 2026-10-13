# Design

## Context

`CacheInspector` já reúne os resultados da sessão. `Store.rawFile(id)` resolve o JSONL local, e `Store.Run.memoryPeak()` guarda o pico de `WorkingSet64` somado dos processos CLI amostrados em Windows. Nenhum adaptador de `Provider` informa endereço físico ou memória do cache de prompt remoto.

## Goals / Non-Goals

**Goals:** deixar a origem de cada dado visível e manter a interface legível no modal existente.

**Non-Goals:** localizar arquivos internos do provedor, estimar RAM a partir de tokens, atribuir eventos locais ao prefixo em cache ou alterar a coleta.

## Decisions

- Acrescentar uma área curta de “Localização e memória” no topo de `CacheInspector`, usando os dados já carregados. Alternativa: colocar a explicação apenas no rodapé ou no README; isso ocultaria a resposta no ponto em que o usuário inspeciona o cache.
- Mostrar o caminho absoluto de `Store.rawFile(id)` somente como registro de eventos do IAMonitor. Alternativa: chamá-lo de arquivo de cache; isso seria falso. O caminho pode existir mesmo sem arquivo, portanto a interface distingue existência e caminho esperado.
- Mostrar `memoryPeak()` como pico amostrado de processos CLI, em MiB, apenas se positivo. A memória do cache fica “não informada”. Alternativa: estimar bytes por token; a relação depende da implementação do provedor e não seria auditável.
- Para Claude, explicar que a documentação da API descreve representações KV mantidas temporariamente em memória, sem extrapolar para endereço físico ou uso de RAM desta sessão. Para os demais, apresentar o mecanismo e local exato como não informado. Alternativa: declarar que todos os provedores usam exclusivamente RAM; isso não está demonstrado pelos eventos.

## Risks / Trade-offs

- Caminho absoluto longo pode ocupar a janela → usar texto com quebra de linha e área compacta.
- Pico local inclui processos filhos e é uma amostra, não uma medida do cache remoto → rotular origem e período na interface.
- A documentação dos provedores pode mudar → texto restrito à evidência e referência oficial no README.
