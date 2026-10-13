# Artefatos identificados e simulação

O bloco **Artefatos identificados** apresenta o que o IAMonitor encontrou no projeto durante a observação. Ele oferece **uma categoria por linha**, com nome completo, quantidade de arquivos, estimativa local de tokens e uma barra legível. Todas as categorias suportadas continuam visíveis mesmo quando têm **zero arquivos**; barra vazia não significa que a IA consumiu zero tokens daquela espécie de contexto.

## Categorias apresentadas

| Categoria | Como interpretar |
| --- | --- |
| Skill | Instrução ou pacote de instruções de uma skill identificado no projeto |
| Agent | Configuração ou instrução de agente identificada |
| Command | Arquivo de comando identificado em `commands/` ou com final `.command.md` |
| Spec | Especificação identificada |
| OpenSpec | Artefato do fluxo OpenSpec identificado |
| Hook | Arquivo de hook identificado |
| Instruções | Outro arquivo de orientação identificado |
| Arquivo | Arquivo observado sem categoria mais específica |

Um comando **executado** não é, por si só, um *arquivo de comando*. Se nenhum arquivo for identificado, a categoria Command permanece em zero. As categorias descrevem **arquivos locais**, não parcelas de consumo real atribuídas pelo provedor.

## Clique para examinar

Ao selecionar uma linha do gráfico, a tela abre somente os arquivos daquela categoria, com seus caminhos e estimativas individuais. Se houver conteúdo textual preservado na sessão, ele pode ser aberto. Para arquivos textuais legíveis de até 1 MB, o aplicativo guarda uma cópia local associada à execução. Quando não existe cópia, só usa a versão atual se ela permanecer na pasta e tiver o hash observado; caso contrário, explica a indisponibilidade.

Isso ajuda a reconstruir **o que foi identificado**. Não comprova que cada byte ou token do arquivo foi incluído no pedido remoto. O modelo pode receber apenas trechos, uma versão transformada, um resumo ou nenhum conteúdo daquele arquivo. Por isso a soma das estimativas **não entra** no total principal de tokens.

## Simular a retirada de origens

Em **Explorar dados**, o simulador permite retirar ou recolocar **Skills**, **Agents**, **Specs** e **Hooks** num cenário hipotético. O valor reportado pela CLI permanece intacto. O cenário desconta a estimativa de cada arquivo identificado apenas uma vez e limita o desconto à **entrada sem cache**. Origem sem arquivo identificado não gera economia presumida.

Exemplo: uma sessão com 900 tokens de entrada sem cache e um arquivo estimado em 1.100 não pode receber um desconto simulado superior a 900. Mesmo esse desconto limitado **não é uma medição de tokens evitados**, pois a CLI não informa quantos tokens daquele arquivo entraram em cada requisição. Também não é previsão de cobrança ou saldo de assinatura.

Para interpretar os valores reais da sessão, volte à [metodologia dos tokens](https://github.com/contatoaugusto/IAMonitor/wiki/Metodologia-dos-tokens). Para entender como o aplicativo registra caminhos e conteúdo, veja [Dados e privacidade](https://github.com/contatoaugusto/IAMonitor/wiki/Dados-e-privacidade).
