#include "gpiohalrpi5.hpp"
#include <string>
#include <android/log.h>

GpioHal::GpioHal() : chip_(nullptr)
{
    chip_ = gpiod_chip_open("/dev/gpiochip0");
    if (!chip_) 
    {
        __android_log_print(ANDROID_LOG_INFO, "RPi5_GPIO", "Failed to open /dev/gpiochip0");
    }
}

GpioHal::~GpioHal()
{
    for (auto& pair : requests_) 
    {
        gpiod_line_request_release(pair.second);
    }
    if (chip_) 
    {
        gpiod_chip_close(chip_);
        __android_log_print(ANDROID_LOG_INFO, "RPi5_GPIO", "/dev/gpiochip0 Closed");
    }
}

bool GpioHal::requestLine(int pin, gpiod_line_direction direction, int value)
{
    __android_log_print(ANDROID_LOG_INFO, "RPi5_GPIO", "GPIO Pin %d requested", pin);
    
    if (!chip_) return false;

    gpiod_line_settings* settings = gpiod_line_settings_new();
    if (!settings) return false;

    gpiod_line_settings_set_direction(settings, direction);
    if (direction == GPIOD_LINE_DIRECTION_OUTPUT) {
        gpiod_line_settings_set_output_value(settings,
            value ? GPIOD_LINE_VALUE_ACTIVE : GPIOD_LINE_VALUE_INACTIVE);
    }

    gpiod_line_config* line_config = gpiod_line_config_new();
    if (!line_config) {
        gpiod_line_settings_free(settings);
        return false;
    }

    unsigned int offset = static_cast<unsigned int>(pin);
    int ret = gpiod_line_config_add_line_settings(line_config, &offset, 1, settings);
    if (ret) {
        gpiod_line_settings_free(settings);
        gpiod_line_config_free(line_config);
        return false;
    }

    gpiod_request_config* config = gpiod_request_config_new();
    if (!config) {
        gpiod_line_settings_free(settings);
        gpiod_line_config_free(line_config);
        return false;
    }
    gpiod_request_config_set_consumer(config, "gpiohal");

    gpiod_line_request* request = gpiod_chip_request_lines(chip_, config, line_config);

    gpiod_line_settings_free(settings);
    gpiod_line_config_free(line_config);
    gpiod_request_config_free(config);

    if (!request) return false;

    if (requests_.count(pin)) {
        gpiod_line_request_release(requests_[pin]);
    }
    requests_[pin] = request;
    return true;
}

bool GpioHal::setGpioDirection(int pin, const char* direction)
{
    if (std::string(direction) == "out") 
    {
        return requestLine(pin, GPIOD_LINE_DIRECTION_OUTPUT, 0);
    }
    else if (std::string(direction) == "in") 
    {
        return requestLine(pin, GPIOD_LINE_DIRECTION_INPUT, 0);
    }
    return false;
}

bool GpioHal::setGpioDirOut(int pin)
{
    return requestLine(pin, GPIOD_LINE_DIRECTION_OUTPUT, 0);
}

bool GpioHal::setGpioDirIn(int pin)
{
    return requestLine(pin, GPIOD_LINE_DIRECTION_INPUT, 0);
}

bool GpioHal::setGpioValue(int pin, int value)
{
    if (!requests_.count(pin)) return false;
    __android_log_print(ANDROID_LOG_INFO, "RPi5_GPIO", "Set GPIO Pin %d to %d", pin, value);
    enum gpiod_line_value val = value ? GPIOD_LINE_VALUE_ACTIVE : GPIOD_LINE_VALUE_INACTIVE;
    return gpiod_line_request_set_value(requests_[pin], static_cast<unsigned int>(pin), val) == 0;
}

bool GpioHal::getGpioValue(int pin, int* value)
{
    if (!requests_.count(pin)) 
        return false;
    
    int val = gpiod_line_request_get_value(requests_[pin], static_cast<unsigned int>(pin));
    
    if (val < 0) 
        return false;
    
    *value = val;
    return true;
}
