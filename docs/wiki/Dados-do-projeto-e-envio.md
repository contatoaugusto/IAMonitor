# Dados do projeto e envio ao modelo

O IAMonitor mostra a trilha observável de uma sessão de IA: o pedido entregue à CLI, comandos locais registrados, prévias das saídas desses comandos, alterações de arquivos e artefatos identificados no projeto. Abra uma sessão e escolha **Explorar dados → Dados e envio**. O relatório PDF também registra um resumo dessa trilha.

## O que cada evidência permite concluir

| Registro | O que comprova | O que não comprova |
| --- | --- | --- |
| Pedido | Texto passado pelo IAMonitor à CLI selecionada | Corpo exato de uma requisição HTTPS |
| Comando local e saída | Execução e saída relatadas pelo fluxo estruturado da CLI | Que a saída completa foi enviada ao modelo |
| Caminho citado | Referência a um arquivo em um comando | Leitura ou envio do conteúdo desse arquivo |
| Mudança de arquivo | Alteração local relatada pela CLI | Envio remoto do arquivo alterado |
| Artefato identificado | Arquivo encontrado ou classificado localmente | Uso efetivo pelo modelo ou parcela de tokens atribuída ao arquivo |
| Tokens de entrada, cache e saída | Contadores reportados pelo provedor para a operação | Composição exata por arquivo, classe ou trecho de código |

**O corpo exato enviado ao provedor não está disponível.** O IAMonitor executa a CLI e lê seus eventos; não intercepta suas requisições HTTPS. A CLI pode selecionar, resumir, truncar e combinar contexto. Por isso, a tela apresenta evidências locais e limites explícitos, sem afirmar que um arquivo inteiro saiu da máquina.

Uma saída de comando que aparece no histórico é um candidato a contexto usado pelo agente. Para demonstrar um envio exato de conteúdo, seria necessária uma instrumentação autorizada na própria CLI ou no transporte, com tratamento apropriado dos dados sensíveis. Os contadores de tokens disponíveis não permitem reconstruir esse corpo.

## Como usar em uma aula ou auditoria

1. Use um projeto didático sem segredos ou dados pessoais.
2. Registre o pedido, a versão da CLI, o provedor e a política de dados da conta utilizada.
3. Durante a execução, observe comandos de leitura, saídas e mudanças de arquivos na trilha.
4. Compare a trilha com os eventos de uso. Explique que leitura local, contexto potencial e envio confirmado são fatos diferentes.
5. Antes de compartilhar JSONL, capturas ou PDF, revise pedidos e saídas: eles podem incluir código e dados privados.

Processamento para responder à solicitação, retenção de dados e uso para treinamento são assuntos diferentes. As regras dependem do produto e das configurações da conta. Consulte os documentos oficiais aplicáveis ao ambiente usado:

- [Fluxo de chamadas de ferramentas e retorno ao modelo](https://developers.openai.com/api/docs/guides/tools-shell)
- [Cache de prompts e contadores de uso](https://developers.openai.com/api/docs/guides/prompt-caching)
- [Controles de dados da API](https://developers.openai.com/api/docs/guides/your-data)
- [Uso do Codex com planos ChatGPT](https://help.openai.com/en/articles/11369540-using-codex-with-your-chatgpt-plan)
- [Como dados podem ser usados para melhorar modelos](https://help.openai.com/en/articles/5722486-how-your-data-is-used-to-improve-model-performance/)
- [Dados de clientes empresariais](https://openai.com/business-data/)
