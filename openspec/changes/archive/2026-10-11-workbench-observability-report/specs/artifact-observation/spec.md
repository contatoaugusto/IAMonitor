## ADDED Requirements

### Requirement: Gráfico de estimativas por tipo de artefato
O aplicativo MUST agrupar os arquivos identificados por categoria e mostrar contagem e estimativa local de tokens em gráfico separado da contagem reportada pela CLI.

#### Scenario: Categorias identificadas
- **WHEN** há artefatos classificados na sessão
- **THEN** painel e PDF mostram barras por categoria e indicam que os valores não são consumo real atribuído aos arquivos

#### Scenario: Nenhum artefato identificado
- **WHEN** não há arquivos observados
- **THEN** o gráfico apresenta estado vazio sem sugerir consumo zero de contexto
