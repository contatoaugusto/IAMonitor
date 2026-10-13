# Design

## Context

O aplicativo já possui README, specs de comportamento e recursos de marca da aplicação. O kit externo do canal contém os logos vetoriais e uma paleta definida. O repositório público tem Wiki habilitado, mas ainda não possui páginas. A documentação deve ser publicável sem copiar `data/`, `target/` ou material pessoal do Drive.

## Goals / Non-Goals

**Goals:** manter a documentação principal versionada junto do código, com navegação no GitHub e uma cópia no Wiki; explicar números, evidências e limites sem atribuir precisão inexistente; usar a marca do canal com boa leitura em telas claras e escuras.

**Non-Goals:** alterar a UI Swing, inventar telemetria, publicar dados de sessões ou depender de arquivos do Drive em tempo de leitura do repositório.

## Decisions

1. **Fonte versionada em `docs/wiki/`.** Cada tema será um Markdown independente, com `Home.md` e `_Sidebar.md`. O README vira índice de entrada e liga para essas páginas. Alternativa: manter tudo no README, rejeitada pela extensão e dificuldade de navegação.
2. **Ativos em `docs/assets/`.** Copiar apenas o logo horizontal e o símbolo do kit de perfil, mantendo créditos em uma página de identidade visual. Usar o retrato já incluído em `src/main/resources/branding/`. Alternativa: redesenhar a marca, rejeitada porque os arquivos oficiais já existem.
3. **Visual compatível com GitHub Markdown.** Usar banner SVG, tabelas, diagramas Mermaid pequenos, tipografia padrão e blocos curtos. Markdown do GitHub não oferece CSS personalizado ao Wiki; a paleta ficará nos ativos visuais e nos diagramas, com texto legível no tema do leitor.
4. **Publicação do Wiki por GitHub Actions.** Como o transporte Git da máquina não alcança `github.com`, um workflow versionado poderá enviar `docs/wiki/` ao repositório `.wiki.git` usando o `GITHUB_TOKEN` do próprio projeto com `contents: write`. Ele não forçará a branch remota. Alternativa: editar página a página na interface web, indisponível nesta sessão e difícil de manter sincronizada.
5. **Conteúdo lastreado no código.** README, `openspec/specs/` e classes `Provider`, `EventLogic`, `Store`, `ReportPdf` e `Dashboard` servem como fontes. Os exemplos numéricos serão didáticos e declarados como exemplos, sem dados reais de sessões.

## Risks / Trade-offs

- [Wiki Git não inicializado] → executar a publicação e conferir resultado; se o GitHub exigir a primeira página criada pela interface, manter a documentação integral acessível em `docs/wiki/` e registrar a limitação sem alegar sucesso.
- [Documentação divergir do produto] → validar links, comandos e afirmações contra specs e código; revisar quando os contratos mudarem.
- [Ativos SVG não renderizarem em algum contexto do Wiki] → oferecer texto alternativo e links diretos aos arquivos no repositório.

## Migration Plan

Adicionar arquivos sem mexer em dados locais; publicar o commit no `main`; executar o workflow e conferir `Home` e links do Wiki. Reversão possível com um novo commit que restaure a documentação anterior e desabilite o workflow.
