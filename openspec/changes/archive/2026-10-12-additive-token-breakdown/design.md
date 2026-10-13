# Desenho

## Contexto

`TokenAccounting` já valida que cache não excede entrada e que raciocínio não excede saída. Sua equação usa Entrada total + Saída. `WorkbenchCharts.CycleCard` mostra Entrada total, Cache, Saída e Raciocínio em barras separadas, exigindo que a pessoa saiba quais barras se sobrepõem.

## Objetivos e limites

- Fazer cada barra e termo da equação representar uma parcela exclusiva do total reportado.
- Distinguir contagem observada de cobrança e cota da assinatura.
- Preservar os valores originais da CLI, a persistência e a consulta oficial de cota.

## Decisões

- Centralizar a validação e a decomposição em `TokenAccounting.Parts`, usada na equação, no gráfico e nas barras do PDF. Entrada sem cache = entrada total − cache; Outros = total − entrada total − saída. As quatro parcelas somam o total. Alternativa: duplicar subtrações em apresentação e contabilidade; permitiria divergência futura entre superfícies.
- Mostrar apenas as quatro parcelas exclusivas no gráfico. Raciocínio permanece documentado em dica de ferramenta e na explicação detalhada da contagem, sempre dentro da saída. Alternativa: manter uma barra adicional recuada; continuaria parecendo parte da soma para quem lê rapidamente.
- Exibir no cabeçalho do total que a contagem da CLI não é cobrança. A variação da cota, quando disponível, continua no quadro de assinatura; nenhuma tarifa ou custo é inferido dos tokens. Alternativa: estimar preço de API para uma sessão de assinatura; misturaria regimes de cobrança e criaria um número enganoso.
- Se os valores de um ciclo violarem as relações de subconjunto ou o total, o cartão mostra divergência sem barras. A contabilidade da sessão já sinaliza divergência no agregado.

## Riscos e compensações

- A entrada total deixará de ser uma barra visível. → A equação e os dados detalhados preservam o valor reportado; o gráfico usa as partes somáveis.
- Texto da equação pode ficar longo com números grandes. → Manter o texto em uma linha na área larga do painel e revisar a prévia; PDF e guia já aceitam texto corrido.
