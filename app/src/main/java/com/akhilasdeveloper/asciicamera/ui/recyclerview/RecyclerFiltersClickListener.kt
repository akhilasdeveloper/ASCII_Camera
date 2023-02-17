package com.akhilasdeveloper.asciicamera.ui.recyclerview

import com.akhilasdeveloper.asciicamera.util.TextBitmapFilter


interface RecyclerFiltersClickListener {
    fun onItemClicked(textBitmapFilter: TextBitmapFilter)
}