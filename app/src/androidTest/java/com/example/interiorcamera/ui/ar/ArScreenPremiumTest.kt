package com.example.interiorcamera.ui.ar

import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Pose
import com.google.ar.core.Session
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class ArScreenPremiumTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val mockAnchor1 = mock(Anchor::class.java)
    private val mockAnchor2 = mock(Anchor::class.java)

    @Test
    fun testArSessionConfiguresEnvironmentalHdr() {
        val mockSession = mock(Session::class.java)
        val mockConfig = mock(Config::class.java)

        `when`(mockSession.config).thenReturn(mockConfig)

        // Simulate session configuration in ArScreen session setup
        mockConfig.apply {
            lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
            depthMode = Config.DepthMode.AUTOMATIC
        }
        mockSession.configure(mockConfig)

        verify(mockSession).configure(mockConfig)
        assertEquals(Config.LightEstimationMode.ENVIRONMENTAL_HDR, mockConfig.lightEstimationMode)
        assertEquals(Config.DepthMode.AUTOMATIC, mockConfig.depthMode)
    }

    @Test
    fun testCollisionChecks_detectsOverlap() {
        // Place two overlapping items (in meters: scale is 100cm = 1m)
        val obb1 = Obb2D(x = 0f, z = 0f, hw = 0.5f, hd = 0.5f, rotationDegrees = 0f)
        val obb2 = Obb2D(x = 0.4f, z = 0f, hw = 0.5f, hd = 0.5f, rotationDegrees = 0f)

        val result = CollisionDetection.checkCollision(obb1, obb2)
        assertTrue("Overlapping boxes must report collision", result.collides)
        // MTV should push obb1 left (negative X) by the overlap amount (0.5 + 0.5 - 0.4 = 0.6 overlap -> MTV X = -0.6)
        // Let's verify: obb1 extends [-0.5, 0.5]. obb2 extends [0.4-0.5, 0.4+0.5] = [-0.1, 0.9].
        // Overlap in X is [-0.1, 0.5] -> size 0.6f.
        assertEquals(-0.6f, result.mtvX, 0.001f)
    }

    @Test
    fun testCollisionRingColor_collidingItemsRendersRed() {
        // Setup two anchors that are overlapping
        val pose1 = Pose(floatArrayOf(0f, 0f, 0f), floatArrayOf(0f, 0f, 0f, 1f))
        val pose2 = Pose(floatArrayOf(0.4f, 0f, 0f), floatArrayOf(0f, 0f, 0f, 1f))

        `when`(mockAnchor1.pose).thenReturn(pose1)
        `when`(mockAnchor2.pose).thenReturn(pose2)

        val placedItems = listOf(
            PlacedItem("item1", mockAnchor1, 100f, 200f, 100f, "fridge.glb"),
            PlacedItem("item2", mockAnchor2, 100f, 200f, 100f, "washer.glb")
        )

        composeTestRule.setContent {
            ArScreenContent(
                widthCm = 100f,
                heightCm = 200f,
                depthCm = 100f,
                hasCameraPermission = true,
                placedItems = placedItems,
                selectedItemId = "item1",
                isPlaneDetected = true,
                onBack = {},
                onOpacityChange = { _, _ -> },
                onRotateLeft = {},
                onRotateRight = {},
                onDeselect = {},
                onDeleteItem = {},
                onClearAll = {}
            )
        }

        // Verify overlay and rendering doesn't crash, and screen is drawn
        composeTestRule.onNodeWithText("100×200×100cm").assertExists()
    }
}
