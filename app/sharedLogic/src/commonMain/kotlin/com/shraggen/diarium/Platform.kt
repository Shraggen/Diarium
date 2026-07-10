package com.shraggen.diarium

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform