package com.akhilasdeveloper.asciicamera.repository.room

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FilterSpecsDao {
    @Query("SELECT * from filter_specs_table")
    fun getFilters(): Flow<List<FilterSpecsTable>>

    @Query("SELECT * from filter_specs_table where id = :id")
    fun getFilterById(id: Int): FilterSpecsTable?

    @Query("SELECT count(*) from filter_specs_table")
    fun getFiltersCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addFilter(filterSpecs: FilterSpecsTable)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addFilters(filterSpecs: List<FilterSpecsTable>)

    @Delete
    fun deleteFilter(filterSpecs: FilterSpecsTable)

    @Query("SELECT * from filter_specs_downloads_table")
    fun getDownloadedFilters(): Flow<List<FilterSpecsDownloadsTable>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addFilterDownloads(filterSpecs: FilterSpecsDownloadsTable)

    @Query("DELETE from filter_specs_downloads_table")
    fun deleteAllDownloads(): Int

}