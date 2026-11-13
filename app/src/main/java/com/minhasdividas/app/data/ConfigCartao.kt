package com.minhasdividas.app.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth

/**
 * Dias de fechamento e vencimento da fatura de um cartão. O cartão em si é só o nome digitado nas
 * compras ([Divida.origem]); [chave] é esse nome normalizado ("nubank" = "Nubank ").
 */
@Entity(tableName = "cartoes")
data class ConfigCartao(
    @PrimaryKey val chave: String,
    val nome: String,
    val diaFechamento: Int,
    val diaVencimento: Int,
)

fun chaveCartao(nome: String): String = nome.trim().lowercase(LocaleBR)

/** Dia [dia] do mês, ou o último dia se o mês for mais curto (31 → 30/11, 28/02). */
private fun YearMonth.noDia(dia: Int): LocalDate = atDay(minOf(dia, lengthOfMonth()))

/**
 * Vencimento da fatura em que entra uma compra feita em [data]. A fatura fecha no [ConfigCartao.diaFechamento]:
 * compra feita a partir do dia do fechamento já vai para a fatura seguinte. Ela vence no primeiro
 * [ConfigCartao.diaVencimento] depois do fechamento.
 */
fun ConfigCartao.vencimentoParaCompra(data: LocalDate): LocalDate {
    var mes = YearMonth.from(data)
    var fechamento = mes.noDia(diaFechamento)
    if (!fechamento.isAfter(data)) {
        mes = mes.plusMonths(1)
        fechamento = mes.noDia(diaFechamento)
    }
    var vencimento = YearMonth.from(fechamento).noDia(diaVencimento)
    if (!vencimento.isAfter(fechamento)) vencimento = YearMonth.from(fechamento).plusMonths(1).noDia(diaVencimento)
    return vencimento
}

/**
 * Ao mudar o vencimento do cartão, as compras em aberto dele passam a vencer no novo dia, no mesmo
 * mês em que já venciam (a fatura continua a mesma).
 */
fun aplicarVencimento(dividas: List<Divida>, config: ConfigCartao): List<Divida> =
    dividas
        .filter { it.categoria == Categoria.CARTAO && !it.paga && chaveCartao(it.origem) == config.chave }
        .map { d ->
            val novo = YearMonth.from(d.vencimento).noDia(config.diaVencimento)
            d.copy(vencimentoEpochDay = novo.toEpochDay(), diaVencimento = config.diaVencimento)
        }

@Dao
interface ConfigCartaoDao {
    @Query("SELECT * FROM cartoes")
    fun observarTodos(): Flow<List<ConfigCartao>>

    @Query("SELECT * FROM cartoes")
    suspend fun todos(): List<ConfigCartao>

    @Upsert
    suspend fun salvar(config: ConfigCartao)

    @Insert
    suspend fun inserirTodos(configs: List<ConfigCartao>)

    @Query("DELETE FROM cartoes")
    suspend fun limpar()
}
