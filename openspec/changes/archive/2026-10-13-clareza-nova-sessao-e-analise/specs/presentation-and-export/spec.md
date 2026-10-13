## MODIFIED Requirements

### Requirement: Terminologia clara da visão principal
O aplicativo MUST identificar a tela principal como visão geral da sessão, com rótulos compreensíveis em português brasileiro.

#### Scenario: Abertura da sessão
- **WHEN** o usuário abre a tela principal
- **THEN** o título visível identifica a visão geral da sessão sem usar “bancada”

#### Scenario: Modo de apresentação
- **WHEN** o usuário ativa “Ampliar visão geral”
- **THEN** as medidas e gráficos permanecem legíveis com a terminologia atualizada

### Requirement: Hierarquia visual da tela principal
O aplicativo MUST mostrar estado, operação, total reportado e gráficos de ciclos e artefatos na visão principal, com simulação e tabelas extensas em espaço secundário. Cada ciclo recente MUST ter cartão próprio. Um ciclo com contagem MUST mostrar barras legíveis com nome e tokens de parcelas que somam o total uma única vez.

#### Scenario: Sessão em execução
- **WHEN** a CLI emite novos eventos e resultados de uso
- **THEN** a tela principal atualiza operação, total e gráficos sem bloquear a interface, com espaço suficiente para ler as medidas do ciclo recente

#### Scenario: Sessão concluída
- **WHEN** o usuário seleciona uma sessão concluída
- **THEN** a visão mantém o resumo e permite abrir dados detalhados, simulação e exportação

#### Scenario: Modo de apresentação
- **WHEN** o usuário ativa “Ampliar visão geral”
- **THEN** a barra lateral é ocultada, a visão geral se expande e o controle oferece “Restaurar painel” sem interromper a sessão

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

## ADDED Requirements

### Requirement: Navegação direta para análise
O aplicativo MUST oferecer ação explícita para abrir dados detalhados e simulação.

#### Scenario: Explorar dados e simular
- **WHEN** o usuário aciona essa ação
- **THEN** a aba de eventos, origens e simulação é selecionada

### Requirement: Orientação legível na nova sessão
O aplicativo MUST exibir a orientação de login, acesso e medição como texto sem caixa de edição ou rolagem própria.

#### Scenario: Trocar provedor
- **WHEN** o usuário escolhe outra IA
- **THEN** a orientação visível se atualiza
