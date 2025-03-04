/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.ui.text.samples

import androidx.annotation.Sampled
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

@Sampled
@Composable
fun BaselineShiftSample() {
    Text(
        fontSize = 20.sp,
        text =
            buildAnnotatedString {
                append(text = "Hello")
                withStyle(SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = 16.sp)) {
                    append("superscript")
                    withStyle(SpanStyle(baselineShift = BaselineShift.Subscript)) {
                        append("subscript")
                    }
                }
            }
    )
}

@Sampled
@Composable
fun BaselineShiftAnnotatedStringSample() {
    val annotatedString = buildAnnotatedString {
        append("Text ")
        withStyle(SpanStyle(baselineShift = BaselineShift.Superscript)) { append("Demo") }
    }
    Text(text = annotatedString)
}
