# Proposta: mostrar ciclos de uso observados no fluxo

## Why

O guia atual desenha um único caminho com uma seta de repetição hipotética. Ele não mostra quantos turnos ou resultados de uso a CLI realmente registrou para a sessão, nem separa as contagens de cada um. Isso pode sugerir que o pedido cruzou a rede apenas uma vez.

## What Changes

- Consultar todos os eventos de início e conclusão de turno da sessão, inclusive quando a lista recente do painel foi truncada.
- Mostrar no fluxo uma faixa para cada unidade de uso concluída, com entrada, cache, saída e total; indicar turnos em andamento ou sem contagem.
- Atualizar o mapa geral para remeter aos ciclos observados e explicar que número de chamadas internas e fragmentos de resposta não é fornecido pela CLI.
- Registrar limites e comportamento no README e em testes.

## Capabilities

- `presentation-and-export`: visualização do fluxo real observado na sessão.

## Impact

Guia Swing, leitura dos eventos armazenados e testes. Os totais de sessão e a persistência existente permanecem sem alteração.
