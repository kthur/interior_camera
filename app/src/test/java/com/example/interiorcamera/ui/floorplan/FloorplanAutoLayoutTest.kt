package com.example.interiorcamera.ui.floorplan

import com.example.interiorcamera.ui.ar.CollisionDetection
import com.example.interiorcamera.ui.ar.Obb2D
import com.example.interiorcamera.ui.ar.Vector2D
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
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

    private fun assertWithinRoomBounds(item: ArPlacedItem, roomWidthCm: Float, roomDepthCm: Float) {
        val rw = abs(roomWidthCm / 100f) / 2f
        val rd = abs(roomDepthCm / 100f) / 2f
        val angleRad = Math.toRadians(item.rotationDegrees.toDouble()).toFloat()
        val c = cos(angleRad)
        val s = sin(angleRad)
        val hw = (item.widthCm / 100f) / 2f
        val hd = (item.depthCm / 100f) / 2f

        val ax = Vector2D(c, s)
        val az = Vector2D(-s, c)

        val aabbHalfW = abs(ax.x * hw) + abs(az.x * hd)
        val aabbHalfD = abs(ax.y * hw) + abs(az.y * hd)

        val minX = -rw + aabbHalfW
        val maxX = rw - aabbHalfW
        val minZ = -rd + aabbHalfD
        val maxZ = rd - aabbHalfD

        val tolerance = 0.001f
        assertTrue(
            "Item ${item.name} center X (${item.offsetX}) is out of room bounds center range [$minX, $maxX] for room width $roomWidthCm",
            item.offsetX in (minX - tolerance)..(maxX + tolerance) || minX > maxX
        )
        assertTrue(
            "Item ${item.name} center Z (${item.offsetZ}) is out of room bounds center range [$minZ, $maxZ] for room depth $roomDepthCm",
            item.offsetZ in (minZ - tolerance)..(maxZ + tolerance) || minZ > maxZ
        )

        // Translate to absolute coordinates (cm) and assert 0 <= X <= roomWidthCm and 0 <= Z <= roomDepthCm
        val absoluteX = item.offsetX * 100f + roomWidthCm / 2f
        val absoluteZ = item.offsetZ * 100f + roomDepthCm / 2f
        assertTrue(
            "Item ${item.name} absolute X $absoluteX is not within [0, $roomWidthCm]",
            absoluteX in -tolerance..(roomWidthCm + tolerance)
        )
        assertTrue(
            "Item ${item.name} absolute Z $absoluteZ is not within [0, $roomDepthCm]",
            absoluteZ in -tolerance..(roomDepthCm + tolerance)
        )
    }

    @Test
    fun testStress100ConcentricItems() {
        val baseItem = ArPlacedItem(
            name = "Concentric Item",
            widthCm = 40f,
            heightCm = 100f,
            depthCm = 40f,
            modelName = "cube.glb",
            offsetX = 0f,
            offsetZ = 0f,
            rotationDegrees = 0f
        )
        val items = List(100) { i -> baseItem.copy(name = "Concentric Item $i") }
        val aligned = FloorplanAutoLayout.align(items, 1000f, 1000f)

        assertEquals(100, aligned.size)

        // Verify bounds preservation for all
        for (item in aligned) {
            assertWithinRoomBounds(item, 1000f, 1000f)
        }

        // Verify pairwise separation
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
    fun testItemsPlacedOutsideRoomBoundaries() {
        val item1 = ArPlacedItem(
            name = "Item Far Right Out",
            widthCm = 50f,
            heightCm = 100f,
            depthCm = 50f,
            modelName = "cube.glb",
            offsetX = 5.0f, // 5m is outside 3m room (-1.5m to 1.5m)
            offsetZ = 0.5f,
            rotationDegrees = 0f
        )
        val item2 = ArPlacedItem(
            name = "Item Far Left Out",
            widthCm = 50f,
            heightCm = 100f,
            depthCm = 50f,
            modelName = "cube.glb",
            offsetX = -5.0f,
            offsetZ = -0.5f,
            rotationDegrees = 90f
        )
        val roomWidth = 300f
        val roomDepth = 300f

        val aligned = FloorplanAutoLayout.align(listOf(item1, item2), roomWidth, roomDepth)

        assertEquals(2, aligned.size)
        for (item in aligned) {
            assertWithinRoomBounds(item, roomWidth, roomDepth)
        }

        // Verify they do not collide
        val obb1 = toObb(aligned[0])
        val obb2 = toObb(aligned[1])
        val result = CollisionDetection.checkCollision(obb1, obb2)
        assertFalse("Aligned items should not overlap", result.collides)
    }

    @Test
    fun testExtremeRoomDimensions() {
        // Very small room
        val itemSmallRoom = ArPlacedItem(
            name = "Item for Small Room",
            widthCm = 50f,
            heightCm = 100f,
            depthCm = 50f,
            modelName = "cube.glb",
            offsetX = 0f,
            offsetZ = 0f,
            rotationDegrees = 0f
        )
        val roomWidthSmall = 10f
        val roomDepthSmall = 10f
        val alignedSmall = FloorplanAutoLayout.align(listOf(itemSmallRoom), roomWidthSmall, roomDepthSmall)
        assertEquals(1, alignedSmall.size)
        assertWithinRoomBounds(alignedSmall[0], roomWidthSmall, roomDepthSmall)

        // Very large room
        val itemLargeRoom = ArPlacedItem(
            name = "Item for Large Room",
            widthCm = 100f,
            heightCm = 100f,
            depthCm = 100f,
            modelName = "cube.glb",
            offsetX = 45f,
            offsetZ = 45f,
            rotationDegrees = 0f
        )
        val roomWidthLarge = 10000f
        val roomDepthLarge = 10000f
        val alignedLarge = FloorplanAutoLayout.align(listOf(itemLargeRoom), roomWidthLarge, roomDepthLarge)
        assertEquals(1, alignedLarge.size)
        assertWithinRoomBounds(alignedLarge[0], roomWidthLarge, roomDepthLarge)
    }

    @Test
    fun testHighDensityOverlapping() {
        val items = mutableListOf<ArPlacedItem>()
        var count = 0
        for (r in -2..2) {
            for (c in -2..2) {
                items.add(
                    ArPlacedItem(
                        name = "GridItem_$count",
                        widthCm = 80f,
                        heightCm = 100f,
                        depthCm = 80f,
                        modelName = "cube.glb",
                        offsetX = c * 0.2f,
                        offsetZ = r * 0.2f,
                        rotationDegrees = 0f
                    )
                )
                count++
            }
        }

        val roomWidth = 300f
        val roomDepth = 300f
        val aligned = FloorplanAutoLayout.align(items, roomWidth, roomDepth)

        assertEquals(25, aligned.size)
        for (item in aligned) {
            assertWithinRoomBounds(item, roomWidth, roomDepth)
        }
    }

    @Test
    fun testLargeAndUnusualRotations() {
        val item1 = ArPlacedItem(
            name = "Item Rotated 450",
            widthCm = 60f,
            heightCm = 100f,
            depthCm = 60f,
            modelName = "cube.glb",
            offsetX = 0f,
            offsetZ = 0f,
            rotationDegrees = 450f
        )
        val item2 = ArPlacedItem(
            name = "Item Rotated -720",
            widthCm = 60f,
            heightCm = 100f,
            depthCm = 60f,
            modelName = "cube.glb",
            offsetX = 0.5f,
            offsetZ = 0f,
            rotationDegrees = -720f
        )
        val item3 = ArPlacedItem(
            name = "Item Rotated 137.5",
            widthCm = 60f,
            heightCm = 100f,
            depthCm = 60f,
            modelName = "cube.glb",
            offsetX = -0.5f,
            offsetZ = 0.5f,
            rotationDegrees = 137.5f
        )

        val roomWidth = 400f
        val roomDepth = 400f
        val aligned = FloorplanAutoLayout.align(listOf(item1, item2, item3), roomWidth, roomDepth)

        assertEquals(3, aligned.size)
        // 450 mod 360 = 90
        assertEquals(90f, aligned[0].rotationDegrees, 0.001f)
        // -720 mod 360 = 0
        assertEquals(0f, aligned[1].rotationDegrees, 0.001f)
        // 137.5 snapped to nearest 90-degree increment -> 180
        assertEquals(180f, aligned[2].rotationDegrees, 0.001f)

        for (item in aligned) {
            assertWithinRoomBounds(item, roomWidth, roomDepth)
        }
    }

    private fun getCorners(item: ArPlacedItem): List<Pair<Float, Float>> {
        val angleRad = Math.toRadians(item.rotationDegrees.toDouble())
        val c = kotlin.math.cos(angleRad).toFloat()
        val s = kotlin.math.sin(angleRad).toFloat()
        val hw = (item.widthCm / 100f) / 2f
        val hd = (item.depthCm / 100f) / 2f

        val localCorners = listOf(
            Pair(-hw, -hd),
            Pair(hw, -hd),
            Pair(hw, hd),
            Pair(-hw, hd)
        )

        return localCorners.map { (lx, lz) ->
            val gx = item.offsetX + (lx * c - lz * s)
            val gz = item.offsetZ + (lx * s + lz * c)
            Pair(gx, gz)
        }
    }

    private fun verifyWithinRoomBounds(item: ArPlacedItem, roomWidthCm: Float, roomDepthCm: Float) {
        val x = item.offsetX * 100f + roomWidthCm / 2f
        val z = item.offsetZ * 100f + roomDepthCm / 2f
        val eps = 1e-3f
        assertTrue("Item ${item.name} center X ($x) must be >= 0", x >= -eps)
        assertTrue("Item ${item.name} center X ($x) must be <= $roomWidthCm", x <= roomWidthCm + eps)
        assertTrue("Item ${item.name} center Z ($z) must be >= 0", z >= -eps)
        assertTrue("Item ${item.name} center Z ($z) must be <= $roomDepthCm", z <= roomDepthCm + eps)
    }

    private fun verifyEntireItemWithinRoomBounds(item: ArPlacedItem, roomWidthCm: Float, roomDepthCm: Float) {
        val rw = abs(roomWidthCm / 100f) / 2f
        val rd = abs(roomDepthCm / 100f) / 2f
        val corners = getCorners(item)
        val eps = 1e-3f

        for (i in corners.indices) {
            val (cx, cz) = corners[i]
            assertTrue(
                "Item ${item.name} corner $i X ($cx) is left of wall (-rw = -$rw)",
                cx >= -rw - eps
            )
            assertTrue(
                "Item ${item.name} corner $i X ($cx) is right of wall (rw = $rw)",
                cx <= rw + eps
            )
            assertTrue(
                "Item ${item.name} corner $i Z ($cz) is top of wall (-rd = -$rd)",
                cz >= -rd - eps
            )
            assertTrue(
                "Item ${item.name} corner $i Z ($cz) is bottom of wall (rd = $rd)",
                cz <= rd + eps
            )
        }
    }

    @Test
    fun test100StackedConcentricItemsAtCenter() {
        val roomWidth = 500f
        val roomDepth = 500f
        val items = List(100) { i ->
            ArPlacedItem(
                name = "Concentric Item $i",
                widthCm = 30f,
                heightCm = 100f,
                depthCm = 30f,
                modelName = "cube.glb",
                offsetX = 0f,
                offsetZ = 0f,
                rotationDegrees = 0f
            )
        }

        val aligned = FloorplanAutoLayout.align(items, roomWidth, roomDepth)

        assertEquals(100, aligned.size)

        // Verify that all final coordinates are strictly within room boundaries
        for (item in aligned) {
            verifyWithinRoomBounds(item, roomWidth, roomDepth)
        }
    }

    @Test
    fun testItemsPlacedOutsideRoomBoundariesInitially() {
        val roomWidth = 400f
        val roomDepth = 400f
        val outsideItems = listOf(
            ArPlacedItem("Item Far Right", 80f, 100f, 80f, "cube.glb", 5.0f, 0f, 0f),
            ArPlacedItem("Item Far Left", 80f, 100f, 80f, "cube.glb", -5.0f, 0f, 0f),
            ArPlacedItem("Item Far Top", 80f, 100f, 80f, "cube.glb", 0f, 5.0f, 0f),
            ArPlacedItem("Item Far Bottom", 80f, 100f, 80f, "cube.glb", 0f, -5.0f, 0f),
            ArPlacedItem("Item Corner TopRight", 80f, 100f, 80f, "cube.glb", 10.0f, 10.0f, 0f)
        )

        val aligned = FloorplanAutoLayout.align(outsideItems, roomWidth, roomDepth)

        for (item in aligned) {
            verifyWithinRoomBounds(item, roomWidth, roomDepth)
            verifyEntireItemWithinRoomBounds(item, roomWidth, roomDepth)
        }
    }

    @Test
    fun testExtremelyLargeOrSmallRoomDimensions() {
        val largeRoomWidth = 1000000f // 10km
        val largeRoomDepth = 1000000f
        val smallRoomWidth = 10f // 10cm (smaller than items)
        val smallRoomDepth = 10f
        val negativeRoomWidth = -300f
        val negativeRoomDepth = -300f

        val items = listOf(
            ArPlacedItem("Item A", 50f, 100f, 50f, "cube.glb", 0f, 0f, 0f),
            ArPlacedItem("Item B", 50f, 100f, 50f, "cube.glb", 0.1f, 0f, 0f)
        )

        // Large room check
        val alignedLarge = FloorplanAutoLayout.align(items, largeRoomWidth, largeRoomDepth)
        for (item in alignedLarge) {
            verifyWithinRoomBounds(item, largeRoomWidth, largeRoomDepth)
            verifyEntireItemWithinRoomBounds(item, largeRoomWidth, largeRoomDepth)
        }

        // Small room check (items too large for room)
        val alignedSmall = FloorplanAutoLayout.align(items, smallRoomWidth, smallRoomDepth)
        for (item in alignedSmall) {
            // Centers must be within bounds (coerced or clamped to 0f center)
            verifyWithinRoomBounds(item, smallRoomWidth, smallRoomDepth)
        }

        // Negative dimensions (should not crash, and should handle sanitization)
        val alignedNegative = FloorplanAutoLayout.align(items, negativeRoomWidth, negativeRoomDepth)
        for (item in alignedNegative) {
            assertTrue(item.offsetX.isFinite())
            assertTrue(item.offsetZ.isFinite())
        }
    }

    @Test
    fun testHighDensitiesOfOverlappingItems() {
        val roomWidth = 300f
        val roomDepth = 300f
        // 40 items in a tight space
        val items = List(40) { i ->
            ArPlacedItem(
                name = "Dense Item $i",
                widthCm = 40f,
                heightCm = 100f,
                depthCm = 40f,
                modelName = "cube.glb",
                offsetX = (i % 5) * 0.05f - 0.1f,
                offsetZ = (i / 5) * 0.05f - 0.1f,
                rotationDegrees = (i * 15f)
            )
        }

        val aligned = FloorplanAutoLayout.align(items, roomWidth, roomDepth)

        for (item in aligned) {
            verifyWithinRoomBounds(item, roomWidth, roomDepth)
        }
    }

    @Test
    fun testLargeOrUnusualRotationAngles() {
        val roomWidth = 300f
        val roomDepth = 300f
        val items = listOf(
            ArPlacedItem("Item 1", 50f, 100f, 50f, "cube.glb", 0f, 0f, 725f),     // snaps to 0 or 720 -> snaps to 0?
            ArPlacedItem("Item 2", 50f, 100f, 50f, "cube.glb", 0f, 0f, -95f),     // snaps to -90 or -100 -> snaps to 270
            ArPlacedItem("Item 3", 50f, 100f, 50f, "cube.glb", 0f, 0f, 180.1f),   // snaps to 180
            ArPlacedItem("Item 4", 50f, 100f, 50f, "cube.glb", 0f, 0f, -720f),    // snaps to 0
            ArPlacedItem("Item 5", 50f, 100f, 50f, "cube.glb", 0f, 0f, 1000000f)  // unusual rotation
        )

        val aligned = FloorplanAutoLayout.align(items, roomWidth, roomDepth)

        for (item in aligned) {
            verifyWithinRoomBounds(item, roomWidth, roomDepth)
            val rot = item.rotationDegrees
            assertTrue("Rotation $rot must be snap to multiples of 90", rot == 0f || rot == 90f || rot == 180f || rot == 270f)
        }
    }

    @Test
    fun testNameUniquenessAfterDeletion() {
        var placedItems = emptyList<ArPlacedItem>()
        val itemProto = ArPlacedItem("소파 (IKEA)", 160f, 85f, 90f, "cube.glb", 0f, 0f, 0f)

        fun addFurniture(baseItem: ArPlacedItem) {
            val baseName = baseItem.name
            val uniqueName = if (placedItems.none { it.name == baseName }) {
                baseName
            } else {
                var suffix = 2
                while (placedItems.any { it.name == "$baseName ($suffix)" }) {
                    suffix++
                }
                "$baseName ($suffix)"
            }
            placedItems = placedItems + baseItem.copy(name = uniqueName)
        }

        addFurniture(itemProto)
        addFurniture(itemProto)
        addFurniture(itemProto)

        assertEquals("소파 (IKEA)", placedItems[0].name)
        assertEquals("소파 (IKEA) (2)", placedItems[1].name)
        assertEquals("소파 (IKEA) (3)", placedItems[2].name)

        placedItems = placedItems.filter { it.name != "소파 (IKEA) (2)" }
        assertEquals(2, placedItems.size)

        addFurniture(itemProto)
        assertEquals(3, placedItems.size)
        val newNames = placedItems.map { it.name }
        assertTrue(newNames.contains("소파 (IKEA)"))
        assertTrue(newNames.contains("소파 (IKEA) (2)"))
        assertTrue(newNames.contains("소파 (IKEA) (3)"))
    }

    @Test
    fun testWallSnappingDoesNotReintroduceOverlaps() {
        val item1 = ArPlacedItem(
            name = "Item 1",
            widthCm = 100f,
            heightCm = 100f,
            depthCm = 100f,
            modelName = "cube.glb",
            offsetX = 0.74f,
            offsetZ = 0.49f,
            rotationDegrees = 0f
        )
        val item2 = ArPlacedItem(
            name = "Item 2",
            widthCm = 100f,
            heightCm = 100f,
            depthCm = 100f,
            modelName = "cube.glb",
            offsetX = 0.49f,
            offsetZ = 0.74f,
            rotationDegrees = 0f
        )

        val aligned = FloorplanAutoLayout.align(listOf(item1, item2), 300f, 300f)

        assertEquals(2, aligned.size)
        val obb1 = toObb(aligned[0])
        val obb2 = toObb(aligned[1])
        val collision = CollisionDetection.checkCollision(obb1, obb2)
        assertFalse("Wall snapping should not re-introduce overlaps", collision.collides)
    }
}
