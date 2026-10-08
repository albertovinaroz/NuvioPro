package com.nuvio.app.features.trailer

import kotlin.test.Test
import kotlin.test.assertNull

class InAppYouTubeExtractorSingleUrlTest {

    private val extractor = InAppYouTubeExtractor()

    @Test
    fun `blank input resolves to nothing without a network call`() = kotlinx.coroutines.test.runTest {
        assertNull(extractor.extractSingleUrl(""))
        assertNull(extractor.extractSingleUrl("   "))
        assertNull(TrailerPlaybackResolver.resolveSingleUrlFromYouTubeUrl(""))
    }
}
