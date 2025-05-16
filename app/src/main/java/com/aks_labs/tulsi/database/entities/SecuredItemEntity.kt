package com.aks_labs.tulsi.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class SecuredItemEntity (
	@PrimaryKey val originalPath: String,
    @ColumnInfo(name = "secured_path") val securedPath: String,
    @ColumnInfo(name = "iv") val iv: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SecuredItemEntity

        if (originalPath != other.originalPath) return false
        if (securedPath != other.securedPath) return false
        if (!iv.contentEquals(other.iv)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = originalPath.hashCode()
        result = 31 * result + securedPath.hashCode()
        result = 31 * result + iv.contentHashCode()
        return result
    }
}


