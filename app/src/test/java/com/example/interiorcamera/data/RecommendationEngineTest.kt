package com.example.interiorcamera.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

data class RecommendedFurniture(
  val name: String,
  val widthCm: Float,
  val heightCm: Float,
  val depthCm: Float,
  val modelName: String
)

class RecommendationEngine(val safetyMarginCm: Float = 5.0f) {
  fun filterRecommendations(
    catalog: List<RecommendedFurniture>,
    spaceWidthCm: Float,
    spaceHeightCm: Float = Float.MAX_VALUE,
    spaceDepthCm: Float = Float.MAX_VALUE
  ): List<RecommendedFurniture> {
    if (spaceWidthCm <= 0f || spaceHeightCm <= 0f || spaceDepthCm <= 0f) {
      return emptyList()
    }
    return catalog.filter { furniture ->
      (furniture.widthCm + safetyMarginCm <= spaceWidthCm) &&
      (furniture.heightCm + safetyMarginCm <= spaceHeightCm) &&
      (furniture.depthCm + safetyMarginCm <= spaceDepthCm)
    }
  }
}

class RecommendationEngineTest {

  private val sampleCatalog = listOf(
    RecommendedFurniture("Sofa", 150f, 85f, 90f, "sofa.glb"),
    RecommendedFurniture("Chair", 60f, 90f, 60f, "chair.glb"),
    RecommendedFurniture("Table", 120f, 75f, 80f, "table.glb"),
    RecommendedFurniture("Wardrobe", 100f, 200f, 60f, "wardrobe.glb")
  )

  @Test
  fun testRecommendation_LoadCatalog() {
    val engine = RecommendationEngine()
    assertEquals(4, sampleCatalog.size)
  }

  @Test
  fun testRecommendation_FilterByWidth() {
    val engine = RecommendationEngine(safetyMarginCm = 5.0f)
    val recommendations = engine.filterRecommendations(sampleCatalog, spaceWidthCm = 110f)
    assertEquals(2, recommendations.size)
    assertTrue(recommendations.any { it.name == "Chair" })
    assertTrue(recommendations.any { it.name == "Wardrobe" })
  }

  @Test
  fun testRecommendation_FilterByHeightAndDepth() {
    val engine = RecommendationEngine(safetyMarginCm = 5.0f)
    val recommendations = engine.filterRecommendations(
      catalog = sampleCatalog,
      spaceWidthCm = 150f,
      spaceHeightCm = 180f,
      spaceDepthCm = 100f
    )
    assertEquals(2, recommendations.size)
    assertTrue(recommendations.any { it.name == "Chair" })
    assertTrue(recommendations.any { it.name == "Table" })
  }

  @Test
  fun testRecommendation_ZeroSpaceDimensions() {
    val engine = RecommendationEngine()
    val recommendations = engine.filterRecommendations(sampleCatalog, spaceWidthCm = 0f, spaceHeightCm = 0f)
    assertTrue(recommendations.isEmpty())
  }

  @Test
  fun testRecommendation_NegativeSpaceDimensions() {
    val engine = RecommendationEngine()
    val recommendations = engine.filterRecommendations(sampleCatalog, spaceWidthCm = -10f, spaceHeightCm = 100f)
    assertTrue(recommendations.isEmpty())
  }

  @Test
  fun testRecommendation_ExtremelySmallSpace() {
    val engine = RecommendationEngine()
    val recommendations = engine.filterRecommendations(sampleCatalog, spaceWidthCm = 10f)
    assertTrue(recommendations.isEmpty())
  }

  @Test
  fun testRecommendation_LargeSafetyMargin() {
    val engine = RecommendationEngine(safetyMarginCm = 50f)
    val recommendations = engine.filterRecommendations(sampleCatalog, spaceWidthCm = 150f)
    assertEquals(2, recommendations.size)
  }

  @Test
  fun testRecommendation_EmptyCatalog() {
    val engine = RecommendationEngine()
    val recommendations = engine.filterRecommendations(emptyList(), spaceWidthCm = 200f)
    assertTrue(recommendations.isEmpty())
  }

  @Test
  fun testCalibrationAndRecommendation_CalibratedFitCheck() {
    val rawSpaceWidthCm = 100f
    val calibrationFactor = 1.2f
    val calibratedSpaceWidth = rawSpaceWidthCm * calibrationFactor
    
    val engine = RecommendationEngine(safetyMarginCm = 5.0f)
    
    val rawRecommendations = engine.filterRecommendations(sampleCatalog, spaceWidthCm = rawSpaceWidthCm)
    assertTrue(rawRecommendations.none { it.name == "Wardrobe" })
    
    val calibratedRecommendations = engine.filterRecommendations(sampleCatalog, spaceWidthCm = calibratedSpaceWidth)
    assertTrue(calibratedRecommendations.any { it.name == "Wardrobe" })
  }
}
