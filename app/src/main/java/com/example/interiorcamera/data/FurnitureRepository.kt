package com.example.interiorcamera.data

interface FurnitureRepository {
  fun getRecommendedFurniture(): List<RecommendedFurniture>
}

class DefaultFurnitureRepository : FurnitureRepository {
  private val furnitureList = listOf(
    RecommendedFurniture("Cozy Sofa", "IKEA", "₩350,000", "Living Room", 160f, 85f, 90f, "cube.glb"),
    RecommendedFurniture("L-Shape Sofa", "Casamia", "₩1,200,000", "Living Room", 240f, 90f, 160f, "cube.glb"),
    RecommendedFurniture("Fabric Armchair", "IKEA", "₩120,000", "Living Room", 85f, 85f, 80f, "cube.glb"),
    RecommendedFurniture("Wooden Dining Table", "Hanssem", "₩450,000", "Kitchen", 140f, 75f, 80f, "cube.glb"),
    RecommendedFurniture("Small Coffee Table", "IKEA", "₩45,000", "Living Room", 90f, 45f, 55f, "cube.glb"),
    RecommendedFurniture("Modern Study Desk", "Hanssem", "₩280,000", "Study", 120f, 75f, 60f, "cube.glb"),
    RecommendedFurniture("Ergonomic Chair", "Sidiz", "₩320,000", "Study", 68f, 115f, 65f, "cube.glb"),
    RecommendedFurniture("Tall Bookcase", "Hanssem", "₩180,000", "Study", 80f, 200f, 30f, "cube.glb"),
    RecommendedFurniture("Single Bed", "IKEA", "₩250,000", "Bedroom", 100f, 45f, 200f, "cube.glb"),
    RecommendedFurniture("Queen Bed", "Casamia", "₩850,000", "Bedroom", 150f, 55f, 210f, "cube.glb"),
    RecommendedFurniture("Double Wardrobe", "Hanssem", "₩600,000", "Bedroom", 120f, 210f, 60f, "cube.glb"),
    RecommendedFurniture("Premium Refrigerator", "Samsung", "₩2,500,000", "Kitchen", 91f, 178f, 75f, "refrigerator.glb"),
    RecommendedFurniture("Kitchen Cabinet", "Hanssem", "₩380,000", "Kitchen", 80f, 180f, 50f, "refrigerator.glb"),
    RecommendedFurniture("Stand Air Conditioner", "LG", "₩1,800,000", "Living Room", 48f, 185f, 35f, "refrigerator.glb"),
    RecommendedFurniture("Microwave Stand", "IKEA", "₩85,000", "Kitchen", 60f, 120f, 45f, "cube.glb")
  )

  override fun getRecommendedFurniture(): List<RecommendedFurniture> = furnitureList
}
