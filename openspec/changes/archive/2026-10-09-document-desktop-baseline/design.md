# Design

## Contexto

O projeto é um aplicativo Java 21/Swing com testes JUnit e dados locais em SQLite/JSONL. O README descreve o uso, mas não oferece um contrato por capacidade nem um histórico estruturado de decisões.

## Objetivos / Fora do escopo

**Objetivos:** tornar o comportamento entregue consultável em specs atuais e registrar decisões, plano e implementação das próximas mudanças.

**Fora do escopo:** mudar contagem de tokens, interface, persistência, CLIs ou reconstruir uma cronologia de decisões antigas que não foi registrada.

## Decisões

### 1. OpenSpec padrão na raiz

Usar o esquema `spec-driven` e `openspec/specs/` para o estado atual. Cada mudança terá pasta própria em `openspec/changes/`, com `proposal.md`, deltas, `design.md` quando houver decisão técnica e `tasks.md`. Após a entrega, arquivar a mudança. Isso mantém contrato atual e histórico separados. Uma pasta livre de notas não seria validada pela CLI.

### 2. Specs por capacidade observável

Separar sessão, execução das CLIs, contagem, artefatos, simulação e apresentação/exportação. Essa divisão acompanha fronteiras de comportamento do aplicativo e evita um documento único difícil de revisar. As specs registram o que existe hoje; propostas futuras descrevem alterações em deltas.

### 3. Decisões e evidências em cada mudança

Registrar em `design.md` a escolha, o motivo, alternativas consideradas e consequências relevantes. Usar `tasks.md` para evidência de implementação e verificação. Não inventar justificativas históricas para código preexistente; esta decisão documenta apenas a adoção do OpenSpec.

### 4. Dados locais fora das specs

Descrever contratos e casos com dados genéricos. Não transcrever prompts, eventos reais, credenciais ou arquivos de `data/`.

## Riscos / Trocas

Specs iniciais resumem o comportamento observável e podem precisar de refinamento quando uma capacidade mudar. A CLI valida estrutura, não comprova por si só que a aplicação cumpre cada cenário; testes e inspeção do código continuam necessários.
