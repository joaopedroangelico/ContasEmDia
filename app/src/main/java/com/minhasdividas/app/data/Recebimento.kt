package com.minhasdividas.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Renda extra, fora do salário: freela, venda, reembolso, aluguel recebido... */
@Entity(tableName = "recebimentos")
data class Recebimento(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val descricao: String,
    val valorCentavos: Long,
    /** Data prevista do (próximo) recebimento, em dias desde 1970-01-01. */
    val dataEpochDay: Long,
    /** Repete todo mês: ao receber, a data avança para o mês seguinte. */
    val recorrente: Boolean = false,
    /** Dia do mês original, para não "escorregar" após meses curtos. */
    val diaDoMes: Int = 0,
    val recebido: Boolean = false,
    val vezesRecebido: Int = 0,
)

val Recebimento.data: LocalDate get() = LocalDate.ofEpochDay(dataEpochDay)

private fun Recebimento.dataDeslocada(meses: Long): LocalDate {
    val alvo = data.plusMonths(meses)
    val dia = if (diaDoMes > 0) diaDoMes else data.dayOfMonth
    return alvo.withDayOfMonth(minOf(dia, alvo.lengthOfMonth()))
}

/** Recorrente cujo valor deste mês já entrou: a próxima data já caiu num mês futuro. */
fun Recebimento.recebidoNoMes(hoje: LocalDate): Boolean =
    !recebido && recorrente && vezesRecebido > 0 &&
        data.isAfter(hoje.withDayOfMonth(hoje.lengthOfMonth()))

fun Recebimento.receber(): Recebimento =
    if (recorrente) {
        copy(vezesRecebido = vezesRecebido + 1, dataEpochDay = dataDeslocada(1).toEpochDay())
    } else {
        copy(recebido = true, vezesRecebido = 1)
    }

fun Recebimento.desfazerRecebimento(): Recebimento =
    if (recorrente) {
        copy(vezesRecebido = (vezesRecebido - 1).coerceAtLeast(0), dataEpochDay = dataDeslocada(-1).toEpochDay())
    } else {
        copy(recebido = false, vezesRecebido = 0)
    }

@Dao
interface RecebimentoDao {
    @Query("SELECT * FROM recebimentos")
    fun observarTodos(): Flow<List<Recebimento>>

    @Upsert
    suspend fun salvar(recebimento: Recebimento)

    @Delete
    suspend fun excluir(recebimento: Recebimento)
}
