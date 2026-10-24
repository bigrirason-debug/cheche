package io.github.bigrirason_debug.platen.model

actual fun newId(): String = java.util.UUID.randomUUID().toString()
actual fun nowMillis(): Long = System.currentTimeMillis()
