package com.akhilasdeveloper.asciicamera.util

import com.akhilasdeveloper.asciicamera.util.asciigenerator.AsciiFilters
import com.akhilasdeveloper.asciicamera.util.asciigenerator.AsciiFilters.Companion.FilterSpecs
import kotlinx.coroutines.*
import timber.log.Timber
import javax.inject.Inject

class FilterGenerator
@Inject constructor(private val utilities: Utilities){

    private val listOfFilters: ArrayList<AsciiFilters> =
        arrayListOf(
            AsciiFilters.WhiteOnBlack,
            AsciiFilters.BlackOnWhite,
            AsciiFilters.OriginalColor,
            AsciiFilters.ANSI
        )

    suspend fun generateFilters() = withContext(Dispatchers.Default){

        val filterJobs = mutableListOf<Deferred<FilterSpecs>>()

        listOfFilters.forEach {
            filterJobs.add(async {
                FilterSpecs(
                    id = it.id.toLong(),
                    density = it.density,
                    fgColor = it.fgColor,
                    bgColor = it.bgColor,
                    fgColorType = it.fgColorType,
                    densityArray = utilities.generateDensityArray(it.density, it.textCharSize)
                )
            })
        }

        filterJobs.awaitAll()
    }

}