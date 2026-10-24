package io.github.bigrirason_debug.platen.model

import kotlin.test.Test
import kotlin.test.assertTrue

class PlatenSchemaTest {
    @Test
    fun minReaderNeverExceedsWriteVersion() {
        assertTrue(PlatenSchema.MIN_READER <= PlatenSchema.WRITE_VERSION)
    }
}
