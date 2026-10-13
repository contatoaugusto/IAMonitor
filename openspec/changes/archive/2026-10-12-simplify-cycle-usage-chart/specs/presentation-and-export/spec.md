## MODIFIED Requirements

### Requirement: Hierarquia visual da bancada
O aplicativo MUST mostrar estado, operação, total reportado e gráficos de ciclos e artefatos na visão principal, com simulação e tabelas extensas em espaço secundário. Cada ciclo recente MUST ter cartão próprio. Um ciclo com contagem MUST mostrar barras legíveis e apenas o nome e a quantidade de tokens de cada medida, sem percentuais nem cartões redundantes acima do gráfico.

#### Scenario: Sessão em execução
- **WHEN** a CLI emite novos eventos e resultados de uso
- **THEN** a bancada atualiza operação, total e gráficos sem bloquear a interface, com espaço suficiente para ler as medidas do ciclo recente

#### Scenario: Sessão concluída
- **WHEN** o usuário seleciona uma sessão concluída
- **THEN** a visão mantém o resumo e permite abrir dados detalhados, simulação e exportação

#### Scenario: Modo de apresentação
- **WHEN** o usuário ativa o modo de apresentação
- **THEN** a barra lateral é ocultada, a visão geral se expande e o usuário pode voltar ao layout anterior sem interromper a sessão

#### Scenario: Vários resultados de uso
- **WHEN** há mais de um ciclo recente na sessão
- **THEN** cada ciclo ocupa um cartão gráfico separado e os cartões podem ser percorridos sem agrupar os resultados em uma única barra

#### Scenario: Parcelas de um ciclo
- **WHEN** um ciclo reporta entrada, cache e saída
- **THEN** cada medida tem uma barra grande com seu nome e número de tokens, sem percentual ou texto auxiliar no cartão; cache continua identificado como subconjunto da entrada

#### Scenario: Raciocínio e parcela sem detalhamento
- **WHEN** um ciclo reporta raciocínio ou total maior que entrada mais saída
- **THEN** o cartão mostra apenas as parcelas opcionais positivas, mantendo raciocínio como parte da saída e sem somar subconjuntos novamente ao total

#### Scenario: Ciclo sem contagem
- **WHEN** um ciclo termina sem uso informado ou permanece pendente
- **THEN** seu cartão não desenha consumo zero e explica o estado da contagem
