# Objetivo e escopo

O IAMonitor foi feito como **aplicativo de estudo e demonstração**. A pessoa cria uma tarefa no aplicativo, escolhe uma CLI de IA e acompanha a execução que o próprio IAMonitor inicia. O resultado é uma narrativa verificável: quando a sessão começou, quais eventos foram emitidos, quais ciclos de uso foram informados, quais arquivos locais foram identificados e quais números podem ser auditados no JSONL.

## Perguntas que o projeto ajuda a responder

1. **O que aconteceu durante a tarefa?** A linha do tempo e o destaque da operação atual vêm dos eventos do processo iniciado.
2. **Quantos tokens a CLI informou?** O total e as parcelas aparecem por sessão e, quando a ferramenta permite, por ciclo ou resultado de uso.
3. **Quanto da entrada foi cache?** O cache lido é mostrado como parte da entrada, com acesso ao registro original e aos limites de interpretação.
4. **Que artefatos locais foram identificados?** O catálogo agrupa arquivos por tipo e abre caminhos, quantidades e estimativas locais.
5. **Como apresentar ou revisar o trabalho?** O modo de apresentação amplia a tela principal; o PDF reúne resumo, fluxo, ciclos, categorias e cronologia.

## O que é observado

| Fonte | O que pode ser mostrado | Limite |
| --- | --- | --- |
| Eventos estruturados da CLI | Operações, mensagens, resultados e contagens que ela emite | Nem toda CLI ou evento fornece todos os campos |
| Projeto local | Caminhos e texto de arquivos identificados, com estimativa local de tokens | Identificação não prova que o texto entrou no pedido do modelo |
| Processo local no Windows | Picos amostrados de CPU e memória da CLI e filhos | Não mede CPU, GPU ou RAM do servidor do provedor |
| Consulta de cota do Codex | Percentuais e janela da conta, quando disponíveis | Variação da conta pode incluir outras atividades |

## Limites que orientam a leitura

O aplicativo **não se anexa** a uma conversa já aberta no terminal ou editor. Também não intercepta o tráfego TLS, não conta pacotes ou bytes enviados ao provedor, não lê credenciais e não conhece a sequência exata de tokens que uma API manteve em cache. Um evento de mensagem visível não é uma requisição de rede; um resultado de uso pode agregar várias operações internas.

O total de tokens **não equivale automaticamente** a dinheiro, créditos ou saldo de assinatura. Uma estimativa de tokens de arquivo local também não pode ser somada ao total reportado pela CLI. Quando uma fonte não informa uso, a interface indica indisponibilidade, parcialidade, incompletude ou divergência conforme o caso. A ausência de medição nunca deve ser lida como zero.

## Para quem serve

- **Educadores e equipes:** uma visão legível da atividade em tempo real para estudo e explicação.
- **Estudantes e pesquisadores:** exemplos concretos da diferença entre contagem reportada, artefatos observados e hipóteses.
- **Desenvolvedores:** eventos JSONL, persistência SQLite, testes e specs para reproduzir e examinar o método.

O IAMonitor é um instrumento de observação do que as ferramentas **expõem**. A metodologia de cada número está em [Metodologia dos tokens](https://github.com/contatoaugusto/IAMonitor/wiki/Metodologia-dos-tokens).
