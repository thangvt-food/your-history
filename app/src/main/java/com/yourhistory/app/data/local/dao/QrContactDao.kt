package com.yourhistory.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.yourhistory.app.data.local.entity.QrContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QrContactDao {
    @Query("SELECT * FROM qr_contacts WHERE isDeleted = 0 ORDER BY lastUsedAt DESC")
    fun getAllQrContacts(): Flow<List<QrContactEntity>>

    @Query("SELECT * FROM qr_contacts WHERE isDeleted = 0 ORDER BY lastUsedAt DESC LIMIT :limit")
    fun getRecentQrContacts(limit: Int): Flow<List<QrContactEntity>>

    @Query("SELECT * FROM qr_contacts WHERE id = :id LIMIT 1")
    suspend fun getQrContactById(id: String): QrContactEntity?

    @Query("SELECT * FROM qr_contacts WHERE bankBin = :bankBin AND accountNumber = :accountNumber AND isDeleted = 0 LIMIT 1")
    suspend fun findByAccount(bankBin: String, accountNumber: String): QrContactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(contact: QrContactEntity)

    @Update
    suspend fun update(contact: QrContactEntity)

    @Query("UPDATE qr_contacts SET lastUsedAt = :timestamp WHERE id = :id")
    suspend fun updateLastUsed(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE qr_contacts SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long = System.currentTimeMillis())
}
