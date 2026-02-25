package com.example.yol_yolakay.feature.publish.components

import com.google.android.gms.maps.model.LatLng

data class MapBasePoint(
    val id: String,            // UI uchun unique id
    val name: String,
    val position: LatLng,
    val pointId: String? = null, // backend popular_points id bo‘lsa shu
    val region: String? = null
)