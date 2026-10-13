# session-lifecycle Specification

## Purpose
Gerenciar sessões locais de acompanhamento, permitindo criar, consultar, reiniciar e excluir medições sem perder o controle sobre os dados da atividade.

## Requirements

### Requirement: Criar e acompanhar uma sessão
O aplicativo MUST criar uma sessão associada à IA, pasta do projeto e pedido escolhidos e selecionar essa sessão no painel ao iniciar a execução.

#### Scenario: Início pela janela Nova sessão
- **WHEN** o usuário aciona Executar e acompanhar com dados válidos
- **THEN** a janela de criação fecha, a sessão fica selecionada e o painel mostra seu estado de execução

### Requirement: Persistência local das medições
O aplicativo MUST guardar as sessões e suas medições em SQLite local e registrar os eventos originais de cada execução em um JSONL próprio, sem depender de uma sessão ainda aberta na CLI.

#### Scenario: Consultar uma sessão anterior
- **WHEN** o usuário seleciona uma sessão já concluída
- **THEN** a interface recupera seus eventos, uso, artefatos e amostras persistidos, quando disponíveis

#### Scenario: Evento recebido da CLI
- **WHEN** a CLI emite um evento estruturado durante uma execução monitorada
- **THEN** o evento original fica associado à sessão e é acrescentado ao seu registro JSONL local

### Requirement: Reiniciar a medição
O aplicativo MUST permitir zerar os dados medidos de uma sessão e executar novamente seu pedido após confirmação.

#### Scenario: Zerar e executar
- **WHEN** o usuário confirma Zerar e executar para uma sessão
- **THEN** os registros de medição daquela sessão são limpos e uma nova execução do mesmo pedido começa

### Requirement: Excluir uma sessão
O aplicativo MUST permitir excluir uma sessão e seus dados locais após confirmação.

#### Scenario: Exclusão confirmada
- **WHEN** o usuário confirma Excluir sessão
- **THEN** a sessão deixa de aparecer na lista e seus dados associados são removidos

### Requirement: Pedido preparado na pasta do projeto
O aplicativo MUST preencher o pedido editável com `pedido.txt` da raiz da pasta escolhida, quando puder ser lido com segurança, e MUST identificar a origem.

#### Scenario: Pasta com pedido preparado
- **WHEN** a janela abre com uma pasta que contém `pedido.txt` ou o usuário escolhe tal pasta
- **THEN** o texto aparece para revisão antes da execução

#### Scenario: Edição manual
- **WHEN** o usuário edita o pedido e altera a pasta
- **THEN** o novo arquivo não substitui o texto digitado

#### Scenario: Arquivo indisponível
- **WHEN** o arquivo está ausente, vazio, ilegível ou excede o limite
- **THEN** o pedido atual permanece e a interface informa que não carregou sugestão
