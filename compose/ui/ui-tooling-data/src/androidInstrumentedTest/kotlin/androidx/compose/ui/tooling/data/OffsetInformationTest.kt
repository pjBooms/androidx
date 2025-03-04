/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.ui.tooling.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import junit.framework.TestCase
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(UiToolingDataApi::class)
class OffsetInformationTest : ToolingTest() {
    @Test
    fun testOffset() {
        val slotTableRecord = CompositionDataRecord.create()
        show { Inspectable(slotTableRecord) { OffsetData() } }

        val table = slotTableRecord.store.first()
        val tree = table.asTree()
        val offsets =
            tree
                .all()
                .filter {
                    it.location?.sourceFile == "OffsetData.kt" &&
                        it.name != null &&
                        it.name != "remember"
                }
                .map { it.name!! to it.location!!.offset }

        assertArrayEquals(
            arrayListOf(
                "MyComposeTheme" to 1598,
                "Column" to 1623,
                "Text" to 1680,
                "Greeting" to 1845,
                "Text" to 2509,
                "Surface" to 1877,
                "Button" to 1959,
                "Text" to 1982,
                "Surface" to 2021,
                "TextButton" to 2102,
                "Row" to 2185
            ),
            offsets
        )
    }

    @Test
    fun testInline() {
        val slotTableRecord = CompositionDataRecord.create()
        show { Inspectable(slotTableRecord) { OffsetData() } }

        val table = slotTableRecord.store.first()
        val tree = table.asTree()
        val inlines =
            tree
                .all()
                .filter {
                    it.location?.sourceFile == "OffsetData.kt" &&
                        it.name != null &&
                        it.name != "remember"
                }
                .map { it.name!! to it.isInline }

        assertArrayEquals(
            arrayListOf(
                "MyComposeTheme" to false,
                "Column" to true,
                "Text" to false,
                "Greeting" to false,
                "Text" to false,
                "Surface" to false,
                "Button" to false,
                "Text" to false,
                "Surface" to false,
                "TextButton" to false,
                "Row" to true
            ),
            inlines
        )
    }
}

@OptIn(UiToolingDataApi::class)
fun Group.all(): Iterable<Group> {
    val result = mutableListOf<Group>()
    fun appendAll(group: Group) {
        result.add(group)
        group.children.forEach { appendAll(it) }
    }
    appendAll(this)
    return result
}

fun <T> assertArrayEquals(
    expected: Collection<T>,
    actual: Collection<T>,
    transform: (T) -> String = { "$it" }
) {
    TestCase.assertEquals(
        expected.joinToString("\n", transform = transform),
        actual.joinToString("\n", transform = transform)
    )
}
