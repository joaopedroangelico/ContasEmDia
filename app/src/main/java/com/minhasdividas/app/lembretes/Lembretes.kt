package com.minhasdividas.app.lembretes

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.minhasdividas.app.MainActivity
import com.minhasdividas.app.R
import com.minhasdividas.app.data.AppDatabase
import com.minhasdividas.app.data.Divida
import com.minhasdividas.app.data.PreferenciasRepo
import com.minhasdividas.app.data.formatarMoeda
import com.minhasdividas.app.data.parcelaAtual
import com.minhasdividas.app.data.parcelada
import com.minhasdividas.app.data.textoVencimento
import com.minhasdividas.app.data.vencimento
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object Lembretes {
    const val CANAL = "vencimentos"
    private const val TRABALHO = "lembretes_diarios"

    fun criarCanal(context: Context) {
        val canal = NotificationChannelCompat.Builder(CANAL, NotificationManagerCompat.IMPORTANCE_DEFAULT)
            .setName("Vencimentos")
            .setDescription("Avisos de dívidas próximas do vencimento")
            .build()
        NotificationManagerCompat.from(context).createNotificationChannel(canal)
    }

    /**
     * Agenda uma verificação diária, começando no próximo [minutosAviso] (minutos desde a meia-noite).
     * Com [substituir], troca o agendamento existente (o usuário mudou o horário); senão, mantém o atual.
     */
    fun agendar(context: Context, minutosAviso: Int, substituir: Boolean = false) {
        val agora = LocalDateTime.now()
        var proxima = agora.toLocalDate().atTime(minutosAviso / 60, minutosAviso % 60)
        if (!proxima.isAfter(agora)) proxima = proxima.plusDays(1)

        val pedido = PeriodicWorkRequestBuilder<LembreteWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(Duration.between(agora, proxima))
            .build()
        val politica = if (substituir) ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE else ExistingPeriodicWorkPolicy.KEEP
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(TRABALHO, politica, pedido)
    }

    /**
     * Nome do toque do canal de vencimentos. O canal nasce com o toque padrão do sistema; o usuário
     * troca na tela do Android ([abrirConfiguracaoDoSom]), já que o app não pode mudar o som de um canal.
     */
    fun nomeDoSom(context: Context): String {
        val canal = NotificationManagerCompat.from(context).getNotificationChannel(CANAL)
        val som = canal?.sound ?: return "Sem som"
        if (som == Settings.System.DEFAULT_NOTIFICATION_URI) return "Padrão do sistema"
        return runCatching { RingtoneManager.getRingtone(context, som)?.getTitle(context) }.getOrNull() ?: "Personalizado"
    }

    fun abrirConfiguracaoDoSom(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .putExtra(Settings.EXTRA_CHANNEL_ID, CANAL),
        )
    }

    fun temPermissao(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun notificar(context: Context, dividas: List<Divida>, hoje: LocalDate) {
        if (!temPermissao(context)) return
        val abrirApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val gerenciador = NotificationManagerCompat.from(context)
        dividas.forEach { d ->
            val detalhe = buildString {
                append(formatarMoeda(d.valorCentavos))
                if (d.parcelada) append(" · parcela ${d.parcelaAtual}/${d.totalParcelas}")
            }
            val notificacao = NotificationCompat.Builder(context, CANAL)
                .setSmallIcon(R.drawable.ic_notificacao)
                .setContentTitle("${textoVencimento(d.vencimento, hoje)}: ${d.descricao}")
                .setContentText(detalhe)
                .setContentIntent(abrirApp)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .build()
            gerenciador.notify(d.id.toInt(), notificacao)
        }
    }
}

class LembreteWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val prefs = PreferenciasRepo(applicationContext).preferencias.first()
        if (!prefs.lembretesAtivos) return Result.success()

        val hoje = LocalDate.now()
        val limite = hoje.plusDays(prefs.diasAntecedencia.toLong()).toEpochDay()
        val dividas = AppDatabase.get(applicationContext).dividaDao().pendentesAte(limite)
        Lembretes.notificar(applicationContext, dividas, hoje)
        return Result.success()
    }
}
