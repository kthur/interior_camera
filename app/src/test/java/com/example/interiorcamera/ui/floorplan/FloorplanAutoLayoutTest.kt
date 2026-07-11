package com.example.interiorcamera.ui.floorplan

import com.example.interiorcamera.ui.ar.CollisionDetection
import com.example.interiorcamera.ui.ar.Obb2D
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

class FloorplanAutoLayoutTest {

    private fun toObb(item: ArPlacedItem): Obb2D {
        val hw = (item.widthCm / 100f) / 2f
        val hd = (item.depthCm / 100f) / 2f
        return Obb2D(
            x = item.offsetX,
            z = item.offsetZ,
            hw = hw,
            hd = hd,
            rotationDegrees = item.rotationDegrees
        )
    }

    @Test
    fun testConcentricDispersion() {
        val item1 = ArPlacedItem(
            name = "Item 1",
            widthCm = 50f,
            heightCm = 100f,
            depthCm = 50f,
            modelName = "cube.glb",
            offsetX = 0f,
            offsetZ = 0f,
            rotationDegrees = 0f
        )
        val item2 = item1.copy(name = "Item 2")
        val item3 = item1.copy(name = "Item 3")

        val aligned = FloorplanAutoLayout.align(listOf(item1, item2, item3), 300f, 300f)

        assertEquals(3, aligned.size)

        // Verify that no two items are within 2cm (0.02m) of each other
        for (i in aligned.indices) {
            for (j in i + 1 until aligned.size) {
                val dx = aligned[i].offsetX - aligned[j].offsetX
                val dz = aligned[i].offsetZ - aligned[j].offsetZ
                val distance = sqrt((dx * dx + dz * dz).toDouble())
                assertTrue("Distance between item $i and item $j should be >= 2cm, but was $distance", distance >= 0.02)
            }
        }
    }

    @Test
    fun testRotationSnapping() {
        val item1 = ArPlacedItem(
            name = "Item 1",
            widthCm = 50f,
            heightCm = 100f,
            depthCm = 50f,
            modelName = "cube.glb",
            offsetX = 0f,
            offsetZ = 0f,
            rotationDegrees = 42f
        )
        val item2 = item1.copy(name = "Item 2", rotationDegrees = 48f)
        val item3 = item1.copy(name = "Item 3", rotationDegrees = -15f)

        val aligned = FloorplanAutoLayout.align(listOf(item1, item2, item3), 300f, 300f)

        assertEquals(0f, aligned[0].rotationDegrees, 0.001f)
        assertEquals(90f, aligned[1].rotationDegrees, 0.001f)
        assertEquals(0f, aligned[2].rotationDegrees, 0.001f)
    }

    @Test
    fun testWallSnapping() {
        // Room: 300cm x 300cm -> rw = 1.5m, rd = 1.5m
        // Item: 100cm x 100cm -> hw = 0.5m, hd = 0.5m
        // Near right wall: offsetX = 0.8m. Edge is at 0.8 + 0.5 = 1.3m. Distance to wall = 1.5 - 1.3 = 0.20m <= 0.25m.
        val itemRight = ArPlacedItem(
            name = "Item Right",
            widthCm = 100f,
            heightCm = 100f,
            depthCm = 100f,
            modelName = "cube.glb",
            offsetX = 0.8f,
            offsetZ = 0f,
            rotationDegrees = 0f
        )

        // Near left wall: offsetX = -0.9m. Edge is at -0.9 - 0.5 = -1.4m. Distance to wall = -1.4 - (-1.5) = 0.10m <= 0.25m.
        val itemLeft = ArPlacedItem(
            name = "Item Left",
            widthCm = 100f,
            heightCm = 100f,
            depthCm = 100f,
            modelName = "cube.glb",
            offsetX = -0.9f,
            offsetZ = 0f,
            rotationDegrees = 0f
        )

        val aligned = FloorplanAutoLayout.align(listOf(itemRight, itemLeft), 300f, 300f)

        // Snapped right item should have offsetX = rw - hw = 1.5 - 0.5 = 1.0m
        assertEquals(1.0f, aligned[0].offsetX, 0.001f)
        // Snapped left item should have offsetX = -rw + hw = -1.5 + 0.5 = -1.0m
        assertEquals(-1.0f, aligned[1].offsetX, 0.001f)
    }

    @Test
    fun testOverlapResolution() {
        // Two overlapping items:
        // A: 100cm x 100cm at (0f, 0f)
        // B: 100cm x 100cm at (0.1f, 0f)
        val itemA = ArPlacedItem(
            name = "Item A",
            widthCm = 100f,
            heightCm = 100f,
            depthCm = 100f,
            modelName = "cube.glb",
            offsetX = 0f,
            offsetZ = 0f,
            rotationDegrees = 0f
        )
        val itemB = ArPlacedItem(
            name = "Item B",
            widthCm = 100f,
            heightCm = 100f,
            depthCm = 100f,
            modelName = "cube.glb",
            offsetX = 0.1f,
            offsetZ = 0f,
            rotationDegrees = 0f
        )

        val aligned = FloorplanAutoLayout.align(listOf(itemA, itemB), 300f, 300f)

        val obbA = toObb(aligned[0])
        val obbB = toObb(aligned[1])
        val result = CollisionDetection.checkCollision(obbA, obbB)

        assertFalse("Aligned items should not overlap", result.collides)
    }

    @Test
    fun testRoomBoundariesPreservation() {
        // Place items outside boundaries or overlapping.
        // Room: 200cm x 200cm -> rw = 1.0m, rd = 1.0m
        // Item: 100cm x 100cm at (2f, 2f)
        val itemA = ArPlacedItem(
            name = "Item A",
            widthCm = 100f,
            heightCm = 100f,
            depthCm = 100f,
            modelName = "cube.glb",
            offsetX = 2f,
            offsetZ = 2f,
            rotationDegrees = 0f
        )
        val itemB = ArPlacedItem(
            name = "Item B",
            widthCm = 100f,
            heightCm = 100f,
            depthCm = 100f,
            modelName = "cube.glb",
            offsetX = -2f,
            offsetZ = -2f,
            rotationDegrees = 0f
        )

        val aligned = FloorplanAutoLayout.align(listOf(itemA, itemB), 200f, 200f)

        for (item in aligned) {
            val (clampedX, clampedZ) = FloorplanCoordinator.clampToRoomBounds(
                offsetX = item.offsetX,
                offsetZ = item.offsetZ,
                rotationDegrees = item.rotationDegrees,
                blockWidthCm = item.widthCm,
                blockDepthCm = item.depthCm,
                roomWidthCm = 200f,
                roomDepthCm = 200f
            )
            assertEquals("Item ${item.name} offsetX should be within bounds", clampedX, item.offsetX, 0.001f)
            assertEquals("Item ${item.name} offsetZ should be within bounds", clampedZ, item.offsetZ, 0.001f)
        }
    }
}
