## MODIFIED Requirements

### Requirement: Hierarquia visual da bancada
O aplicativo MUST apresentar na visão principal o estado da sessão, a operação atual, a contagem reportada e gráficos dos ciclos e dos artefatos, deixando simulação e tabelas extensas em espaço secundário acessível. Cada ciclo recente MUST ter um gráfico individual com suas próprias parcelas e estado.

#### Scenario: Sessão em execução
- **WHEN** a CLI emite novos eventos e resultados de uso
- **THEN** a bancada atualiza operação, totais e gráficos sem bloquear a interface

#### Scenario: Sessão concluída
- **WHEN** o usuário seleciona uma sessão concluída
- **THEN** a visão mantém o resumo e permite abrir dados detalhados, simulação e exportação

#### Scenario: Apresentação em vídeo
- **WHEN** o usuário ativa o modo de apresentação
- **THEN** a barra lateral é ocultada, a visão geral se expande e o usuário pode voltar ao layout anterior sem interromper a sessão

#### Scenario: Vários resultados de uso
- **WHEN** há mais de um ciclo recente na sessão
- **THEN** cada ciclo ocupa um cartão gráfico separado, com número absoluto e decomposição próprios, e os cartões podem ser percorridos sem agrupar os resultados em uma única barra

#### Scenario: Ciclo sem contagem
- **WHEN** um ciclo termina sem uso informado ou permanece pendente
- **THEN** seu cartão não desenha consumo zero e explica o estado da contagem
