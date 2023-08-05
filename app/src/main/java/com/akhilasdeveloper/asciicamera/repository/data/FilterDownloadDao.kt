package com.akhilasdeveloper.asciicamera.repository.data

data class FilterDownloadDao (
    val name: String,
    var density: String,
    var fgColor: Int,
    var fgColorType: Int,
    var bgColor: Int
)