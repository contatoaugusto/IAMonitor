## Why

O painel mostra tokens da execução, mas não apresenta a cota da assinatura quando ela está disponível. O usuário precisa acompanhar o estado da IA selecionada e relacionar a sessão com uma mudança observada na cota, sem confundir tokens com cobrança.

## What Changes

- Exibir na tela principal um quadro de assinatura para as cinco CLIs.
- Ler a cota oficial do Codex autenticado pelo ChatGPT antes e depois da execução e mostrar a variação observada na conta.
- Identificar como indisponíveis os dados de cota que as outras CLIs não expõem no fluxo estruturado usado pelo aplicativo.
- Guardar as leituras por sessão, sem credenciais, e oferecer acesso à consulta oficial.

## Capacidades afetadas

- `presentation-and-export`: quadro visível na sessão selecionada.
- `token-accounting`: separação entre tokens da operação e cota da conta.
- `provider-execution`: consulta independente e não bloqueante da cota do Codex.
