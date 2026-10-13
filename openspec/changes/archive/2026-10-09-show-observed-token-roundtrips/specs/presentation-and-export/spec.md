# Spec Delta

## MODIFIED Requirements

### Requirement: Explicação do ciclo de tokens
O aplicativo MUST mostrar como pedido, contexto, CLI local, serviço remoto e resposta se relacionam com as contagens da sessão, e MUST distinguir ciclos de uso observados de chamadas de rede não observáveis.

#### Scenario: Abertura do guia
- **WHEN** o usuário abre Como são medidos os tokens
- **THEN** o fluxo distingue processamento local, envio, modelo remoto e retorno, sem somar o mesmo total repetido nas etapas

#### Scenario: Vários ciclos reportados
- **WHEN** a sessão contém mais de um turno ou resultado de uso concluído
- **THEN** o guia mostra uma faixa por unidade reportada com entrada, cache, saída e total, em ordem, e deixa claro que o pedido pode ter percorrido o caminho mais de uma vez

#### Scenario: Somente resultado agregado
- **WHEN** a CLI informa apenas um resultado final que pode reunir chamadas internas
- **THEN** o guia mostra uma unidade reportada e indica que a quantidade de requisições internas ou fragmentos de resposta é desconhecida

#### Scenario: Eventos de mensagem visíveis
- **WHEN** a CLI registra vários eventos de mensagem entre resultados de uso
- **THEN** o guia mostra a quantidade desses eventos no intervalo e não os apresenta como número de pacotes de rede
#### Scenario: Uso ausente ou turno pendente
- **WHEN** há conclusão sem contagem ou início sem conclusão
- **THEN** o guia mostra a unidade sem presumir zero tokens nem uma chamada remota concluída
