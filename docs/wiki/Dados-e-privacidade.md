# Dados locais e privacidade

O IAMonitor conserva as sessões no computador, para que uma execução possa ser revisada depois de concluída. Os caminhos abaixo são relativos à pasta de dados configurada; por padrão, ela é `data/` na raiz do projeto.

| Caminho | Conteúdo |
| --- | --- |
| `data/iamonitor.sqlite` | Sessões, eventos interpretados, uso, artefatos, cópias textuais, amostras de recursos e leituras de cota disponíveis |
| `data/runs/<id>.jsonl` | Eventos originais da execução em linhas JSON separadas |
| `target/` | Resultado de build, pacote Windows e ferramentas locais; não é armazenamento de sessão |

O SQLite usa persistência local; os JSONL permitem examinar a origem de uma contagem e a sequência de eventos. A pasta `data/` é preservada por builds e ignorada pelo Git. Não a publique junto com o código, pois pedidos, caminhos, eventos e cópias de arquivos podem conter informações pessoais ou do projeto. `target/` também é ignorado pelo Git.

## O que é salvo sobre artefatos

Para arquivos textuais identificados no projeto, legíveis e com até 1 MB, o aplicativo pode guardar uma cópia do texto associada à sessão. Isso permite examinar depois a versão observada, mesmo que o arquivo original seja editado. Se não houver cópia, a versão atual do arquivo só é aberta quando seu hash corresponde ao observado. O caminho, tamanho, estimativa e hash ajudam a explicar a evidência, mas não demonstram que o texto foi enviado integralmente ao modelo.

## O que não é armazenado pelo IAMonitor

O aplicativo usa a autenticação mantida pelas próprias CLIs; ele não precisa persistir senhas ou tokens delas. Tampouco cria uma cópia local do **cache de prompt remoto**. O JSONL de eventos é um registro da execução, não o arquivo físico desse cache. Consulte [Cache e evidências](https://github.com/contatoaugusto/IAMonitor/wiki/Cache-e-evidencias).

## Ações sobre sessões

- **Zerar e executar** limpa as medições da sessão escolhida e roda o mesmo pedido novamente, após confirmação.
- **Excluir sessão** remove o registro e seus dados locais após confirmação.
- **Exportar JSONL** cria uma cópia dos eventos da sessão para análise. Revise esse arquivo antes de compartilhá-lo.
- **Relatório PDF** reúne medidas e cronologia e também pode conter detalhes sensíveis da tarefa; confira seu conteúdo antes de publicar.

Se quiser isolar um estudo, abra o aplicativo com `-DataDir` apontando para uma pasta nova. Isso facilita guardar ou remover um conjunto de medições sem tocar em outras sessões.
