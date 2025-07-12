
#include "Switch3Hal.h"

Switch3Hal::Switch3Hal(int pinA, int pinB)
    : mGpioA(pinA, "in"),
      mGpioB(pinB, "in") {}

Switch3Hal::~Switch3Hal() = default;

Switch3Hal::State Switch3Hal::getState() 
{
    int valueA = 0;
    int valueB = 0;

    mGpioA.getGpioValue(mGpioA.getPin(), &valueA);
    mGpioB.getGpioValue(mGpioB.getPin(), &valueB);

    if (valueA == PRESSED && valueB == UNPRESSED) 
    {
        return State::LEFT;
    }
    else if (valueA == UNPRESSED && valueB == PRESSED)
    {
        return State::RIGHT;
    }
    else if (valueA == UNPRESSED && valueB == UNPRESSED)
    {
        return State::CENTER;
    }
    else /* if (valueA == PRESSED && valueB == PRESSED) */
    {
        return State::INVALID;
    }
}