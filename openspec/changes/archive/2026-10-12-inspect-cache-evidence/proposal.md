# Proposta: inspeção das evidências de cache

## Why

As contagens de cache aparecem em vários pontos, mas o usuário não consegue abrir a composição observada a partir de cada um deles. A CLI fornece quantidades de tokens reaproveitados, enquanto o conteúdo exato do prefixo reutilizado e sua divisão por linha ou arquivo não estão disponíveis nos eventos capturados.

## What Changes

- Tornar clicáveis os indicadores de cache da bancada, tabelas, guia visual e contagem local de desenvolvimento.
- Abrir uma tela de inspeção com total da sessão, resultados de uso individuais, campos originais da CLI e eventos registrados no intervalo; permitir abrir cada resultado e evento.
- Explicar o que é cache de entrada, como ele compõe o total e quais partes do conteúdo e do tráfego não são observáveis.
- Na tela Sobre, detalhar separadamente os registros locais de desenvolvimento que compõem aquela contagem, sem misturá-los à sessão monitorada.

## Capabilities

### New Capabilities

- `cache-inspection`: navegação pelas contagens e evidências de cache, com limites explícitos da observação.

### Modified Capabilities

Nenhuma.

## Impact

Interface Swing, consulta de eventos persistidos, leitura de registros locais de desenvolvimento, testes e README. Nenhuma alteração na contabilização principal ou na captura de tráfego de rede.
