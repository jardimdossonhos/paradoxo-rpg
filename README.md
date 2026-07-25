# Paradoxo RPG

Um jogo mobile de RPG Textual Multiplayer guiado por IA, onde os jogadores constroem a narrativa junto com o sistema (Game Master), criam personagens dinâmicos e se aventuram em Lores (Campanhas) personalizáveis. Toda a infraestrutura foi projetada visando **Custo Zero** utilizando Free Tiers de serviços na nuvem (Firebase, Gemini 1.5 Flash, e hospedagem de backend).

---

## 📖 História do Projeto e Concepção
O projeto nasceu da necessidade de criar um RPG de texto robusto, moderno e imersivo, similar a jogos narrativos impulsionados por Inteligência Artificial (como o Nomi), mas focado no cooperativo (salas multiplayer) e personalização profunda (criação de Lore).

**Principais decisões arquiteturais tomadas durante o desenvolvimento:**
1. **Thin Client Nativo (Android):** O cliente mobile foi construído com Kotlin e Jetpack Compose (MVVM). Ele age apenas como uma "casca visual" (Thin Client) para evitar sobrecarga de hardware no celular do usuário e evitar vazamento de regras de negócios. 
2. **Middleware Inteligente (Python + FastAPI):** Para manter o controle absoluto das regras (XP, Lores ocultas, interações com NPCs, prompts seguros) e proteger a chave da API da IA (evitando engenharia reversa do APK), foi construído um backend em Python.
3. **Mestre de Jogo como IA (Gemini 1.5 Flash):** O Gemini foi escolhido pelo seu nível de *Free Tier* agressivo e excelente suporte a *Structured Outputs* (JSON). Ele processa a narrativa, as ações dos jogadores e atualiza a matriz de sentimentos dos NPCs na sala.
4. **Resiliência e Rotação de Chaves:** Como as APIs gratuitas sofrem com erros "HTTP 429 - Quota Exceeded", a arquitetura do Middleware foi atualizada (Fase 4) para rotacionar dinamicamente múltiplas chaves (API Keys) cadastradas no `.env`.
5. **Autenticação Real (Firebase):** Substituímos o estado mockado inicial por um login genuíno do Google (via Credential Manager e Firebase Auth), garantindo a persistência do progresso, personagens e salas de cada jogador.

---

## 🏛️ Arquitetura do Sistema

O sistema é dividido em duas camadas principais que se comunicam através de tokens JWT do Firebase:

### 1. `android_app/` (Frontend)
- **Tecnologias:** Kotlin, Jetpack Compose, Retrofit, Coroutines, StateFlow, Material 3.
- **Navegação:** Migrada de um BottomNavigation básico para um **ModalNavigationDrawer** (Menu Lateral) mais expansivo, com FABs (Floating Action Buttons) dinâmicos. As sub-telas (como sessão do jogo e detalhes) escondem o menu para garantir imersão.
- **Gestão de Estado:** Usamos `RpgRepository` como Single Source of Truth, cacheando o estado recebido via chamadas Retrofit (REST) e distribuindo para as ViewModels.

### 2. `python_middleware/` (Backend / Brain)
- **Tecnologias:** Python 3.11, FastAPI, Uvicorn, Firebase Admin SDK, Google GenAI SDK.
- **Segurança:** A rota exige `Bearer Token`. O servidor valida o JWT direto com o Google (Firebase Auth).
- **Banco de Dados (Firebase RTDB):** O backend faz todo o CRUD seguro das rotas `/characters`, `/rooms`, `/lores` isolando o acesso por `uid`.
- **Geração de Imagens:** O endpoint `/generate-image` intercepta a chamada de IA. Em caso de restrição de quota (Erro 404/429 na API Imagen-3.0), foi implementado o bypass para a **Pollinations.ai**, garantindo retorno instantâneo e livre de imagem sem tela branca.

---

## ✅ O Que Já Foi Feito (Status Atual - Final da Fase 4)

- [x] **Login com Google** (via Credential Manager + Firebase Auth).
- [x] **Backend RESTful Python** integrando Firebase Realtime Database.
- [x] **Wizard de Criação de Personagens** multi-passo (com geração de arte em tempo real).
- [x] **Gestão Completa de Lores/Campanhas**, onde o usuário descreve a narrativa base (contexto invisível para ser passado ao Gemini).
- [x] **Iniciação de Sessão Rápida:** Botão para iniciar automaticamente uma Sala a partir de uma Lore.
- [x] **Gestão de Salas:** Visualização de jogadores (1/4), tela de administração e possibilidade do criador (AdminUid) deletar a sala.
- [x] **Sistema de Experiência e Nível:** O backend injeta regras no Gemini para analisar ações bem sucedidas e incrementar o XP e o Nível dos personagens. Visível no Android via selos de LVL e pontos.
- [x] **Diálogo Pré-Sessão (Identificação):** Antes de enviar interações no chat de uma sala, a estrutura prepara a identificação do seu personagem autoral.
- [x] **Bugs Corrigidos:** Salvamento múltiplo evitado com desabilitação de botões; Identidade e mock residual (Garrick e Aragorn) removidos.

---

## 🚧 O Que Falta e Próximos Passos (To-Do List Histórico)

1. **Atualização Visual (VFX) no Android:**
   - Adicionar mais micro-animações (como barras de XP subindo dinamicamente, "typing indicator" quando a IA está escrevendo o próximo turno).
2. **Canais WebSockets (Tempo Real):**
   - Atualmente a leitura da sala usa Fetch (polling) ou necessita pull-to-refresh no chat. O próximo passo de infraestrutura é conectar o Android diretamente a um `Listener` WebSocket do Firebase RTDB para o chat aparecer simultaneamente na tela de todos sem precisar recarregar.
3. **Múltiplos Jogadores de Fato (Party UI):**
   - Na GameSessionScreen, mostrar quem está online, o ping (status) dos outros jogadores, e diferenciar as bolhas de chat de outros humanos x narrativa do GM (IA).
4. **Gerenciamento de Inventário:**
   - O backend já processa (`inventory` na matriz), mas o app Android ainda precisa de uma UI (Aba extra no CharacterDetailScreen) para mostrar Mochila, Itens Equipados e Artefatos.
5. **Hospedagem em Nuvem:**
   - Atualmente rodando via localhost (`adb reverse tcp:8000`). Para produção real, fazer o deploy do Middleware no **Render.com** (Free Tier) e alterar a `BASE_URL` no Retrofit do Kotlin.

---

## 🔧 Como Executar o Projeto Localmente

1. **Backend (Python):**
   ```bash
   cd python_middleware
   pip install -r requirements.txt
   uvicorn main:app --host 0.0.0.0 --port 8000 --reload
   ```

2. **Habilitar conexão Android via USB (Se testando em dispositivo físico):**
   - Execute no terminal (com celular conectado e depuração ativa):
   ```bash
   adb reverse tcp:8000 tcp:8000
   ```

3. **Frontend (Android):**
   - Abra o `android_app` no Android Studio.
   - Execute `Shift+F10` (Run 'app') ou instale via terminal:
   ```bash
   cd android_app
   ./gradlew installDebug
   ```

---
*Projeto versionado e estruturado para continuidade no GitHub.*
