# Solução de problemas

## A IA não inicia

Confirme que a CLI escolhida está instalada, responde no PowerShell e está autenticada **na mesma conta do Windows** que abriu o IAMonitor. Abra a ferramenta diretamente uma vez para concluir o login. Confira também se a pasta do projeto existe e se o pedido foi preenchido. Quando a ferramenta não estiver no `PATH`, use a variável `IAMONITOR_<PROVEDOR>_BIN` correspondente; os nomes estão em [Provedores e assinatura](https://github.com/contatoaugusto/IAMonitor/wiki/Provedores-e-assinatura).

## Vejo eventos, mas não vejo tokens

Isso pode ser um limite do formato estruturado da CLI ou do resultado específico. Cursor e Copilot não garantem tokens nesse fluxo; uma execução ainda em curso também pode não ter resultado de uso concluído. Abra **Explorar dados** ou o JSONL para ver o evento original. **Indisponível** não significa zero.

## A soma de entrada, cache e saída parece maior que o total

Cache já integra a entrada; se você usar **entrada total**, não adicione cache outra vez. Some **entrada sem cache + cache + saída + outros**. Raciocínio, quando separado, já faz parte da saída. Veja o [exemplo numérico](https://github.com/contatoaugusto/IAMonitor/wiki/Metodologia-dos-tokens).

## O gráfico de artefatos marca zero em uma categoria

Zero significa que nenhum **arquivo daquela categoria foi identificado** na sessão. Isso não prova ausência de instruções ou de contexto no pedido remoto. Clique na categoria para conferir os arquivos observados ou a explicação de ausência. A estimativa local não altera os tokens da CLI.

## Não encontro o arquivo físico do cache

O cache de prompt do provedor não é exposto pelas CLIs monitoradas como um arquivo local verificável. O caminho `data/runs/<id>.jsonl` é o log de eventos do IAMonitor. O valor de memória exibido, se houver, pertence ao processo local da CLI, não ao cache do modelo. Veja [Cache e evidências](https://github.com/contatoaugusto/IAMonitor/wiki/Cache-e-evidencias).

## A cota da assinatura aparece indisponível

A leitura de cota integrada existe para Codex com conta ChatGPT compatível. Ela pode falhar sem afetar a tarefa ou deixar de ser comparável se a janela renovar. Para as outras CLIs, consulte o painel oficial indicado na interface. Tokens observados na operação não são saldo de assinatura.

## Não encontro uma sessão antiga

Verifique qual pasta de dados foi usada ao iniciar o aplicativo. `run.ps1` usa `data/` por padrão, mas `-DataDir` aponta para outra pasta. O build não apaga `data/`. Se uma sessão foi excluída após confirmação, seus dados locais associados são removidos.

## O aplicativo empacotado não abre depois do build

O executável precisa das pastas `app` e `runtime` ao lado dele. `run.ps1` seleciona o pacote indicado por `target/current-app.txt` ou a alternativa em `target/package/`. Se uma versão anterior estiver aberta durante o empacotamento, o novo pacote pode ir para uma pasta datada. Recompile com `./build.ps1 -AppImage` e confira a saída do script.

## Ainda há dúvida sobre um número

Abra o detalhe do ciclo ou de cache, confira o JSON original e anote: **provedor, estado da sessão, evento, campo emitido e cálculo apresentado**. Esse caminho evita atribuir ao aplicativo um número que a CLI não informou. Um exemplo reproduzível com dados não sensíveis ajuda a abrir uma issue no repositório.
