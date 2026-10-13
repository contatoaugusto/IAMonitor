# Relatórios e exportação

O botão **Relatório PDF** gera uma apresentação da sessão selecionada. Seu propósito é permitir que outra pessoa siga o trabalho e entenda **a origem e o limite de cada número**. **Mais ações → Exportar JSONL** entrega o registro bruto dos eventos emitidos pela CLI para conferência independente.

## Estrutura do PDF

| Parte | O que responde |
| --- | --- |
| Resumo | Qual tarefa, provedor, estado e intervalo foram observados? |
| Conta dos tokens | Como entrada sem cache, cache, saída e eventual diferença compõem o total reportado? |
| Fluxo lógico | Como pedido, CLI, modelo, resposta e operações se relacionam? |
| Ciclos ou fases | Qual uso foi informado por unidade observada e em que ordem? |
| Artefatos | Quais categorias e arquivos locais foram identificados, com estimativas rotuladas? |
| Recursos locais | Que picos de CPU e memória da CLI foram amostrados? |
| Cronologia | Quais eventos persistidos compõem o histórico completo? |

O gráfico de fluxo descreve **etapas lógicas**, não a quantidade de pacotes, bytes, chamadas HTTP ou a rota física dos tokens dentro do provedor. Um mesmo total mostrado em mais de uma etapa serve de orientação visual e não deve ser somado novamente. Cache é uma parte da entrada; raciocínio informado faz parte da saída.

![Primeira página de um relatório PDF de exemplo, com total, equação e fluxo lógico](https://raw.githubusercontent.com/contatoaugusto/IAMonitor/main/docs/assets/relatorio-exemplo.png)

*Prévia gerada por teste com uma sessão fictícia; caminhos e números são ilustrativos.*

Se houver resultado sem uso, execução parcial ou números divergentes, o relatório deve conservar esse estado e a evidência disponível, sem apresentar zero ou um total final fabricado. Estimativas por categoria de artefato ficam separadas do consumo reportado.

## Conferência com JSONL

O JSONL contém **um objeto JSON por linha**. Para revisar a conta, procure os eventos de resultado de uso, confira seus campos originais e compare com as parcelas do PDF. Uma saída de Claude pode, por exemplo, distinguir cache lido e criado. O inspetor da aplicação permite abrir o JSON original de um resultado sem precisar fazer a busca manual.

Antes de divulgar o PDF ou o JSONL, revise pedidos, nomes de arquivo, caminhos e respostas: são registros da sua própria tarefa. Uma sessão de teste com conteúdo não sensível é a melhor amostra para apresentação pública.

Para entender a tela da sessão, veja [Visão geral e fluxo](https://github.com/contatoaugusto/IAMonitor/wiki/Visao-geral-e-fluxo). Para a fórmula e os estados da contagem, veja [Metodologia dos tokens](https://github.com/contatoaugusto/IAMonitor/wiki/Metodologia-dos-tokens).
