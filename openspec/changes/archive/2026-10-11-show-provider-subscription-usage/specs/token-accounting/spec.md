## ADDED Requirements

### Requirement: Separação entre operação e cota da conta
O aplicativo MUST distinguir tokens reportados para a operação de percentuais de cota da conta e MUST descrever a variação como movimento observado na conta.

#### Scenario: Leituras comparáveis
- **WHEN** leituras antes e depois do Codex têm o mesmo limite, duração e instante de renovação
- **THEN** o painel mostra os dois percentuais e a diferença em pontos percentuais, avisando que outras atividades da conta podem contribuir

#### Scenario: Janela renovada ou leitura ausente
- **WHEN** a janela muda ou uma leitura falha
- **THEN** a variação aparece como indisponível, sem subtrair percentuais incompatíveis
