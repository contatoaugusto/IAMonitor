# Design

## Context

O guia existente é um diálogo Swing com um conteúdo rolável. O projeto já usa JTokkit com `O200K_BASE` para estimativas locais de arquivos. As cinco ferramentas não têm um tokenizer universal: Cursor e Copilot podem escolher modelos de outros provedores, enquanto Anthropic e Google oferecem endpoints de contagem vinculados ao modelo.

## Goals / Non-Goals

**Goals:** explicar a tokenização por ferramenta, oferecer uma comparação imediata de texto puro e evitar números falsamente exatos.

**Non-Goals:** pedir chaves de API, ler logins das CLIs, enviar texto digitado pela rede, inferir consumo de assinatura ou reconstruir o payload completo de uma execução real.

## Decisions

### 1. Duas abas no diálogo existente

Manter o diagrama e as contagens da sessão em `Fluxo da sessão`. Criar `Como cada IA conta` com seletor, explicação e simulador. O conteúdo novo terá rolagem própria e contraste visual com o diálogo principal.

### 2. Contagem reproduzível somente para uma codificação identificada

Usar JTokkit `O200K_BASE` para contar exatamente os tokens do texto isolado nessa codificação da OpenAI. O número não representa necessariamente o modelo selecionado em Codex nem inclui mensagens, ferramentas ou contexto.

### 3. Aproximações explícitas para as demais ferramentas

Para Claude, Cursor e Copilot, usar a mesma contagem `O200K_BASE` apenas como referência local comparativa; exibir `≈` e explicar que é um proxy OpenAI, não o tokenizer real. Para Gemini, usar a regra didática aproximada de quatro caracteres por token documentada pelo Google. Todos os resultados não oficiais são marcados como estimativa. Uma linha por ferramenta é mais útil para comparação do que apresentar zero ou um número sem proveniência.

### 4. Documentação vinculada ao provedor

Cada opção descreve método, dependência de modelo, componentes que alteram a contagem e caminho oficial para obter valor do provedor. Os links oficiais são visíveis e podem ser abertos no navegador.

### 5. Privacidade e isolamento

O texto fica somente no componente Swing; nenhuma chamada de rede é feita ao calcular. O resultado do simulador não entra no armazenamento nem no total de sessão.

## Risks / Trade-offs

A referência local pode diferir bastante dos tokenizers de Claude ou do modelo selecionado por Cursor/Copilot, sobretudo para português, código e Unicode. O rótulo e a explicação devem permanecer junto a cada número. A regra de quatro caracteres do Gemini é aproximada, não uma implementação do `countTokens`.
