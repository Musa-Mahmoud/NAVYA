## Switch property in VHAL 
#### Steps :
##### 
1. In hardware/interfaces/automotive/vehicle/aidl/impl/utils/test_vendor_properties/androidhardware/automotive/vehicle/TestVendorProperty.aidl add Switch property
```cpp
    /**
     * Property used for {Turn signal / Switch check}
     * VehiclePropertyGroup.VENDOR | VehicleArea.GLOBAL | VehiclePropertyGroup.INT32
     * PropertyID = 557842692
     */
    VENDOR_EXTENSION_SWITCH_CAMERA_CONTROL_PROPERTY = 0x104 + 0x20000000 + 0x01000000 + 0x00400000,
```
##### 2. In hardware/interfaces/automotive/vehicle/aidl/impl/default_config/config/TestProperties.json 
```cpp
        {
            "property": "TestVendorProperty::VENDOR_EXTENSION_SWITCH_CAMERA_CONTROL_PROPERTY",
            "access": "VehiclePropertyAccess::READ_WRITE",
            "changeMode": "VehiclePropertyChangeMode::ON_CHANGE",
            "defaultValue":{
            	"int32Values":[
            		0
            	]
            }
        },
```
##### 3. packages/services/Car/service/src/com/android/car/hal/fakevhal/FakeVhalConfigParser.java
```cpp
            Map.entry("VENDOR_EXTENSION_SWITCH_CAMERA_CONTROL_PROPERTY",
                    TestVendorProperty.VENDOR_EXTENSION_SWITCH_CAMERA_CONTROL_PROPERTY)   
```
##### 4. hardware/interfaces/automotive/vehicle/aidl/impl/fake_impl/hardware/src/FakeVehicleHardware.cpp 
######    add the switch3hal.hpp 
```cpp
   #include <switch3hal.hpp>
```
###### add the property logic inside the 
```cpp
FakeVehicleHardware::ValueResultType FakeVehicleHardware::maybeGetSpecialValue(
        const VehiclePropValue& value, bool* isSpecialValue) const {
```  
```cpp
        case toInt(TestVendorProperty::VENDOR_EXTENSION_SWITCH_CAMERA_CONTROL_PROPERTY):{
            *isSpecialValue = true;
            static Switch3Hal switch3(17, 27); // initialize once

            int switchValue = 0;
            auto state = switch3.getState();

            if (state == Switch3Hal::State::LEFT) {
                switchValue = 1;
            } else if(state == Switch3Hal::State::CENTER){
                switchValue = 0;
            }

            result = mValuePool->obtainInt32(switchValue);
            ALOGD("Switch state (LEFT=1): %d", switchValue);
            return result;

        }
```          