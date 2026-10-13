# Metodologia dos tokens

Um token é uma unidade de texto definida pelo tokenizer e pelo modelo usado pelo provedor. Não é um caractere, uma palavra, um byte de rede ou uma unidade monetária fixa. Para uma sessão, o IAMonitor prioriza **os números de uso que a própria CLI emitiu**; não tenta reconstruir a requisição remota a partir dos arquivos encontrados no projeto.

## A equação do total

Quando os campos são coerentes:

```text
entrada sem cache = entrada total − cache lido
total = entrada sem cache + cache lido + saída + outros
```

**Outros** é a diferença positiva quando a CLI informa um total maior que as parcelas detalhadas. O IAMonitor preserva o total reportado e revela essa lacuna, sem inventar sua origem. Se cache exceder entrada, raciocínio exceder saída ou parcelas excederem o total, a conta é sinalizada como divergente.

### Exemplo didático

| Campo emitido pela CLI | Tokens |
| --- | ---: |
| Entrada total | 4.709.555 |
| Cache lido, dentro da entrada | 4.635.904 |
| Saída total | 15.200 |
| Total informado | 4.724.755 |

Logo, **73.651 entrada sem cache + 4.635.904 cache + 15.200 saída = 4.724.755 tokens**. Somar novamente os 4.709.555 de entrada produziria dupla contagem. Se 2.000 tokens de raciocínio forem informados dentro dos 15.200 de saída, eles também não se somam uma segunda vez.

## O que compõe cada medida

- **Entrada:** tokens processados pelo modelo no serviço remoto, a partir do pedido e do contexto efetivamente selecionado pela ferramenta. Uma sessão longa pode reenviar contexto em novos turnos e contá-lo novamente.
- **Cache lido:** parte da entrada que o provedor reaproveitou. É uma contagem de uso, não uma lista de linhas ou arquivos identificáveis. [Inspecione a evidência disponível.](https://github.com/contatoaugusto/IAMonitor/wiki/Cache-e-evidencias)
- **Saída:** tokens produzidos pelo modelo, inclusive conteúdo de chamadas de ferramenta e, quando separado pelo provedor, raciocínio interno. A resposta visível pode ser menor.
- **Total:** valor reportado pela CLI ou uma soma auditável dos resultados que ela expôs. Não é uma medição de bytes transferidos.

## Estado da contagem

| Estado | Significado |
| --- | --- |
| Final | Execução concluída com resultados de uso e parcelas coerentes |
| Parcial | Ainda em curso ou interrompida; novos resultados podem aparecer |
| Incompleto (≥) | Há resultados sem uso; o valor conhecido é apenas um mínimo |
| Indisponível (—) | A CLI não forneceu contagem utilizável |
| Divergência (—) | Campos reportados não fecham uma conta coerente |
| Histórico sem eventos (—) | Um registro antigo com total isolado não permite auditoria por evento |

## Três números que não devem ser misturados

1. **Tokens da operação:** uso emitido pela CLI para a nova execução iniciada pelo IAMonitor.
2. **Estimativa de artefato:** cálculo local para o texto de um arquivo identificado; ele pode não ter sido enviado ao modelo, integral ou parcialmente.
3. **Cota da assinatura:** percentual ou janela da conta obtido de uma fonte oficial quando disponível. Pode incluir outras atividades e não traduz diretamente os tokens da operação em cobrança.

No guia **Como cada IA conta**, o aplicativo também compara um texto isolado. Para OpenAI, usa localmente `o200k_base` via JTokkit; para Gemini, apresenta aproximação por caracteres; para Claude, Cursor e Copilot, o valor comparativo não é o tokenizer oficial dessas ferramentas. Esse simulador não envia o texto pela rede e **não altera a sessão**. Consulte [Provedores e assinatura](https://github.com/contatoaugusto/IAMonitor/wiki/Provedores-e-assinatura) para o alcance de cada integração.
