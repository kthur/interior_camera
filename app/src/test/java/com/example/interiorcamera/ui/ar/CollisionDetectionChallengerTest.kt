package com.example.interiorcamera.ui.ar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import java.util.Random

class CollisionDetectionChallengerTest {

    // --- Independent Reference Geometry Checker ---

    private fun pointsWithinTolerance(p1: Vector2D, p2: Vector2D, eps: Float = 1e-4f): Boolean {
        return abs(p1.x - p2.x) < eps && abs(p1.y - p2.y) < eps
    }

    // Segment intersection check. Returns intersection point if segments AB and CD intersect
    private fun lineIntersection(a: Vector2D, b: Vector2D, c: Vector2D, d: Vector2D): Vector2D? {
        val denom = (b.x - a.x) * (d.y - c.y) - (b.y - a.y) * (d.x - c.x)
        if (abs(denom) < 1e-6f) return null // Parallel or collinear
        val t = ((c.x - a.x) * (d.y - c.y) - (c.y - a.y) * (d.x - c.x)) / denom
        val u = ((c.x - a.x) * (b.y - a.y) - (c.y - a.y) * (b.x - a.x)) / denom
        return if (t in 0.0f..1.0f && u in 0.0f..1.0f) {
            Vector2D(a.x + t * (b.x - a.x), a.y + t * (b.y - a.y))
        } else {
            null
        }
    }

    // Checks if point P is strictly inside OBB (with eps margin)
    private fun isPointStrictlyInsideObb(p: Vector2D, obb: Obb2D, eps: Float = 1e-4f): Boolean {
        val center = Vector2D(obb.x, obb.z)
        val axes = obb.getAxes()
        val localX = (p - center).dot(axes[0])
        val localZ = (p - center).dot(axes[1])
        return abs(localX) < (obb.hw - eps) && abs(localZ) < (obb.hd - eps)
    }

    private fun getSegmentProjectionFactor(p: Vector2D, a: Vector2D, b: Vector2D): Float {
        val ab = b - a
        val ap = p - a
        val abLenSq = ab.dot(ab)
        if (abLenSq < 1e-6f) return 0f
        return ap.dot(ab) / abLenSq
    }

    // Reference collision oracle: returns true if two OBBs overlap by a positive area
    private fun refCollides(obb1: Obb2D, obb2: Obb2D): Boolean {
        val v1 = obb1.getVertices()
        val v2 = obb2.getVertices()

        // 1. Any vertex of obb1 is strictly inside obb2
        for (v in v1) {
            if (isPointStrictlyInsideObb(v, obb2)) return true
        }

        // 2. Any vertex of obb2 is strictly inside obb1
        for (v in v2) {
            if (isPointStrictlyInsideObb(v, obb1)) return true
        }

        // 3. Any edge of obb1 strictly intersects any edge of obb2 (not just touching at endpoints)
        // Vertices are ordered clockwise/counterclockwise:
        // center + extX + extY, center + extX - extY, center - extX + extY, center - extX - extY
        // Wait, let's map the edges correctly:
        // Vertices returned by getVertices():
        // 0: C + ex + ez
        // 1: C + ex - ez
        // 2: C - ex + ez
        // 3: C - ex - ez
        // Let's connect them:
        // ex + ez (0) -> ex - ez (1)
        // ex - ez (1) -> -ex - ez (3)
        // -ex - ez (3) -> -ex + ez (2)
        // -ex + ez (2) -> ex + ez (0)
        val edges1 = listOf(
            Pair(v1[0], v1[1]),
            Pair(v1[1], v1[3]),
            Pair(v1[3], v1[2]),
            Pair(v1[2], v1[0])
        )
        val edges2 = listOf(
            Pair(v2[0], v2[1]),
            Pair(v2[1], v2[3]),
            Pair(v2[3], v2[2]),
            Pair(v2[2], v2[0])
        )

        for (e1 in edges1) {
            for (e2 in edges2) {
                val intersect = lineIntersection(e1.first, e1.second, e2.first, e2.second)
                if (intersect != null) {
                    val t1 = getSegmentProjectionFactor(intersect, e1.first, e1.second)
                    val t2 = getSegmentProjectionFactor(intersect, e2.first, e2.second)
                    val eps = 1e-4f
                    if (t1 > eps && t1 < 1f - eps && t2 > eps && t2 < 1f - eps) {
                        return true
                    }
                }
            }
        }
        return false
    }

    // --- Tests ---

    @Test
    fun runRandomizedTests() {
        val rand = Random(42) // Fixed seed for reproducibility
        var passCount = 0
        var failCount = 0
        val totalCases = 1000

        println("=== STARTING RANDOMIZED OBB COLLISION TESTS ===")
        for (i in 1..totalCases) {
            // Generate active
            val active = Obb2D(
                x = rand.nextFloat() * 10f - 5f,
                z = rand.nextFloat() * 10f - 5f,
                hw = rand.nextFloat() * 2f + 0.1f,
                hd = rand.nextFloat() * 2f + 0.1f,
                rotationDegrees = rand.nextFloat() * 360f
            )

            // Generate static
            val static = Obb2D(
                x = rand.nextFloat() * 10f - 5f,
                z = rand.nextFloat() * 10f - 5f,
                hw = rand.nextFloat() * 2f + 0.1f,
                hd = rand.nextFloat() * 2f + 0.1f,
                rotationDegrees = rand.nextFloat() * 360f
            )

            val refResult = refCollides(active, static)
            val satResult = CollisionDetection.checkCollision(active, static)

            if (satResult.collides != refResult) {
                failCount++
                println("Mismatch Case $i:")
                println("  Active: $active")
                println("  Static: $static")
                println("  Reference Collision: $refResult, SAT Collision: ${satResult.collides}")
            } else {
                passCount++
                // If they collide, test MTV resolution
                if (satResult.collides) {
                    val resolved = active.copy(
                        x = active.x + satResult.mtvX,
                        z = active.z + satResult.mtvZ
                    )
                    val overlapAfterMtv = refCollides(resolved, static)
                    if (overlapAfterMtv) {
                        println("MTV Resolution Failure Case $i:")
                        println("  Active: $active")
                        println("  Static: $static")
                        println("  MTV: (${satResult.mtvX}, ${satResult.mtvZ})")
                        println("  Resolved: $resolved")
                        println("  Overlap after resolution: $overlapAfterMtv")
                    }
                }
            }
        }
        println("Randomized tests completed: Pass=$passCount, Fail=$failCount")
        // We do not fail the test here immediately, so we can run the entire suite and collect logs.
    }

    @Test
    fun testConcentricBoxes_EdgeCase() {
        println("=== TESTING CONCENTRIC BOXES EDGE CASE ===")
        // Center at (0, 0), active is 1x1, static is 2x2.
        val active = Obb2D(x = 0f, z = 0f, hw = 0.5f, hd = 0.5f, rotationDegrees = 0f)
        val static = Obb2D(x = 0f, z = 0f, hw = 1f, hd = 1f, rotationDegrees = 0f)

        val refResult = refCollides(active, static)
        val satResult = CollisionDetection.checkCollision(active, static)

        println("Concentric boxes:")
        println("  Reference Collides: $refResult")
        println("  SAT Collides: ${satResult.collides}")
        println("  MTV: (${satResult.mtvX}, ${satResult.mtvZ})")

        if (satResult.collides) {
            val resolved = active.copy(
                x = active.x + satResult.mtvX,
                z = active.z + satResult.mtvZ
            )
            val resolvedCollision = refCollides(resolved, static)
            println("  Resolved Active: $resolved")
            println("  Resolved Collides: $resolvedCollision")
        }
    }

    @Test
    fun testCoincidentVertices_EdgeCase() {
        println("=== TESTING COINCIDENT VERTICES EDGE CASE ===")
        // Two 2x2 boxes touching exactly at a single corner (1, 1) and (1, 1).
        // active: center (0, 0), extents 1x1. Vertices: (1,1), (1,-1), (-1,1), (-1,-1)
        // static: center (2, 2), extents 1x1. Vertices: (3,3), (3,1), (1,3), (1,1)
        val active = Obb2D(x = 0f, z = 0f, hw = 1f, hd = 1f, rotationDegrees = 0f)
        val static = Obb2D(x = 2f, z = 2f, hw = 1f, hd = 1f, rotationDegrees = 0f)

        val refResult = refCollides(active, static)
        val satResult = CollisionDetection.checkCollision(active, static)

        println("Coincident vertices:")
        println("  Reference Collides: $refResult")
        println("  SAT Collides: ${satResult.collides}")
    }

    @Test
    fun testParallelEdgesFlush_EdgeCase() {
        println("=== TESTING PARALLEL EDGES FLUSH EDGE CASE ===")
        // active: center (0, 0), extents 1x1. X bounds: [-1, 1]
        // static: center (2, 0), extents 1x1. X bounds: [1, 3]
        // They touch exactly along the edge X = 1.
        val active = Obb2D(x = 0f, z = 0f, hw = 1f, hd = 1f, rotationDegrees = 0f)
        val static = Obb2D(x = 2f, z = 0f, hw = 1f, hd = 1f, rotationDegrees = 0f)

        val refResult = refCollides(active, static)
        val satResult = CollisionDetection.checkCollision(active, static)

        println("Parallel edges flush:")
        println("  Reference Collides: $refResult")
        println("  SAT Collides: ${satResult.collides}")
    }
}
