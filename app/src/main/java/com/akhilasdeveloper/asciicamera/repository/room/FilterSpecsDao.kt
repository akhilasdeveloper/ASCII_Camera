package com.akhilasdeveloper.asciicamera.repository.room

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FilterSpecsDao {
    @Query("SELECT * from filter_specs_table")
    fun getFilters(): Flow<List<FilterSpecsTable>>

    @Query("SELECT count(*) from filter_specs_table")
    fun getFiltersCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addFilter(filterSpecs:FilterSpecsTable)

    @Delete
    fun deleteFilter(filterSpecs:FilterSpecsTable)

}