# NAVYA — *Next-gen Automotive Voice and Vision Assistant*

Welcome to **NAVYA**, a futuristic embedded system that blends vision, voice, and environmental awareness to enhance driving safety and user interaction in smart vehicles.

NAVYA transforms conventional car systems by integrating:
- **Blind spot detection & Real-time Camera mirroring** via side cameras
- **Ambient light signaling** for object proximity
- **Voice-activated assistant**

Made for AOSP (Android Open Source Project) platforms — designed with embedded innovation in mind.

---

## Features

### Object and Blind Spot Detection System

> A smart enhancement for traditional side mirrors.

- **Side Camera Integration**: Mirrors are enhanced with high-definition side-mounted cameras.
- **Real-Time Object Detection**:
  - Uses computer vision to detect nearby vehicles or obstacles in blind spots.
  - Draws a **green circular safety indicator** on the display when an object is too far. 
  - Draws a **yellow circular warning indicator** on the display when an object is too closer.
  - Draws a **red circular warning indicator** on the display when an object is too close indicate there are a danger.
- **On-Screen Feedback**: Drivers get intuitive visual alerts, reducing the chance of missing threats.

---

### Ambient Light Warning System

> Because your eyes shouldn't be the only way to sense danger.

- **LED Ambient Lighting Strip** embedded near the display or window frame.
- **Context-Aware Color**:
  - Default: Cool ambient tones.
  - **Red Warning Glow**: When an object approaches dangerously close, the ambient light turns red to give a clear visual cue even in peripheral vision.
- Perfect for low-visibility or high-speed driving conditions.

---

### Live Camera Feed Display

> Mirrors are outdated. Live digital vision is here.

- **Screen Mirroring** of the side cameras.
- Built using Android’s SurfaceView or CameraX for efficient, low-latency rendering.
- Seamless integration with AOSP UI layers.

---

### Voice Assistant 

> Your car listens, understands, and talks back.

- **Wake Word Detection**: Say `"HiCar"` to activate the assistant.
- **Voice Response**:
  - Assistant greets back with `"Hi, [User]"`.
- Built using **Android's Vosk API** and **Text-to-Speech (TTS)** engine.
- Potential for extended functionality: Navigation, weather updates, or media control.

---
## Technologies & Tools
- **Android Open Source Project (AOSP)**
- **TensorFlow Lite** – for real-time object detection
- **CameraX / SurfaceView** – for rendering side camera feeds
- **Android Vosk / TTS** – for voice commands and replies
- **HAL + GPIO Control** – for LED hardware integration
- **VHAL** (Vehicle HAL)
- **SELinux** – to enforce security policies on camera/light access

---
##  Project Structure
**NAVYA/**<br>
│<br>
├── **app/**<br>
├── **camera/**<br>
├── **voice_assistant/**<br>
├── **ambient_light/**<br>
├── **hardware/**<br>
└── **sepolicy/**

---
##  Developed By

- **Abdallah Salah Mohammed**
- **Aliaa Ahmed Mortada** 
- **Mostafa Mohammed Mohammed**
- **Mousa Mahmoud Salah**
- **Youssef Mostafa Mohammed** 
- AOSP Project Team – Embedded Systems & Android Automotive Engineers
---
##  Future Improvements

-  SOME/IP Communication for supporting Two-Cameras
-  Send SOS message when crash happened
-  Bare-Metal Programming and CAN Bus Protocol and simulate turn signal
-  Gesture controls for touchless interaction
-  Companion mobile app for status and control
---
## System Requirements Specification (SRS)

### Functional System Requirements

| ID              | Requirement Description                                                                 |
|-----------------|-----------------------------------------------------------------------------------------|
| SRS-FUNC-001    | The system shall detect objects in the blind spot using side-mounted cameras and computer vision. |
| SRS-FUNC-002    | The system shall display visual safety indicators (Green/Yellow/Red) based on object proximity. |
| SRS-FUNC-003    | The system shall mirror the side camera feeds on the digital display in real time.       |
| SRS-FUNC-004    | The system shall provide ambient light warnings using an LED strip based on object proximity. |
| SRS-FUNC-005    | The system shall activate the voice assistant via a wake word ("HiCar").                |
| SRS-FUNC-006    | The voice assistant shall respond using TTS and perform limited interactions like greeting the user. |
| SRS-FUNC-007    | The system shall enforce security access to camera and LED using SELinux.               |

---
## Software Requirements Specification (SwRS)

### Software Functional Requirements

| ID              | Requirement Description                                                                 |
|-----------------|-----------------------------------------------------------------------------------------|
| SWRS-FUNC-001   | App shall run as a privileged system app in AOSP and use SurfaceView or CameraX for camera feed. |
| SWRS-FUNC-002   | TensorFlow Lite shall be used for object detection in real-time on camera frames.       |
| SWRS-FUNC-003   | Vosk API shall be used to detect wake word and speech-to-text conversion.               |
| SWRS-FUNC-004   | Android TTS engine shall be used to reply back to the user.                             |
| SWRS-FUNC-005   | App shall control the GPIO-based LED strip using HAL or JNI interface.                  |
| SWRS-FUNC-006   | SELinux policies shall restrict access to camera and GPIO control to the NAVYA system app only. |
| SWRS-FUNC-007   | App shall draw proximity circles using Android Canvas or OpenGL overlay.                |
---

> **NAVYA** aims to redefine the driver experience by combining safety, intelligence, and interaction — built fully on embedded Android technologies.
