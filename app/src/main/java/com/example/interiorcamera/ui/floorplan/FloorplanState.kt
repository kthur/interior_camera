package com.example.interiorcamera.ui.floorplan

import kotlinx.serialization.Serializable
import com.example.interiorcamera.ui.ar.Vector2D
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Serializable
data class ArPlacedItem(
  val name: String,
  val widthCm: Float,
  val heightCm: Float,
  val depthCm: Float,
  val modelName: String,
  val offsetX: Float, // relative 2D offset from room center in meters
  val offsetZ: Float, // relative 2D offset from room center in meters
  val rotationDegrees: Float
)

object FloorplanCoordinator {
    /**
     * Converts a 2D canvas pixel coordinate to a relative meter offset from the room center.
     */
    fun screenToRelative(
        screenX: Float,
        screenY: Float,
        canvasWidth: Float,
        canvasHeight: Float,
        roomWidthCm: Float,
        roomDepthCm: Float
    ): Pair<Float, Float> {
        val dxPx = screenX - (canvasWidth / 2f)
        val dyPx = screenY - (canvasHeight / 2f)

        val scaleX = canvasWidth / roomWidthCm
        val scaleY = canvasHeight / roomDepthCm

        val offsetXMeter = (dxPx / scaleX) / 100f
        val offsetZMeter = (dyPx / scaleY) / 100f
        return Pair(offsetXMeter, offsetZMeter)
    }

    /**
     * Converts a relative meter offset from the room center to a 2D canvas pixel coordinate.
     */
    fun relativeToScreen(
        offsetXMeter: Float,
        offsetZMeter: Float,
        canvasWidth: Float,
        canvasHeight: Float,
        roomWidthCm: Float,
        roomDepthCm: Float
    ): Pair<Float, Float> {
        val scaleX = canvasWidth / roomWidthCm
        val scaleY = canvasHeight / roomDepthCm

        val dxPx = (offsetXMeter * 100f) * scaleX
        val dyPx = (offsetZMeter * 100f) * scaleY

        val screenX = dxPx + (canvasWidth / 2f)
        val screenY = dyPx + (canvasHeight / 2f)
        return Pair(screenX, screenY)
    }

    /**
     * Clamps relative coordinates so the placed item (rotated OBB) remains entirely inside the room boundaries.
     */
    fun clampToRoomBounds(
        offsetX: Float,
        offsetZ: Float,
        rotationDegrees: Float,
        blockWidthCm: Float,
        blockDepthCm: Float,
        roomWidthCm: Float,
        roomDepthCm: Float
    ): Pair<Float, Float> {
        val rw = (roomWidthCm / 100f) / 2f
        val rd = (roomDepthCm / 100f) / 2f

        val angleRad = Math.toRadians(rotationDegrees.toDouble()).toFloat()
        val c = cos(angleRad)
        val s = sin(angleRad)

        val hw = (blockWidthCm / 100f) / 2f
        val hd = (blockDepthCm / 100f) / 2f

        val ax = Vector2D(c, s)
        val az = Vector2D(-s, c)

        // Calculate AABB half extents
        val aabbHalfW = abs(ax.x * hw) + abs(az.x * hd)
        val aabbHalfD = abs(ax.y * hw) + abs(az.y * hd)

        val clampedX = offsetX.coerceIn(-rw + aabbHalfW, rw - aabbHalfW)
        val clampedZ = offsetZ.coerceIn(-rd + aabbHalfD, rd - aabbHalfD)
        return Pair(clampedX, clampedZ)
    }
}
