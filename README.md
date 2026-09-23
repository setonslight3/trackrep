# TrackRep

> **Developed by Setons**  
> Adaptive full-body workout app with **Track** AI and live camera-based form tracking.

---

## 🎯 Product Overview

**TrackRep** is a local-first Android fitness application designed for full-body muscle development across chest, shoulders, arms, back, core, glutes, quads, hamstrings, and calves.

### Key Highlights
- **Track AI Coach**: Adaptive assistant powered by Gemini for intelligent routine adjustments, exercise substitutions, recovery safeguards, and progressive overload.
- **Live On-Device Camera Coach**: Signature real-time rep counting and form feedback using on-device pose estimation. Zero video streaming ensures 100% privacy.
- **Hardware-Optimized**: Engineered with high-efficiency frame pipelines specifically targeted for the **Infinix Smart 9**.
- **Signature Design Language**:
  - **Dark Mode**: Black (`#000000`) & Gold (`#FFD700`)
  - **Light Mode**: White (`#FFFFFF`) & Red (`#D32F2F`)
- **Local-First Privacy**:
  - Workout history, metrics, and profiles are persisted locally in SQLite / Room.
  - Controlled, schema-validated AI action layer prevents unauthorized modifications.
  - Versioned backup export and import.

---

## 🛠️ Tech Stack & Architecture

- **Language & UI**: Kotlin 2.x + Jetpack Compose + AndroidX Navigation 3 + Material 3
- **Vision & Pose Estimation**: CameraX + on-device pose estimation with rep-phase state machines
- **Local Persistence**: Room SQLite + Jetpack DataStore
- **Background Work & Scheduling**: Android WorkManager
- **AI Integration**: Gemini API via strict schema-validated function dispatcher

---

## 🏗️ Phased Development Roadmap

| Phase | Description | Status |
| :---: | :--- | :---: |
| **0** | **Project Foundation** (Identity, Black/Gold & White/Red themes, Navigation 3, build setup) | ✅ Completed |
| **1** | **Camera Foundation** (CameraX preview, runtime permissions, framing calibration) | ✅ Completed |
| **2** | **Pose Detection** (On-device landmark estimator, debug overlay, confidence tracking) | ✅ Completed |
| **3** | **Push-up Analyzer** (Rep counting state machine, ROM, alignment, timing) | ✅ Completed |
| **4** | **Live Track Coach** (Real-time counter UI, form feedback, fatigue indicators) | ✅ Completed |
| **5** | **Squat + Plank** (Multi-exercise vision expansion) | ✅ Completed |
| **6** | **Exercise Library + Workout Engine** (Catalog, 5 difficulty levels, balanced workouts) | ✅ Completed |
| **7** | **Local History + Adaptive Engine** (Room DB, progression, recovery rules) | ✅ Completed |
| **8** | **Track AI** (Gemini integration, schema-validated action layer, chat UI) | ✅ Completed |
| **9** | **Onboarding + Scheduling** (User setup, first week generator, reminders, missed sessions) | ✅ Completed |
| **10** | **Progress + Export/Import** (Dashboard, versioned JSON backup/restore, workout sharing) | ✅ Completed |
| **11** | **Release Hardening** (Performance profiling on Infinix Smart 9, release APK) | ⏳ Upcoming |

---

## 🚀 Building & Running

### Prerequisites
- JDK 17+ (e.g. OpenJDK 25)
- Android SDK (API 34+)
- Gradle 9+ / Android Gradle Plugin 9+

### Build Debug APK
```bash
./gradlew assembleDebug
```
The resulting APK is generated at:
`app/build/outputs/apk/debug/app-debug.apk`

### Install to Connected Device (Infinix Smart 9)
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 📄 License & Attribution
Designed and developed by **Setons**. All rights reserved.
