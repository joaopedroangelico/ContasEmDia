# Minhas Dívidas

App Android para cadastrar e acompanhar dívidas: cartão de crédito, contas mensais, empréstimos, financiamentos.

## Funcionalidades

- **Cadastro** com descrição, valor, vencimento, categoria e tipo de pagamento:
  - **À vista**: paga uma vez e fica quitada.
  - **Parcelada**: parcelas mensais com fim (é possível informar parcelas já pagas).
  - **Mensal fixa**: contas sem última parcela (luz, internet, assinaturas), que se renovam todo mês.
- **Lista** com o vencimento destacado: vencidas em vermelho, próximas (até 3 dias) em âmbar.
- **Marcar como paga**: parceladas e mensais fixas avançam o vencimento um mês, mantendo o dia original (31 → 28/02 → 31/03); a última parcela quita a dívida. Toda ação pode ser desfeita pela barra inferior.
- **Resumo** no topo: pendente no mês (inclui vencidas), total restante e quantidade de vencidas.
- **Filtros**: Pendentes / Pagas / Todas, por categoria, e ordenação por vencimento, valor ou nome.
- **Lembretes**: notificação diária às 9h para dívidas que vencem no dia ou em 1, 3, 5 ou 7 dias, e para as vencidas.
- **Temas**: Sistema, Lavanda, Menta, Pêssego, Oceano e Rosé. Todos seguem o modo claro/escuro do celular.

Os dados ficam só no celular (banco local Room); o app não usa internet.

## Tecnologias

Kotlin 2.1 · Jetpack Compose (Material 3) · Room · DataStore · WorkManager. Android 8.0 (API 26) ou superior.

## Estrutura

```
app/src/main/java/com/minhasdividas/app/
├── data/        modelo Divida, banco Room, preferências, formatação (R$, datas)
├── lembretes/   agendamento diário e notificações
└── ui/          telas (lista, formulário, ajustes), ViewModel e temas
```

## Compilar

Pré-requisitos: JDK 17+ e Android SDK (caminho em `local.properties`).

```bash
./gradlew testDebugUnitTest   # testes da lógica de parcelas e formatação
./gradlew assembleRelease     # APK otimizado em app/build/outputs/apk/release/
```

A versão release é assinada com a chave em `keystore/` e as senhas ficam em `keystore.properties`. **Guarde esses dois arquivos com cuidado**: sem eles não é possível publicar atualizações que instalem por cima da versão atual. Ambos estão no `.gitignore`.
