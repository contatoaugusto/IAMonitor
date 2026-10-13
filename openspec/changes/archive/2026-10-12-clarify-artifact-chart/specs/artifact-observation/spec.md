# Delta da capacidade de observação de artefatos

## MODIFIED Requirements

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
