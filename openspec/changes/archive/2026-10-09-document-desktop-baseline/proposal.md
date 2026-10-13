# Proposta: documentar a base desktop no OpenSpec

## Why

O IAMonitor já possui funcionalidades implementadas, mas as decisões e os contratos de comportamento estavam dispersos entre código, testes e README. Mudanças futuras precisam de um registro revisável do que foi planejado, decidido, implementado e entregue.

## What Changes

- Inicializar o OpenSpec na raiz do projeto com integração para Codex.
- Registrar o comportamento atual em specs por capacidade.
- Registrar nesta mudança as decisões de organização, as tarefas executadas e as verificações.
- Instruir contribuições futuras a usar proposta, delta de spec, design e tarefas antes de alterar capacidades.

## Capacidades

- `session-lifecycle`
- `provider-execution`
- `token-accounting`
- `artifact-observation`
- `savings-simulation`
- `presentation-and-export`

## Impacto

A mudança afeta somente documentação e fluxo de trabalho. Não altera a execução do aplicativo nem os dados pessoais em `data/`.
