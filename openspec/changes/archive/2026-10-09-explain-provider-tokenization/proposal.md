# Proposta: explicar a tokenização por provedor

## Why

O guia atual explica entrada, saída e fluxo da sessão, mas não mostra como o texto é dividido em tokens pelos provedores. Isso dificulta comparar um pedido curto e cria a impressão de que todas as ferramentas usam a mesma contagem.

## What Changes

- Acrescentar uma aba própria no diálogo Como são medidos os tokens.
- Permitir selecionar Codex/OpenAI, Claude/Anthropic, Gemini/Google, Cursor e GitHub Copilot para ler a metodologia, os limites e as fontes oficiais.
- Incluir um simulador de texto que apresenta uma linha numérica para cada ferramenta, indicando explicitamente quando o valor é contagem local de uma codificação conhecida ou aproximação.
- Manter o texto digitado apenas em memória; o simulador não envia o texto a serviços externos nem modifica as medições de sessões.

## Capabilities

- `token-methodology`: explicação e comparação de contagens para texto isolado.

## Impact

Interface Swing do guia, lógica de simulação local, testes JUnit e documentação. Não altera o total reportado pelas CLIs, o SQLite ou os eventos JSONL.
