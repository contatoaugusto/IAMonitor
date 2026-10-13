# Comece aqui

## Requisitos

- Windows para o aplicativo empacotado.
- A CLI da IA escolhida instalada e autenticada na **mesma conta do Windows** usada para abrir o IAMonitor.
- Para compilar: Java 21 ou posterior e Maven 3. O pacote Windows inclui runtime para execução.

O aplicativo oferece Codex, Claude Code, Gemini CLI, Cursor Agent e GitHub Copilot CLI. Verifique o preparo e os limites de cada uma em [Provedores e assinatura](https://github.com/contatoaugusto/IAMonitor/wiki/Provedores-e-assinatura).

## Abrir o aplicativo

No PowerShell, na raiz do projeto:

```powershell
.\run.ps1
```

`run.ps1` usa o pacote Windows selecionado em `target/current-app.txt`, com alternativa em `target/package/IAMonitor/IAMonitor.exe`. As sessões ficam em `data/` por padrão. Para escolher outra pasta de dados:

```powershell
.\run.ps1 -DataDir 'D:\Estudos\IAMonitor-data'
```

Também é possível abrir o executável do pacote diretamente, mantendo as pastas `app` e `runtime` ao lado dele.

## Primeira medição

1. Abra **Nova sessão**.
2. Escolha a IA. Leia o aviso do formulário sobre a contagem que essa CLI consegue fornecer.
3. Informe a pasta do projeto que a IA deverá usar e descreva a tarefa.
4. Clique **Executar e acompanhar**. O IAMonitor inicia **uma execução nova** da CLI nessa pasta, usando o login mantido pela própria ferramenta.
5. Acompanhe a operação em destaque, a linha do tempo e os cartões de uso. Ao terminar, abra **Explorar dados** para fontes e eventos, ou **Relatório PDF** para exportar.

O pedido é enviado à CLI selecionada. Portanto, escolha a tarefa e a pasta considerando as permissões e políticas dessa ferramenta. O IAMonitor não inicia uma tarefa apenas ao abrir o aplicativo e não recolhe sessões já executadas fora dele.

## Compilar no computador de desenvolvimento

```powershell
.\build.ps1 -AppImage
```

O script executa os testes, gera o JAR e cria o aplicativo Windows em `target/`. Sem `-AppImage`, gera o JAR. Para somente os testes, use `mvn test` quando Maven estiver no `PATH`. `build.ps1` também procura uma toolchain local em `target/toolchain/`.

## Depois da primeira sessão

- Leia [Visão geral e fluxo](https://github.com/contatoaugusto/IAMonitor/wiki/Visao-geral-e-fluxo) para não confundir ciclo de uso com chamada de rede.
- Leia [Metodologia dos tokens](https://github.com/contatoaugusto/IAMonitor/wiki/Metodologia-dos-tokens) antes de somar valores.
- Veja [Dados e privacidade](https://github.com/contatoaugusto/IAMonitor/wiki/Dados-e-privacidade) para localizar e proteger os registros locais.
