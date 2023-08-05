package com.akhilasdeveloper.asciicamera.repository

import com.akhilasdeveloper.asciicamera.repository.data.FilterDownloadDao
import retrofit2.Call
import retrofit2.http.GET

interface RetrofitAPI {
    @GET("filters.json")
    fun getCourse(): Call<List<FilterDownloadDao>?>?
}