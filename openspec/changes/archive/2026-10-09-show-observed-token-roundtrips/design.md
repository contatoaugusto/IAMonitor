# Design

## Context

`Store.detail` carrega somente os 500 eventos mais recentes, enquanto `auditedUsage` soma todos os `turn.completed` armazenados. O guia usa totais agregados e uma seta visual genérica. Codex emite turnos; Claude e Gemini são normalizados a partir de um resultado final, que pode agregar chamadas internas. A aplicação não intercepta o tráfego.

## Goals / Non-Goals

**Goals:** mostrar cada unidade de uso reportada pela CLI, revelar repetição observada e separar contagens por unidade.

**Non-Goals:** inferir o número exato de requisições HTTP, retransmissões do mesmo texto ou fragmentos de streaming sem eventos que os identifiquem.

## Decisions

### 1. Consultar eventos de fluxo sem o limite da lista recente

Adicionar em `Store` uma consulta ordenada dos eventos `turn.started`, `turn.completed` e de mensagem da sessão. Reusar `EventLogic.usage` para interpretar as parcelas. Isso evita que o diagrama omita turnos antigos enquanto exibe o total de todos.

### 2. Modelar ciclos reportados, não pacotes de rede

Emparelhar inícios e conclusões quando existirem; criar uma faixa por conclusão mesmo sem evento de início. Um início sem conclusão aparece como em andamento. Cada faixa é explicitamente um turno/resultado de uso reportado, que pode abranger mais de uma chamada interna.

### 3. Distinguir granularidade por provedor

Para Codex, rotular as faixas como turnos. Para os demais provedores, rotular como resultados de uso, pois a normalização pode produzir apenas o total final. Exibir 'chamadas internas não observáveis' e não transformar uma única faixa em alegação de uma única requisição.

### 4. Renderizar a repetição na primeira área do guia

Acrescentar uma seção visual de faixas repetidas antes do mapa geral. Cada faixa mostra entrada → modelo → saída, com cache incluído na entrada, status da contagem e quantidade de eventos de mensagem no intervalo quando houver. O mapa geral deixa de sugerir um único percurso medido.

## Risks / Trade-offs

Sessões com muitos turnos criam uma área rolável longa, mas preservam cada unidade observada. Eventos antigos sem uso mostram indisponibilidade, em vez de zero. A consulta é somente leitura e não muda a contabilidade da sessão.
