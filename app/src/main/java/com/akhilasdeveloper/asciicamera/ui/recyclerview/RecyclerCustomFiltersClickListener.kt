package com.akhilasdeveloper.asciicamera.ui.recyclerview

import com.akhilasdeveloper.asciicamera.util.TextBitmapFilter
import com.akhilasdeveloper.asciicamera.util.TextBitmapFilter.Companion.FilterSpecs


interface RecyclerCustomFiltersClickListener {
    fun onCustomItemClicked(filterSpecs: FilterSpecs)
    fun onCustomDeleteClicked(filterSpecs: FilterSpecs)
}