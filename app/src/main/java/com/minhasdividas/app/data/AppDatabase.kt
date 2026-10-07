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

@Database(entities = [Divida::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dividaDao(): DividaDao

    companion object {
        @Volatile
        private var instancia: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "dividas.db")
                    .addMigrations(MIGRACAO_1_2)
                    .build()
                    .also { instancia = it }
            }
    }
}
