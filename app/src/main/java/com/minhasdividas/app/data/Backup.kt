package com.minhasdividas.app.data

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.time.LocalDate
import java.time.ZoneId
import java.util.Base64
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

private const val APP_BACKUP = "ContasEmDia"
private const val FORMATO_BACKUP = 1
private const val ITERACOES = 100_000

/** Conteúdo de um arquivo de backup. Importar substitui todos os dados do app por este conteúdo. */
data class DadosBackup(
    val criadoEmMillis: Long,
    /** Início do período exportado; null = tudo. */
    val desdeEpochDay: Long?,
    val salarioCentavos: Long,
    val dividas: List<Divida>,
    val pagamentos: List<Pagamento>,
    val recebimentos: List<Recebimento>,
)

/**
 * Seleciona o que vai para o backup. Contas e valores a receber em aberto sempre entram (sem eles a
 * restauração perderia o que ainda falta pagar); o período [desde] limita o histórico: pagamentos,
 * contas quitadas e recebimentos únicos já recebidos. Quitadas sem histórico (de antes da v1.4) não
 * têm data para filtrar e sempre entram: são poucos bytes, e restaurar sem elas as apagaria.
 */
fun montarBackup(
    dividas: List<Divida>,
    pagamentos: List<Pagamento>,
    recebimentos: List<Recebimento>,
    salarioCentavos: Long,
    desde: LocalDate?,
    agoraMillis: Long,
    zona: ZoneId,
): DadosBackup {
    if (desde == null) {
        return DadosBackup(agoraMillis, null, salarioCentavos, dividas, pagamentos, recebimentos)
    }
    val inicio = desde.atStartOfDay(zona).toInstant().toEpochMilli()
    val noPeriodo = pagamentos.filter { it.pagoEmMillis >= inicio }
    val comPagamento = noPeriodo.mapTo(HashSet()) { it.dividaId }
    val comHistorico = pagamentos.mapTo(HashSet()) { it.dividaId }
    val escolhidas = dividas.filter { !it.paga || it.id in comPagamento || it.id !in comHistorico }
    val ids = escolhidas.mapTo(HashSet()) { it.id }
    return DadosBackup(
        criadoEmMillis = agoraMillis,
        desdeEpochDay = desde.toEpochDay(),
        salarioCentavos = salarioCentavos,
        dividas = escolhidas,
        pagamentos = noPeriodo.filter { it.dividaId in ids },
        recebimentos = recebimentos.filter { !it.recebido || it.dataEpochDay >= desde.toEpochDay() },
    )
}

/** Resultado de abrir um arquivo de backup. */
sealed interface LeituraBackup {
    data class Ok(val dados: DadosBackup) : LeituraBackup
    data object PrecisaSenha : LeituraBackup
    data object SenhaIncorreta : LeituraBackup
    data object Invalido : LeituraBackup
}

/** Texto do arquivo. Com [senha], os dados vão cifrados (AES-256-GCM, chave derivada da senha). */
fun gerarArquivoBackup(dados: DadosBackup, senha: String?): String {
    val arquivo = JSONObject()
        .put("app", APP_BACKUP)
        .put("formato", FORMATO_BACKUP)
        .put("criadoEm", dados.criadoEmMillis)
    if (senha.isNullOrEmpty()) {
        arquivo.put("cifrado", false).put("dados", dados.paraJson())
    } else {
        val sal = aleatorio(16)
        val iv = aleatorio(12)
        val cifra = Cipher.getInstance("AES/GCM/NoPadding")
        cifra.init(Cipher.ENCRYPT_MODE, chave(senha, sal, ITERACOES), GCMParameterSpec(128, iv))
        val cifrado = cifra.doFinal(dados.paraJson().toString().toByteArray(Charsets.UTF_8))
        arquivo.put("cifrado", true)
            .put("iteracoes", ITERACOES)
            .put("sal", base64(sal))
            .put("iv", base64(iv))
            .put("dados", base64(cifrado))
    }
    return arquivo.toString()
}

fun lerArquivoBackup(texto: String, senha: String?): LeituraBackup {
    val arquivo = runCatching { JSONObject(texto) }.getOrNull() ?: return LeituraBackup.Invalido
    if (arquivo.optString("app") != APP_BACKUP || arquivo.optInt("formato") > FORMATO_BACKUP) {
        return LeituraBackup.Invalido
    }
    return try {
        if (!arquivo.optBoolean("cifrado")) {
            LeituraBackup.Ok(dadosDeJson(arquivo.getJSONObject("dados")))
        } else {
            if (senha.isNullOrEmpty()) return LeituraBackup.PrecisaSenha
            val decoder = Base64.getDecoder()
            val sal = decoder.decode(arquivo.getString("sal"))
            val iv = decoder.decode(arquivo.getString("iv"))
            val cifra = Cipher.getInstance("AES/GCM/NoPadding")
            cifra.init(Cipher.DECRYPT_MODE, chave(senha, sal, arquivo.getInt("iteracoes")), GCMParameterSpec(128, iv))
            val claro = cifra.doFinal(decoder.decode(arquivo.getString("dados")))
            LeituraBackup.Ok(dadosDeJson(JSONObject(String(claro, Charsets.UTF_8))))
        }
    } catch (e: AEADBadTagException) {
        LeituraBackup.SenhaIncorreta
    } catch (e: GeneralSecurityException) {
        LeituraBackup.Invalido
    } catch (e: JSONException) {
        LeituraBackup.Invalido
    } catch (e: IllegalArgumentException) {
        LeituraBackup.Invalido
    }
}

private fun chave(senha: String, sal: ByteArray, iteracoes: Int): SecretKeySpec {
    val spec = PBEKeySpec(senha.toCharArray(), sal, iteracoes, 256)
    val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    spec.clearPassword()
    return SecretKeySpec(bytes, "AES")
}

private fun aleatorio(tamanho: Int) = ByteArray(tamanho).also { SecureRandom().nextBytes(it) }

private fun base64(bytes: ByteArray) = Base64.getEncoder().encodeToString(bytes)

// --- JSON (nomes curtos e iguais aos campos, para o arquivo ficar leve e legível) ---

private fun DadosBackup.paraJson(): JSONObject = JSONObject()
    .put("criadoEm", criadoEmMillis)
    .put("desde", desdeEpochDay ?: JSONObject.NULL)
    .put("salario", salarioCentavos)
    .put("dividas", JSONArray(dividas.map { it.paraJson() }))
    .put("pagamentos", JSONArray(pagamentos.map { it.paraJson() }))
    .put("recebimentos", JSONArray(recebimentos.map { it.paraJson() }))

private fun dadosDeJson(o: JSONObject) = DadosBackup(
    criadoEmMillis = o.getLong("criadoEm"),
    desdeEpochDay = if (o.isNull("desde")) null else o.getLong("desde"),
    salarioCentavos = o.optLong("salario"),
    dividas = o.getJSONArray("dividas").objetos().map(::dividaDeJson),
    pagamentos = o.getJSONArray("pagamentos").objetos().map(::pagamentoDeJson),
    recebimentos = o.getJSONArray("recebimentos").objetos().map(::recebimentoDeJson),
)

private fun JSONArray.objetos(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

private fun formaDe(o: JSONObject, campo: String): FormaPagamento? =
    if (o.isNull(campo)) null else FormaPagamento.entries.firstOrNull { it.name == o.optString(campo) }

private fun Divida.paraJson() = JSONObject()
    .put("id", id)
    .put("descricao", descricao)
    .put("valor", valorCentavos)
    .put("vencimento", vencimentoEpochDay)
    .put("categoria", categoria.name)
    .put("totalParcelas", totalParcelas)
    .put("parcelasPagas", parcelasPagas)
    .put("paga", paga)
    .put("recorrente", recorrente)
    .put("diaVencimento", diaVencimento)
    .put("origem", origem)
    .put("forma", formaPagamento?.name ?: JSONObject.NULL)

private fun dividaDeJson(o: JSONObject) = Divida(
    id = o.getLong("id"),
    descricao = o.getString("descricao"),
    valorCentavos = o.getLong("valor"),
    vencimentoEpochDay = o.getLong("vencimento"),
    categoria = Categoria.entries.firstOrNull { it.name == o.optString("categoria") } ?: Categoria.OUTROS,
    totalParcelas = o.optInt("totalParcelas", 1),
    parcelasPagas = o.optInt("parcelasPagas"),
    paga = o.optBoolean("paga"),
    recorrente = o.optBoolean("recorrente"),
    diaVencimento = o.optInt("diaVencimento"),
    origem = o.optString("origem"),
    formaPagamento = formaDe(o, "forma"),
)

private fun Pagamento.paraJson() = JSONObject()
    .put("id", id)
    .put("divida", dividaId)
    .put("valor", valorCentavos)
    .put("vencimento", vencimentoEpochDay)
    .put("pagoEm", pagoEmMillis)
    .put("parcela", parcela)
    .put("forma", forma?.name ?: JSONObject.NULL)

private fun pagamentoDeJson(o: JSONObject) = Pagamento(
    id = o.getLong("id"),
    dividaId = o.getLong("divida"),
    valorCentavos = o.getLong("valor"),
    vencimentoEpochDay = o.getLong("vencimento"),
    pagoEmMillis = o.getLong("pagoEm"),
    parcela = o.optInt("parcela", 1),
    forma = formaDe(o, "forma"),
)

private fun Recebimento.paraJson() = JSONObject()
    .put("id", id)
    .put("descricao", descricao)
    .put("valor", valorCentavos)
    .put("data", dataEpochDay)
    .put("recorrente", recorrente)
    .put("diaDoMes", diaDoMes)
    .put("recebido", recebido)
    .put("vezesRecebido", vezesRecebido)

private fun recebimentoDeJson(o: JSONObject) = Recebimento(
    id = o.getLong("id"),
    descricao = o.getString("descricao"),
    valorCentavos = o.getLong("valor"),
    dataEpochDay = o.getLong("data"),
    recorrente = o.optBoolean("recorrente"),
    diaDoMes = o.optInt("diaDoMes"),
    recebido = o.optBoolean("recebido"),
    vezesRecebido = o.optInt("vezesRecebido"),
)
