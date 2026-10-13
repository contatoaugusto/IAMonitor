# Glossário

| Termo | Significado neste projeto |
| --- | --- |
| **Artefato identificado** | Arquivo local observado ou referenciado na execução, agrupado por categoria. Não é prova de envio integral ao modelo. |
| **Cache lido** | Parte da entrada que o provedor declarou reutilizada. Já integra a entrada total. |
| **Cache criado** | Campo específico que alguns provedores podem emitir para preparação de cache; não é cache lido. |
| **CLI** | Programa de linha de comando da IA, iniciado pelo IAMonitor para uma nova tarefa. |
| **Cota** | Limite e janela da conta do provedor, quando consultáveis; distinto dos tokens da operação. |
| **Ciclo observado** | Unidade de uso emitida pela CLI, como turno concluído ou resultado final; não representa necessariamente uma única requisição de rede. |
| **Entrada** | Tokens apresentados ao modelo remoto para trabalhar, conforme o uso reportado. |
| **Entrada sem cache** | Entrada total menos cache lido. |
| **Estimativa local** | Cálculo do tamanho em tokens de texto identificado no computador; não é medição do que entrou no modelo. |
| **Evento** | Registro estruturado emitido pela CLI durante a execução e persistido pelo monitor. |
| **JSONL** | Arquivo de texto com um objeto JSON por linha; guarda os eventos originais da sessão. |
| **Outros** | Parcela positiva sem detalhamento quando o total reportado excede a soma das partes conhecidas. |
| **Raciocínio** | Quando informado, subconjunto da saída, não uma parcela adicional do total. |
| **Saída** | Tokens gerados pelo modelo, inclusive material que pode não aparecer literalmente na resposta visível. |
| **Sessão** | Registro local de uma tarefa iniciada pelo IAMonitor, com projeto, provedor, eventos e medições. |
| **Token** | Unidade definida pelo tokenizer/modelo; não é igual a palavra, byte ou valor monetário fixo. |

Se um número parecer contraditório, comece por [Metodologia dos tokens](https://github.com/contatoaugusto/IAMonitor/wiki/Metodologia-dos-tokens) e pelo registro original em [Dados e privacidade](https://github.com/contatoaugusto/IAMonitor/wiki/Dados-e-privacidade).
