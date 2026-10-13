## MODIFIED Requirements

### Requirement: Hierarquia visual da bancada
O aplicativo MUST mostrar estado, operação, contagem e gráficos de ciclos e artefatos na visão principal, com simulação e tabelas extensas em espaço secundário. Cada ciclo recente MUST ter cartão próprio e estado. Em ciclo com contagem, Entrada, Cache, Saída e Outros MUST ter barras distintas, valores absolutos e percentuais do total. Subconjuntos MUST ser identificados sem duplicação.

#### Scenario: Sessão em execução
- **WHEN** a CLI emite novos eventos e resultados de uso
- **THEN** a bancada atualiza operação, totais e gráficos sem bloquear a interface

#### Scenario: Sessão concluída
- **WHEN** o usuário seleciona uma sessão concluída
- **THEN** a visão mantém o resumo e permite abrir dados detalhados, simulação e exportação

#### Scenario: Modo de apresentação
- **WHEN** o usuário ativa o modo de apresentação
- **THEN** a barra lateral é ocultada, a visão geral se expande e o usuário pode voltar ao layout anterior sem interromper a sessão

#### Scenario: Vários resultados de uso
- **WHEN** há mais de um ciclo recente na sessão
- **THEN** cada ciclo ocupa um cartão gráfico separado, com número absoluto e decomposição próprios, e os cartões podem ser percorridos sem agrupar os resultados em uma única barra

#### Scenario: Parcelas de um ciclo
- **WHEN** um ciclo reporta entrada, cache e saída
- **THEN** o cartão apresenta uma barra independente por medida, cada uma com tokens e percentual em escala do total do ciclo, indicando que cache está contido na entrada

#### Scenario: Raciocínio e parcela sem detalhamento
- **WHEN** um ciclo reporta raciocínio ou total maior que entrada mais saída
- **THEN** o cartão mostra raciocínio dentro da saída e a parcela restante como outros, sem contar subconjuntos novamente no total

#### Scenario: Ciclo sem contagem
- **WHEN** um ciclo termina sem uso informado ou permanece pendente
- **THEN** seu cartão não desenha consumo zero e explica o estado da contagem
