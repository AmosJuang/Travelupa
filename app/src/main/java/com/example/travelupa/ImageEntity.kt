package com.example.travelupa

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "images")
data class ImageEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "localPath")
    val localPath: String,

    @ColumnInfo(name = "tempatWisataId")
    val tempatWisataId: String? = null
)
