package br.iamonitor.desktop;

final class FlowExplainer {
  private FlowExplainer() {}

  static String describe(String providerId) {
    String cli = Provider.name(providerId);
    String instructions = "codex".equals(providerId)
        ? "No Codex, instruções aplicáveis como AGENTS.md podem entrar no contexto inicial. "
            + "Skills e ferramentas ficam disponíveis conforme a configuração; seus conteúdos "
            + "e arquivos do projeto podem ser consultados ao longo da tarefa."
        : "As instruções, ferramentas e arquivos disponíveis dependem da configuração "
            + "e do comportamento desta CLI.";
    return "COMO A EXECUÇÃO FUNCIONA\n\n"
        + "1. IAMONITOR → CLI LOCAL\n"
        + "O IAMonitor inicia " + cli + " na pasta escolhida e entrega o pedido. "
        + "Ele acompanha eventos e contagens que a CLI expõe.\n\n"
        + "2. PREPARAÇÃO LOCAL\n"
        + instructions + " Isso não significa que todo o repositório, todas as specs "
        + "ou todas as skills sejam enviadas automaticamente.\n\n"
        + "3. MODELO REMOTO\n"
        + "A CLI conversa com o serviço do provedor por uma conexão protegida. "
        + "O modelo processa o contexto recebido e pode responder ou solicitar ferramentas. "
        + "A inferência do modelo ocorre no serviço remoto, não dentro do IAMonitor.\n\n"
        + "4. FERRAMENTAS E NOVAS INTERAÇÕES\n"
        + "Quando o modelo solicita uma ferramenta local, a CLI pode ler arquivos, "
        + "editar código ou executar comandos na máquina, dentro das permissões configuradas. "
        + "A saída pode alimentar uma nova interação com o modelo. Testes e builds rodam "
        + "localmente quando executados por ferramentas locais. O ciclo pode se repetir "
        + "várias vezes em uma única sessão; nem toda ferramenta é necessariamente local.\n\n"
        + "5. O QUE FOI OBSERVADO\n"
        + "Pedido entregue à CLI: confirmado pelo IAMonitor. Comandos e saídas no fluxo "
        + "estruturado: evidência local. Arquivos identificados: presença ou referência "
        + "local, sem prova de leitura. Resultados de ferramenta podem virar contexto "
        + "remoto, mas o IAMonitor não intercepta o corpo das requisições HTTPS; "
        + "não comprova quais bytes ou linhas foram enviados. Tokens reportados pela "
        + "CLI não são uma divisão por arquivo nem o saldo da assinatura.";
  }
}
