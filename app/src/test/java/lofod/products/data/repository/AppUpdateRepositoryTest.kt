package lofod.products.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class AppUpdateRepositoryTest {

    @Test
    fun serverPathIsMadeRelativeToKeepApiPrefix() {
        assertEquals("app/download", AppUpdateRepository.relativeDownloadPath("/app/download"))
        assertEquals("app/download", AppUpdateRepository.relativeDownloadPath("app/download"))
        assertEquals("app/download", AppUpdateRepository.relativeDownloadPath(" /app/download "))
    }

    @Test
    fun blankPathFallsBackToDefault() {
        assertEquals("app/download", AppUpdateRepository.relativeDownloadPath(""))
        assertEquals("app/download", AppUpdateRepository.relativeDownloadPath("   "))
        assertEquals("app/download", AppUpdateRepository.relativeDownloadPath("/"))
    }

    @Test
    fun absoluteUrlIsKept() {
        assertEquals(
            "https://example.com/app/download",
            AppUpdateRepository.relativeDownloadPath("https://example.com/app/download"),
        )
    }
}
