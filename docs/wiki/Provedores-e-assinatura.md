# Provedores, CLIs e assinatura

O IAMonitor inicia **uma nova execução** da ferramenta escolhida. O processo usa o login que a própria CLI mantém na conta do Windows; o aplicativo não armazena senha, chave de API ou token de autenticação do provedor. O monitor interpreta apenas os eventos estruturados recebidos desse processo.

| Ferramenta | Fluxo estruturado usado | Uso observado no IAMonitor |
| --- | --- | --- |
| Codex | `codex exec --json` | Resultados por turno concluído, quando emitidos |
| Claude Code | `claude -p --output-format stream-json --verbose` | Resultado final, que pode agregar interações |
| Gemini CLI | `gemini -p --output-format stream-json` | Estatísticas do resultado final |
| Cursor Agent | `agent -p --output-format stream-json` | Eventos; tokens não garantidos |
| GitHub Copilot CLI | `copilot -p --output-format json` | Eventos; tokens e créditos não garantidos |

Os adaptadores de Claude, Gemini, Cursor e Copilot foram verificados com eventos de exemplo nos testes. Isso não equivale a uma execução autenticada em cada conta ou modelo. Quando a ferramenta não informa contagem utilizável, a tela principal escreve **indisponível**; não estima uso real com base no tamanho do prompt.

## Preparo da conta

Instale e autentique a CLI escolhida antes de iniciar a sessão. Use as instruções da própria ferramenta: [Codex](https://help.openai.com/en/articles/11381614-api-codex-cli-and-sign-in-with-chatgpt), [Claude Code](https://code.claude.com/docs/en/quickstart), [Gemini CLI](https://geminicli.com/docs/get-started/authentication/), [Cursor Agent](https://prod.cursor.com/help/integrations/cli) e [GitHub Copilot CLI](https://docs.github.com/en/copilot/how-tos/copilot-cli/set-up-copilot-cli/authenticate-copilot-cli). A conta autenticada precisa pertencer ao mesmo usuário do Windows que abre o IAMonitor.

Se uma CLI estiver instalada em um caminho não descoberto automaticamente, o executável pode ser indicado por `IAMONITOR_CODEX_BIN`, `IAMONITOR_CLAUDE_BIN`, `IAMONITOR_GEMINI_BIN`, `IAMONITOR_CURSOR_BIN` ou `IAMONITOR_COPILOT_BIN`.

## Quadro Assinatura

O quadro lateral separa **tokens da operação** da **cota da conta**. Para Codex autenticado com ChatGPT, o IAMonitor consulta os limites pelo `codex app-server` antes e depois da execução. Ele pode mostrar percentual usado e restante, duração ou renovação da janela e a variação observada em **pontos percentuais**, desde que as leituras se refiram à mesma janela comparável.

A diferença é da **conta inteira durante o intervalo**: outra atividade simultânea pode influenciá-la. Se a janela renovar, faltar leitura ou a conta usar autenticação sem cota compatível, a variação fica indisponível. Essa consulta não inicia uma tarefa de IA; a falha não impede a execução monitorada. São guardados apenas percentuais e horários da consulta, não credenciais.

Para Claude, Gemini, Cursor e Copilot, a cota da assinatura não está disponível no fluxo estruturado usado pelo monitor. O botão **Consultar fonte oficial** abre a página do provedor. O total de tokens que uma CLI informar não deve ser convertido automaticamente em créditos, custo monetário ou percentual da assinatura.

## Tokenização e modelo

O número de tokens depende do modelo e de como ele representa texto, ferramentas e contexto. A aba **Como cada IA conta** do guia explica as diferenças e indica fontes oficiais. A comparação local de texto puro é didática; para contagem oficial de uma entrada específica, use o contador oferecido pela API do modelo quando houver. Para a tarefa executada, consulte os eventos de uso que a CLI expôs.
