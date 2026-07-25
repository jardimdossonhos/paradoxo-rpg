# Paradoxo RPG - Contexto e Instruções do Projeto

## Resumo do Projeto
Jogo mobile de RPG de texto multiplayer sincronizado com mecânicas de relacionamento dinâmico entre NPCs, orquestrado pela API do Google Gemini 1.5 Flash como Mestre de Jogo (GM).

## Restrições do Desenvolvedor & Arquitetura
1. **Custo ZERO:** Infraestrutura 100% em Free Tiers (Firebase RTDB Spark Plan, Gemini 1.5 Flash Free Tier, FastAPI local/Render).
2. **Hardware Limitado (4GB RAM):** Proibido emulador Android local. Desenvolvimento e build feitos via CLI (`./gradlew installDebug`) e testes rodando diretamente em dispositivo físico via USB (`adb reverse tcp:8000 tcp:8000`).

## Estrutura de Camadas
- `android_app/`: Thin Client Nativo Android em Kotlin + Jetpack Compose + MVVM + Retrofit.
- `python_middleware/`: Backend em FastAPI (`main.py`) integrado ao Gemini 1.5 Flash (com Pydantic Structured Outputs) e Firebase Admin SDK.
- `firebase_schema/`: Esquema JSON de regras e mock de estado para o Realtime Database (`paradoxo-rpg`).

## Estado Atual da Configuração
- `google-services.json` configurado em `android_app/app/`.
- `serviceAccountKey.json` configurado em `python_middleware/`.
- `.env` configurado com a chave do Gemini em `python_middleware/.env`.

## Instruções para o Agente AI (Antigravity)
- Este projeto está pronto para execução autônoma.
- Sempre prefira utilizar comandos Gradle CLI para o Android.
- Mantenha a arquitetura Thin Client leve no Android e toda a inteligência/regras no Middleware Python.
