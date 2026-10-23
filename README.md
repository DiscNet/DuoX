# DuoX — Windows .exe no Android

**Status: v1.0.1 experimental (ARM64).** DuoX é um pacote Android independente de execução local de programas Windows baseado no runtime do projeto [Winlator](https://github.com/brunodev85/winlator), que reúne Wine, Box64, um servidor X e componentes gráficos. **Não é necessário instalar Termux, Wine, Winlator ou ter root.** É uma derivação atribuída, não um emulador escrito do zero.

## Como usar

1. Instale o APK gerado na aba [Releases](https://github.com/DiscNet/DuoX/releases) deste repositório.
2. Abra o DuoX e permita que conclua a instalação do ambiente Windows na primeira execução.
3. Entre em **Containers**, crie um ambiente com o botão **+** e execute-o.
4. Copie ou selecione o `.exe` no armazenamento do Android; ele geralmente fica acessível no disco **D:** do Explorador do ambiente Windows. Toque duas vezes no `.exe` para tentar abrir.

O processamento acontece no próprio aparelho. O APK integra o runtime, sem precisar de serviços externos na execução. No primeiro uso, a extração interna pode ocupar aproximadamente 2 GB ou mais.

## Limites da V1

- Android ARM64, Android 8.0+; desempenho e compatibilidade dependem de processador, memória, GPU, drivers, programa e jogo.
- Não há garantia de execução de todos os `.exe`; programas que exigem .NET, bibliotecas adicionais, anticheat ou drivers Windows podem falhar.
- Esta versão é uma **distribuição experimental com identidade DuoX** sobre uma base de compatibilidade Winlator; ainda não possui um motor de emulação desenvolvido especificamente para o DuoX.
- O APK é montado pelo GitHub Actions a partir de uma versão identificada do Winlator, sem importar binários terceiros para o histórico Git.
- Não execute arquivos `.exe` de procedência desconhecida. O Wine não é uma barreira de segurança.

## Compilação

O workflow [Android DuoX](.github/workflows/android.yml) obtém o APK público da versão `v11.2.0` do Winlator, adapta nome, ícone e identificador Android (preservando as classes originais), executa testes de regressão, verifica a estrutura do APK, assina, e publica `DuoX-v1.0.1.apk` em Releases e Artifacts.

> Para atualizar uma instalação anterior sem desinstalar, forneça uma chave de assinatura permanente nos segredos `DUOX_KEYSTORE_BASE64`, `DUOX_KEYSTORE_PASSWORD`, `DUOX_KEY_ALIAS` e `DUOX_KEY_PASSWORD`. Sem eles, a automação usa uma chave temporária de teste, que não permite atualização direta entre execuções.

## Licenças e atribuições

- Motor e código-base: [Winlator / brunodev85](https://github.com/brunodev85/winlator-app), disponibilizado sob LGPL-2.1. Esta distribuição não é afiliada ao autor original.
- Wine: [winehq.org](https://www.winehq.org/).
- Box64: [ptitSeb/box64](https://github.com/ptitSeb/box64).
- Outros componentes e respectivas licenças: Mesa, PRoot, DXVK, VKD3D e ferramentas incorporadas pelo projeto original.
- Patch de distribuição, script de marca, documentação e workflow: este repositório. As bibliotecas mantêm seus próprios avisos e licenças.

Fontes da base estão disponíveis em https://github.com/brunodev85/winlator-app .

## Testes e limitações de validação

O workflow usa Apktool **v3.0.2**, verifica a identidade `com.duoxappx` e se a atividade inicial existe sob `com.winlator.MainActivity`. O script `tests/test_rebrand.py` cobre referências relativas de componentes, a versão do pacote e a integridade dos checksums DEX após alterar literais.

**A presença dos componentes dentro do APK não é prova de compatibilidade com qualquer `.exe`.** A inicialização completa do Wine/Box64 e o desempenho precisam ser avaliados em um dispositivo ARM64 real. Até esse teste, trate esta distribuição como experimental. O código anterior do jogo continua salvo na branch `backup-arena-before-exe-app`.
