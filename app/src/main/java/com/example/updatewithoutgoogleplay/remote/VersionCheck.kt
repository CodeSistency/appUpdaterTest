package com.example.updatewithoutgoogleplay.remote

data class VersionCheck(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val releaseNotes: String
)
