# Proposal

## Why

A inspeção atual mostra quantidades de tokens em cache, mas não esclarece onde o cache está nem se existe um arquivo ou consumo de memória atribuível a ele. Isso pode levar o usuário a confundir o registro JSONL local e a memória da CLI com o cache de prompt do provedor.

## What Changes

- Mostrar na inspeção da sessão o estado conhecido da localização física e da memória do cache de prompt.
- Mostrar o caminho absoluto do registro local de eventos, rotulado explicitamente como registro do IAMonitor, não como arquivo de cache.
- Mostrar o pico de memória medido dos processos locais da CLI quando disponível, separado do consumo desconhecido do cache do provedor.
- Explicar a distinção para sessões sem amostras e para o caso documentado de cache em memória da API Claude.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `cache-inspection`: acrescentar localização, registro local e memória com origem e limitações explícitas.

## Impact

Inspetor Swing de cache, documentação e testes de interface e dados. Sem alteração de formato persistido ou dependências.
