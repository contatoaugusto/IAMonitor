# Desenho técnico

## Context

Ver `proposal.md`. `Store` persiste o JSON original de cada evento e limita `detail()` a 500 eventos recentes. `reportDetail()` lê a cronologia completa. `EventLogic.Usage` contém entrada, cache lido, saída e total; `Provider` preserva o evento original nos adaptadores. `DevelopmentUsage` soma o último acumulado de cada arquivo JSONL local incluído.

## Goals / Non-Goals

**Objetivos:** tornar cada contagem de cache navegável, carregar o histórico sem bloquear Swing, mostrar valores por resultado e permitir abrir os eventos e o JSON original.

**Fora do escopo:** interceptar conexões, recuperar memória interna do servidor, atribuir tokens a linhas/arquivos sem evidência, calcular cobrança da assinatura ou alterar a medição existente.

## Decisions

1. Uma visão `CacheInspector` será aberta por `Dashboard` e `UsageGuide`. Ela carregará `Store.reportDetail()` em `SwingWorker`, montará unidades a partir dos eventos `turn.completed` e mostrará resultados separados. A alternativa de usar apenas `Dashboard.current.events()` perderia dados após 500 eventos.
2. Os eventos registrados entre dois resultados serão chamados de **eventos do intervalo**, sem relação de causalidade comprovada com o cache. Cada linha abrirá `DetailsDialog` com o registro original buscado de `Store` por sessão e sequência. A alternativa de apresentar esses eventos como prefixo em cache seria falsa.
3. A decomposição exibirá entrada sem cache = entrada total − cache lido. O JSON de Claude permitirá mostrar `cacheReadInputTokens` e `cacheCreationInputTokens` por modelo quando disponíveis; criação não será incorporada ao cache lido. Campos indisponíveis serão marcados como tal. A alternativa de mesclar leitura e criação distorceria a semântica.
4. `DevelopmentUsage` disponibilizará o mesmo recorte por arquivo JSONL para a tela Sobre. O detalhe terá uma lista por registro, sem misturá-la ao inspetor da sessão monitorada.
5. Os pontos visuais de cache serão ligados por callbacks ao mesmo inspetor. O usuário poderá clicar em linhas de gráfico, células de tabela e indicadores do guia. A alternativa de manter detalhes separados produziria explicações divergentes.

## Risks / Trade-offs

- Histórico grande pode demorar para consultar → consulta em segundo plano e interface de carregamento.
- JSON original pode conter conteúdo da sessão → exibição somente após clique explícito, na máquina local; nenhum dado de sessão entra em specs ou testes.
- Resultado de Claude ou Gemini pode agregar chamadas internas → rótulo de unidade reportada, sem inventar chamadas ou linhas.
