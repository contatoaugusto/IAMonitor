# Desenho

## Contexto

`WorkbenchCharts.CycleCard` usa rótulos de 10 pontos, barras de 8 pixels e percentuais em cada linha. `Dashboard` reserva 63 pixels para três cartões de Entrada, Saída e Cache, embora essas medidas apareçam no gráfico. A área de gráficos perde altura quando a execução exibe operação e eventos.

## Objetivos e limites

- Priorizar leitura rápida do número de tokens por medida, inclusive durante apresentações.
- Conservar um cartão por ciclo, o total reportado e o tratamento de uso ausente.
- Não alterar cálculos, persistência ou relatório PDF.

## Decisões

- Em cada linha, colocar nome e valor em fonte maior sobre uma barra espessa de largura útil do cartão. A barra continua proporcional ao total, mas a interface não imprime percentuais. Alternativa: manter a linha compacta e apenas aumentar a fonte; ainda deixaria a barra curta e o gráfico apertado.
- Exibir Entrada, Cache e Saída para qualquer ciclo com uso. Raciocínio e Outros aparecem apenas quando positivos. Cache e Raciocínio recebem recuo discreto e a explicação de subconjuntos fica em dica de ferramenta e no resumo já existente. Alternativa: manter todas as linhas opcionais vazias; adicionaria ruído sem informar consumo.
- Remover os três cartões redundantes de `Dashboard`, preservando o painel de total e a equação da CLI. A altura liberada amplia a área central dos gráficos também durante execução. Alternativa: aumentar a altura da janela; não resolveria a duplicação e prejudicaria telas menores.

## Riscos e compensações

- Um ciclo com cinco medidas ocupa mais altura. → Manter a lista rolável e o ciclo mais recente no topo.
- Sem percentuais, a proporção exata exige cálculo. → O valor absoluto em tokens é a medida pedida; o comprimento da barra conserva a comparação visual.
