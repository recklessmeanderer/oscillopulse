package com.example.model

enum class ScanDirection(val displayName: String, val description: String) {
    VERTICAL_WATERFALL("Vertical Waterfall", "Cascades top-to-bottom across horizontal image rows"),
    HORIZONTAL("Horizontal Scan", "Sweeps left-to-right across vertical image columns")
}
