package com.akhilasdeveloper.asciicamera.repository.data

data class FilterDownloadDao (
    val name: String,
    var density: String,
    var fgColor: String,
    var fgColorType: Int,
    var bgColor: String
)