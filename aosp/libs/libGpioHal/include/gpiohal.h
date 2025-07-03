#ifndef GPIO_HAL_H
#define GPIO_HAL_H

#include <string>

class GpioHal {

public:

    bool exportGpio(int pin);

    bool setGpioDirection(int pin, const char* direction);

    bool setGpioValue(int pin, int value);

    bool getGpioValue(int pin, int* value);

private:

    bool gpioExists(int pin);
};

#endif