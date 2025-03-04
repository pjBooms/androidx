/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.material3

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
@LargeTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
class DateInputScreenshotTest(private val scheme: ColorSchemeWrapper) {

    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    private val wrap = Modifier.wrapContentSize(Alignment.Center)
    private val wrapperTestTag = "dateInputWrapper"

    @Test
    fun dateInput_initialState() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(wrapperTestTag)) {
                DatePicker(
                    state = rememberDatePickerState(initialDisplayMode = DisplayMode.Input),
                    showModeToggle = false
                )
            }
        }
        assertAgainstGolden("dateInput_initialState_${scheme.name}")
    }

    @Test
    fun dateInput_withModeToggle() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(wrapperTestTag)) {
                DatePicker(state = rememberDatePickerState(initialDisplayMode = DisplayMode.Input))
            }
        }
        assertAgainstGolden("dateInput_withModeToggle_${scheme.name}")
    }

    @Test
    fun dateInput_withEnteredDate() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(wrapperTestTag)) {
                val dayMillis = dayInUtcMilliseconds(year = 2021, month = 3, dayOfMonth = 6)
                DatePicker(
                    state =
                        rememberDatePickerState(
                            initialSelectedDateMillis = dayMillis,
                            initialDisplayMode = DisplayMode.Input
                        ),
                    showModeToggle = false
                )
            }
        }
        assertAgainstGolden("dateInput_withEnteredDate_${scheme.name}")
    }

    @Test
    fun dateInput_invalidDateInput() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(wrapperTestTag)) {
                val monthInUtcMillis = dayInUtcMilliseconds(year = 2000, month = 6, dayOfMonth = 1)
                DatePicker(
                    state =
                        rememberDatePickerState(
                            initialDisplayedMonthMillis = monthInUtcMillis,
                            initialDisplayMode = DisplayMode.Input,
                            selectableDates =
                                object : SelectableDates {
                                    // All dates are invalid for the sake of this test.
                                    override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                                        false
                                }
                        ),
                    showModeToggle = false
                )
            }
        }
        assertAgainstGolden("dateInput_invalidDateInput_${scheme.name}")
    }

    @Test
    fun dateInput_inDialog() {
        rule.setMaterialContent(scheme.colorScheme) {
            val selectedDayMillis = dayInUtcMilliseconds(year = 2021, month = 3, dayOfMonth = 6)
            DatePickerDialog(
                onDismissRequest = {},
                confirmButton = { TextButton(onClick = {}) { Text("OK") } },
                dismissButton = { TextButton(onClick = {}) { Text("Cancel") } }
            ) {
                DatePicker(
                    state =
                        rememberDatePickerState(
                            initialSelectedDateMillis = selectedDayMillis,
                            initialDisplayMode = DisplayMode.Input
                        ),
                    showModeToggle = false
                )
            }
        }
        rule
            .onNode(isDialog())
            .captureToImage()
            .assertAgainstGolden(
                rule = screenshotRule,
                goldenIdentifier = "dateInput_inDialog_${scheme.name}"
            )
    }

    // Returns the given date's day as milliseconds from epoch. The returned value is for the day's
    // start on midnight.
    private fun dayInUtcMilliseconds(year: Int, month: Int, dayOfMonth: Int): Long =
        LocalDate.of(year, month, dayOfMonth)
            .atTime(LocalTime.MIDNIGHT)
            .atZone(ZoneId.of("UTC"))
            .toInstant()
            .toEpochMilli()

    private fun assertAgainstGolden(goldenName: String) {
        rule
            .onNodeWithTag(wrapperTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenName)
    }

    // Provide the ColorScheme and their name parameter in a ColorSchemeWrapper.
    // This makes sure that the default method name and the initial Scuba image generated
    // name is as expected.
    companion object {
        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun parameters() =
            arrayOf(
                ColorSchemeWrapper("lightTheme", lightColorScheme()),
                ColorSchemeWrapper("darkTheme", darkColorScheme()),
            )
    }

    class ColorSchemeWrapper(val name: String, val colorScheme: ColorScheme) {
        override fun toString(): String {
            return name
        }
    }
}
