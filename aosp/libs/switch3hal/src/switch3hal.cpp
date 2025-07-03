#include "switch3hal.hpp"

Switch3Hal::Switch3Hal(int pinA, int pinB)
    : mPinA(pinA), mPinB(pinB) {
    gpio.exportGpio(mPinA);
    gpio.exportGpio(mPinB);
    gpio.setGpioDirection(mPinA, "in");
    gpio.setGpioDirection(mPinB, "in");
}

Switch3Hal::~Switch3Hal() {}

Switch3Hal::State Switch3Hal::getState() 
{
    int valueA = 0;
    int valueB = 0;

    gpio.getGpioValue(mPinA, &valueA);
    gpio.getGpioValue(mPinB, &valueB);

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
