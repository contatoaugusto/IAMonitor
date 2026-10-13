# cache-inspection Specification

## Purpose
Permitir que o usuário investigue de onde vêm as contagens de cache observadas, percorra seus registros de origem e compreenda os limites do que a CLI revela sobre o conteúdo reutilizado.

## Requirements

### Requirement: Acesso contextual às contagens de cache
O aplicativo MUST oferecer acesso à inspeção de cache a partir dos indicadores de cache da sessão na tela principal, nas tabelas e no guia visual. Um indicador de ciclo MUST abrir a inspeção naquele ciclo.

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

#### Scenario: Inspecionar uma leitura local de desenvolvimento
- **WHEN** o usuário abre uma linha de leitura de um registro JSONL de desenvolvimento
- **THEN** a interface mostra o caminho absoluto do registro e os totais cumulativos daquela linha, identificando o JSONL como registro local e não como arquivo do cache do provedor

### Requirement: Localização e memória do cache
A inspeção MUST apresentar separadamente a localização do cache de prompt do provedor, o caminho absoluto do registro local de eventos e o consumo de memória dos processos CLI medido na sessão. Informação não exposta pela CLI MUST ser identificada como indisponível, sem atribuição presumida.

#### Scenario: Caminho físico do cache não exposto
- **WHEN** a sessão não fornece um caminho físico verificável para o cache de prompt
- **THEN** a inspeção informa que o caminho e os arquivos físicos do cache não foram expostos pelo provedor ou pela CLI
- **AND** não apresenta um caminho de arquivo local como se fosse o cache

#### Scenario: Registro de eventos local
- **WHEN** há arquivo JSONL da sessão no IAMonitor
- **THEN** a inspeção mostra seu caminho absoluto e o identifica explicitamente como registro de eventos, não como cache de prompt

#### Scenario: Memória dos processos medida
- **WHEN** há uma amostra positiva de memória dos processos CLI da sessão
- **THEN** a inspeção mostra o pico medido e esclarece que ele não quantifica a memória do cache do provedor

#### Scenario: Memória do cache não medida
- **WHEN** a CLI não informa a memória atribuída ao cache ou a sessão não tem amostras locais
- **THEN** a inspeção sinaliza a grandeza correspondente como indisponível, sem tratá-la como zero

#### Scenario: Cache da API Claude em memória
- **WHEN** o provedor selecionado é Claude
- **THEN** a inspeção pode explicar o comportamento documentado de cache de prompt em memória na API Claude sem alegar conhecer a máquina, o endereço ou os bytes de RAM da entrada observada

#### Scenario: Informação de armazenamento da API OpenAI
- **WHEN** o provedor selecionado é Codex
- **THEN** a inspeção informa que a documentação da API OpenAI descreve cache em máquinas do serviço e possível armazenamento na GPU, sem atribuir uma política, arquivo ou quantidade de RAM específica à sessão
