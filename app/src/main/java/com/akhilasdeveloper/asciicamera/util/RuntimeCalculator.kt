package com.akhilasdeveloper.asciicamera.util

import timber.log.Timber

class RuntimeCalculator {

    private val currTime = hashMapOf<String, Long>()
    private val minTime = hashMapOf<String, Long>()
    private val maxTime = hashMapOf<String, Long>()

    fun start(key: String) {
        val time = System.currentTimeMillis()
        currTime[key] = time
    }

    fun finish(key: String) {
        val time = System.currentTimeMillis()
        val curr = currTime[key]?: 0L
        val min = minTime[key]?: Long.MAX_VALUE
        val max = maxTime[key]?: 0L
        val diff = time - curr
        if (min > diff)
            minTime[key] = diff
        if (max < diff)
            maxTime[key] = diff

        Timber.d("RuntimeCalculator $key:: min:${minTime[key]}|max:${maxTime[key]}")
    }
}