package com.minhasdividas.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

enum class Tema(val rotulo: String) {
    SISTEMA("Sistema"),
    LAVANDA("Lavanda"),
    MENTA("Menta"),
    PESSEGO("Pêssego"),
    OCEANO("Oceano"),
    ROSE("Rosé"),
}

data class Preferencias(
    val tema: Tema = Tema.SISTEMA,
    val lembretesAtivos: Boolean = true,
    val diasAntecedencia: Int = 3,
    /** Salário líquido mensal, em centavos. 0 = não informado. */
    val salarioCentavos: Long = 0,
    /** Horário do aviso diário, em minutos desde a meia-noite (padrão 9h). */
    val minutosAviso: Int = MINUTOS_AVISO_PADRAO,
    /** Período do backup: últimos N meses, [BACKUP_TUDO] ou [BACKUP_DESDE_DATA]. */
    val mesesBackup: Int = MESES_BACKUP_PADRAO,
    /** Início escolhido pelo usuário, usado quando [mesesBackup] é [BACKUP_DESDE_DATA]. */
    val backupDesdeEpochDay: Long = 0,
    val backupComSenha: Boolean = false,
)

const val MINUTOS_AVISO_PADRAO = 9 * 60
const val MESES_BACKUP_PADRAO = 6
const val BACKUP_TUDO = 0
const val BACKUP_DESDE_DATA = -1

/** Início do período do backup (null = tudo): os últimos N meses contam a partir do dia 1º. */
fun Preferencias.inicioBackup(hoje: LocalDate): LocalDate? = when (mesesBackup) {
    BACKUP_TUDO -> null
    BACKUP_DESDE_DATA -> LocalDate.ofEpochDay(backupDesdeEpochDay)
    else -> hoje.withDayOfMonth(1).minusMonths(mesesBackup - 1L)
}

/** "09:00", "18:30". */
fun formatarHorario(minutos: Int): String = "%02d:%02d".format(minutos / 60, minutos % 60)

private val Context.dataStore by preferencesDataStore(name = "preferencias")

class PreferenciasRepo(private val context: Context) {
    private val chaveTema = stringPreferencesKey("tema")
    private val chaveLembretes = booleanPreferencesKey("lembretes_ativos")
    private val chaveDias = intPreferencesKey("dias_antecedencia")
    private val chaveSalario = longPreferencesKey("salario_centavos")
    private val chaveMinutosAviso = intPreferencesKey("minutos_aviso")
    private val chaveMesesBackup = intPreferencesKey("meses_backup")
    private val chaveBackupDesde = longPreferencesKey("backup_desde")
    private val chaveBackupSenha = booleanPreferencesKey("backup_com_senha")

    val preferencias: Flow<Preferencias> = context.dataStore.data.map { p ->
        Preferencias(
            tema = p[chaveTema]?.let { nome -> Tema.entries.firstOrNull { it.name == nome } } ?: Tema.SISTEMA,
            lembretesAtivos = p[chaveLembretes] ?: true,
            diasAntecedencia = p[chaveDias] ?: 3,
            salarioCentavos = p[chaveSalario] ?: 0,
            minutosAviso = p[chaveMinutosAviso] ?: MINUTOS_AVISO_PADRAO,
            mesesBackup = p[chaveMesesBackup] ?: MESES_BACKUP_PADRAO,
            backupDesdeEpochDay = p[chaveBackupDesde] ?: 0,
            backupComSenha = p[chaveBackupSenha] ?: false,
        )
    }

    suspend fun definirTema(tema: Tema) {
        context.dataStore.edit { it[chaveTema] = tema.name }
    }

    suspend fun definirLembretes(ativos: Boolean) {
        context.dataStore.edit { it[chaveLembretes] = ativos }
    }

    suspend fun definirSalario(centavos: Long) {
        context.dataStore.edit { it[chaveSalario] = centavos }
    }

    suspend fun definirDiasAntecedencia(dias: Int) {
        context.dataStore.edit { it[chaveDias] = dias }
    }

    suspend fun definirMinutosAviso(minutos: Int) {
        context.dataStore.edit { it[chaveMinutosAviso] = minutos }
    }

    suspend fun definirPeriodoBackup(meses: Int, desdeEpochDay: Long? = null) {
        context.dataStore.edit {
            it[chaveMesesBackup] = meses
            if (desdeEpochDay != null) it[chaveBackupDesde] = desdeEpochDay
        }
    }

    suspend fun definirBackupComSenha(ativo: Boolean) {
        context.dataStore.edit { it[chaveBackupSenha] = ativo }
    }
}
