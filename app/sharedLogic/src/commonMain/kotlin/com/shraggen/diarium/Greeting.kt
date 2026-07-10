package com.shraggen.diarium

import kotlin.js.JsExport

@JsExport
class Greeting {
    private val platform = getPlatform()

    fun greet(): String {
        return sayHello(platform.name)
    }

    private fun sayHello(name: String): String {
        return "hello"
    }
}