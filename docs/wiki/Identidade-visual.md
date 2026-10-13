# Identidade visual

Esta documentação acompanha o canal **Dev Terceira Idade**, apresentado por **Antonio Augusto**, com a assinatura **“Do DOS ao deploy com IA”**. O IAMonitor é uma ferramenta de estudo ligada a esse universo; as páginas usam o símbolo de terminal, a paleta do kit de perfil e uma apresentação clara para leitura em tela e na documentação.

<p align="center"><img src="https://raw.githubusercontent.com/contatoaugusto/IAMonitor/main/docs/assets/logo-horizontal.svg" alt="Logo horizontal Dev Terceira Idade" width="680"></p>

## Paleta

| Papel | Cor | Uso |
| --- | --- | --- |
| Base | `#2E4057` · azul ardósia | Fundos do banner, diagramas e áreas de destaque |
| Acento | `#F2A541` · âmbar terminal | Borda, marcador e referência ao terminal |
| Detalhe | `#3DDC97` · verde | Sinal secundário de código e progresso |
| Claro | `#F4F5F7` · cinza claro | Texto e superfícies claras |

O âmbar aparece como acento pontual. Diagramas usam contraste forte e rótulos completos; o corpo do Wiki respeita o tema claro ou escuro que o leitor escolher no GitHub.

## Tipografia e recursos

- **Títulos e elementos de terminal:** JetBrains Mono ou IBM Plex Mono, com fallback monoespaçado.
- **Texto corrido:** Inter, Poppins ou Arial, com fallback sem serifa.
- **Logo horizontal oficial:** [`docs/assets/logo-horizontal.svg`](https://github.com/contatoaugusto/IAMonitor/blob/main/docs/assets/logo-horizontal.svg).
- **Símbolo oficial:** [`docs/assets/logo-avatar.svg`](https://github.com/contatoaugusto/IAMonitor/blob/main/docs/assets/logo-avatar.svg).
- **Capa da documentação:** [`docs/assets/banner-iamonitor.png`](https://github.com/contatoaugusto/IAMonitor/blob/main/docs/assets/banner-iamonitor.png), composição derivada da paleta e do símbolo para o produto.
- **Retrato já usado no aplicativo:** [`src/main/resources/branding/antonio-augusto.png`](https://github.com/contatoaugusto/IAMonitor/blob/main/src/main/resources/branding/antonio-augusto.png).

Os dois logos foram copiados sem redesenho do **kit de perfil e apresentação — Dev Terceira Idade** localizado no projeto de marca do canal. A foto já fazia parte dos recursos do aplicativo. O arquivo SVG é a fonte editável; o texto alternativo mantém o significado quando a imagem não carrega.

## Aplicação no IAMonitor

O banner de entrada traz a marca do canal e o nome do produto. Cada página usa títulos curtos, tabelas legíveis e diagramas com as cores acima. O Wiki do GitHub não aceita uma folha de estilos própria para toda a interface; por isso o tratamento visual fica nos ativos e no conteúdo, sem comprometer acessibilidade ou o tema escolhido pelo leitor.

Para personalizar o cabeçalho do aplicativo desktop, os textos e a foto padrão estão em `src/main/resources/`. As variáveis `IAMONITOR_CHANNEL_NAME`, `IAMONITOR_CHANNEL_SUBTITLE` e `IAMONITOR_CHANNEL_LOGO` podem substituí-los em uma instalação local.
