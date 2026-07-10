package com.example.interiorcamera.ui.floorplan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import java.util.Random

class FloorplanCoordinatorStressTest {

    private val random = Random(42) // Fixed seed for reproducibility

    // 1. Coordinate translation round-trip test
    @Test
    fun testCoordinateRoundTripRandomized() {
        val iterations = 10000
        for (i in 0 until iterations) {
            val canvasWidth = random.nextFloat() * 1920f + 10f
            val canvasHeight = random.nextFloat() * 1080f + 10f
            val roomWidthCm = random.nextFloat() * 2000f + 10f
            val roomDepthCm = random.nextFloat() * 2000f + 10f

            val originalX = random.nextFloat() * canvasWidth
            val originalY = random.nextFloat() * canvasHeight

            val (relX, relZ) = FloorplanCoordinator.screenToRelative(
                originalX, originalY, canvasWidth, canvasHeight, roomWidthCm, roomDepthCm
            )
            val (scrX, scrY) = FloorplanCoordinator.relativeToScreen(
                relX, relZ, canvasWidth, canvasHeight, roomWidthCm, roomDepthCm
            )

            // Assert that the round-trip matches the original coordinate within a very small threshold
            assertEquals("Round-trip X failed at iteration $i", originalX, scrX, 0.01f)
            assertEquals("Round-trip Y failed at iteration $i", originalY, scrY, 0.01f)
        }
    }

    // Auxiliary function to get the 4 corners of a block in relative coordinates (meters)
    private fun getCorners(
        offsetX: Float,
        offsetZ: Float,
        rotationDegrees: Float,
        widthCm: Float,
        depthCm: Float
    ): List<Pair<Float, Float>> {
        val angleRad = Math.toRadians(rotationDegrees.toDouble())
        val c = cos(angleRad).toFloat()
        val s = sin(angleRad).toFloat()
        val hw = (widthCm / 100f) / 2f
        val hd = (depthCm / 100f) / 2f

        val localCorners = listOf(
            Pair(-hw, -hd),
            Pair(hw, -hd),
            Pair(hw, hd),
            Pair(-hw, hd)
        )

        return localCorners.map { (lx, lz) ->
            val gx = offsetX + (lx * c - lz * s)
            val gz = offsetZ + (lx * s + lz * c)
            Pair(gx, gz)
        }
    }

    // 2. Room bounds clamping correctness for any rotation angle
    @Test
    fun testRoomBoundsClampingRandomized() {
        val iterations = 10000
        var successCount = 0
        var skippedDueToSize = 0

        for (i in 0 until iterations) {
            val roomWidthCm = random.nextFloat() * 1000f + 50f
            val roomDepthCm = random.nextFloat() * 1000f + 50f

            val blockWidthCm = random.nextFloat() * 200f + 10f
            val blockDepthCm = random.nextFloat() * 200f + 10f

            val rotationDegrees = random.nextFloat() * 720f - 360f

            // Let's compute the half-extents to ensure the block CAN fit in the room
            val rw = (roomWidthCm / 100f) / 2f
            val rd = (roomDepthCm / 100f) / 2f

            val angleRad = Math.toRadians(rotationDegrees.toDouble()).toFloat()
            val c = cos(angleRad)
            val s = sin(angleRad)

            val hw = (blockWidthCm / 100f) / 2f
            val hd = (blockDepthCm / 100f) / 2f

            val aabbHalfW = abs(c * hw) + abs(-s * hd)
            val aabbHalfD = abs(s * hw) + abs(c * hd)

            if (aabbHalfW > rw || aabbHalfD > rd) {
                // The item cannot fit inside the room at this rotation angle
                skippedDueToSize++
                continue
            }

            // Generate an offset that is potentially outside the room
            val offsetX = random.nextFloat() * 20f - 10f
            val offsetZ = random.nextFloat() * 20f - 10f

            val (clampedX, clampedZ) = FloorplanCoordinator.clampToRoomBounds(
                offsetX, offsetZ, rotationDegrees, blockWidthCm, blockDepthCm, roomWidthCm, roomDepthCm
            )

            // Verify all 4 corners are inside the room bounds
            val corners = getCorners(clampedX, clampedZ, rotationDegrees, blockWidthCm, blockDepthCm)
            val eps = 1e-4f
            for (corner in corners) {
                assertTrue(
                    "Corner $corner X is outside room bounds X [-rw, rw] = [-$rw, $rw] at iteration $i",
                    corner.first >= -rw - eps && corner.first <= rw + eps
                )
                assertTrue(
                    "Corner $corner Z is outside room bounds Z [-$rd, rd] = [-$rd, $rd] at iteration $i",
                    corner.second >= -rd - eps && corner.second <= rd + eps
                )
            }
            successCount++
        }
        println("Randomized clamping verification: $successCount succeeded, $skippedDueToSize skipped due to size limits.")
    }

    // 3. Edge cases and extreme inputs
    @Test
    fun testClampingWithInvalidOrExtremeInputs() {
        // Case A: Block is too large for the room (Clamps to 0f)
        val (clampedX1, clampedZ1) = FloorplanCoordinator.clampToRoomBounds(
            offsetX = 0f,
            offsetZ = 0f,
            rotationDegrees = 0f,
            blockWidthCm = 500f,  // 5m wide
            blockDepthCm = 500f,  // 5m deep
            roomWidthCm = 300f,   // 3m wide room
            roomDepthCm = 300f    // 3m deep room
        )
        assertEquals(0f, clampedX1, 0.001f)
        assertEquals(0f, clampedZ1, 0.001f)

        // Case B: Negative dimensions (Sanitized)
        val (clampedX2, clampedZ2) = FloorplanCoordinator.clampToRoomBounds(
            offsetX = 0f,
            offsetZ = 0f,
            rotationDegrees = 0f,
            blockWidthCm = 100f,
            blockDepthCm = 100f,
            roomWidthCm = -300f,   // Negative room width -> sanitized to 300
            roomDepthCm = 300f
        )
        assertTrue(clampedX2.isFinite())
        assertTrue(clampedZ2.isFinite())

        // Case C: Extreme rotation degrees (10^9 degrees)
        // Should not crash, and should compute correct clamped coordinates
        val (clampedX, clampedZ) = FloorplanCoordinator.clampToRoomBounds(
            offsetX = 0f,
            offsetZ = 0f,
            rotationDegrees = 1000000000f,
            blockWidthCm = 100f,
            blockDepthCm = 100f,
            roomWidthCm = 300f,
            roomDepthCm = 300f
        )
        // Check that result is finite and not NaN
        assertTrue("Extreme rotation should yield finite X", clampedX.isFinite())
        assertTrue("Extreme rotation should yield finite Z", clampedZ.isFinite())
    }
}
