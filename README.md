# Minhas Dívidas

App Android para cadastrar e acompanhar dívidas: cartão de crédito, contas mensais, empréstimos, financiamentos.

## Funcionalidades

- **Cadastro** com descrição, valor, vencimento, categoria e tipo de pagamento:
  - **À vista**: paga uma vez e fica quitada.
  - **Parcelada**: parcelas mensais com fim (é possível informar parcelas já pagas).
  - **Mensal fixa**: contas sem última parcela (luz, internet, assinaturas), que se renovam todo mês.
- **Gavetas por categoria**: cada categoria é uma seção recolhível com resumo (pendentes, pagas no mês, total em aberto, próxima a vencer e vencidas). Vencidas aparecem em vermelho; próximas (até 3 dias), em âmbar.
- **A receber**: renda extra fora do salário (freela, venda, reembolso), única ou "todo mês", numa gaveta própria.
- **Salário e sobra**: informe o salário líquido e o app mostra quanto sobra no fim do mês (salário + renda extra − contas do mês, pagas e a pagar).
- **Botão "+"** expansível com as opções "Nova dívida" e "A receber".
- **Marcar como paga/recebido**: o círculo fica verde com um check antes da lista mudar. Parceladas e mensais fixas avançam o vencimento um mês, mantendo o dia original (31 → 28/02 → 31/03); a última parcela quita a dívida. Toda ação pode ser desfeita pela barra inferior.
- **Resumo** no topo: pendente no mês (inclui vencidas), total restante e quantidade de vencidas.
- **Filtros**: Pendentes / Pagas / Todas, por categoria, e ordenação por vencimento, valor ou nome.
- **Lembretes**: notificação diária às 9h para dívidas que vencem no dia ou em 1, 3, 5 ou 7 dias, e para as vencidas.
- **Temas**: Sistema, Lavanda, Menta, Pêssego, Oceano e Rosé. Todos seguem o modo claro/escuro do celular.

**Privacidade:** os dados ficam só no celular (banco local Room). O app não tem permissão de internet, não compartilha nada com ninguém e fica fora do backup na nuvem. Veja a [Política de Privacidade](PRIVACIDADE.md).

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

## Licença

Copyright © 2026 João Pedro Angélico. **Todos os direitos reservados.** Uso pessoal permitido; cópia, modificação, redistribuição ou uso comercial dependem de autorização do autor. Veja o arquivo [LICENSE](LICENSE).

## Auxiliar

Projeto idealizado por João Pedro Angélico e desenvolvido com o auxílio do **Claude** (IA da Anthropic), usando o Claude Code no VS Code. O Claude:

- escreveu o código do app (Kotlin + Jetpack Compose), os testes automáticos e este README;
- testou cada versão em um emulador Android, conferindo as telas nos modos claro e escuro;
- instalou e testou o app diretamente no smartphone (POCO F3) via cabo USB, incluindo as migrações do banco de dados sem perda dos dados já cadastrados;
- publicou o código e as versões (Releases) neste repositório.
