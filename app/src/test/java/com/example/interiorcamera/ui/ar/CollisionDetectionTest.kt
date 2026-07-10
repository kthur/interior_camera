package com.example.interiorcamera.ui.ar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CollisionDetectionTest {

    @Test
    fun testNoCollision_distantBoxes() {
        val active = Obb2D(x = 0f, z = 0f, hw = 1f, hd = 1f, rotationDegrees = 0f)
        val static = Obb2D(x = 3f, z = 0f, hw = 1f, hd = 1f, rotationDegrees = 0f)

        val result = CollisionDetection.checkCollision(active, static)

        assertFalse("Boxes are separated and should not collide", result.collides)
        assertEquals(0f, result.mtvX, 0.001f)
        assertEquals(0f, result.mtvZ, 0.001f)
    }

    @Test
    fun testCollision_axisAlignedOverlapX() {
        val active = Obb2D(x = 0.5f, z = 0f, hw = 1f, hd = 1f, rotationDegrees = 0f)
        val static = Obb2D(x = 2.0f, z = 0f, hw = 1f, hd = 1f, rotationDegrees = 0f)

        // active: center X=0.5, extends from -0.5 to 1.5
        // static: center X=2.0, extends from 1.0 to 3.0
        // Overlap region is X=[1.0, 1.5] -> overlap size is 0.5f
        // Active box needs to be pushed left by 0.5f (i.e. -0.5f along X axis)
        val result = CollisionDetection.checkCollision(active, static)

        assertTrue("Boxes overlap and should collide", result.collides)
        assertEquals(-0.5f, result.mtvX, 0.001f)
        assertEquals(0f, result.mtvZ, 0.001f)
    }

    @Test
    fun testCollision_axisAlignedOverlapZ() {
        val active = Obb2D(x = 0f, z = 0.8f, hw = 1f, hd = 1f, rotationDegrees = 0f)
        val static = Obb2D(x = 0f, z = 2.0f, hw = 1f, hd = 1f, rotationDegrees = 0f)

        // active: center Z=0.8, extends from -0.2 to 1.8
        // static: center Z=2.0, extends from 1.0 to 3.0
        // Overlap region is Z=[1.0, 1.8] -> overlap size is 0.8f
        // Active box needs to be pushed down/south by 0.8f (i.e. -0.8f along Z axis)
        val result = CollisionDetection.checkCollision(active, static)

        assertTrue("Boxes overlap and should collide", result.collides)
        assertEquals(0f, result.mtvX, 0.001f)
        assertEquals(-0.8f, result.mtvZ, 0.001f)
    }

    @Test
    fun testCollision_rotatedOBB() {
        // active: center (0, 0), rotated 45 degrees, extents 1x1
        // static: center (1.2, 1.2), rotated 0 degrees, extents 1x1
        val active = Obb2D(x = 0f, z = 0f, hw = 1f, hd = 1f, rotationDegrees = 45f)
        val static = Obb2D(x = 1.2f, z = 1.2f, hw = 1f, hd = 1f, rotationDegrees = 0f)

        val result = CollisionDetection.checkCollision(active, static)

        // Verify that rotation-based SAT correctly identifies collision
        assertTrue("Rotated boxes should collide", result.collides)
        // MTV should be non-zero to resolve collision
        assertTrue("MTV X should resolve the collision", result.mtvX != 0f || result.mtvZ != 0f)
    }

    @Test
    fun testNoCollision_rotatedSeparated() {
        // active: center (0,0), rotated 45 degrees, extents 0.5x0.5
        // static: center (2,2), rotated 45 degrees, extents 0.5x0.5
        val active = Obb2D(x = 0f, z = 0f, hw = 0.5f, hd = 0.5f, rotationDegrees = 45f)
        val static = Obb2D(x = 2f, z = 2f, hw = 0.5f, hd = 0.5f, rotationDegrees = 45f)

        val result = CollisionDetection.checkCollision(active, static)

        assertFalse("Distant rotated boxes should not collide", result.collides)
    }

    @Test
    fun testCollision_nestedContainment() {
        // One inside another
        val active = Obb2D(x = 0f, z = 0f, hw = 0.5f, hd = 0.5f, rotationDegrees = 30f)
        val static = Obb2D(x = 0f, z = 0f, hw = 2.0f, hd = 2.0f, rotationDegrees = 0f)

        val result = CollisionDetection.checkCollision(active, static)

        assertTrue("Nested boxes should collide", result.collides)
    }
}
