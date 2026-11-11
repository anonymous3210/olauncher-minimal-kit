# Checklist de auditoria — Olauncher Minimal Kit

Base: branch `main`, commit `3f45728`. As correções abaixo foram feitas apenas na cópia local de trabalho; não foram enviadas ao GitHub.

## Correções aplicadas

- [x] **Descrição do filtro** — a seção agora diz que o efeito altera os elementos Compose da tela inicial, e não o papel de parede do sistema. Isso corrige a promessa incorreta; o wallpaper continua sendo controlado pelo Android.
- [x] **Lista de apps após retornar ao launcher** — `MainActivity` atualiza o cache em retornos subsequentes ao primeiro `onResume`, cobrindo o retorno de fluxos de instalação/remoção sem duplicar a carga inicial.
- [x] **Retry do wallpaper legado** — `WallpaperWorker` só grava o URL como aplicado depois de `setWallpaper()` retornar sucesso, permitindo nova tentativa após falha.
- [x] **Controles de mídia** — `play`, `pause`, `next` e `previous` agora toleram exceções de sessão inválida em vez de derrubar o fluxo de clique.
- [x] **Memória da capa de álbum** — a imagem mantida pela UI é reduzida a no máximo 512 px no maior lado. A bitmap original ainda pode existir temporariamente nos metadados fornecidos pelo app de mídia.
- [x] **Blur compatível por versão** — Android 12+ preserva o caminho público existente, sujeito à disponibilidade reportada pelo sistema. Em Samsung anterior ao Android 12, o app tenta por reflexão o método interno `semAddExtensionFlags(0x10)` e o `FLAG_BLUR_BEHIND`; se fabricante/API/reflection não corresponderem, mantém o fallback translúcido. A extensão Samsung não é API pública estável e não foi testada em dispositivo Samsung.

## Ainda precisa de validação

- [ ] **Tela verde intermitente** — nenhuma falha grave nos caminhos examinados foi confirmada como causa. Reproduzir no Samsung, registrar modelo/Android/One UI, versão e origem do APK, tela em que ocorre e ação anterior. Comparar captura de tela com o painel ajuda a triagem, mas não é diagnóstico definitivo.
- [ ] **Testes/build Android** — não executados: ambiente sem Android SDK (`ANDROID_HOME`/`sdk.dir`).
- [ ] **Blur Samsung Android 11** — confirmar em hardware compatível se a extensão proprietária funciona, se precisa de radius/ajuste adicional e se limpa corretamente ao fechar a gaveta. Reflexão captura falha e cai para fallback, mas a compatibilidade deve ser validada em modelos/firmwares reais.
- [ ] **Worker legado** — o fluxo Compose atual cancela o trabalho ao iniciar (`MainActivity`), mas o código corrigido no módulo legado ainda deve ser validado se essa UI antiga voltar a ser ativada.

## Auditoria estática

- [x] Confirmado que `MainActivity` inicia `MinimalLauncher`.
- [x] Inspecionados módulos ativo e legado, manifesto, acessibilidade, listener de mídia, tarefas de wallpaper, preferências e efeitos Android encontrados.
- [x] Nenhuma evidência de captura/transmissão da tela ou controle irrestrito do aparelho nos fontes examinados.
- [x] Nenhum bug crítico/alto confirmado que explique a tela verde; isso não descarta uma interação específica com firmware/hardware Samsung.
