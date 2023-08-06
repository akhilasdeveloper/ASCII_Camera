package com.akhilasdeveloper.asciicamera.repository

import android.graphics.Color
import androidx.datastore.preferences.core.intPreferencesKey
import com.akhilasdeveloper.asciicamera.repository.data.FilterDownloadDao
import com.akhilasdeveloper.asciicamera.repository.datastore.DataStoreFunctions
import com.akhilasdeveloper.asciicamera.repository.room.FilterSpecsDao
import com.akhilasdeveloper.asciicamera.repository.room.FilterSpecsTable
import com.akhilasdeveloper.asciicamera.util.Constants.DEFAULT_BACK_CAMERA
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import javax.inject.Inject


class Repository
@Inject constructor(
    private val dataStore: DataStoreFunctions,
    private val filterSpecsDao: FilterSpecsDao
) {

    private val LENS_VALUE = intPreferencesKey("LENS_VALUE")
    private val FILTER_VALUE = intPreferencesKey("FILTER_VALUE")

    suspend fun setCurrentLens(lens: Int) {
        dataStore.saveValueToPreferencesStore(LENS_VALUE, lens)
    }

    fun getCurrentLens() =
        dataStore.getValueAsFlowFromPreferencesStore(LENS_VALUE).map { it ?: DEFAULT_BACK_CAMERA }

    suspend fun getCurrentLensRaw() =
        dataStore.getValueFromPreferencesStore(LENS_VALUE) ?: DEFAULT_BACK_CAMERA

    suspend fun setCurrentFilter(id: Int) {
        dataStore.saveValueToPreferencesStore(FILTER_VALUE, id)
    }

    fun getCurrentFilter() =
        dataStore.getValueAsFlowFromPreferencesStore(FILTER_VALUE)

    suspend fun getCurrentFilterRaw() =
        dataStore.getValueFromPreferencesStore(FILTER_VALUE)

    suspend fun addFilter(filterSpecsTable: FilterSpecsTable) {
        withContext(Dispatchers.IO) {
            Timber.d("Add filter data $filterSpecsTable")
            filterSpecsDao.addFilter(filterSpecsTable)
        }
    }

    suspend fun addFilters(filterSpecsTable: List<FilterSpecsTable>) {
        withContext(Dispatchers.IO) {
            filterSpecsDao.addFilters(filterSpecsTable)
        }
    }

    suspend fun deleteCustomFilter(filterSpecsTable: FilterSpecsTable) {
        withContext(Dispatchers.IO) {
            filterSpecsDao.deleteFilter(filterSpecsTable)
        }
    }

    fun getCustomFilters() = filterSpecsDao.getFilters().flowOn(Dispatchers.IO)
    fun getDownloadedFilters(): Flow<List<FilterSpecsTable>> {

        return filterSpecsDao.getDownloadedFilters().flowOn(Dispatchers.IO)
    }

    suspend fun getFilterById(id: Int) = withContext(Dispatchers.IO) {filterSpecsDao.getFilterById(id)}

    suspend fun getFiltersCount() = withContext(Dispatchers.IO) { filterSpecsDao.getFiltersCount() }

    fun getFilters() {

        val retrofit = Retrofit.Builder()
            .baseUrl("https://akhilasdeveloper.github.io/asciicamera/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val retrofitAPI = retrofit.create(RetrofitAPI::class.java)
        val call: Call<List<FilterDownloadDao>?>? = retrofitAPI.getCourse()
        call?.enqueue(object : Callback<List<FilterDownloadDao>?> {
            override fun onResponse(
                call: Call<List<FilterDownloadDao>?>,
                response: Response<List<FilterDownloadDao>?>
            ) {
                if (response.isSuccessful) {
                    CoroutineScope(Dispatchers.IO).launch {
                        filterSpecsDao.deleteAllDownloads()

                        val data:List<FilterDownloadDao>? = response.body()

                        data?.forEach {

                            val tab = FilterSpecsTable(
                                fgColor = Color.parseColor(it.fgColor),
                                bgColor = Color.parseColor(it.bgColor),
                                density = it.density,
                                densityArray = byteArrayOf(),
                                fgColorType = it.fgColorType,
                                name = it.name,
                                isDownloaded = true
                            )

                            filterSpecsDao.addFilter(tab)
                            Timber.d("Data : %s", it.name)
                        }
                    }
                }
            }

            override fun onFailure(call: Call<List<FilterDownloadDao>?>, t: Throwable) {

            }
        })
    }
}