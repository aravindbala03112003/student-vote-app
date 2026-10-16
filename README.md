# 🗳️ Campus Vote — Student Voting & University Election Android Application

[![Release](https://img.shields.io/github/v/release/aravindbala03112003/student-vote-app?color=blue&label=Latest%20Release)](https://github.com/aravindbala03112003/student-vote-app/releases/tag/v1.0.0)
[![Download APK](https://img.shields.io/badge/Download-CampusVote--v1.0.0.apk-success?logo=android&logoColor=white)](https://github.com/aravindbala03112003/student-vote-app/releases/download/v1.0.0/CampusVote-v1.0.0.apk)
[![Android](https://img.shields.io/badge/Platform-Android%207.0%2B%20(API%2024%2B)-green)](https://developer.android.com)
[![License](https://img.shields.io/badge/License-MIT-lightgrey.svg)](LICENSE)

**Campus Vote** is an advanced, secure Android application designed for managing student council and university elections. Built with Java, XML, Firebase Realtime Database, Room offline caching, Biometric authentication, and Google ML Kit OCR text recognition.

---

## 📥 Direct APK Download

Anyone can download and install the signed production-ready APK directly to an Android phone:

👉 **[Download CampusVote-v1.0.0.apk (50.3 MB)](https://github.com/aravindbala03112003/student-vote-app/releases/download/v1.0.0/CampusVote-v1.0.0.apk)**

Or visit the **[GitHub Releases Page](https://github.com/aravindbala03112003/student-vote-app/releases/tag/v1.0.0)**.

### 📲 Installation Instructions:
1. Tap the download link on your Android device.
2. If prompted by Android, tap **Settings** and enable **"Allow from this source"** (Install unknown apps).
3. Tap **Install** and open **Campus Vote**.
4. Grant Camera and Location permissions when prompted to use ID card scanning and geofencing verification.

---

## 🏛️ Application Roles & Features

### 🎓 Student Role
* **Registration & Secure Login**: Account creation with Firebase Auth and optional biometric fingerprint confirmation.
* **Student ID Card OCR Scanner**: Uses Google ML Kit Text Recognition to scan student ID cards with the phone's camera, automatically parsing and binding student roll/registration numbers.
* **Geofence Radius Validation**: Employs Google Play Services `FusedLocationProviderClient` to ensure the student is physically within university campus boundaries before enabling vote casting.
* **Biometric Vote Confirmation**: Prompts `BiometricPrompt` fingerprint verification before submitting the ballot.
* **Voter Feedback Dialog**: Prompts voter reason/feedback per candidate.
* **Duplicate Vote Prevention**: Double-layered verification across local Room database and Firebase cloud records ensures every student can cast exactly one vote.

### 👔 Candidate Role
* **Candidate Application Flow**: Dedicated registration where candidates provide personal details, department, year, and party affiliations.
* **Document Verification Uploads**: Built-in image pickers for Student ID card and Marksheet proof, encoded into optimized base64 format for cloud review.
* **Eligibility & Backlog Checks**: Declares backlog status and academic credentials.
* **Application Status Tracking**: Real-time review status (`PENDING`, `APPROVED`, `REJECTED`) dynamically retrieved from Firebase.

### 🛡️ Election Administrator Role
* **Candidate Management**:
  * View pending candidate requests with direct access to inspect submitted ID cards and marksheet documents.
  * One-tap candidate approval and rejection.
  * Instant manual candidate creation with department, party, and gender attributes.
  * Long-press candidate deletion to remove active candidates.
* **Geofence & Session Management**:
  * Set election start and end dates/times with native Date and Time pickers.
  * Live campus coordinates acquisition (`FusedLocationProviderClient`) or manual latitude/longitude entry.
  * Configurable geofence radius (meters).
* **Broadcast Announcements & SMS**:
  * Publish live election announcements stored in Firebase Realtime Database.
  * Optional bulk SMS dispatch directly to student contact numbers via Android `SmsManager`.
* **Live Results & Analytics**:
  * Real-time calculation of vote totals and candidate tallies.
  * Interactive Pie Charts and Bar Charts powered by `MPAndroidChart`.
  * Chronological ballot audit stream with voter ID, candidate, timestamp, and feedback.
* **Official PDF Report Export**:
  * Native Android `PdfDocument` generation producing formatted 5-column election result tables.
  * Instant viewing and sharing via Android `FileProvider`.
* **Admin Database Management**:
  * Secure cloud data reset option restricted exclusively to authorized administrators.

---

## 🏗️ Technical Architecture

| Layer | Technologies |
|---|---|
| **Language & Platform** | Java 11 / 21, Android SDK (minSdk 24, compileSdk 35, targetSdk 35) |
| **UI Framework** | Native Android XML layouts, Material Components 3, ViewBinding |
| **Cloud Backend** | Firebase Authentication, Firebase Realtime Database (with offline persistence) |
| **Local Cache** | Android Room Database (SQLite) with multi-entity DAOs |
| **Location Services** | Google Play Services Location (`FusedLocationProviderClient`, Priority.PRIORITY_HIGH_ACCURACY) |
| **Vision & ML** | Google ML Kit Text Recognition Latin (`com.google.mlkit:text-recognition:16.0.1`) |
| **Biometrics** | AndroidX Biometric (`androidx.biometric:biometric:1.1.0`) |
| **Data Visualization** | MPAndroidChart (`com.github.PhilJay:MPAndroidChart:v3.1.0`) |
| **Animations** | Airbnb Lottie Android (`com.airbnb.android:lottie:6.4.0`) |
| **File Sharing** | AndroidX `FileProvider` with scoped storage support |

---

## 🔒 Security Architecture & Firebase Rules

To secure the voting system against unauthorized tampering, the application enforces:
1. **Server-Enforced Vote Immutability**: Rules prevent modifying or deleting previously recorded votes (`!data.exists() && newData.exists()`).
2. **Administrative Access Control**: Candidate creation, session management, geofence configuration, and broadcast publishing are restricted to authenticated admins.
3. **Double Duplicate Protection**: Client-side Room caching blocks duplicate attempts instantly even offline; Firebase Realtime Database validates cloud-wide uniqueness.

Recommended database security rules are located in [`database.rules.json`](database.rules.json).

---

## 🛠️ Building From Source

### Prerequisites:
- Android Studio Ladybug / Meerkat or later
- JDK 17 or JDK 21 (bundled in Android Studio at `<Android Studio>/jbr`)
- Android SDK Platform 35 and Build-Tools 35.0.0

### Build Commands:
```bash
# Clone the repository
git clone https://github.com/aravindbala03112003/student-vote-app.git
cd student-vote-app

# Run Unit Tests
./gradlew test

# Assemble Debug APK
./gradlew assembleDebug

# Assemble Signed Release APK
./gradlew assembleRelease
```

Generated APKs will be output to:
- **Release APK**: `app/build/outputs/apk/release/app-release.apk`
- **Debug APK**: `app/build/outputs/apk/debug/app-debug.apk`

---

## 📜 Release Verification

- **APK Name**: `CampusVote-v1.0.0.apk`
- **File Size**: 52,755,875 bytes (~50.31 MB)
- **SHA-256 Checksum**: `39E036E13E27D17BBF3CB5A988C66B91D0D1A8F5F7E5C20A28331A9677528916`
- **Signature Scheme**: APK Signature Scheme v2 (2048-bit RSA)
- **Cert SHA-256**: `41:67:22:91:5E:9A:2E:51:07:32:CE:99:18:D0:A8:1D:0F:2C:49:69:FB:E5:64:74:F6:94:CD:56:EC:E3:EF:4A`
