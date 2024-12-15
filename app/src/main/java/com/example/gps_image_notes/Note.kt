package com.example.gps_image_notes

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity (tableName = "notes")
data class Note (
    @ColumnInfo(name = "title") val title: String?,
    @ColumnInfo(name = "message") val message: String?,
    @ColumnInfo(name="latitude") var latitude: String? = null,
    @ColumnInfo(name="longitude") var longitude: String? = null,
    @ColumnInfo(name="image") var image: String? = null,
    @ColumnInfo(name="temp") var temp: String? = null,
    @ColumnInfo(name="weather") var weather: String? = null,
    @PrimaryKey (autoGenerate = true) val id:Int = 0
)