package com.minhasdividas.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

/** Como a conta foi paga. Só organizacional: o app não faz pagamentos. */
enum class FormaPagamento(val rotulo: String) {
    PIX("PIX"),
    BOLETO("Boleto"),
    DEBITO_AUTOMATICO("Débito automático"),
    CARTAO("Cartão"),
    DINHEIRO("Dinheiro"),
    TRANSFERENCIA("Transferência"),
}

/**
 * Histórico: cada parcela ou conta do mês marcada como paga vira uma linha. A [Divida] guarda só a
 * situação atual (próximo vencimento, parcelas pagas); é aqui que fica quando e como foi pago.
 */
@Entity(tableName = "pagamentos", indices = [Index("dividaId")])
data class Pagamento(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dividaId: Long,
    /** Valor pago, em centavos (o da parcela no momento do pagamento). */
    val valorCentavos: Long,
    /** Vencimento da parcela/conta que foi paga, em dias desde 1970-01-01. */
    val vencimentoEpochDay: Long,
    /** Quando foi marcado como pago, em milissegundos desde 1970-01-01 (UTC). */
    val pagoEmMillis: Long,
    /** Número da parcela paga (1 = primeira). Em mensal fixa, quantas vezes a conta já foi paga. */
    val parcela: Int,
    val forma: FormaPagamento? = null,
)

val Pagamento.vencimento: LocalDate get() = LocalDate.ofEpochDay(vencimentoEpochDay)

fun Pagamento.pagoEm(zona: ZoneId = ZoneId.systemDefault()): LocalDateTime =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(pagoEmMillis), zona)

fun Pagamento.mes(zona: ZoneId = ZoneId.systemDefault()): YearMonth = YearMonth.from(pagoEm(zona))

/** Pago depois do dia do vencimento. */
fun Pagamento.emAtraso(zona: ZoneId = ZoneId.systemDefault()): Boolean =
    pagoEm(zona).toLocalDate().isAfter(vencimento)

/** O registro do pagamento da parcela atual (chamar ANTES de [pagarParcela], que avança o vencimento). */
fun Divida.registrarPagamento(agoraMillis: Long, forma: FormaPagamento?): Pagamento = Pagamento(
    dividaId = id,
    valorCentavos = valorCentavos,
    vencimentoEpochDay = vencimentoEpochDay,
    pagoEmMillis = agoraMillis,
    parcela = parcelasPagas + 1,
    forma = forma,
)

@Dao
interface PagamentoDao {
    @Query("SELECT * FROM pagamentos")
    fun observarTodos(): Flow<List<Pagamento>>

    @Query("SELECT * FROM pagamentos")
    suspend fun todos(): List<Pagamento>

    @Query("SELECT * FROM pagamentos WHERE dividaId = :dividaId")
    suspend fun daDivida(dividaId: Long): List<Pagamento>

    @Query("SELECT * FROM pagamentos WHERE dividaId = :dividaId ORDER BY pagoEmMillis DESC, id DESC LIMIT 1")
    suspend fun ultimoDa(dividaId: Long): Pagamento?

    @Insert
    suspend fun inserir(pagamento: Pagamento): Long

    @Insert
    suspend fun inserirTodos(pagamentos: List<Pagamento>)

    @Update
    suspend fun atualizar(pagamento: Pagamento)

    @Delete
    suspend fun excluir(pagamento: Pagamento)

    @Delete
    suspend fun excluirTodos(pagamentos: List<Pagamento>)

    @Query("DELETE FROM pagamentos WHERE id = :id")
    suspend fun excluirPorId(id: Long)

    @Query("DELETE FROM pagamentos WHERE dividaId = :dividaId")
    suspend fun excluirDaDivida(dividaId: Long)

    @Query("DELETE FROM pagamentos")
    suspend fun limpar()
}
