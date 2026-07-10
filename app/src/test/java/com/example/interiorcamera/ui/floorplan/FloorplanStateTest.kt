package com.example.interiorcamera.ui.floorplan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FloorplanStateTest {

    @Test
    fun testScreenToRelativeMapping_centerOfRoom() {
        val (relX, relZ) = FloorplanCoordinator.screenToRelative(
            screenX = 500f,
            screenY = 500f,
            canvasWidth = 1000f,
            canvasHeight = 1000f,
            roomWidthCm = 400f,
            roomDepthCm = 400f
        )

        assertEquals("Screen center should map to room center (0,0)", 0f, relX, 0.001f)
        assertEquals("Screen center should map to room center (0,0)", 0f, relZ, 0.001f)
    }

    @Test
    fun testRelativeToScreenMapping_centerOfRoom() {
        val (scrX, scrY) = FloorplanCoordinator.relativeToScreen(
            offsetXMeter = 0f,
            offsetZMeter = 0f,
            canvasWidth = 1000f,
            canvasHeight = 1000f,
            roomWidthCm = 400f,
            roomDepthCm = 400f
        )

        assertEquals("Room center (0,0) should map to screen center", 500f, scrX, 0.001f)
        assertEquals("Room center (0,0) should map to screen center", 500f, scrY, 0.001f)
    }

    @Test
    fun testCoordinateRoundTrip() {
        val originalX = 250f
        val originalY = 750f
        val cw = 1000f
        val ch = 800f
        val rw = 300f
        val rd = 400f

        val (relX, relZ) = FloorplanCoordinator.screenToRelative(originalX, originalY, cw, ch, rw, rd)
        val (scrX, scrY) = FloorplanCoordinator.relativeToScreen(relX, relZ, cw, ch, rw, rd)

        assertEquals(originalX, scrX, 0.01f)
        assertEquals(originalY, scrY, 0.01f)
    }

    @Test
    fun testClampToRoomBounds_insideBounds() {
        // Room: 3m x 3m (300cm x 300cm) -> range is [-1.5, 1.5]
        // Item: 1m x 1m (100cm x 100cm) -> half size is 0.5m
        // Target offset is center: (0, 0)
        val (clampedX, clampedZ) = FloorplanCoordinator.clampToRoomBounds(
            offsetX = 0f,
            offsetZ = 0f,
            rotationDegrees = 0f,
            blockWidthCm = 100f,
            blockDepthCm = 100f,
            roomWidthCm = 300f,
            roomDepthCm = 300f
        )

        assertEquals(0f, clampedX, 0.001f)
        assertEquals(0f, clampedZ, 0.001f)
    }

    @Test
    fun testClampToRoomBounds_exceedsXRight() {
        // Room: 3m x 3m -> range is [-1.5, 1.5]
        // Item: 1m x 1m -> half size is 0.5m. Allowed center offset range is [-1.0, 1.0]
        // Target offset is: 1.2m
        val (clampedX, clampedZ) = FloorplanCoordinator.clampToRoomBounds(
            offsetX = 1.2f,
            offsetZ = 0f,
            rotationDegrees = 0f,
            blockWidthCm = 100f,
            blockDepthCm = 100f,
            roomWidthCm = 300f,
            roomDepthCm = 300f
        )

        assertEquals("Should be clamped to maximum allowed center offset 1.0m", 1.0f, clampedX, 0.001f)
        assertEquals(0f, clampedZ, 0.001f)
    }

    @Test
    fun testClampToRoomBounds_exceedsZTop() {
        // Room: 3m x 3m -> range is [-1.5, 1.5]
        // Item: 1m x 1m -> half size is 0.5m. Allowed center offset range is [-1.0, 1.0]
        // Target offset is: -1.5m
        val (clampedX, clampedZ) = FloorplanCoordinator.clampToRoomBounds(
            offsetX = 0f,
            offsetZ = -1.5f,
            rotationDegrees = 0f,
            blockWidthCm = 100f,
            blockDepthCm = 100f,
            roomWidthCm = 300f,
            roomDepthCm = 300f
        )

        assertEquals(0f, clampedX, 0.001f)
        assertEquals("Should be clamped to minimum allowed center offset -1.0m", -1.0f, clampedZ, 0.001f)
    }

    @Test
    fun testClampToRoomBounds_rotatedExceedsBounds() {
        // Room: 3m x 3m
        // Item: 1m x 1m rotated by 45 degrees
        // AABB half-extents are computed based on rotation.
        // Rotation by 45 deg increases the projected width and depth.
        // projected width = cos(45)*0.5 + sin(45)*0.5 = 0.707
        // So allowed range is [-1.5 + 0.707, 1.5 - 0.707] = [-0.793, 0.793]
        // Try offset 0.9m
        val (clampedX, clampedZ) = FloorplanCoordinator.clampToRoomBounds(
            offsetX = 0.9f,
            offsetZ = 0f,
            rotationDegrees = 45f,
            blockWidthCm = 100f,
            blockDepthCm = 100f,
            roomWidthCm = 300f,
            roomDepthCm = 300f
        )

        assertTrue("Rotated item should clamp earlier than non-rotated item", clampedX < 0.8f)
    }
}
