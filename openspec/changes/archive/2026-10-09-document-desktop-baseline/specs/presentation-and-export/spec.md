# Spec Delta

## Purpose

Oferecer um painel legível para apresentação de sessões, com andamento da execução, explicação do ciclo de tokens, ajuda e exportação dos registros disponíveis.

## ADDED Requirements

### Requirement: Foco na execução em andamento
O aplicativo MUST destacar a operação observada mais recente, seu detalhe, o tempo decorrido e os eventos enquanto a CLI está ativa.

#### Scenario: Evento novo
- **WHEN** chega uma operação durante uma sessão em execução
- **THEN** o painel atualiza a operação em destaque e a lista de eventos

### Requirement: Explicação do ciclo de tokens
O aplicativo MUST mostrar como pedido, contexto, CLI local, serviço remoto e resposta se relacionam com as contagens informadas na sessão.

#### Scenario: Abertura do guia
- **WHEN** o usuário abre Como são medidos os tokens
- **THEN** o fluxo distingue processamento local, envio, modelo remoto e retorno, sem somar o mesmo total repetido nas etapas

### Requirement: Ajuda e proveniência do desenvolvimento
O aplicativo MUST oferecer acesso à tela Sobre com arquitetura, tecnologias, decisões e origem das contagens de desenvolvimento disponíveis.

#### Scenario: Contagem de desenvolvimento
- **WHEN** o usuário consulta IA e tokens na tela Sobre
- **THEN** a interface distingue o total da sessão selecionada das contagens derivadas de registros locais do desenvolvimento

### Requirement: Exportação dos registros
O aplicativo MUST permitir exportar relatório PDF e eventos JSONL da sessão selecionada pelos controles do painel.

#### Scenario: Exportar sessão
- **WHEN** o usuário aciona uma exportação para a sessão selecionada
- **THEN** o aplicativo produz o arquivo correspondente a partir dos dados disponíveis nessa sessão
