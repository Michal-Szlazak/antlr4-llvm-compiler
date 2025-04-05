package org.example.compiler

import kotlin.test.Test
import kotlin.test.assertEquals

class TypeTest {

    @Test
    fun `there should be only one type name each`() {
        val allTypeNames = Type.entries.map { it.typeName }
        val uniqueTypes = allTypeNames.toSet()

        assertEquals(uniqueTypes.size, uniqueTypes.size)
    }

}