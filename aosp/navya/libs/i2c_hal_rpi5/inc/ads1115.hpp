#include <iostream>
#include <unistd.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <linux/i2c-dev.h>
#include <stdint.h>
#include <stdexcept>

#define ADS1115_ADDRESS 0x48  // Default I2C address for ADS1115
#define ADS1115_CONVERSION_REGISTER 0x00
#define ADS1115_CONFIG_REGISTER 0x01


int open_i2c(const char* device, int address) ;
void configure_ads1115(int file, int channel);
int16_t read_ads1115(int file);
int getI2c();



/*#pragma once
class ItoCHal {public:   
bool getADCValue(int channel, int *value);
}

;*/
