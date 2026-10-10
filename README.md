# DuoX

Arena 2D de combate entre duas gosminhas, feita para **2 pessoas no mesmo celular Android**, em tela horizontal. Inspirada no ritmo de jogos como Bopl Battle; código, personagens e gráficos são próprios.

## Como jogar
- **Player 1 (azul):** controles na metade esquerda da tela.
- **Player 2 (rosa):** controles na metade direita da tela.
- Ambos podem jogar simultaneamente usando multitoque.
- **◀ ▶** mover; **↑** pular e pular novamente no ar; **●** atirar (segure para tiros sucessivos); **»** dash (também causa dano no contato); **◇** escudo temporário.
- Elimine o rival com tiros, colisão durante o dash ou derrubando-o da arena. O primeiro a atingir **5 pontos** vence.
- As habilidades têm recargas, mostradas por arcos nos botões. Para recomeçar, toque no centro ao final da partida.

## Gerar o APK
1. Abra a aba [Actions](https://github.com/DiscNet/DuoX/actions) do repositório.
2. Selecione **Android APK**, e use **Run workflow** (ou aguarde o build iniciado por um push em `main`).
3. Após um build bem-sucedido, abra a execução e baixe o artefato **DuoX-debug-apk**.
4. Extraia o ZIP e instale `app-debug.apk` no Android.

Build local: `gradle --no-daemon :app:assembleDebug`, com JDK 17 e Android SDK instalados.

## Stack
- Kotlin + Android Canvas 2D, atualização de 60 FPS (alvo), multitoque por identificador de dedo.
- Sem conexão à internet, serviços externos nem dependências de jogos.
- Android 6.0+ (API 23), modo paisagem.

**Status:** protótipo jogável. Ainda não reproduz todas as habilidades, arenas, animações, balanceamento e opções de Bopl Battle. O APK depende da compilação bem-sucedida no GitHub Actions ou ambiente Android local.
