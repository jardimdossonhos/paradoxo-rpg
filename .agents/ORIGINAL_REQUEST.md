# Original User Request

## Initial Request — 2026-06-29T01:07:35Z

<USER_REQUEST>
# Teamwork Project Prompt — Draft

> Status: Launched
> Goal: Craft prompt → get user approval → delegate to teamwork_preview

Desenvolver a evolução arquitetural e visual de um RPG de Texto com IA (estilo Nomi), integrando Firebase Auth, Firebase Storage, e refatoração da UI em Jetpack Compose com Navigation Drawer, além de Lores com missões ocultas no backend Python.

Working directory: c:\Users\Dell\.gemini\antigravity\scratch\rpg_game
Integrity mode: development

## Requirements

### R1. Backend Python & Banco de Dados
- Refatorar o schema do Firebase RTDB para ser isolado por usuário (`/users/{uid}/...`).
- Implementar validação de JWT via Firebase Admin Auth nas rotas.
- Injetar as diretrizes secretas da Lore no `system_instruction` do Gemini.
- Criar rotas para geração de imagem nativa (via Gemini) com upload para o Firebase Storage.

### R2. Interface Android (Jetpack Compose)
- Implementar Google Sign-In via Credential Manager API.
- Refatorar a navegação para usar um Menu Lateral (Navigation Drawer) ao invés de Bottom Navigation.
- Criar a tela Home contendo a lista de salas ativas e um FAB (+).
- Criar o Wizard de Criação de Personagens (Gerar Arte -> Aprovar -> Preencher Ficha -> Salvar).

## Acceptance Criteria

### Backend & Autenticação
- [ ] O backend python inicia sem erros na porta 8000.
- [ ] As rotas de criação de imagem via Gemini funcionam (retornam URL mockada se a API real falhar ou não estiver com quota, mas o código de integração com Storage deve existir).
- [ ] Requisições com Token JWT falso ou ausente recebem status HTTP 401 Unauthorized.

### Android App
- [ ] O comando `.\gradlew.bat build` compila o projeto sem erros de sintaxe ou lint.
- [ ] O Navigation Drawer abre e contém opções para "Minhas Salas", "Meus Personagens", "Lores e Campanhas", e "Perfil".
- [ ] O fluxo do Wizard de Personagem navega corretamente pelas telas de Geração de Arte -> Preenchimento de Ficha.

---
*Next: Acompanhar o progresso da equipe de agentes.*
</USER_REQUEST>
