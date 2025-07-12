#ifndef GPIO_HAL_RPI5_H
#define GPIO_HAL_RPI5_H

extern "C" {
#include <gpiod.h>
}
#include <iostream>
#include <unordered_map>


class GpioHal {
public:
    GpioHal();
    ~GpioHal();

    bool setGpioDirection(int pin, const char* direction);
    bool setGpioDirOut(int pin);
    bool setGpioDirIn(int pin);
    bool setGpioValue(int pin, int value);
    bool getGpioValue(int pin, int* value);

private:
    gpiod_chip* chip_;
    std::unordered_map<int, gpiod_line_request*> requests_;
    bool requestLine(int pin, gpiod_line_direction direction, int value = 0);
};

#endif
