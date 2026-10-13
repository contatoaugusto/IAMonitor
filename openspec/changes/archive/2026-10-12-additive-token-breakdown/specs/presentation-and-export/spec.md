## MODIFIED Requirements

### Requirement: Exportação dos registros
O aplicativo MUST permitir exportar relatório PDF e eventos JSONL da sessão selecionada. O PDF MUST apresentar resumo, decomposição dos tokens reportados, fluxo lógico das operações, fases ou ciclos observados, estimativas por categoria de artefato e cronologia de todos os eventos persistidos, com paginação legível.

#### Scenario: Exportar sessão
- **WHEN** o usuário aciona uma exportação para a sessão selecionada
- **THEN** o aplicativo produz o arquivo correspondente a partir dos dados disponíveis nessa sessão

#### Scenario: Uso parcial ou indisponível
- **WHEN** faltam resultados de uso ou a contagem não é auditável
- **THEN** o PDF e o painel mostram o estado da medição e não apresentam zero ou total final como se fossem verificados

#### Scenario: Decomposição no PDF
- **WHEN** o relatório contém entrada total e cache reportados pela CLI
- **THEN** sua equação e as legendas das barras apresentam entrada sem cache, cache, saída e eventual diferença como parcelas exclusivas, sem somar novamente a entrada total

### Requirement: Hierarquia visual da bancada
O aplicativo MUST mostrar estado, operação, total reportado e gráficos de ciclos e artefatos na visão principal, com simulação e tabelas extensas em espaço secundário. Cada ciclo recente MUST ter cartão próprio. Um ciclo com contagem MUST mostrar barras legíveis com nome e tokens de parcelas que somam o total uma única vez.

#### Scenario: Sessão em execução
- **WHEN** a CLI emite novos eventos e resultados de uso
- **THEN** a bancada atualiza operação, total e gráficos sem bloquear a interface, com espaço suficiente para ler as medidas do ciclo recente

#### Scenario: Sessão concluída
- **WHEN** o usuário seleciona uma sessão concluída
- **THEN** a visão mantém o resumo e permite abrir dados detalhados, simulação e exportação

#### Scenario: Apresentação em vídeo
- **WHEN** o usuário ativa o modo de apresentação
- **THEN** a barra lateral é ocultada, a visão geral se expande e o usuário pode voltar ao layout anterior sem interromper a sessão

#### Scenario: Vários resultados de uso
- **WHEN** há mais de um ciclo recente na sessão
- **THEN** cada ciclo ocupa um cartão gráfico separado e os cartões podem ser percorridos sem agrupar os resultados em uma única barra

#### Scenario: Parcelas de um ciclo
- **WHEN** um ciclo reporta entrada, cache e saída
- **THEN** as barras mostram entrada sem cache, cache e saída com seus valores em tokens, e a soma das barras corresponde ao total informado, sem barra adicional para a entrada total

#### Scenario: Raciocínio e parcela sem detalhamento
- **WHEN** um ciclo reporta raciocínio ou total maior que entrada mais saída
- **THEN** o raciocínio é explicado como parte da saída, e a diferença positiva aparece como Outros na soma, sem duplicar a saída

#### Scenario: Ciclo sem contagem
- **WHEN** um ciclo termina sem uso informado ou permanece pendente
- **THEN** seu cartão não desenha consumo zero e explica o estado da contagem

#### Scenario: Contagem divergente
- **WHEN** cache excede entrada, raciocínio excede saída ou as parcelas reportadas excedem o total de um ciclo
- **THEN** seu cartão informa a divergência e não desenha barras que pareçam somar corretamente
