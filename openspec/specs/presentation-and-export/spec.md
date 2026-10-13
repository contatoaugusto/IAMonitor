# presentation-and-export Specification

## Purpose
Oferecer um painel legível para acompanhamento e apresentação de sessões, com andamento da execução, explicação do ciclo de tokens, ajuda e exportação dos registros disponíveis.

## Requirements

### Requirement: Terminologia clara da visão principal
O aplicativo MUST identificar a tela principal como visão geral da sessão, com rótulos compreensíveis em português brasileiro.

#### Scenario: Abertura da sessão
- **WHEN** o usuário abre a tela principal
- **THEN** o título visível identifica a visão geral da sessão sem usar “bancada”

#### Scenario: Modo de apresentação
- **WHEN** o usuário ativa Apresentar
- **THEN** as medidas e gráficos permanecem legíveis com a terminologia atualizada

### Requirement: Foco na execução em andamento
O aplicativo MUST destacar a operação observada mais recente, seu detalhe, o tempo decorrido e os eventos enquanto a CLI está ativa, em uma visão principal legível durante a execução.

#### Scenario: Evento novo
- **WHEN** chega uma operação durante uma sessão em execução
- **THEN** o painel atualiza a operação em destaque e a lista de eventos, e mantém visíveis as contagens e seus gráficos

### Requirement: Explicação do ciclo de tokens
O aplicativo MUST mostrar como pedido, contexto, CLI local, serviço remoto e resposta se relacionam com as contagens da sessão, e MUST distinguir ciclos de uso observados de chamadas de rede não observáveis.

#### Scenario: Abertura do guia
- **WHEN** o usuário abre Como são medidos os tokens
- **THEN** o fluxo distingue processamento local, envio, modelo remoto e retorno, sem somar o mesmo total repetido nas etapas

#### Scenario: Vários ciclos reportados
- **WHEN** a sessão contém mais de um turno ou resultado de uso concluído
- **THEN** o guia mostra uma faixa por unidade reportada com entrada, cache, saída e total, em ordem, e deixa claro que o pedido pode ter percorrido o caminho mais de uma vez

#### Scenario: Somente resultado agregado
- **WHEN** a CLI informa apenas um resultado final que pode reunir chamadas internas
- **THEN** o guia mostra uma unidade reportada e indica que a quantidade de requisições internas ou fragmentos de resposta é desconhecida

#### Scenario: Eventos de mensagem visíveis
- **WHEN** a CLI registra vários eventos de mensagem entre resultados de uso
- **THEN** o guia mostra a quantidade desses eventos no intervalo e não os apresenta como número de pacotes de rede
#### Scenario: Uso ausente ou turno pendente
- **WHEN** há conclusão sem contagem ou início sem conclusão
- **THEN** o guia mostra a unidade sem presumir zero tokens nem uma chamada remota concluída

### Requirement: Ajuda e proveniência do desenvolvimento
O aplicativo MUST oferecer acesso à tela Sobre com arquitetura, tecnologias, decisões e origem das contagens de desenvolvimento disponíveis.

#### Scenario: Contagem de desenvolvimento
- **WHEN** o usuário consulta IA e tokens na tela Sobre
- **THEN** a interface distingue o total da sessão selecionada das contagens derivadas de registros locais do desenvolvimento

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

#### Scenario: Recursos locais no PDF
- **WHEN** a sessão possui amostras de CPU e memória da CLI
- **THEN** o relatório apresenta os picos como recursos do processo local, separados da contagem de tokens da operação

### Requirement: Quadro de assinatura na sessão selecionada
O aplicativo MUST apresentar o provedor, o estado da execução, os tokens da operação e o estado da cota no painel principal.

#### Scenario: Provedor sem cota estruturada
- **WHEN** a sessão selecionada usa Claude, Gemini, Cursor ou Copilot e o fluxo não informa cota verificável
- **THEN** o quadro mostra a cota como indisponível e oferece acesso à consulta oficial, sem exibir saldo presumido

#### Scenario: Sem sessão selecionada
- **WHEN** nenhuma sessão está selecionada
- **THEN** o quadro solicita a seleção de uma sessão e não mostra leituras antigas

### Requirement: Hierarquia visual da tela principal
O aplicativo MUST mostrar estado, operação, total reportado e gráficos de ciclos e artefatos na visão principal, com simulação e tabelas extensas em espaço secundário. Cada ciclo recente MUST ter cartão próprio. Um ciclo com contagem MUST mostrar barras legíveis com nome e tokens de parcelas que somam o total uma única vez.

#### Scenario: Sessão em execução
- **WHEN** a CLI emite novos eventos e resultados de uso
- **THEN** a tela principal atualiza operação, total e gráficos sem bloquear a interface, com espaço suficiente para ler as medidas do ciclo recente

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

### Requirement: Fluxo visual auditável
O aplicativo MUST identificar quais números vêm da CLI e quais são estimativas locais e MUST distinguir fluxo lógico de contagem de chamadas ou bytes de rede.

#### Scenario: Visualizar fluxo no PDF
- **WHEN** a sessão contém uso reportado
- **THEN** o diagrama mostra entrada, cache como subconjunto da entrada, saída e eventuais parcelas sem detalhamento, sem somar novamente as etapas

### Requirement: Reutilização da configuração para estudo repetido
O aplicativo MUST permitir que uma nova sessão reutilize o pedido e os parâmetros observáveis da sessão selecionada, mantendo a pasta do projeto editável. A reutilização MUST criar uma nova execução e não alterar a sessão de origem.

#### Scenario: Sessão selecionada
- **WHEN** o usuário abre Nova sessão com uma sessão selecionada e aciona Reutilizar sessão selecionada
- **THEN** o formulário preenche pedido, provedor, modelo, permissão e pasta a partir da sessão selecionada para revisão antes de iniciar a nova execução

#### Scenario: Sem sessão selecionada
- **WHEN** o usuário abre Nova sessão sem uma sessão selecionada
- **THEN** o controle de reutilização fica indisponível e os campos permanecem editáveis

#### Scenario: Comparação na mesma pasta
- **WHEN** o formulário apresenta a opção de reutilização
- **THEN** explica que execuções sucessivas na mesma pasta podem partir de contextos diferentes e recomenda cópias equivalentes para estudo comparável

### Requirement: Limite explícito da observação de envio remoto
O aplicativo MUST distinguir o pedido entregue à CLI, os dados locais observados nos eventos e o conteúdo exato das requisições HTTPS, que permanece indisponível nas sessões atuais.

#### Scenario: Abrir trilha da sessão
- **WHEN** o usuário abre a inspeção de dados da sessão
- **THEN** a tela explica as evidências locais, mostra a trilha disponível e não apresenta arquivo identificado como prova de envio à IA

#### Scenario: Exportar PDF
- **WHEN** o usuário exporta uma sessão com eventos locais
- **THEN** o PDF resume evidências disponíveis e limite de observação remota, sem adicionar estimativas ao total de tokens
