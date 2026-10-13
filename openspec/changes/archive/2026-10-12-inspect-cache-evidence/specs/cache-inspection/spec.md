# Delta da inspeção de cache

## Purpose

Permitir que o usuário investigue de onde vêm as contagens de cache observadas, percorra seus registros de origem e compreenda os limites do que a CLI revela sobre o conteúdo reutilizado.

## ADDED Requirements

### Requirement: Acesso contextual às contagens de cache
O aplicativo MUST oferecer acesso à inspeção de cache a partir dos indicadores de cache da sessão na bancada, nas tabelas e no guia visual. Um indicador de ciclo MUST abrir a inspeção naquele ciclo.

#### Scenario: Clicar no gráfico de ciclo
- **WHEN** o usuário clica na linha ou barra Cache de um ciclo observado
- **THEN** a inspeção abre com esse resultado de uso selecionado

#### Scenario: Clicar em outra contagem da sessão
- **WHEN** o usuário clica na parcela cache do resumo, na coluna Cache, na origem cache ou em um indicador do guia
- **THEN** a inspeção abre para a sessão selecionada ou para o ciclo correspondente, quando identificado

#### Scenario: Sessão sem cache ou sem medição
- **WHEN** o indicador mostra zero ou não há uso auditável
- **THEN** a inspeção explica o estado sem presumir conteúdo ou consumo zero não reportado

### Requirement: Decomposição auditável do cache
O aplicativo MUST mostrar a relação entre entrada total, cache lido, entrada sem cache e total por resultado de uso, bem como os campos originais da CLI disponíveis. As unidades reportadas MUST permanecer separadas.

#### Scenario: Vários resultados de uso
- **WHEN** a sessão tem vários resultados com cache
- **THEN** cada resultado pode ser selecionado e seus números, horário e registro de origem podem ser abertos individualmente

#### Scenario: Metadados específicos do provedor
- **WHEN** o registro original informa leitura ou criação de cache por modelo
- **THEN** a inspeção apresenta esses campos separadamente e não confunde criação com cache lido

#### Scenario: Eventos no intervalo
- **WHEN** há eventos registrados entre resultados de uso
- **THEN** o usuário pode abrir cada evento disponível, sem que ele seja rotulado como conteúdo comprovadamente em cache

### Requirement: Limites do conteúdo observado
O aplicativo MUST explicar o significado de cache de entrada e MUST indicar quando não conhece a sequência exata de tokens, linhas, arquivos, prefixo reutilizado, bytes da rede ou cobrança. Ele MUST não inferir essa composição a partir de arquivos locais ou mensagens visíveis.

#### Scenario: Conteúdo do cache indisponível
- **WHEN** a CLI fornece apenas a quantidade de tokens em cache
- **THEN** a inspeção identifica a quantidade como reportada e informa que o conteúdo e a atribuição por linha ou arquivo são indisponíveis

### Requirement: Cache dos registros locais de desenvolvimento
O aplicativo MUST manter a contagem de cache da tela Sobre separada da sessão monitorada e permitir inspecionar os registros locais que compõem sua soma, sem atribuir conteúdo aos tokens em cache.

#### Scenario: Abrir cache de desenvolvimento
- **WHEN** o usuário clica na contagem de cache em IA e tokens na tela Sobre
- **THEN** a interface lista os registros incluídos e a contagem de cache de cada um, distinguindo essa soma da sessão selecionada
