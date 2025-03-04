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

package androidx.wear.compose.material3

import android.os.Build
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.RevealActionType
import androidx.wear.compose.foundation.RevealValue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class SwipeToRevealScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule val testName = TestName()

    @OptIn(ExperimentalWearFoundationApi::class)
    @Test
    fun swipeToReveal_showsPrimaryAction() {
        rule.verifyScreenshot(screenshotRule = screenshotRule, methodName = testName.methodName) {
            SwipeToReveal(
                modifier = Modifier.testTag(TEST_TAG),
                revealState = rememberRevealState(initialValue = RevealValue.Revealing),
                actions = {
                    primaryAction(
                        {},
                        { Icon(Icons.Outlined.Close, contentDescription = "Clear") },
                        "Clear"
                    )
                }
            ) {
                Button({}, Modifier.fillMaxWidth()) {
                    Text("This text should be partially visible.")
                }
            }
        }
    }

    @OptIn(ExperimentalWearFoundationApi::class)
    @Test
    fun swipeToReveal_showsPrimaryAndSecondaryActions() {
        rule.verifyScreenshot(screenshotRule = screenshotRule, methodName = testName.methodName) {
            SwipeToReveal(
                modifier = Modifier.testTag(TEST_TAG),
                revealState =
                    rememberRevealState(
                        initialValue = RevealValue.Revealing,
                        anchorWidth = SwipeToRevealDefaults.DoubleActionAnchorWidth
                    ),
                actions = {
                    primaryAction(
                        {},
                        { Icon(Icons.Outlined.Close, contentDescription = "Clear") },
                        "Clear"
                    )
                    secondaryAction(
                        {},
                        { Icon(Icons.Outlined.MoreVert, contentDescription = "More") },
                        "More"
                    )
                }
            ) {
                Button({}, Modifier.fillMaxWidth()) {
                    Text("This text should be partially visible.")
                }
            }
        }
    }

    @OptIn(ExperimentalWearFoundationApi::class)
    @Test
    fun swipeToReveal_showsUndoPrimaryAction() {
        rule.verifyScreenshot(screenshotRule = screenshotRule, methodName = testName.methodName) {
            SwipeToReveal(
                modifier = Modifier.testTag(TEST_TAG),
                revealState = rememberRevealState(initialValue = RevealValue.Revealed),
                actions = {
                    primaryAction({}, /* Empty for testing */ {}, /* Empty for testing */ "")
                    undoPrimaryAction({}, "Undo Primary Action")
                }
            ) {
                Button({}) { Text(/* Empty for testing */ "") }
            }
        }
    }

    @OptIn(ExperimentalWearFoundationApi::class)
    @Test
    fun swipeToReveal_showsUndoSecondaryAction() {
        rule.verifyScreenshot(screenshotRule = screenshotRule, methodName = testName.methodName) {
            SwipeToReveal(
                modifier = Modifier.testTag(TEST_TAG),
                revealState =
                    rememberRevealState(initialValue = RevealValue.Revealed).apply {
                        lastActionType = RevealActionType.SecondaryAction
                    },
                actions = {
                    primaryAction({}, /* Empty for testing */ {}, /* Empty for testing */ "")
                    undoPrimaryAction({}, /* Empty for testing */ "")
                    secondaryAction({}, /* Empty for testing */ {}, /* Empty for testing */ "")
                    undoSecondaryAction({}, "Undo Secondary Action")
                }
            ) {
                Button({}) { Text(/* Empty for testing */ "") }
            }
        }
    }

    @OptIn(ExperimentalWearFoundationApi::class)
    @Test
    fun swipeToReveal_showsContent() {
        rule.verifyScreenshot(screenshotRule = screenshotRule, methodName = testName.methodName) {
            SwipeToReveal(
                modifier = Modifier.testTag(TEST_TAG),
                actions = {
                    primaryAction({}, /* Empty for testing */ {}, /* Empty for testing */ "")
                }
            ) {
                Button({}, Modifier.fillMaxWidth()) {
                    Text("This content should be fully visible.")
                }
            }
        }
    }

    @OptIn(ExperimentalWearFoundationApi::class)
    @Test
    fun swipeToRevealCard_showsLargePrimaryAction() {
        rule.verifyScreenshot(testName.methodName, screenshotRule) {
            SwipeToReveal(
                modifier = Modifier.testTag(TEST_TAG),
                revealState = rememberRevealState(initialValue = RevealValue.Revealing),
                actionButtonHeight = SwipeToRevealDefaults.LargeActionButtonHeight,
                actions = {
                    primaryAction(
                        {},
                        { Icon(Icons.Outlined.Close, contentDescription = "Clear") },
                        "Clear"
                    )
                }
            ) {
                Card({}, Modifier.fillMaxWidth()) {
                    Text("This content should be partially visible.")
                }
            }
        }
    }

    @OptIn(ExperimentalWearFoundationApi::class)
    @Test
    fun swipeToRevealCard_showsLargePrimaryAndSecondaryActions() {
        rule.verifyScreenshot(screenshotRule = screenshotRule, methodName = testName.methodName) {
            SwipeToReveal(
                modifier = Modifier.testTag(TEST_TAG),
                revealState =
                    rememberRevealState(
                        initialValue = RevealValue.Revealing,
                        anchorWidth = SwipeToRevealDefaults.DoubleActionAnchorWidth
                    ),
                actionButtonHeight = SwipeToRevealDefaults.LargeActionButtonHeight,
                actions = {
                    primaryAction(
                        {},
                        { Icon(Icons.Outlined.Close, contentDescription = "Clear") },
                        "Clear"
                    )
                    secondaryAction(
                        {},
                        { Icon(Icons.Outlined.MoreVert, contentDescription = "More") },
                        "More"
                    )
                }
            ) {
                Card({}, Modifier.fillMaxWidth()) {
                    Text("This content should be partially visible.")
                }
            }
        }
    }
}
