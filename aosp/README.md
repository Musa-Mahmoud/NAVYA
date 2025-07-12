# 🚗 Multi-User & Multi-Display Support for AOSP15 on Raspberry Pi 5

This document explains how to configure **Android Automotive (AOSP15)** on **Raspberry Pi 5** to support **multiple displays** with **separate users and touch input handling**.

It includes:
- Framework and vendor overlays
- Display-to-user mappings
- Input routing per display
- UX restriction overrides

---

## 📦 Summary of Changes

✅ Independent touch input for each display  
✅ Separate UI per user on each display  
✅ Multi-user UI enabled system-wide  
✅ UX restrictions disabled for testing  
✅ Default home activity per display  

---

## 📁 Directory Structure

```
vendor/navya/
├── MultiDisplay/                # Overlay for display + user mappings
│   ├── Android.bp
│   ├── AndroidManifest.xml
│   ├── res/values/config.xml
│   └── xml/car_ux_restrictions_map.xml
├── MultiDisplayFramework/      # Framework-level multi-user flags
│   ├── Android.bp
│   ├── AndroidManifest.xml
│   └── res/values/config.xml
├── input_screen/               # Touch input port mapping
│   └── input-port-associations.xml
└── aosp_rpi5_car_navya.mk      # Build configuration file
```

---

## 🔌 Input Routing Per Display

To solve the issue of **mirrored touch input across screens**, we manually assigned input devices to specific displays.

### 📄 `input_screen/input-port-associations.xml`

```xml
<ports>
    <port display="0" input="usb-xhci-hcd.1-1.2/input0" />
    <port display="1" input="usb-xhci-hcd.1-1.3/input0" />
</ports>
```

💡 Replace device names with actual paths from `/proc/bus/input/devices`.

### 📥 Add to build:

```makefile
PRODUCT_COPY_FILES += \
    vendor/navya/input_screen/input-port-associations.xml:$(TARGET_COPY_OUT_VENDOR)/etc/input-port-associations.xml
```

---

## 🧩 Vendor Overlay: MultiDisplay

This overlay adds support for **mapping displays to users**, **enabling UI on secondary display**, and **customizing default activity**.

### 📄 `MultiDisplay/AndroidManifest.xml`

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.android.car.multidisplay.rpi"
    android:versionCode="1"
    android:versionName="1.0">
    <application android:hasCode="false" />
    <overlay
        android:targetPackage="com.android.car.updatable"
        android:targetName="CarServiceCustomization"
        android:isStatic="true"
        android:priority="100" />
</manifest>
```

### 📄 `MultiDisplay/Android.bp`

```bp
runtime_resource_overlay {
    name: "MutliDisplayRpiOverlay",
    resource_dirs: ["res"],
    sdk_version: "current",
    vendor: true,
}
```

### 📄 `MultiDisplay/res/values/config.xml`

```xml
<resources xmlns:xliff="urn:oasis:names:tc:xliff:document:1.2">
    <!-- Assign display to each user zone -->
    <string-array name="config_occupant_zones" translatable="false">
        <item>occupantZoneId=0,occupantType=DRIVER,seatRow=1,seatSide=driver</item>
        <item>occupantZoneId=1,occupantType=REAR_PASSENGER,seatRow=2,seatSide=left</item>
    </string-array>

    <string-array name="config_occupant_display_mapping" translatable="false">
        <item>displayPort=0,displayType=MAIN,occupantZoneId=0,inputTypes=TOUCH_SCREEN|DPAD_KEYS|NAVIGATE_KEYS|ROTARY_NAVIGATION</item>
        <item>displayPort=1,displayType=MAIN,occupantZoneId=1,inputTypes=TOUCH_SCREEN</item>
    </string-array>

    <!-- Optional: Set custom launcher -->
    <string name="defaultHomeActivity" translatable="false">android.vendor.cmtest/.MainActivity</string>

    <!-- Enable user profile per display -->
    <bool name="enablePassengerSupport">true</bool>
    <bool name="config_multiuserVisibleBackgroundUsers">true</bool>
    <bool name="enableProfileUserAssignmentForMultiDisplay" translatable="false">true</bool>
    <bool name="config_enableOccupantZoneUserAssignmentFromOccupantType" translatable="false">true</bool>
</resources>
```

---

## 🚦 UX Restriction Override

By default, Android restricts interaction while driving. This file removes UX restrictions on all displays.

### 📄 `MultiDisplay/xml/car_ux_restrictions_map.xml`

```xml
<UxRestrictions xmlns:car="http://schemas.android.com/apk/res-auto">
    <RestrictionMapping physicalPort="0">
        <DrivingState state="parked">
            <Restrictions requiresDistractionOptimization="false" uxr="none"/>
        </DrivingState>
        <DrivingState state="idling">
            <Restrictions requiresDistractionOptimization="false" uxr="none"/>
        </DrivingState>
        <DrivingState state="moving">
            <Restrictions requiresDistractionOptimization="false" uxr="none"/>
        </DrivingState>
    </RestrictionMapping>

    <RestrictionMapping physicalPort="1">
        <DrivingState state="parked">
            <Restrictions requiresDistractionOptimization="false" uxr="none"/>
        </DrivingState>
        <DrivingState state="idling">
            <Restrictions requiresDistractionOptimization="false" uxr="none"/>
        </DrivingState>
        <DrivingState state="moving">
            <Restrictions requiresDistractionOptimization="false" uxr="none"/>
        </DrivingState>
    </RestrictionMapping>
</UxRestrictions>
```

---

## 🧠 Framework Overlay: MultiDisplayFramework

This module enables **multi-user UI**, **per-display focus**, and raises user limits.

### 📄 `MultiDisplayFramework/Android.bp`

```bp
runtime_resource_overlay {
    name: "AndroidRpiFrameWorkMultiDisplay",
    resource_dirs: ["res"],
    sdk_version: "current",
    vendor: true,
}
```

### 📄 `MultiDisplayFramework/AndroidManifest.xml`

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="android.rpi.multidis"
    android:versionCode="1"
    android:versionName="1.0" >
    <application android:hasCode="false" />
    <overlay
        android:targetPackage="android"
        android:isStatic="true"
        android:priority="2000" />
</manifest>
```

### 📄 `MultiDisplayFramework/res/values/config.xml`

```xml
<resources>
    <bool name="config_perDisplayFocusEnabled">true</bool>
    <integer name="config_multiuserMaximumUsers">10</integer>
    <integer name="config_multiuserMaxRunningUsers">5</integer>
    <bool name="config_multiuserDelayUserDataLocking">true</bool>
    <bool name="config_multiuserVisibleBackgroundUsers">true</bool>
    <bool name="config_multiuserVisibleBackgroundUsersOnDefaultDisplay">true</bool>
    <bool name="config_enableMultiUserUI">true</bool>
    <bool name="config_showUserSwitcherByDefault">true</bool>
    <bool name="config_customUserSwitchUi">true</bool>
    <bool name="config_keyguardUserSwitcher">true</bool>
    <bool name="enableProfileUserAssignmentForMultiDisplay">true</bool>
</resources>
```

---

## 🧰 Build Integration: Makefile

### 📄 `aosp_rpi5_car_navya.mk`

```makefile
PRODUCT_PACKAGES += \
    libgpiod \
    libgpiohalrpi5 \
    tinycap \
    libswitch3hal \
    libads1115 \
    neopixelhal \
    MutliDisplayRpiOverlay \
    AndroidRpiFrameWorkMultiDisplay

PRODUCT_COPY_FILES += \
    vendor/navya/libs/init.navyahw.rc:root/init.navyahw.rc \
    vendor/navya/input_screen/input-port-associations.xml:$(TARGET_COPY_OUT_VENDOR)/etc/input-port-associations.xml
```

---

## 🔨 Build the System

```bash
source build/envsetup.sh
lunch aosp_rpi5_car_navya-userdebug
make -j$(nproc)
```

---

## ✅ Expected Result

- 🧑 Display 0 shows Driver UI, display 1 shows Rear Passenger UI.
- 📱 Touch input works separately on each screen.
- 🔁 User switching UI available on both displays.
- 🎯 Different home apps per user/display (if set).
---



