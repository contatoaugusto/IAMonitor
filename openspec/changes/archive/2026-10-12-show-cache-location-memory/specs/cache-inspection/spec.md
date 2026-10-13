# Spec Delta

## ADDED Requirements

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
