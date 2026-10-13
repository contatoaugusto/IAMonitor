## MODIFIED Requirements

### Requirement: Gráfico de estimativas por tipo de artefato
O aplicativo MUST agrupar os arquivos identificados por categoria e mostrar contagem e estimativa local de tokens em gráfico separado da contagem reportada pela CLI. O gráfico MUST exibir todas as categorias suportadas, mesmo quando nenhuma foi identificada.

#### Scenario: Categorias identificadas
- **WHEN** há artefatos classificados na sessão
- **THEN** painel e PDF mostram barras por categoria e indicam que os valores não são consumo real atribuído aos arquivos

#### Scenario: Nenhum artefato identificado
- **WHEN** não há arquivos observados
- **THEN** painel e PDF mostram todas as categorias com zero arquivos e barras vazias, sem sugerir consumo zero de contexto

#### Scenario: Algumas categorias ausentes
- **WHEN** somente parte das categorias tem arquivos identificados
- **THEN** as demais continuam visíveis com barra vazia e contagem zero

## ADDED Requirements

### Requirement: Arquivos de comandos
O aplicativo MUST classificar arquivos de comandos identificados como categoria Command quando o caminho estiver em `commands/` ou o nome terminar em `.command.md`.

#### Scenario: Arquivo de comando observado
- **WHEN** um arquivo de comando do projeto é identificado
- **THEN** ele contribui para a categoria Command com estimativa local, sem atribuir a ele tokens reportados pela CLI

#### Scenario: Apenas execução de comando
- **WHEN** há um evento de comando local sem arquivo de comando identificado
- **THEN** a categoria Command permanece com zero arquivos e barra vazia
