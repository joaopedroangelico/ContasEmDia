package com.minhasdividas.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DividaDao {
    @Query("SELECT * FROM dividas")
    fun observarTodas(): Flow<List<Divida>>

    @Query("SELECT * FROM dividas WHERE paga = 0 AND vencimentoEpochDay <= :limiteEpochDay ORDER BY vencimentoEpochDay")
    suspend fun pendentesAte(limiteEpochDay: Long): List<Divida>

    @Query("SELECT * FROM dividas")
    suspend fun todas(): List<Divida>

    @Upsert
    suspend fun salvar(divida: Divida)

    @Insert
    suspend fun inserirTodas(dividas: List<Divida>)

    @Delete
    suspend fun excluirTodas(dividas: List<Divida>)

    @Query("DELETE FROM dividas")
    suspend fun limpar()

    @Delete
    suspend fun excluir(divida: Divida)
}
