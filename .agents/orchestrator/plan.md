# FitCheck AR UI/UX Improvements Project Plan

This document outlines the milestones, requirements decomposition, implementation details, and verification plans for introducing 5 key UI/UX improvements to the FitCheck AR application.

## Requirements Overview
- **R1: Opacity Slider Control**: Dynamic alpha adjustment of placed 3D models using a slider in the AR screen overlay.
- **R2: 3D Rotation Controls**: Two-finger rotation gesture on 3D models and "Rotate Left"/"Rotate Right" overlay buttons for precise rotation increments (15 or 45 degrees).
- **R3: Plane Detection Guide UI**: Coachmark/message instructing the user to "slowly move the camera" until a surface is detected. Hidden when plane tracking is stable.
- **R4: Multi-Anchor Placement**: Multiple models rendered simultaneously. Support selecting a node, scaling/rotating/deleting the selected node, and clearing all nodes.
- **R5: Custom Preset Storage**: Local storage (database or preferences) for custom dimensions (width, height, depth, name). Custom presets appear on the Main Screen alongside default ones.

---

## Architecture & Code Layout
- **Main Screen**:
  - `MainScreen.kt` & `MainScreenViewModel.kt`: To display presets list (default + custom presets) and a form to add new custom presets.
  - `DataRepository.kt`: Persistent storage mechanism (such as Shared Preferences or Room Database) to store custom presets.
- **AR Screen**:
  - `ArScreen.kt`: Handles `ARSceneView` integration, rendering, overlays (opacity slider, rotate buttons, plane guide, clear button, delete active node button), and gestures.

---

## Decomposed Milestones & Dual Track Strategy

We will use the **Project Pattern Dual Track**:
1. **E2E Testing Track**: Build an opaque-box test suite.
2. **Implementation Track**: Implement the features in milestones.

### Implementation Milestones

#### Milestone 1: Custom Preset Storage ("My List") & Main Screen (R5)
- **Scope**:
  - Update `DataRepository` to serialize/deserialize custom presets locally (e.g. using `SharedPreferences` or local JSON storage).
  - Update `MainScreenViewModel` and `MainScreen` to display default presets + custom presets.
  - Add a form (UI inputs) on the Main Screen to name and save a custom preset (width, height, depth, name).
- **Files**: `DataRepository.kt`, `MainScreen.kt`, `MainScreenViewModel.kt`, `Navigation.kt`, `MainActivity.kt`.
- **Verification**: Run unit tests and instrumentation tests for main screen & storage.

#### Milestone 2: AR Screen Basic Overlay & Plane Detection Guide (R1 & R3)
- **Scope**:
  - Add Opacity Slider in `ArScreen` overlay. Wire it to adjust the alpha value of model node instances.
  - Implement Plane Detection Guide UI. Show visual guidance/coachmark when no plane is detected. Hide the guide once tracking is stable (i.e. first plane detected).
- **Files**: `ArScreen.kt`.
- **Verification**: Run unit/UI tests on `ArScreen`.

#### Milestone 3: Multi-Anchor Placement & Model Interaction (R4)
- **Scope**:
  - Enable placing multiple models in the SceneView simultaneously.
  - Manage a list of active anchors/nodes instead of a single anchor.
  - Add selection state to nodes. Highlight the selected node.
  - Add controls (scale, delete, clear all) on the selected node.
- **Files**: `ArScreen.kt`.
- **Verification**: Verify placing multiple items, selecting, deleting, and clearing.

#### Milestone 4: 3D Rotation Controls & Gestures (R2)
- **Scope**:
  - Implement "Rotate Left" and "Rotate Right" buttons in the AR overlay that rotate the selected model node by fixed increments (e.g., 15 or 45 degrees).
  - Implement two-finger rotation gestures to dynamically rotate the selected model node.
- **Files**: `ArScreen.kt`.
- **Verification**: Verify button-based and gesture-based rotation.

#### Milestone 5: E2E Verification & Adversarial Hardening (Dual Track Integration)
- **Scope**:
  - Run all E2E tests written by the testing track.
  - Perform White-Box Adversarial coverage hardening (Tier 5).
- **Verification**: 100% test pass on all tiers, auditor integrity check.
