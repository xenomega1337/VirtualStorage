package com.example.virtualstorage.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Category::class, InventoryItem::class, StockHistory::class],
    version = 3,
    exportSchema = false
)
abstract class InventoryRoomDatabase : RoomDatabase() {
    abstract fun inventoryDao(): InventoryDao

    companion object {
        @Volatile
        private var instance: InventoryRoomDatabase? = null

        fun getInstance(context: Context): InventoryRoomDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    InventoryRoomDatabase::class.java,
                    "virtual_storage_room.db"
                )
                    .allowMainThreadQueries()
                    .addMigrations(migration1To2, migration2To3)
                    .build()
                    .also { instance = it }
            }

        private val migration1To2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                rebuildTables(db, userIdSql = "'legacy'")
            }
        }

        private val migration2To3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                rebuildTables(db, userIdSql = "user_id")
            }
        }

        private fun rebuildTables(db: SupportSQLiteDatabase, userIdSql: String) {
            db.execSQL(
                """
                CREATE TABLE categories_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    user_id TEXT NOT NULL,
                    name TEXT NOT NULL,
                    parent_id INTEGER
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO categories_new(id, user_id, name, parent_id)
                SELECT id, $userIdSql, name, parent_id FROM categories
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE items_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    user_id TEXT NOT NULL,
                    name TEXT NOT NULL,
                    description TEXT NOT NULL,
                    quantity INTEGER NOT NULL,
                    min_quantity INTEGER NOT NULL,
                    category_id INTEGER,
                    image_uri TEXT,
                    updated_at INTEGER NOT NULL,
                    FOREIGN KEY(category_id) REFERENCES categories(id) ON DELETE SET NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO items_new(id, user_id, name, description, quantity, min_quantity, category_id, image_uri, updated_at)
                SELECT id, $userIdSql, name, description, quantity, min_quantity, category_id, image_uri, updated_at FROM items
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE stock_history_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    user_id TEXT NOT NULL,
                    item_id INTEGER NOT NULL,
                    action TEXT NOT NULL,
                    details TEXT NOT NULL,
                    created_at INTEGER NOT NULL,
                    FOREIGN KEY(item_id) REFERENCES items(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO stock_history_new(id, user_id, item_id, action, details, created_at)
                SELECT id, $userIdSql, item_id, action, details, created_at FROM stock_history
                """.trimIndent()
            )

            db.execSQL("DROP TABLE stock_history")
            db.execSQL("DROP TABLE items")
            db.execSQL("DROP TABLE categories")
            db.execSQL("ALTER TABLE categories_new RENAME TO categories")
            db.execSQL("ALTER TABLE items_new RENAME TO items")
            db.execSQL("ALTER TABLE stock_history_new RENAME TO stock_history")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_items_category_id ON items(category_id)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_items_user_id ON items(user_id)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_stock_history_item_id ON stock_history(item_id)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_stock_history_user_id ON stock_history(user_id)")
        }
    }
}
