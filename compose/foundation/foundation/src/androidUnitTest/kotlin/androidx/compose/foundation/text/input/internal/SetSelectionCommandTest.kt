/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.foundation.text.input.internal

import androidx.compose.ui.text.TextRange
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SetSelectionCommandTest {

    @Test
    fun test_set() {
        val eb = TextFieldBuffer("ABCDE", TextRange.Zero)

        eb.setSelection(1, 4)

        assertThat(eb.toString()).isEqualTo("ABCDE")
        assertThat(eb.selection.start).isEqualTo(1)
        assertThat(eb.selection.end).isEqualTo(4)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_preserve_ongoing_composition() {
        val eb = TextFieldBuffer("ABCDE", TextRange.Zero)

        eb.setComposition(1, 3)

        eb.setSelection(2, 4)

        assertThat(eb.toString()).isEqualTo("ABCDE")
        assertThat(eb.selection.start).isEqualTo(2)
        assertThat(eb.selection.end).isEqualTo(4)
        assertThat(eb.hasComposition()).isTrue()
        assertThat(eb.composition?.start).isEqualTo(1)
        assertThat(eb.composition?.end).isEqualTo(3)
    }

    @Test
    fun test_cancel_ongoing_selection() {
        val eb = TextFieldBuffer("ABCDE", TextRange(1, 4))

        eb.setSelection(2, 5)

        assertThat(eb.toString()).isEqualTo("ABCDE")
        assertThat(eb.selection.start).isEqualTo(2)
        assertThat(eb.selection.end).isEqualTo(5)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_set_reversed() {
        val eb = TextFieldBuffer("ABCDE", TextRange.Zero)

        eb.setSelection(4, 1)

        assertThat(eb.toString()).isEqualTo("ABCDE")
        assertThat(eb.selection.start).isEqualTo(4)
        assertThat(eb.selection.end).isEqualTo(1)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_set_too_small() {
        val eb = TextFieldBuffer("ABCDE", TextRange.Zero)

        eb.setSelection(-1000, -1000)

        assertThat(eb.toString()).isEqualTo("ABCDE")
        assertThat(eb.selection.start).isEqualTo(0)
        assertThat(eb.selection.end).isEqualTo(0)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_set_too_large() {
        val eb = TextFieldBuffer("ABCDE", TextRange.Zero)

        eb.setSelection(1000, 1000)

        assertThat(eb.toString()).isEqualTo("ABCDE")
        assertThat(eb.selection.start).isEqualTo(5)
        assertThat(eb.selection.end).isEqualTo(5)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_set_too_too_large() {
        val eb = TextFieldBuffer("ABCDE", TextRange.Zero)

        eb.setSelection(0, 1000)

        assertThat(eb.toString()).isEqualTo("ABCDE")
        assertThat(eb.selection.start).isEqualTo(0)
        assertThat(eb.selection.end).isEqualTo(5)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_set_too_large_reversed() {
        val eb = TextFieldBuffer("ABCDE", TextRange.Zero)

        eb.setSelection(1000, 0)

        assertThat(eb.toString()).isEqualTo("ABCDE")
        assertThat(eb.selection.start).isEqualTo(5)
        assertThat(eb.selection.end).isEqualTo(0)
        assertThat(eb.hasComposition()).isFalse()
    }
}
