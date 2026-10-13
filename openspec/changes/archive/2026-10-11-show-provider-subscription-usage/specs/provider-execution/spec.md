## ADDED Requirements

### Requirement: Consulta independente da cota do Codex
O aplicativo MUST consultar a cota oficial do Codex com a autenticação já mantida pela CLI, sem ler ou persistir credenciais e sem impedir a execução quando a consulta falhar.

#### Scenario: Conta ChatGPT autenticada
- **WHEN** uma execução Codex começa e termina com acesso à cota
- **THEN** são guardadas leituras datadas antes e depois para a sessão

#### Scenario: Sem autenticação compatível ou consulta falha
- **WHEN** a conta usa chave de API ou a consulta não retorna cota
- **THEN** a sessão prossegue e o painel indica cota indisponível
