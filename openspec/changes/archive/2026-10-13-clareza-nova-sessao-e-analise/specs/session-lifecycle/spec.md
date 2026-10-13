## ADDED Requirements

### Requirement: Pedido preparado na pasta do projeto
O aplicativo MUST preencher o pedido editável com `pedido.txt` da raiz da pasta escolhida, quando puder ser lido com segurança, e MUST identificar a origem.

#### Scenario: Pasta com pedido preparado
- **WHEN** a janela abre com uma pasta que contém `pedido.txt` ou o usuário escolhe tal pasta
- **THEN** o texto aparece para revisão antes da execução

#### Scenario: Edição manual
- **WHEN** o usuário edita o pedido e altera a pasta
- **THEN** o novo arquivo não substitui o texto digitado

#### Scenario: Arquivo indisponível
- **WHEN** o arquivo está ausente, vazio, ilegível ou excede o limite
- **THEN** o pedido atual permanece e a interface informa que não carregou sugestão
