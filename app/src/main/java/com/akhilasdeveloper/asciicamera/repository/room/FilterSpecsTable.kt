package com.akhilasdeveloper.asciicamera.repository.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "filter_specs_table")
data class FilterSpecsTable(
    @PrimaryKey(autoGenerate = true)
    val id: Long? = null,
    val name: String,
    var density: String,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    var densityArray: ByteArray,
    var fgColor: Int,
    var fgColorType: Int,
    var bgColor: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FilterSpecsTable

        if (id != other.id) return false
        if (density != other.density) return false
        if (!densityArray.contentEquals(other.densityArray)) return false
        if (fgColor != other.fgColor) return false
        if (fgColorType != other.fgColorType) return false
        if (bgColor != other.bgColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + density.hashCode()
        result = 31 * result + densityArray.contentHashCode()
        result = 31 * result + fgColor
        result = 31 * result + fgColorType
        result = 31 * result + bgColor
        return result
    }
}