package com.yourhistory.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.yourhistory.app.data.local.dao.CategoryDao
import com.yourhistory.app.data.local.dao.QrContactDao
import com.yourhistory.app.data.local.dao.TransactionDao
import com.yourhistory.app.data.local.entity.CategoryEntity
import com.yourhistory.app.data.local.entity.QrContactEntity
import com.yourhistory.app.data.local.entity.TransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

@Database(
    entities = [
        CategoryEntity::class,
        QrContactEntity::class,
        TransactionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun qrContactDao(): QrContactDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        private const val DATABASE_NAME = "your_history.db"

        fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed initial categories
                        CoroutineScope(Dispatchers.IO).launch {
                            val defaultCategories = listOf(
                                CategoryEntity(
                                    id = "cat_food",
                                    name = "Ăn uống",
                                    type = "EXPENSE",
                                    iconName = "Restaurant",
                                    colorHex = "#FF5722",
                                    isDefault = true
                                ),
                                CategoryEntity(
                                    id = "cat_shopping",
                                    name = "Mua sắm",
                                    type = "EXPENSE",
                                    iconName = "ShoppingBag",
                                    colorHex = "#E91E63",
                                    isDefault = true
                                ),
                                CategoryEntity(
                                    id = "cat_transport",
                                    name = "Di chuyển",
                                    type = "EXPENSE",
                                    iconName = "DirectionsCar",
                                    colorHex = "#2196F3",
                                    isDefault = true
                                ),
                                CategoryEntity(
                                    id = "cat_bill",
                                    name = "Hóa đơn & Dịch vụ",
                                    type = "EXPENSE",
                                    iconName = "Receipt",
                                    colorHex = "#9C27B0",
                                    isDefault = true
                                ),
                                CategoryEntity(
                                    id = "cat_entertainment",
                                    name = "Giải trí & Cà phê",
                                    type = "EXPENSE",
                                    iconName = "LocalCafe",
                                    colorHex = "#795548",
                                    isDefault = true
                                ),
                                CategoryEntity(
                                    id = "cat_salary",
                                    name = "Lương",
                                    type = "INCOME",
                                    iconName = "Payments",
                                    colorHex = "#4CAF50",
                                    isDefault = true
                                ),
                                CategoryEntity(
                                    id = "cat_other_income",
                                    name = "Thu nhập khác",
                                    type = "INCOME",
                                    iconName = "AccountBalanceWallet",
                                    colorHex = "#009688",
                                    isDefault = true
                                ),
                                CategoryEntity(
                                    id = "cat_other_expense",
                                    name = "Khác",
                                    type = "EXPENSE",
                                    iconName = "MoreHoriz",
                                    colorHex = "#607D8B",
                                    isDefault = true
                                )
                            )
                            val instance = buildDatabase(context)
                            instance.categoryDao().insertCategories(defaultCategories)
                        }
                    }
                })
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
