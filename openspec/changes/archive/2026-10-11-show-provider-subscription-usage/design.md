## Decisões

### Consulta do Codex

Usar um processo `codex app-server` separado, com a autenticação que a própria CLI já mantém, e JSON-RPC em stdio (`initialize`, `initialized`, `account/read`, `account/rateLimits/read`). Ele não inicia turno nem consome tokens de inferência. Consultar antes da execução e após seu fim fora da thread Swing, com limite de tempo. Alternativa rejeitada: interpretar a saída de `/status`, destinada ao terminal interativo.

### Variação da conta

Persistir apenas identificador do limite, janela, percentual usado, horário da leitura e tipo de plano quando informado. Mostrar diferença em pontos percentuais somente para o mesmo limite, mesma janela e mesmo instante de renovação. A diferença é movimento observado na conta durante a sessão, que pode incluir outras atividades. Alternativa rejeitada: converter tokens da operação em percentual da assinatura.

### Demais provedores

Manter o total auditável de tokens da execução e mostrar cota indisponível para Claude, Gemini, Cursor e Copilot enquanto o fluxo estruturado executado pelo monitor não trouxer quota verificável. Apontar para a consulta oficial de cada um. O custo calculado pelo Claude é uma estimativa do cliente, não um débito verificável da assinatura.

### Persistência e interface

Adicionar tabela de leituras de cota por sessão no `Store`, limpando-a em reinício/exclusão. Um serviço de consulta próprio concentra processo, leitura e interpretação do JSON-RPC; `CodexRunner` solicita as duas leituras. `Dashboard` apresenta o quadro na barra lateral da tela principal, ao lado das sessões, com estado, fonte, atualização e botão para abrir a consulta oficial. Assim, a área de operações mantém sua altura. Falhas da cota não interrompem a sessão.
