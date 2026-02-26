Android Multi-Camera Device Monitor App
(This is an independent demo project; all trademarks belong to their respective owners.)

An Android application that combines camera control, device/system data monitoring, hardware sensor access, and background task management in a single UI.
This project is designed as a hands-on demo for Android app development involving:

Features:
1) Camera Usage
Multi-camera switching (e.g. front / rear camera)
Video recording
Camera scene zoom (zoom in / zoom out)

2) System / Device Data Reading
Displays device storage information in the top-right corner
Dynamically updates the Camera directory size (e.g. recorded videos folder size)

3) Hardware Interfaces Access
Reads gyroscope status/data
Displays gyroscope information in the top-left corner

4) Background Processes
Starts a background process/task
Executes an operation every 5 seconds
Shows task status with a counter at the bottom of the screen

UI Overview
Top-left: Gyroscope status / sensor data
Top-right: Device storage info + Camera directory size
Center: Camera preview, zoom control, recording control, camera switch
Bottom: Background task counter / status

Tech Highlights

This app demonstrates practical Android development skills in:
Camera integration (camera switching, recording, zoom)
File system monitoring (directory size calculation and dynamic UI update)
Hardware sensor access (gyroscope via SensorManager)
Periodic background execution (timer/handler/coroutine/worker-based task)
Real-time UI updates from multiple data sources
Possible Tech Stack (Customize to Your Implementation)
Update this section based on your actual code.

Language: Kotlin / Java
UI: XML / Jetpack Compose
Camera: Camera2 API / CameraX
Sensors: SensorManager (Gyroscope)
Background Task: Handler / Coroutine / WorkManager / Foreground Service
Storage Info: StatFs, file APIs

Permissions
This app may require the following permissions (depending on implementation):
CAMERA
RECORD_AUDIO (if video recording includes audio)
READ_MEDIA_VIDEO / READ_EXTERNAL_STORAGE (Android version dependent)
WRITE_EXTERNAL_STORAGE (legacy Android versions only, if applicable)

Demo
APK file: https://github.com/ruanhongfu/WEROCK/releases/download/demo/app-debug.apk
Demo video: https://github.com/ruanhongfu/WEROCK/releases/download/demo/demo.mp4
<img width="1080" height="2160" alt="demo" src="https://github.com/user-attachments/assets/ad1ca066-d8d5-4577-8faf-e6df8a1e4d9c" />

