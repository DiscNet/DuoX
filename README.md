# DuoX

Jogo original de combate de gosminhas para **duas pessoas no mesmo celular**. Inspirado nas mecânicas de arena de Bopl Battle, sem utilizar seus assets, personagens ou código.

## Como jogar
- Gire o celular na horizontal.
- **Player 1 (azul)** usa os botões na metade esquerda.
- **Player 2 (rosa)** usa os botões na metade direita.
- Cada lado tem direção, pulo, tiro, impulso e escudo.
- Tiros eliminam o adversário se ele não estiver protegido. Primeiro a **5 pontos** vence.
- Há plataformas, gravidade, colisões, respawn por rodada e recarga de habilidades.
- Toque em **REINICIAR** ao final da partida para jogar novamente.

## APK
Abra **Actions → Android APK → Run workflow** e baixe o artefato **DuoX-debug-apk** após a compilação, ou execute `gradle :app:assembleDebug` com Android SDK e Java 17 instalados. O APK estará em `app/build/outputs/apk/debug/`.

## Projeto
Android nativo, Kotlin, Canvas 2D, multitouch independente, sem permissões de rede nem dependências de runtime. Requer Android 6.0+.

**Estado:** protótipo jogável inicial; não é uma reprodução 1:1 de Bopl Battle.
