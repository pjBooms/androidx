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

package androidx.compose.ui.viewinterop

import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class EditTextInteropTest {
    @get:Rule val rule = createAndroidComposeRule<TestActivity>()

    @Test
    fun hardwareKeyInEmbeddedView() {
        // Arrange.
        lateinit var editText: EditText
        lateinit var ownerView: View
        rule.setContent {
            ownerView = LocalView.current
            AndroidView({
                EditText(it).apply {
                    width = 500
                    editText = this
                }
            })
        }
        rule.runOnIdle { editText.requestFocus() }

        // Act.
        val keyConsumed =
            rule.runOnIdle {
                ownerView.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_A))
            }

        // Assert.
        // TODO(b/171997891): Right now we just assert that we reach here without crashing. Once we
        //  propagate hardware keys correctly, assert that the EditText receives the entered value.
        assertThat(keyConsumed).isFalse()
    }
}
