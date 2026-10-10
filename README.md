# DuoX

Protótipo original de duelo de gosminhas para **2 jogadores no mesmo celular Android**, em modo paisagem. Inspirado no gênero de batalhas de arena de Bopl Battle, sem utilizar personagens, arte ou código do jogo original.

## Jogabilidade

- **P1 (azul):** botões da metade esquerda
- **P2 (rosa):** botões da metade direita
- **◀ ▶** movimentar · **↑** pular · **●** atirar · **»** dash
- Plataformas com gravidade, disparos letais, recarga, partículas e escudo breve durante o dash
- Rodadas automáticas: o primeiro a **5 pontos** vence; toque para reiniciar

## APK de desenvolvimento

Acesse **Actions → Android APK → Run workflow** em https://github.com/DiscNet/DuoX/actions. Ao finalizar com sucesso, baixe o artefato **DuoX-debug-apk**.

Para compilar localmente, instale JDK 17, Android SDK e Gradle 8.10.2 e execute `gradle :app:assembleDebug`. O arquivo gerado ficará em `app/build/outputs/apk/debug/app-debug.apk`.

Código nativo **Java**, Android Canvas e controles multitoque. **Android 6.0+**. O projeto ainda não foi validado em aparelhos físicos e não é um clone fiel 1:1 de Bopl Battle.
