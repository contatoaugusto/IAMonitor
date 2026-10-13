## Contexto

Os eventos da CLI informam uso por turno ou resultado e operações locais observadas. Não informam os fragmentos exatos transmitidos em cada chamada remota nem tokens consumidos por arquivo. O banco já guarda eventos, artefatos e agregados; a apresentação pode derivar gráficos sem mudar o esquema.

## Decisões

- Uma visão principal concentra operação atual, total e dois gráficos. Um segundo espaço reúne simulação e tabelas para investigação. A navegação mantém exportação e interrupção acessíveis durante a execução.
- Um modelo derivado de `Store.Detail` agrega ciclos reportados e categorias de artefatos. Os gráficos de uso se baseiam apenas em contagens auditáveis da CLI. Estimativas de arquivos ficam em gráfico separado, sem porcentagem do total reportado.
- O fluxo do PDF será lógico: pedido/contexto local → CLI → modelo remoto → resposta/operações. Setas mostram etapas conceituais; quantidades só aparecem onde foram reportadas. O desenho explica que chamadas internas e tráfego de rede não são contados.
- O PDF usa desenho vetorial com PDFBox e paginação controlada; o resumo vem antes do pedido integral e da cronologia.
- A consulta da tela mantém limite de 500 eventos para atualização frequente. A exportação lê todos os eventos persistidos em uma consulta separada, fora da thread da interface.

## Alternativas consideradas

- Atribuir tokens reportados a categorias de arquivo por proporção de tamanho: descartado porque criaria números aparentemente exatos sem evidência.
- Instrumentar rede ou API de cada provedor: fora desta mudança; exigiria outra arquitetura e poderia não abranger todos os provedores.
- Adicionar biblioteca de gráficos: o conjunto necessário é pequeno e pode ser desenhado diretamente no Swing e no PDF, sem dependência adicional.

## Riscos e mitigação

- Eventos sem uso ou histórico parcial: exibir ausência e incompletude, nunca zero presumido.
- Muitas fases e artefatos: gráficos resumem; listas completas ficam na área de investigação e no apêndice paginado do PDF.
- Telas menores: usar rolagem e tamanhos mínimos legíveis.
