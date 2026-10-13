## MODIFIED Requirements

### Requirement: Foco na execução em andamento
O aplicativo MUST destacar a operação observada mais recente, seu detalhe, o tempo decorrido e os eventos enquanto a CLI está ativa, em uma visão principal legível para demonstração em vídeo.

#### Scenario: Evento novo
- **WHEN** chega uma operação durante uma sessão em execução
- **THEN** o painel atualiza a operação em destaque e a lista de eventos, e mantém visíveis as contagens e seus gráficos

### Requirement: Exportação dos registros
O aplicativo MUST permitir exportar relatório PDF e eventos JSONL da sessão selecionada. O PDF MUST apresentar resumo, decomposição dos tokens reportados, fluxo lógico das operações, fases ou ciclos observados, estimativas por categoria de artefato e cronologia de todos os eventos persistidos, com paginação legível.

#### Scenario: Exportar sessão
- **WHEN** o usuário aciona uma exportação para a sessão selecionada
- **THEN** o aplicativo produz o arquivo correspondente a partir dos dados disponíveis nessa sessão

#### Scenario: Uso parcial ou indisponível
- **WHEN** faltam resultados de uso ou a contagem não é auditável
- **THEN** o PDF e o painel mostram o estado da medição e não apresentam zero ou total final como se fossem verificados

## ADDED Requirements

### Requirement: Hierarquia visual da bancada
O aplicativo MUST apresentar na visão principal o estado da sessão, a operação atual, a contagem reportada e gráficos dos ciclos e dos artefatos, deixando simulação e tabelas extensas em espaço secundário acessível.

#### Scenario: Sessão em execução
- **WHEN** a CLI emite novos eventos e resultados de uso
- **THEN** a bancada atualiza operação, totais e gráficos sem bloquear a interface

#### Scenario: Sessão concluída
- **WHEN** o usuário seleciona uma sessão concluída
- **THEN** a visão mantém o resumo e permite abrir dados detalhados, simulação e exportação

#### Scenario: Apresentação em vídeo
- **WHEN** o usuário ativa o modo de apresentação
- **THEN** a barra lateral é ocultada, a visão geral se expande e o usuário pode voltar ao layout anterior sem interromper a sessão

### Requirement: Fluxo visual auditável
O aplicativo MUST identificar quais números vêm da CLI e quais são estimativas locais e MUST distinguir fluxo lógico de contagem de chamadas ou bytes de rede.

#### Scenario: Visualizar fluxo no PDF
- **WHEN** a sessão contém uso reportado
- **THEN** o diagrama mostra entrada, cache como subconjunto da entrada, saída e eventuais parcelas sem detalhamento, sem somar novamente as etapas
