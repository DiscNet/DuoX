# DuoX

Jogo de arena 2D para **duas pessoas no mesmo celular Android**, em paisagem, inspirado na dinâmica caótica de Bopl Battle. Arte, código e identidade próprios.

## Combate local
- **P1 azul** usa a metade esquerda; **P2 rosa** usa a metade direita.
- Os dois usam controles independentes, simultaneamente, por **multitoque**.
- Cada lado tem seis botões: **◀ ▶** mover, **↑** salto duplo, **●** disparo, **»** dash ofensivo, **◇** escudo.
- O dash causa dano por contato **somente durante a investida**; o escudo bloqueia projéteis e ataques de dash.
- Projéteis colidem com plataformas; há partículas de impacto, indicador de recarga, placar e rodadas.
- Primeiro jogador a marcar **5 pontos** vence. Toque para reiniciar após a vitória.
- Funciona offline; não usa anúncios nem permissões de rede.

## APK
Abra [Actions](https://github.com/DiscNet/DuoX/actions), selecione **Android APK** e execute **Run workflow**. Após a conclusão com sucesso, baixe o artefato `DuoX-debug-apk`, extraia o ZIP e instale o arquivo `app-debug.apk`.

Build local: `gradle --no-daemon :app:assembleDebug` (Android SDK + JDK 17).

## Tecnologia
Android Canvas 2D em Java, API 23+, taxa-alvo de 60 FPS, orientação horizontal. O projeto usa os arquivos **Groovy** (`build.gradle` e `settings.gradle`) para compilação; as variantes Kotlin DSL e a antiga atividade Kotlin não integram o build ativo.

**Estado:** protótipo jogável aprimorado, não cópia integral de Bopl Battle. Falta validar o APK compilado em um Android real; balanceamento, mais arenas e habilidades originais são etapas futuras.
