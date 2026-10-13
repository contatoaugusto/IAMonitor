# Cache e evidências

No IAMonitor, **cache** significa, principalmente, *tokens de entrada lidos de um contexto que o provedor conseguiu reutilizar*. Não é uma resposta pronta armazenada pelo IAMonitor. O número de cache já está dentro da **entrada total** e participa da conta apenas uma vez.

## Como abrir a inspeção

Clique em **Cache** na equação do total, na barra de um ciclo, na coluna Cache, na origem cache de **Explorar dados** ou no guia de tokens. Ao abrir a partir de um ciclo, a inspeção seleciona o resultado correspondente. Também há uma inspeção separada para os registros de desenvolvimento em **Sobre → IA e tokens**.

## O que a tela pode comprovar

| Dado | Origem e significado |
| --- | --- |
| Entrada total, cache lido, entrada sem cache e saída | Campos de uso do resultado emitido pela CLI, decompostos sem duplicação |
| Horário e evento de origem | Registro da execução persistido em JSONL |
| JSON original do resultado | Evidência primária da contagem apresentada |
| Cache criado, quando informado pelo Claude | Metadado distinto de cache **lido**; não se soma como se fosse leitura |
| Eventos entre resultados | Contexto temporal para investigação, sem provar que seu texto entrou no cache |

É possível selecionar cada resultado de uso, abrir o evento correspondente e conferir o JSON original. Essa navegação responde **de qual registro veio a contagem**. A CLI geralmente não revela **quais tokens, linhas, prefixos ou arquivos** formaram o trecho reutilizado. Uma Skill encontrada no disco ou uma mensagem na linha do tempo não prova que integraram o cache.

## Onde o cache está fisicamente?

O cache de prompt pertence à infraestrutura do provedor. As CLIs monitoradas não fornecem ao IAMonitor um caminho físico verificável, uma relação de arquivos nem o número de bytes de RAM atribuídos à entrada em cache. Por isso a tela mostra essas informações como **indisponíveis**. Ela não apresenta um caminho local fictício.

O caminho absoluto `data/runs/<id>.jsonl` é o **registro de eventos do IAMonitor**, não um arquivo de cache do modelo. O pico de memória da sessão, quando medido, soma amostras do processo local da CLI e de seus filhos no Windows; não mede a memória do cache remoto. Sem amostra, a interface indica indisponibilidade, não zero.

## E os registros de desenvolvimento?

Em **Sobre → IA e tokens**, o aplicativo pode ler registros JSONL locais de desenvolvimento do Codex relacionados à pasta deste projeto. Ele utiliza a **última leitura cumulativa válida de cada registro** para não somar repetidamente os valores que crescem a cada linha. Esse conjunto pode incluir outros trabalhos na mesma pasta e não é a sessão selecionada na tela principal. O inspetor mostra cada caminho como registro local, nunca como localização física do cache do provedor.

## Leitura segura

Se a CLI mostrar 1.200 tokens de entrada e 300 em cache, leia **900 de entrada sem cache + 300 de cache = 1.200 de entrada total**. Isso demonstra reuso contado pelo provedor. Não demonstra a economia em dinheiro, o conteúdo exato reaproveitado ou o tamanho de um arquivo de cache. Consulte [Metodologia dos tokens](https://github.com/contatoaugusto/IAMonitor/wiki/Metodologia-dos-tokens) para somar com a saída.
