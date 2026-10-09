# Contas em Dia

App Android para cadastrar e acompanhar dívidas: cartão de crédito, contas mensais, empréstimos, financiamentos.

> Versão beta. O app se chamava **Minhas Dívidas** até a versão 1.2.1; quem já tem o app instalado recebe a atualização por cima, sem perder os dados.

## Funcionalidades

- **Cadastro** com descrição, origem opcional ("de onde é a fatura": banco, loja, empresa; em cartão de crédito, o nome do cartão, com sugestão dos já usados), valor, vencimento, categoria e tipo de pagamento:
  - **À vista**: paga uma vez e fica quitada.
  - **Parcelada**: parcelas mensais com fim (é possível informar parcelas já pagas).
  - **Mensal fixa**: contas sem última parcela (luz, internet, assinaturas), que se renovam todo mês.
- **Gavetas por categoria**: cada categoria é uma seção recolhível com resumo (pendentes, pagas, total em aberto, total do mês, próxima a vencer e vencidas). Aberta, mostra o total pendente e o botão **Pagar tudo** (ex.: todas as compras de uma fatura de cartão de uma vez), com confirmação e desfazer. Vencidas aparecem em vermelho; próximas (até 3 dias), em âmbar.
- **Forma de pagamento**: PIX, boleto, débito automático, cartão, dinheiro ou transferência. Cada conta pode ter uma forma padrão; ao pagar, ela já vem marcada e dá para trocar. O "Pagar tudo" usa a forma padrão de cada conta.
- **Histórico de pagamentos**: nas abas **Pagas** e **Todas**, as contas ficam em gavetas por mês, pelo mês em que foram pagas. Cada pagamento mostra o vencimento, a forma e o dia/hora do pagamento (em âmbar quando foi pago com atraso); tocar abre os detalhes. Em **Todas**, as contas em aberto entram no mês em que vencem, e o mês ganha um ✓ quando tudo dele foi pago.
- **Fatura por cartão**: na gaveta Cartão de crédito, as compras ficam separadas por cartão (Nubank, Inter...), cada um com a fatura em aberto e o botão **Pagar fatura**.
- **A receber**: renda extra fora do salário (freela, venda, reembolso), única ou "todo mês", numa gaveta própria.
- **Salário e sobra**: informe o salário líquido e o app mostra quanto sobra no fim do mês (salário + renda extra − contas do mês, pagas e a pagar).
- **Botão "+"** expansível com as opções "Nova dívida" e "A receber".
- **Marcar como paga/recebido**: o círculo fica verde com um check antes da lista mudar. Parceladas e mensais fixas avançam o vencimento um mês, mantendo o dia original (31 → 28/02 → 31/03); a última parcela quita a dívida. Toda ação pode ser desfeita pela barra inferior.
- **Resumo** no topo: pendente no mês (inclui vencidas), total restante e vencidas (quantidade e valor somado).
- **Filtros**: Pendentes / Pagas / Todas, por categoria, e ordenação por vencimento, valor ou nome.
- **Lembretes**: notificação diária, no horário escolhido (padrão 9h), para dívidas que vencem no dia ou em 1, 3, 5 ou 7 dias. O aviso se repete todo dia, inclusive depois do vencimento, até a conta ser paga. O som segue o padrão do sistema e pode ser trocado em Ajustes.
- **Backup**: exporta um arquivo com os dados dos últimos 3, 6 ou 12 meses, de tudo ou a partir de uma data escolhida, opcionalmente protegido com senha (AES-256), e salva onde você escolher. A importação restaura tudo a partir do arquivo.
- **Apagar um mês**: remove o histórico de pagamentos de um mês escolhido, sem mexer em contas em aberto, parceladas ou mensais fixas.
- **Armazenamento**: Ajustes mostra o espaço livre do celular, quanto o app ocupa e quanto disso são as suas informações (MB e %).
- **Temas**: Sistema, Lavanda, Menta, Pêssego, Oceano e Rosé. Todos seguem o modo claro/escuro do celular.

**Privacidade:** os dados ficam só no celular (banco local Room). O app não tem permissão de internet, não compartilha nada com ninguém e fica fora do backup na nuvem; o backup manual é um arquivo que só você cria e guarda onde quiser. Veja a [Política de Privacidade](PRIVACIDADE.md).

## Desenvolvimento futuro

- **Gráficos**: gastos por mês, divisão por categoria e renda × contas, a partir do histórico de pagamentos.
- **Integração com a agenda**: botão "Adicionar à agenda" em cada conta, abrindo o Google Agenda (ou a agenda do celular) já preenchido para você confirmar. A sincronização automática foi descartada: levaria nomes e valores das contas para fora do aparelho.

Hoje os lembretes não dependem de agenda: usam o WorkManager do Android, que guarda o agendamento no próprio celular e o refaz sozinho depois que o aparelho reinicia ou desliga de repente (o aviso chega quando o celular volta a ligar).

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
./gradlew testDebugUnitTest   # testes de parcelas, histórico por mês, backup e formatação
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
