# Spec Delta

## Purpose

Gerenciar sessões locais de acompanhamento, permitindo criar, consultar, reiniciar e excluir medições sem perder o controle sobre os dados da atividade.

## ADDED Requirements

### Requirement: Criar e acompanhar uma sessão
O aplicativo MUST criar uma sessão associada à IA, pasta do projeto e pedido escolhidos e selecionar essa sessão no painel ao iniciar a execução.

#### Scenario: Início pela janela Nova sessão
- **WHEN** o usuário aciona Executar e acompanhar com dados válidos
- **THEN** a janela de criação fecha, a sessão fica selecionada e o painel mostra seu estado de execução

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
