package com.example.interiorcamera.test

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.ComposeContentTestRule

/**
 * Utility extensions and custom matchers for Jetpack Compose UI tests.
 */
object ComposeTestHelpers {

    /**
     * Asserts that a Compose Slider node has the expected progress/value.
     */
    fun SemanticsNodeInteraction.assertSliderValue(expectedValue: Float) {
        this.assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.ProgressBarRangeInfo,
                ProgressBarRangeInfo(current = expectedValue, range = 0f..1f)
            )
        )
    }

    /**
     * Waits for a Compose node matching the given [matcher] to exist in the tree.
     */
    fun ComposeContentTestRule.waitUntilNodeExists(
        matcher: SemanticsMatcher,
        timeoutMillis: Long = 5000
    ) {
        this.waitUntil(timeoutMillis) {
            this.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
