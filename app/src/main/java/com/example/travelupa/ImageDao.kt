package com.example.travelupa

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(image: ImageEntity): Long

    @Query("SELECT * FROM images WHERE id = :imageId LIMIT 1")
    suspend fun getImageById(imageId: Long): ImageEntity?

    @Query("SELECT * FROM images WHERE tempatWisataId = :firestoreId LIMIT 1")
    suspend fun getImageByTempatWisataId(firestoreId: String): ImageEntity?

    @Query("SELECT * FROM images")
    fun getAllImages(): Flow<List<ImageEntity>>

    @Delete
    suspend fun delete(image: ImageEntity): Int
}
