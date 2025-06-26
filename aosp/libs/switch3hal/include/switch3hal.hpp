#ifndef SWITCH3_HAL_H_
#define SWITCH3_HAL_H_

#include <GpioHal.h>
#include <string>

class Switch3Hal
{
public:
    enum class State : int
    {
        INVALID = 0,
        LEFT = 1,
        RIGHT = 2,
        CENTER = 3
    };

    Switch3Hal(int pinA, int pinB);
    ~Switch3Hal();

    State getState();

private:
    GpioHal mGpioA;
    GpioHal mGpioB;

    constexpr int PRESSED = 0;
    constexpr int UNPRESSED = 1;
};

#endif /* SWITCH3_HAL_H_ */