package com.akhilasdeveloper.asciicamera.repository.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "filter_specs_table")
data class FilterSpecsTable(
    @PrimaryKey(autoGenerate = true)
    val id: Long? = null,
    var density: String,
    var fgColor: Int,
    var fgColorType: Int,
    var bgColor: Int)