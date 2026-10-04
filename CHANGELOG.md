# 📜 TrackRep Changelog

All notable changes, additions, fixes, and architectural evolutions of **TrackRep** are documented in this file from project inception to the current version.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## 📑 Table of Contents

- [v1.2.14 - Enhanced APK Signature Compatibility & Schedule Customization](#v1214---2026-10-03)
- [v1.2.13 - Beginner Difficulty Filtering, Customizable Weekly Schedule & Track AI Assistant](#v1213---2026-10-02)
- [v1.2.12 - Dual Camera Flashlight Torch, Workout State Persistence & Week Ahead Preview](#v1212---2026-10-02)
- [v1.2.11 - UI Alignment, Anti-Overlap Controls & Horizontal Filter Chips](#v1211---2026-10-02)
- [v1.2.10 - Track AI 'Take Me There' Navigation & Custom Exercises with AI Vision](#v1210---2026-10-02)
- [v1.2.9 - Custom Theme & Color Studio with Hue Wheel & RGB Sliders](#v129---2026-10-02)
- [v1.2.8 - High-Contrast Light Mode & Dark Mode Polish](#v128---2026-10-01)
- [v1.2.7 - Personal Record Milestone Sorting & Auto Set Termination](#v127---2026-10-01)
- [v1.2.6 - Save to Gallery Format Modal & Real Telemetry Persistence](#v126---2026-10-01)
- [v1.2.5 - Biomechanically Authentic Stickman Demonstrations](#v125---2026-10-01)
- [v1.2.4 - Zero-Distortion Camera Framing & Latency-Free Gemini Reasoning](#v124---2026-10-01)
- [v1.2.3 - Camera-Off Stickman Demo & Gemini 2.5 Flash Migration](#v123---2026-10-01)
- [v1.2.2 - Android 15 Target SDK & Production Signing Pipeline](#v122---2026-10-01)
- [v1.2.0 - v1.2.1 - ANR Fixes, Room Migration 2→3 & Immersive Fullscreen](#v120---v121---2026-09-30)
- [v1.1.8 - v1.1.9 - Exercise Auto-Detect Persistence & Apparatus Demonstration](#v118---v119---2026-09-30)
- [v1.1.0 - v1.1.7 - Animated Stickman Demo Player, Universal Vision & Speed Dial](#v110---v117---2026-09-24)
- [v1.0.0 GA - General Availability, ProGuard Optimization & QA Test Guide](#v100-ga---2026-09-23)
- [v0.13.0 - Progress Analytics, JSON Backup Export/Import & Workout Sharing](#v0130---2026-09-23)
- [v0.12.0 - Onboarding Wizard, First-Week Schedule Generator & Smart Reminders](#v0120---2026-09-23)
- [v0.11.0 - Track AI Assistant with Gemini Service & Schema Actions](#v0110---2026-09-23)
- [v0.10.0 - Local History & Adaptive Engine with Room SQLite](#v0100---2026-09-23)
- [v0.9.0 - Full-Body 35+ Calisthenics Exercise Catalog & 5 Tiers](#v090---2026-09-23)
- [v0.8.0 - Squat & Plank Real-Time Vision Analyzers](#v080---2026-09-20)
- [v0.7.0 - Live Track Coach with TTS Guidance & Fatigue Loss Analyzer](#v070---2026-09-20)
- [v0.6.1 - ARM ABI Architecture Optimization](#v061---2026-09-20)
- [v0.5.0 - v0.6.0 - Zero-Overlap Athletic HUD Redesign](#v050---v060---2026-09-19)
- [v0.4.0 - Real-Time Push-up Vision Analyzer & Depth Validation](#v040---2026-09-19)
- [v0.3.0 - Skeletal Pose Estimation, Set Recording & Privacy Playback](#v030---2026-09-19)
- [v0.2.0 - CameraX Foundation & Luxury Gold Theme](#v020---2026-09-13)
- [v0.1.0 - Project Foundation, Design Language & Navigation 3](#v010---2026-09-12)

---

## [v1.2.14] - 2026-10-03

### Fixed
- **Android "Invalid Package" Installation Error**: Resolved sideloading and manual installation failures (`INSTALL_PARSE_FAILED_NO_CERTIFICATES` / `"App not installed as package appears to be invalid"`) on Android 11–15 OEM packages (Samsung One UI, Xiaomi HyperOS/MIUI, and Google Pixel).
- **Multi-Scheme APK Signing**: Configured a dedicated `release` signing profile in `app/build.gradle.kts` enforcing both APK Signature Scheme v1 (JAR signing) and v2, verified with `apksigner` and 4-byte `zipalign`.
- **Install Upgrade Safety**: Documented clean uninstallation requirements when migrating across debug keystore updates.

---

## [v1.2.13] - 2026-10-02

### Added
- **Beginner & Novice Workout Routines**:
  - `Beginner Upper Body & Arms` (`routine_upper_beginner`): Wall Push-ups, Scapular Arm Circles, Knee Planks, Bird Dogs (15 min).
  - `Beginner Legs & Glute Strength` (`routine_lower_beginner`): Chair Squats, Glute Bridges, Double Calf Raises, Bird Dogs (15 min).
  - `Novice Upper Body & Press` (`routine_upper_novice`): Incline Push-ups, Bench Dips, Superman Holds, Side Planks (18 min).
  - `Novice Lower Body & Hips` (`routine_lower_novice`): Reverse Lunges, Glute Bridges, Good Mornings, Double Calf Raises (18 min).
- **Customizable Weekly Schedule (Manual Editing)**:
  - Added Edit Routine button on selected day cards in `HomeScreen.kt` and within the Week Ahead preview sheet.
  - Interactive `EditDayScheduleSheet` allows swapping any day of the week to any curated routine or assigning it as an active Rest & Recovery Day.
  - Instant Room DB persistence via `UserProfileRepository.kt`.
- **Track AI Schedule Assistant (`UpdateScheduleDayAction`)**:
  - Athletes can ask: *"What is my workout on Wednesday?"* or *"What is my schedule?"* for a full breakdown of target muscles and movements.
  - Direct natural language modification: *"Change Wednesday to leg day"* or *"Make Friday a rest day"*.
  - In-chat confirmation card with one-tap "View in Schedule" navigation.

### Fixed
- **Difficulty Tier Leakage**: Updated `selectRoutinesForLevelAndEquipment()` and recovery safeguards in `WorkoutScheduleEngine.kt` to ensure beginner athletes never receive advanced exercises (Diamond Push-ups, Pike Push-ups, Decline Push-ups) on Upper Body days.

---

## [v1.2.12] - 2026-10-02

### Added
- **Camera Flashlight / Torch Toggle**: Dedicated flashlight button on Coach screen with dual camera intelligence:
  - Activates high-luminance white screen flash when front-facing selfie camera is active.
  - Activates hardware LED torch via CameraX `CameraControl.enableTorch()` when rear camera is active.
- **Week Ahead Schedule Preview**: Upcoming workout schedule preview sheet to inspect upcoming training days beforehand (Wednesday, Friday, Monday, etc.).

### Fixed
- **Home Workout Switching Bug**: Fixed an issue where navigating between the Coach and Home screen caused the selected workout to reset or unexpectedly flip from Upper Body to Full Body.

---

## [v1.2.11] - 2026-10-02

### Fixed
- **Dark Mode Switch Text Overlap**: Resolved text collision where description text slid underneath and touched the toggle switch component.
- **Box Text Layout Cleanliness**: Replaced clunky long text descriptions in compact boxes with clean athletic icon buttons.
- **Experience Level Chip Overflow**: Enabled horizontal scrolling for experience level selector chips on the Profile screen to prevent awkward wrapping on narrower devices.

---

## [v1.2.10] - 2026-10-02

### Added
- **Track AI "Take Me There" Deep Navigation**: Action buttons embedded directly in Track AI responses that navigate the athlete straight to relevant screens (Exercise Library, History, Settings, Coach).
- **Custom Exercise Creator**: Athletes can create and configure custom exercises with custom names, primary muscle group tags, difficulty ratings, and equipment tags.
- **Universal AI Vision for Custom Exercises**: Extended pose estimation heuristics to track and count reps on athlete-created custom movements.

---

## [v1.2.9] - 2026-10-02

### Added
- **Custom Theme & Color Studio**: Complete theming engine supporting dynamic accents and surface styling.
- **Rainbow Presets & Hue Wheel**: Visual color picker featuring circular Hue Wheel, RGB sliders, and quick-access palette presets.

---

## [v1.2.8] - 2026-10-01

### Fixed
- **Light & Dark Theme Visual Polish**: Fixed high-contrast text rendering in Light Mode (White & Crimson Red) and eliminated background bleed artifacts in Dark Mode.

---

## [v1.2.7] - 2026-10-01

### Fixed
- **Personal Record (PR) Milestones**: Corrected milestone sorting and classification in the progress analytics dashboard.
- **Coach Set Auto-Termination**: Fixed set ending edge-cases when rep targets or rest intervals elapsed.

---

## [v1.2.6] - 2026-10-01

### Added
- **Save to Gallery Export Modal**: Choose between 3 video export formats:
  1. Clean Camera Video.
  2. Video with Pose Skeleton Overlay.
  3. Motion Sticks Only (privacy-safe skeletal animation).
- **Real Telemetry Persistence**: Workout joint telemetry is persisted for accurate post-workout form playback.

---

## [v1.2.5] - 2026-10-01

### Changed
- **Stickman Demonstration Biomechanics**: Complete overhaul of animated Stickman demo player with authentic kinematics for Bird Dog, Wall Angels, Dragon Flag, and Push-up variations.

---

## [v1.2.4] - 2026-10-01

### Changed
- **Zero-Distortion Fullscreen Camera**: Removed restrictive pill borders and distracting vertical gridlines for an unobstructed 16:9 view.
- **Track AI Latency Optimization**: Configured Gemini API `thinkingBudget = 0` to provide instantaneous responses without thinking delays.

---

## [v1.2.3] - 2026-10-01

### Added
- **Camera-Off Stickman Demo**: Displays an animated stickman exercise demonstration directly on screen when the live camera feed is disabled.
- **Gemini 2.5 Flash Migration**: Upgraded Track AI to use Google Gemini 2.5 Flash for improved reasoning and athletic coaching accuracy.

---

## [v1.2.2] - 2026-10-01

### Changed
- **Android 15 Compatibility**: Updated `targetSdk` to 35 (Android 15) and validated runtime permissions.
- **Signing Pipeline**: Configured release APK signing with v1 and v2 schemes.

---

## [v1.2.0 - v1.2.1] - 2026-09-30

### Fixed
- **Startup ANR / Crash**: Fixed startup deadlock and implemented safe Room database migration 2→3.
- **Immersive Fullscreen**: Hidden system status bar and navigation bars during live workout sessions.

### Added
- Dynamic notification icon featuring metallic gold flexed bicep.
- Completed workouts synchronization with Home dashboard statistics.

---

## [v1.1.8 - v1.1.9] - 2026-09-30

### Added
- **Exercise Auto-Detect Profile Persistence**: User preference for automatic pose-based exercise switching is saved to Room DB.
- **Apparatus Demonstrations**: Stickman demo player renders contextual apparatus (chairs, walls, yoga mats) matching the exercise.
- **Dynamic Text-to-Speech (TTS) Voice Sync**: Synchronized voice coaching cadence with rep execution velocity.

---

## [v1.1.0 - v1.1.7] - 2026-09-24

### Added
- **Animated Stickman Demo Player**: Vector-drawn skeletal animation player providing visual movement instructions for all exercises.
- **AI Pose Auto-Classifier**: Automatically identifies which exercise the athlete is performing based on body position.
- **Floating Speed Dial Menu**: Quick-action launcher for instant access to the exercise catalog, camera coach, and Track AI.
- **3D Metallic Gold Bicep Icon**: Custom launcher icon and splash branding.
- **Dynamic Version Synchronization**: Automatic app version display in Settings via `BuildConfig.VERSION_NAME`.

---

## [v1.0.0 GA] - 2026-09-23

### Added
- **General Availability (GA) Release**: First production-ready release of TrackRep.
- **ProGuard / R8 Optimization**: Code shrinking, dead-code elimination, and obfuscation rules (`proguard-rules.pro`).
- **Interactive QA Test Guide**: Comprehensive 9-journey verification runbook ([`QA_VERIFICATION_CHECKLIST.html`](QA_VERIFICATION_CHECKLIST.html) and [`QA_VERIFICATION_CHECKLIST.md`](QA_VERIFICATION_CHECKLIST.md)).
- **Hardware Optimization**: Frame pipeline tuned for smooth performance on entry-level Android devices (Infinix Smart 9).

---

## [v0.13.0] - 2026-09-23 (Phase 10)

### Added
- **Progress Analytics Dashboard**: Visual rep volume progression, muscle group distribution charts, and weekly consistency heatmaps.
- **Versioned JSON Backup & Restore**: Export complete workout logs, user profile, and custom routines to a timestamped JSON file with full import validation.
- **Workout Sharing**: Share completed workout summaries and personal records as formatted text and graphic cards.

---

## [v0.12.0] - 2026-09-23 (Phase 9)

### Added
- **First-Run Onboarding Wizard**: Fitness baseline evaluation, primary goals selector, equipment availability, and target training days setup.
- **Coherent First-Week Schedule Generator**: Automatically builds an initial 7-day training schedule balancing muscle recovery.
- **Smart Workout Reminders**: Morning and evening notifications scheduled via Android `AlarmManager` with missed-workout rollover handling.

---

## [v0.11.0] - 2026-09-23 (Phase 8)

### Added
- **Track AI Coach**: Integrated Google Gemini API for personalized calisthenics coaching.
- **Structured Context Builder**: Feeds athlete fitness level, recent volume, muscle fatigue, and available equipment to the AI.
- **Schema-Validated Action Dispatcher**: Safe execution framework (`TrackActionExecutor`, `TrackActionValidator`) enabling Track AI to adjust routines, substitute exercises, and modify schedules with user confirmation.

---

## [v0.10.0] - 2026-09-23 (Phase 7)

### Added
- **Local History & Adaptive Engine**: Room SQLite database persisting workout sessions, sets, reps, timestamps, and form flaws.
- **Progressive Overload Rules**: Adaptive algorithms recommending rep increases, tempo variations, or harder exercise progressions.
- **Muscle Recovery Safeguards**: Tracks worked muscle groups to suggest rest or alternative muscle targets when fatigue is detected.

---

## [v0.9.0] - 2026-09-23 (Phase 6)

### Added
- **Exercise Library (35+ Catalog)**: Comprehensive calisthenics movements covering Chest, Shoulders, Arms, Back, Core, Glutes, Quads, Hamstrings, and Calves.
- **5 Difficulty Tiers**: Beginner, Novice, Intermediate, Advanced, and Elite.
- **Workout Engine**: Generates balanced workout splits (Upper Body, Lower Body, Core, Full Body) tailored to athlete difficulty and equipment.

---

## [v0.8.0] - 2026-09-20 (Phase 5)

### Added
- **Multi-Exercise Vision Expansion**:
  - **Bodyweight Squat Analyzer**: Real-time knee angle tracking, hip crease depth validation, and knee cave / heel lift flaw detection.
  - **Plank Hold Analyzer**: Real-time spinal alignment monitoring, hip sagging detection, and continuous hold timer.
- **High-Contrast Exercise Selector**: Visual exercise picker with difficulty badges and target muscle tags.

---

## [v0.7.0] - 2026-09-20 (Phase 4)

### Added
- **Live Track Coach**:
  - Real-time Android Text-to-Speech (TTS) audio cues ("Go lower", "Lock out", "Rep counted").
  - Multi-rep velocity loss and fatigue tracking.
  - Tracking-lost auto-pause state machine with automatic countdown resumption when athlete returns to frame.
  - Set and rest interval timers.
  - Post-set summary card with performance stats and subjective RPE rating.

---

## [v0.6.1] - 2026-09-20

### Changed
- **ARM Architecture Optimization**: Filtered NDK ABI packaging to `arm64-v8a` and `armeabi-v7a` in `build.gradle.kts`, significantly reducing APK download footprint.

---

## [v0.5.0 - v0.6.0] - 2026-09-19

### Changed
- **Athletic HUD Redesign**: Re-engineered camera HUD with zero overlapping elements, clean typography hierarchy, high-contrast numeric counters, and responsive mobile controls.

---

## [v0.4.0] - 2026-09-19 (Phase 3)

### Added
- **Push-up Analyzer**:
  - Real-time rep counting finite state machine: `START` $\rightarrow$ `DESCENT` $\rightarrow$ `INFLECTION` $\rightarrow$ `ASCENT` $\rightarrow$ `COMPLETE`.
  - Elbow flexion angle calculation ($90^\circ$ depth validation).
  - Spinal alignment (sagging / pike detection).
  - Rep completion green flash burst animation.

---

## [v0.3.0] - 2026-09-19 (Phase 2)

### Added
- **ML Kit Pose Estimation**: On-device 33-landmark pose detection pipeline running at 30 FPS.
- **Real-Time Skeleton Lines**: Visual joint telemetry overlay drawn directly on camera preview.
- **Workout Set Recording & Playback**: Records video clips during workout sets and provides synchronized form flaw review.
- **Privacy "Motion Sticks Only" Replay**: View playback strictly as an animated stickman without storing or showing physical camera footage.
- **Phone Album Export**: Save recorded sets and telemetry videos to device gallery.

---

## [v0.2.0] - 2026-09-13 (Phase 1)

### Added
- **Camera Foundation**: CameraX preview pipeline with front and rear camera switching.
- **Runtime Permissions**: Android runtime camera and storage permission handling.
- **Framing Calibration Overlay**: Silhouette guide helping athletes position phone at optimal distance and angle.
- **Phone Setup Tutorial**: Interactive step-by-step positioning walkthrough.
- **Signature Luxury Gold Palette**: Dark mode metallic gold (`#FFD700`) accents on true black (`#000000`).

---

## [v0.1.0] - 2026-09-12 (Phase 0)

### Added
- **Project Foundation**: Initial repository structure (`com.setons.trackrep`), Gradle setup, and dependency management (`libs.versions.toml`).
- **Core Tech Stack**: Kotlin 2.x, Jetpack Compose, Material 3, AndroidX Navigation 3.
- **Dual Theme Support**:
  - Dark Mode: Black & Luxury Gold.
  - Light Mode: White & Crimson Red.
- **Navigation Architecture**: Type-safe navigation framework connecting Home, Coach, Exercise Library, History, and Profile screens.
