# Política de Privacidade — Contas em Dia

**Última atualização:** 9 de outubro de 2026
**Responsável:** João Pedro Angélico

## Resumo

**Seus dados ficam apenas no seu celular.** O Contas em Dia não coleta, não envia e não compartilha nenhuma informação com ninguém, nem com o desenvolvedor.

## 1. Quais informações o app guarda

O app guarda **somente o que você digita**:

- dívidas (descrição, valor, vencimento, categoria, parcelas e forma de pagamento padrão);
- histórico de pagamentos (dia e hora em que você marcou cada conta como paga, valor e forma de pagamento: PIX, boleto etc.);
- valores a receber (renda extra);
- salário mensal, se você informar;
- preferências (tema, lembretes e período do backup).

Tudo isso fica armazenado **localmente**, num banco de dados interno do app no seu aparelho.

O app **não** pede nem acessa nome, e-mail, telefone, contatos, localização, fotos, câmera, microfone ou dados bancários. Também **não** existe cadastro, conta ou login.

## 2. Nenhum dado sai do seu celular

- O app **não tem permissão de acesso à internet**. Por isso, o próprio Android impede que ele envie qualquer informação para fora do aparelho.
- **Não há** anúncios, rastreamento, estatísticas de uso (analytics) nem serviços de terceiros.
- **O desenvolvedor não tem acesso** às suas informações em nenhum momento.

## 3. Backup e troca de celular

- Os dados do app são **excluídos do backup automático na nuvem** (Google Drive).
- **Backup manual (opcional):** em *Ajustes → Backup e dados*, você pode exportar um arquivo com suas informações (do período que escolher) e salvá-lo onde quiser: na pasta Downloads, no Google Drive, enviar por um app de mensagens etc. Quem escolhe o destino e leva o arquivo até lá é **você**, pela tela padrão do Android; o app continua sem acesso à internet e não envia nada sozinho.
- O arquivo pode ser **protegido com senha** (criptografia AES-256). Sem senha, qualquer pessoa com acesso ao arquivo consegue ler o conteúdo, por isso recomendamos usar senha se for guardá-lo na nuvem ou compartilhá-lo. A senha não fica guardada em lugar nenhum e não pode ser recuperada.
- **Importar** um backup substitui os dados atuais do app pelos do arquivo.
- No Android 12 ou superior, se **você** fizer uma transferência direta de um celular para outro (por cabo ou Wi-Fi, na configuração do novo aparelho), os dados podem ser levados junto. Essa transferência é feita pelo próprio sistema, sob seu controle, sem passar por nenhum servidor do app.

## 4. Permissões usadas

| Permissão | Para quê |
|---|---|
| Notificações | Avisar sobre vencimentos próximos (lembrete diário às 9h). |
| Executar ao iniciar o aparelho | Reagendar os lembretes depois que o celular é reiniciado. |
| Manter o aparelho ativo / serviço em primeiro plano | Usadas pelo agendador de lembretes do Android (WorkManager). |
| Ver estado da rede | Usada internamente pelo agendador do Android. Não dá acesso à internet. |

As notificações são criadas no próprio aparelho. Dependendo das configurações do seu celular, elas podem aparecer na tela de bloqueio.

## 5. Como apagar seus dados

- **Excluir itens:** dentro do app, abra a dívida ou o recebimento e toque em "Excluir".
- **Apagar um mês:** em *Ajustes → Backup e dados → Apagar dados de um mês*, remova o histórico de pagamentos daquele mês (contas em aberto, parceladas e mensais fixas continuam).
- **Apagar tudo:** desinstale o app ou use *Configurações do Android → Apps → Contas em Dia → Armazenamento → Limpar dados*.

Fora os arquivos de backup que você mesmo exportar, não existe cópia fora do seu aparelho: **dados apagados sem backup não podem ser recuperados**, nem pelo desenvolvedor. Arquivos de backup que você guardou continuam onde estão até você apagá-los.

## 6. Segurança

Seus dados ficam protegidos pelos mecanismos de segurança do próprio Android: outros apps não conseguem ler o armazenamento interno do Contas em Dia. Um arquivo de backup exportado sai dessa proteção e fica sob a sua guarda: use senha se ele for para a nuvem ou para outra pessoa. Recomendamos manter um bloqueio de tela (PIN, senha ou biometria) no aparelho.

## 7. LGPD

Como o app não coleta nem transmite dados pessoais, **não há tratamento de dados pessoais pelo desenvolvedor**, nos termos da Lei Geral de Proteção de Dados (Lei nº 13.709/2018). As informações permanecem sob o seu controle exclusivo.

## 8. Alterações nesta política

Se esta política mudar, a nova versão será publicada neste repositório e no próprio app, com a data atualizada no topo.

## 9. Contato

Dúvidas: abra uma *issue* em [github.com/joaopedroangelico/minhas-despesas](https://github.com/joaopedroangelico/minhas-despesas/issues).
