package de.roboticmind.apkcreator.core.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildProfileRepositoryTest {

    private val repository = BuildProfileRepository()

    @Test
    fun `profiles are not empty`() {
        assertTrue(repository.profiles().isNotEmpty())
    }

    @Test
    fun `profile ids are unique`() {
        val ids = repository.profiles().map(BuildProfile::id)
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `encode and decode round trip keeps all fields`() {
        val original = repository.profiles()

        val decoded = repository.decode(repository.encode(original))

        assertEquals(original, decoded)
    }
}
