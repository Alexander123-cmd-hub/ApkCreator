package de.roboticmind.apkcreator

import de.roboticmind.apkcreator.core.data.BuildProfileRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prueft, dass die vom App-Modul konsumierten Profile die von der UI
 * erwarteten Invarianten erfuellen.
 */
class BuildProfileFormattingTest {

    private val repository = BuildProfileRepository()

    @Test
    fun `every profile has a non blank application id`() {
        assertTrue(repository.profiles().all { it.applicationId.isNotBlank() })
    }

    @Test
    fun `exactly one profile is marked as signed`() {
        assertEquals(1, repository.profiles().count { it.signed })
    }
}
