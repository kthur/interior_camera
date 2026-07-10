package com.example.interiorcamera.ui.ar

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.abs

data class Vector2D(val x: Float, val y: Float) {
    operator fun plus(other: Vector2D) = Vector2D(x + other.x, y + other.y)
    operator fun minus(other: Vector2D) = Vector2D(x - other.x, y - other.y)
    operator fun times(scalar: Float) = Vector2D(x * scalar, y * scalar)
    fun dot(other: Vector2D): Float = x * other.x + y * other.y
    fun length(): Float = sqrt(x * x + y * y)
    fun normalized(): Vector2D {
        val len = length()
        return if (len > 0f) Vector2D(x / len, y / len) else Vector2D(0f, 0f)
    }
}

data class Obb2D(
    val x: Float,
    val z: Float,
    val hw: Float, // half-width (extent along local X)
    val hd: Float, // half-depth (extent along local Z)
    val rotationDegrees: Float
) {
    fun getAxes(): List<Vector2D> {
        val angleRad = Math.toRadians(rotationDegrees.toDouble()).toFloat()
        val c = cos(angleRad)
        val s = sin(angleRad)
        return listOf(
            Vector2D(c, s), // Local X axis
            Vector2D(-s, c) // Local Z axis
        )
    }

    fun getVertices(): List<Vector2D> {
        val center = Vector2D(x, z)
        val axes = getAxes()
        val extX = axes[0] * hw
        val extY = axes[1] * hd
        return listOf(
            center + extX + extY,
            center + extX - extY,
            center - extX + extY,
            center - extX - extY
        )
    }
}

data class CollisionResult(
    val collides: Boolean,
    val mtvX: Float = 0f,
    val mtvZ: Float = 0f
)

object CollisionDetection {
    /**
     * Checks collision between activeObb (moving) and staticObb using Separating Axis Theorem (SAT).
     * If they collide, calculates the Minimum Translation Vector (MTV) to push activeObb out of staticObb.
     */
    fun checkCollision(activeObb: Obb2D, staticObb: Obb2D): CollisionResult {
        val axes = activeObb.getAxes() + staticObb.getAxes()
        var minOverlap = Float.MAX_VALUE
        var mtvAxis = Vector2D(0f, 0f)

        val centerA = Vector2D(activeObb.x, activeObb.z)
        val centerB = Vector2D(staticObb.x, staticObb.z)
        val dir = centerA - centerB

        for (rawAxis in axes) {
            val axis = rawAxis.normalized()
            // Project activeObb
            val projA = projectObb(activeObb, axis)
            // Project staticObb
            val projB = projectObb(staticObb, axis)

            // Calculate overlap
            val minA = projA.first
            val maxA = projA.second
            val minB = projB.first
            val maxB = projB.second

            val overlap = minOf(maxA, maxB) - maxOf(minA, minB)
            if (overlap <= 0f) {
                // Separating axis found, no collision
                return CollisionResult(false)
            }

            if (overlap < minOverlap) {
                minOverlap = overlap
                // Orient axis to point from staticObb (B) to activeObb (A)
                mtvAxis = if (dir.dot(axis) < 0f) axis * -1f else axis
            }
        }

        // Collision detected on all axes
        val mtv = mtvAxis * minOverlap
        return CollisionResult(
            collides = true,
            mtvX = mtv.x,
            mtvZ = mtv.y
        )
    }

    private fun projectObb(obb: Obb2D, axis: Vector2D): Pair<Float, Float> {
        val center = Vector2D(obb.x, obb.z)
        val axes = obb.getAxes()
        val cProj = center.dot(axis)
        val r = obb.hw * abs(axes[0].dot(axis)) + obb.hd * abs(axes[1].dot(axis))
        return Pair(cProj - r, cProj + r)
    }
}
