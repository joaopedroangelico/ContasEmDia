package com.minhasdividas.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** v2: contas mensais fixas e dia original do vencimento. Preserva todas as dívidas já cadastradas. */
val MIGRACAO_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE dividas ADD COLUMN recorrente INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE dividas ADD COLUMN diaVencimento INTEGER NOT NULL DEFAULT 0")
        db.execSQL(
            "UPDATE dividas SET diaVencimento = " +
                "CAST(strftime('%d', vencimentoEpochDay * 86400, 'unixepoch') AS INTEGER)",
        )
    }
}

/** v3: tabela de recebimentos (renda extra). As dívidas não são tocadas. */
val MIGRACAO_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `recebimentos` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`descricao` TEXT NOT NULL, " +
                "`valorCentavos` INTEGER NOT NULL, " +
                "`dataEpochDay` INTEGER NOT NULL, " +
                "`recorrente` INTEGER NOT NULL, " +
                "`diaDoMes` INTEGER NOT NULL, " +
                "`recebido` INTEGER NOT NULL, " +
                "`vezesRecebido` INTEGER NOT NULL)",
        )
    }
}

/** v4: origem opcional da dívida ("de onde é a fatura"). Dívidas existentes ficam sem origem. */
val MIGRACAO_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE dividas ADD COLUMN origem TEXT NOT NULL DEFAULT ''")
    }
}

@Database(entities = [Divida::class, Recebimento::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dividaDao(): DividaDao
    abstract fun recebimentoDao(): RecebimentoDao

    companion object {
        @Volatile
        private var instancia: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "dividas.db")
                    .addMigrations(MIGRACAO_1_2, MIGRACAO_2_3, MIGRACAO_3_4)
                    .build()
                    .also { instancia = it }
            }
    }
}
