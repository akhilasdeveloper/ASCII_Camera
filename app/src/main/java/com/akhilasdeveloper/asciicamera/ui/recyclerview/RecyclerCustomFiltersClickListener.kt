package com.akhilasdeveloper.asciicamera.ui.recyclerview

import com.akhilasdeveloper.asciicamera.util.asciigenerator.AsciiFilters


interface RecyclerCustomFiltersClickListener {
    fun onCustomItemClicked(filterSpecs: AsciiFilters.Companion.FilterSpecs)
    fun onCustomDeleteClicked(filterSpecs: AsciiFilters.Companion.FilterSpecs)
}