#ifndef SWITCH3_HAL_H_
#define SWITCH3_HAL_H_

#include <gpiohalrpi5.hpp>
#include <string>

#define PRESSED    1
#define UNPRESSED  0

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

    int mPinA;
    int mPinB;

    GpioHal gpio;

};

#endif /* SWITCH3_HAL_H_ */
