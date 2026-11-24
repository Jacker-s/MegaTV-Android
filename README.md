# 🎬 Mega TV (Android Mobile & Android TV)

Aplicativo de streaming nativo moderno para Android desenvolvido em Kotlin, projetado para rodar perfeitamente tanto em **Smartphones Android** quanto em **Android TV / Smart TVs** com uma única base de código e um único arquivo APK.

---

## 📥 Download da Versão Release

- **APK Release Direto:** [MegaTV-v1.0.0.apk](https://github.com/Jacker-s/MegaTV-Android/releases/download/v1.0.0/MegaTV-v1.0.0.apk)
- **Página de Releases:** [Releases](https://github.com/Jacker-s/MegaTV-Android/releases)

---

## ✨ Recursos Principais

- **Dual-Mode Universal (Mobile & TV):**
  - **Mobile:** Interface vertical fluida, gestos de toque, cards adaptáveis e respeito integral às barras de status e navegação do sistema (Edge-to-Edge).
  - **Android TV / Smart TV:** Layout panorâmico (16:9), navegação por controle remoto (D-Pad), efeito glow e elevação em foco, menu lateral estilo Netflix e teclado virtual na tela.
- **Cache Local de Catálogo:**
  - Armazenamento em disco de todo o catálogo (Filmes VOD, Séries e Canais Ao Vivo) para inicialização instantânea e busca local offline ultrarrápida.
- **Busca Global Integrada:**
  - Pesquisa dinâmica de filmes, séries e canais com filtros por categorias.
- **Integração de APIs:**
  - Consumo da API Deli MegaEmbed via `POST` JSON com dados estruturados.
  - Consumo da API Xtream Codes via `GET` com preservação dos cabeçalhos e parâmetros de pacote.
  - Enriquecimento visual e metadados via TMDB (The Movie Database).
- **Player Avançado Media3 ExoPlayer:**
  - Suporte a HLS (m3u8), MP4 e MKV.
  - Controles por toque de brilho/volume no celular e atalhos D-Pad no controle de TV.
  - Histórico de progresso ("Continuar Assistindo") com retomada automática de onde parou.
- **Assinaturas e Pagamentos:**
  - Integração de planos com geração de QR Code Pix e verificação de status em tempo real.

---

## 🛠️ Tecnologias Utilizadas

- **Linguagem:** Kotlin
- **Build System:** Gradle (Kotlin DSL)
- **UI:** XML com ViewBinding, ConstraintLayout, Leanback TV e Material Design 3
- **Network:** Retrofit 2 + OkHttp 4 (com interceptor de pacote)
- **Images:** Coil
- **Video:** AndroidX Media3 ExoPlayer (Core, HLS, DASH, UI)
- **QR Code:** ZXing

---

## 📦 Como Compilar

1. Clone o repositório:
   ```bash
   git clone https://github.com/Jacker-s/MegaTV-Android.git
   cd MegaTV-Android
   ```
2. Compile a versão debug:
   ```bash
   ./gradlew assembleDebug
   ```
3. Compile a versão release assinada:
   ```bash
   ./gradlew assembleRelease
   ```
   O APK gerado estará em: `app/build/outputs/apk/release/app-release.apk`.
