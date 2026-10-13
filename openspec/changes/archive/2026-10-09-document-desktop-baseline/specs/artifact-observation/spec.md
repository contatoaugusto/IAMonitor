# Spec Delta

## Purpose

Identificar arquivos e origens locais relacionados à atividade e permitir examinar evidências disponíveis sem confundir estimativas locais com tokens efetivamente enviados ao modelo.

## ADDED Requirements

### Requirement: Identificação das origens
O aplicativo MUST classificar origens identificadas como prompt, skills, agents, specs, hooks e outras categorias disponíveis nos eventos e arquivos locais.

#### Scenario: Arquivo identificado
- **WHEN** um evento ou referência local aponta para um artefato do projeto
- **THEN** sua origem e caminho ficam disponíveis nos detalhes da sessão

### Requirement: Detalhes do conteúdo
O aplicativo MUST abrir os detalhes de uma origem ou etapa com o conteúdo realmente registrado ou capturado para ela.

#### Scenario: Detalhe de skill
- **WHEN** o usuário abre os detalhes de uma linha de skills
- **THEN** a tela lista as skills identificadas e os caminhos disponíveis no projeto

#### Scenario: Conteúdo ausente
- **WHEN** não existe conteúdo capturado para a origem selecionada
- **THEN** a tela não inventa texto como se fizesse parte do pedido enviado

### Requirement: Limite da estimativa local
O aplicativo MUST rotular estimativas locais de arquivos como estimativas e MUST mantê-las fora do total reportado pela CLI.

#### Scenario: Arquivo observado
- **WHEN** um arquivo local é identificado e seu tamanho em tokens é estimado
- **THEN** essa estimativa não é adicionada ao total principal da execução
