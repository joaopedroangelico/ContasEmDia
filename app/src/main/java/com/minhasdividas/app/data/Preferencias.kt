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
)

const val MINUTOS_AVISO_PADRAO = 9 * 60

/** "09:00", "18:30". */
fun formatarHorario(minutos: Int): String = "%02d:%02d".format(minutos / 60, minutos % 60)

private val Context.dataStore by preferencesDataStore(name = "preferencias")

class PreferenciasRepo(private val context: Context) {
    private val chaveTema = stringPreferencesKey("tema")
    private val chaveLembretes = booleanPreferencesKey("lembretes_ativos")
    private val chaveDias = intPreferencesKey("dias_antecedencia")
    private val chaveSalario = longPreferencesKey("salario_centavos")
    private val chaveMinutosAviso = intPreferencesKey("minutos_aviso")

    val preferencias: Flow<Preferencias> = context.dataStore.data.map { p ->
        Preferencias(
            tema = p[chaveTema]?.let { nome -> Tema.entries.firstOrNull { it.name == nome } } ?: Tema.SISTEMA,
            lembretesAtivos = p[chaveLembretes] ?: true,
            diasAntecedencia = p[chaveDias] ?: 3,
            salarioCentavos = p[chaveSalario] ?: 0,
            minutosAviso = p[chaveMinutosAviso] ?: MINUTOS_AVISO_PADRAO,
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
}
