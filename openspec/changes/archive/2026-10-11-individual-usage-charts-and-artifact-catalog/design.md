## Contexto

`SessionFlow` já separa os ciclos observados; a bancada os desenha como linhas de um mesmo gráfico. `WorkbenchMetrics` só agrega categorias presentes. `EventLogic` classifica Skill, Agent, Spec, OpenSpec, Hook e Arquivo, mas ainda não reconhece arquivos em diretórios de comandos.

## Decisões

- Cada ciclo recente terá um componente gráfico próprio em uma lista rolável, com o resultado mais recente no topo. Ciclos sem contagem aparecerão como cartão sem barra, sem presumir zero.
- Cada barra individual mostrará a composição proporcional daquele ciclo. O número absoluto de tokens e as parcelas permanecerão escritos no cartão para evitar comparação enganosa de comprimentos entre cartões.
- O catálogo fixo de categorias será Skill, Agent, Command, Spec, OpenSpec, Hook, Instruções e Arquivo. Categorias ausentes terão `0 arquivos` e barra vazia; isso não representa consumo zero de contexto.
- Arquivos em diretórios `commands/` ou com sufixo `.command.md` serão classificados como Command. Eventos de execução de comando continuam eventos locais e não viram artefatos de arquivo sem evidência de arquivo.
- O gráfico de artefatos distribuirá o catálogo em duas colunas para exibir todas as categorias na visão principal. O PDF usará o mesmo agregado.

## Alternativas consideradas

- Uma única barra empilhada para todos os eventos: não atende à leitura de cada ciclo.
- Atribuir tokens de execução de comando a um artefato Command: descartado porque a CLI não informa essa atribuição.
