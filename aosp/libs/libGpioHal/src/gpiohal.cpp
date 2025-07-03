#include "gpiohal.h"
#include <fstream>
#include <string>
#include <sys/stat.h>
#include <iostream>


bool GpioHal::gpioExists(int pin) {
    std::string gpioPath = "/sys/class/gpio/gpio" + std::to_string(pin);
    struct stat statBuffer;
    return (stat(gpioPath.c_str(), &statBuffer) == 0 && S_ISDIR(statBuffer.st_mode));
}


bool GpioHal::exportGpio(int pin) {
    if (gpioExists(pin)) {
        std::cout << "GPIO pin " << pin << " already exported." << std::endl;
        return true; 
    }

    std::ofstream exportFile("/sys/class/gpio/export");
    if (!exportFile) return false;
    exportFile << pin;
    return exportFile.good();
}


bool GpioHal::setGpioDirection(int pin, const char* direction) {
    std::string directionPath = "/sys/class/gpio/gpio" + std::to_string(pin) + "/direction";
    std::ofstream directionFile(directionPath);
    if (!directionFile) return false;
    directionFile << direction;
    return directionFile.good();
}


bool GpioHal::setGpioValue(int pin, int value) {
    std::string valuePath = "/sys/class/gpio/gpio" + std::to_string(pin) + "/value";
    std::ofstream valueFile(valuePath);
    if (!valueFile) return false;
    valueFile << (value ? 1 : 0);
    return valueFile.good();
}


bool GpioHal::getGpioValue(int pin, int* value) {
    std::string valuePath = "/sys/class/gpio/gpio" + std::to_string(pin) + "/value";
    std::ifstream valueFile(valuePath);
    if (!valueFile) return false;
    int gpioValue;
    valueFile >> gpioValue;
    *value = gpioValue;
    return valueFile.good();
}