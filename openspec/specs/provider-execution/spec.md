# provider-execution Specification

## Purpose
Iniciar uma execução nova da CLI escolhida na pasta do projeto e interpretar seus eventos sem acessar credenciais diretamente nem interceptar sessões já abertas.

## Requirements

### Requirement: Seleção da IA antes da execução
O aplicativo MUST permitir escolher Codex, Claude Code, Gemini CLI, Cursor Agent ou GitHub Copilot CLI antes de iniciar uma sessão.

#### Scenario: Escolha da ferramenta
- **WHEN** o usuário escolhe uma IA no formulário de nova sessão
- **THEN** o formulário apresenta instruções de preparo e limites de medição específicos dessa CLI

### Requirement: Execução pela CLI autenticada
O aplicativo MUST iniciar uma nova execução da CLI na pasta escolhida e usar a autenticação que a própria CLI já mantém no ambiente do usuário.

#### Scenario: Pedido enviado
- **WHEN** o usuário inicia uma sessão com a CLI instalada e autenticada
- **THEN** o pedido é enviado ao novo processo da CLI com a pasta do projeto como diretório de trabalho

### Requirement: Proveniência dos eventos
O aplicativo MUST obter operações e uso de tokens apenas dos eventos emitidos pelo processo que iniciou e MUST informar quando a CLI não fornece contagem.

#### Scenario: CLI sem uso estruturado
- **WHEN** os eventos de uma execução não contêm contagem de tokens
- **THEN** o painel informa indisponibilidade em vez de apresentar zero como medição

### Requirement: Consulta independente da cota do Codex
O aplicativo MUST consultar a cota oficial do Codex com a autenticação já mantida pela CLI, sem ler ou persistir credenciais e sem impedir a execução quando a consulta falhar.

#### Scenario: Conta ChatGPT autenticada
- **WHEN** uma execução Codex começa e termina com acesso à cota
- **THEN** são guardadas leituras datadas antes e depois para a sessão

#### Scenario: Sem autenticação compatível ou consulta falha
- **WHEN** a conta usa chave de API ou a consulta não retorna cota
- **THEN** a sessão prossegue e o painel indica cota indisponível

### Requirement: Recursos do processo local
O aplicativo MUST distinguir amostras locais de CPU e memória dos processos da CLI do uso de tokens e dos recursos do serviço remoto.

#### Scenario: Amostragem no Windows
- **WHEN** a CLI monitorada está ativa no Windows e a consulta aos processos funciona
- **THEN** o aplicativo registra amostras de CPU e memória do processo iniciado e de seus filhos e apresenta os picos da sessão

#### Scenario: Amostragem indisponível
- **WHEN** não há amostra válida para a sessão
- **THEN** a interface não atribui a ausência de medição ao cache de prompt nem a apresenta como consumo medido do modelo remoto
