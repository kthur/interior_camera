package com.example.interiorcamera.ui.floorplan

import com.example.interiorcamera.ui.ar.CollisionDetection
import com.example.interiorcamera.ui.ar.Obb2D
import com.example.interiorcamera.ui.ar.Vector2D
import kotlin.math.*

object FloorplanAutoLayout {

    fun align(
        placedItems: List<ArPlacedItem>,
        roomWidthCm: Float,
        roomDepthCm: Float
    ): List<ArPlacedItem> {
        if (placedItems.isEmpty()) return emptyList()

        // 1. Disperse concentric items
        val dispersed = disperseConcentric(placedItems)

        // 2. Snap rotations to nearest 90-degree increments
        val rotated = dispersed.map { item ->
            val snappedRot = snapRotation(item.rotationDegrees)
            item.copy(rotationDegrees = snappedRot)
        }

        // 3. Snap to room boundaries/walls and initial clamp
        val snappedAndClamped = rotated.map { item ->
            val snappedPos = snapToWalls(
                item.offsetX,
                item.offsetZ,
                item.rotationDegrees,
                item.widthCm,
                item.depthCm,
                roomWidthCm,
                roomDepthCm
            )
            val clamped = FloorplanCoordinator.clampToRoomBounds(
                offsetX = snappedPos.first,
                offsetZ = snappedPos.second,
                rotationDegrees = item.rotationDegrees,
                blockWidthCm = item.widthCm,
                blockDepthCm = item.depthCm,
                roomWidthCm = roomWidthCm,
                roomDepthCm = roomDepthCm
            )
            item.copy(offsetX = clamped.first, offsetZ = clamped.second)
        }.toMutableList()

        // 4. Iteratively resolve overlaps
        val maxIterations = 25
        for (iter in 0 until maxIterations) {
            var collisionFound = false
            for (i in snappedAndClamped.indices) {
                for (j in i + 1 until snappedAndClamped.size) {
                    val itemI = snappedAndClamped[i]
                    val itemJ = snappedAndClamped[j]
                    val obbI = toObb(itemI)
                    val obbJ = toObb(itemJ)
                    val result = CollisionDetection.checkCollision(obbI, obbJ)
                    if (result.collides) {
                        collisionFound = true
                        // Translate by MTV. activeObb (obbI) gets pushed by result.mtvX, result.mtvZ
                        // We divide the displacement between both items.
                        val dx = result.mtvX / 2f
                        val dz = result.mtvZ / 2f

                        val nextI = itemI.copy(offsetX = itemI.offsetX + dx, offsetZ = itemI.offsetZ + dz)
                        val clampedI = FloorplanCoordinator.clampToRoomBounds(
                            offsetX = nextI.offsetX,
                            offsetZ = nextI.offsetZ,
                            rotationDegrees = nextI.rotationDegrees,
                            blockWidthCm = nextI.widthCm,
                            blockDepthCm = nextI.depthCm,
                            roomWidthCm = roomWidthCm,
                            roomDepthCm = roomDepthCm
                        )
                        snappedAndClamped[i] = nextI.copy(offsetX = clampedI.first, offsetZ = clampedI.second)

                        val nextJ = itemJ.copy(offsetX = itemJ.offsetX - dx, offsetZ = itemJ.offsetZ - dz)
                        val clampedJ = FloorplanCoordinator.clampToRoomBounds(
                            offsetX = nextJ.offsetX,
                            offsetZ = nextJ.offsetZ,
                            rotationDegrees = nextJ.rotationDegrees,
                            blockWidthCm = nextJ.widthCm,
                            blockDepthCm = nextJ.depthCm,
                            roomWidthCm = roomWidthCm,
                            roomDepthCm = roomDepthCm
                        )
                        snappedAndClamped[j] = nextJ.copy(offsetX = clampedJ.first, offsetZ = clampedJ.second)
                    }
                }
            }
            if (!collisionFound) break
        }

        // 5. Final wall-snapping and room-bounds clamping pass
        return snappedAndClamped.map { item ->
            val snappedPos = snapToWalls(
                item.offsetX,
                item.offsetZ,
                item.rotationDegrees,
                item.widthCm,
                item.depthCm,
                roomWidthCm,
                roomDepthCm
            )
            val clamped = FloorplanCoordinator.clampToRoomBounds(
                offsetX = snappedPos.first,
                offsetZ = snappedPos.second,
                rotationDegrees = item.rotationDegrees,
                blockWidthCm = item.widthCm,
                blockDepthCm = item.depthCm,
                roomWidthCm = roomWidthCm,
                roomDepthCm = roomDepthCm
            )
            item.copy(offsetX = clamped.first, offsetZ = clamped.second)
        }
    }

    private fun disperseConcentric(placedItems: List<ArPlacedItem>): List<ArPlacedItem> {
        val dispersed = mutableListOf<ArPlacedItem>()
        for (item in placedItems) {
            var currentX = item.offsetX
            var currentZ = item.offsetZ
            var attempts = 0
            while (dispersed.any { other ->
                    val dx = currentX - other.offsetX
                    val dz = currentZ - other.offsetZ
                    dx * dx + dz * dz < 0.02f * 0.02f
                } && attempts < 100) {
                attempts++
                val theta = attempts * 0.7853982f // 45 degrees
                val radius = 0.05f * attempts // 5cm increments
                currentX = item.offsetX + radius * cos(theta)
                currentZ = item.offsetZ + radius * sin(theta)
            }
            dispersed.add(item.copy(offsetX = currentX, offsetZ = currentZ))
        }
        return dispersed
    }

    private fun snapRotation(rotationDegrees: Float): Float {
        val rounded = Math.round(rotationDegrees / 90.0) * 90.0
        var normalized = rounded % 360.0
        if (normalized < 0.0) {
            normalized += 360.0
        }
        return normalized.toFloat()
    }

    private fun snapToWalls(
        offsetX: Float,
        offsetZ: Float,
        rotationDegrees: Float,
        widthCm: Float,
        depthCm: Float,
        roomWidthCm: Float,
        roomDepthCm: Float
    ): Pair<Float, Float> {
        val rw = abs(roomWidthCm / 100f) / 2f
        val rd = abs(roomDepthCm / 100f) / 2f

        val angleRad = Math.toRadians(rotationDegrees.toDouble()).toFloat()
        val c = cos(angleRad)
        val s = sin(angleRad)

        val hw = (widthCm / 100f) / 2f
        val hd = (depthCm / 100f) / 2f

        val ax = Vector2D(c, s)
        val az = Vector2D(-s, c)

        val aabbHalfW = abs(ax.x * hw) + abs(az.x * hd)
        val aabbHalfD = abs(ax.y * hw) + abs(az.y * hd)

        val distToLeftWall = abs((offsetX - aabbHalfW) - (-rw))
        val distToRightWall = abs(rw - (offsetX + aabbHalfW))
        val distToTopWall = abs((offsetZ - aabbHalfD) - (-rd))
        val distToBottomWall = abs(rd - (offsetZ + aabbHalfD))

        val threshold = 0.25f // 25cm

        var snappedX = offsetX
        if (distToLeftWall <= threshold || distToRightWall <= threshold) {
            snappedX = if (distToLeftWall <= distToRightWall) {
                -rw + aabbHalfW
            } else {
                rw - aabbHalfW
            }
        }

        var snappedZ = offsetZ
        if (distToTopWall <= threshold || distToBottomWall <= threshold) {
            snappedZ = if (distToTopWall <= distToBottomWall) {
                -rd + aabbHalfD
            } else {
                rd - aabbHalfD
            }
        }

        return Pair(snappedX, snappedZ)
    }

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
}
