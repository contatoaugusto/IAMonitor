# artifact-observation Specification

## Purpose
Identificar arquivos e origens locais relacionados à atividade e permitir examinar evidências disponíveis sem confundir estimativas locais com tokens efetivamente enviados ao modelo.

## Requirements

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

#### Scenario: Arquivo textual capturado
- **WHEN** um arquivo identificado dentro do projeto é textual, legível e tem no máximo 1 MB
- **THEN** o aplicativo preserva uma cópia local do conteúdo associado à sessão para posterior inspeção, mesmo se o arquivo do projeto mudar

#### Scenario: Arquivo sem cópia preservada
- **WHEN** a sessão não contém cópia do conteúdo identificado
- **THEN** a interface só usa o arquivo atual se ele permanecer na pasta do projeto e corresponder ao hash observado, ou informa por que o conteúdo está indisponível

### Requirement: Limite da estimativa local
O aplicativo MUST rotular estimativas locais de arquivos como estimativas e MUST mantê-las fora do total reportado pela CLI.

#### Scenario: Arquivo observado
- **WHEN** um arquivo local é identificado e seu tamanho em tokens é estimado
- **THEN** essa estimativa não é adicionada ao total principal da execução

### Requirement: Gráfico de estimativas por tipo de artefato
O aplicativo MUST agrupar os arquivos identificados por categoria e mostrar nome completo, contagem de arquivos e estimativa local de tokens em gráfico legível separado da contagem reportada pela CLI. O gráfico MUST exibir todas as categorias suportadas, mesmo quando nenhuma foi identificada, e MUST permitir consultar os arquivos da categoria selecionada sem misturar outras categorias.

#### Scenario: Categorias identificadas
- **WHEN** há artefatos classificados na sessão
- **THEN** o painel mostra barras por categoria com nomes completos, quantidades e estimativas legíveis, e o PDF continua indicando que os valores não são consumo real atribuído aos arquivos

#### Scenario: Nenhum artefato identificado
- **WHEN** não há arquivos observados
- **THEN** painel e PDF mostram todas as categorias com zero arquivos e barras vazias, sem sugerir consumo zero de contexto

#### Scenario: Algumas categorias ausentes
- **WHEN** somente parte das categorias tem arquivos identificados
- **THEN** as demais continuam visíveis com barra vazia e contagem zero

#### Scenario: Abrir categoria identificada
- **WHEN** o usuário seleciona uma categoria no gráfico
- **THEN** o aplicativo lista somente os arquivos identificados naquela categoria, com caminho e estimativa local individual, ou informa que não há arquivos observados nela

### Requirement: Arquivos de comandos
O aplicativo MUST classificar arquivos de comandos identificados como categoria Command quando o caminho estiver em `commands/` ou o nome terminar em `.command.md`.

#### Scenario: Arquivo de comando observado
- **WHEN** um arquivo de comando do projeto é identificado
- **THEN** ele contribui para a categoria Command com estimativa local, sem atribuir a ele tokens reportados pela CLI

#### Scenario: Apenas execução de comando
- **WHEN** há um evento de comando local sem arquivo de comando identificado
- **THEN** a categoria Command permanece com zero arquivos e barra vazia

### Requirement: Trilha de contexto local com grau de evidência
O aplicativo MUST permitir investigar o pedido entregue à CLI, os comandos e saídas locais registrados e os arquivos identificados, sem classificar automaticamente essas evidências como conteúdo transmitido ao provedor.

#### Scenario: Comando concluído com saída
- **WHEN** um evento estruturado do Codex registra um comando concluído e sua saída
- **THEN** a inspeção mostra sequência, comando, saída registrada e caminhos citados, identificando a saída como observação local e possível contexto, não corpo remoto confirmado

#### Scenario: Arquivo apenas identificado
- **WHEN** um arquivo aparece no catálogo sem saída de leitura registrada
- **THEN** a inspeção mostra o arquivo como identificado localmente e não afirma que suas linhas foram lidas ou enviadas

#### Scenario: Evidência ausente
- **WHEN** a CLI não registra saída de ferramenta ou conteúdo de requisição
- **THEN** a inspeção declara a informação indisponível sem substituir por zero ou estimativa

#### Scenario: Sessão longa
- **WHEN** há mais eventos que o limite da inspeção
- **THEN** a tela informa o limite e oferece o JSONL original para análise completa
