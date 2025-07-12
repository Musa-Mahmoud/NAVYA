# Integrating Switch3Hal Library into AOSP

## Overview
This guide explains how to integrate the `libswitch3hal` library into the Android Open Source Project (AOSP) repository. The library provides hardware abstraction for a 3-position toggle switch that simulates car turn signal states using GPIO pins.

## Directory Structure
Create the following directory structure in your AOSP tree:

```
vendor/
└── iti/
    ├── libs/
    │   └── switch3hal/
    │       ├── include/
    │       │   └── Switch3Hal.h
    │       ├── src/
    │       │   └── Switch3Hal.cpp
    │       └── Android.bp
    └── aosp_rpi4_car_iti.mk 
```

## Step 1: Add Library to Build System
Edit `vendor/iti/aosp_rpi4_car_iti.mk ` to include the library in the build:

```makefile
# Add to existing PRODUCT_PACKAGES
PRODUCT_PACKAGES += libswitch3hal
```

## Step 2: Verify Dependencies
Ensure the following dependency exists in your AOSP build:

- `libgpiohal` (GPIO Hardware Abstraction Library)

## Step 3: Build and Verify
Run the following commands to build and verify the library:

```bash
# Build the library
make libswitch3hal

# Verify library inclusion
ls out/target/product/<your_target>/system/lib[64]/libswitch3hal.so
```