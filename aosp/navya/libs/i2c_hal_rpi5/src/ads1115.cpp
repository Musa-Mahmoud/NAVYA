#include <fcntl.h>
#include <linux/i2c-dev.h>
#include <stdint.h>
#include <sys/ioctl.h>
#include <unistd.h>
#include <iostream>
#include <stdexcept>
#include "ads1115.hpp"

#define ADS1115_ADDRESS 0x48
#define ADS1115_CONVERSION_REGISTER 0x00
#define ADS1115_CONFIG_REGISTER 0x01

int open_i2c(const char* device, int address) {
    int file = open(device, O_RDWR);
    if (file < 0) {
        std::cerr << "Failed to open the bus" << std::endl;
        return -1;
    }

    if (ioctl(file, I2C_SLAVE, address) < 0) {
        std::cerr << "Failed to acquire bus access and/or talk to slave" << std::endl;
        close(file);
        return -1;
    }
    return file;
}

void configure_ads1115(int file, int channel) {
    uint16_t config = 0xC383;  

    switch (channel) {
        case 0:
            config |= 0x4000;
            break; 
        case 1:
            config |= 0x5000;
            break;  
        case 2:
            config |= 0x6000;
            break;  
        case 3:
            config |= 0x7000;
            break;  
        default:
            break;
    }

    uint8_t data[3];
    data[0] = ADS1115_CONFIG_REGISTER;
    data[1] = (config >> 8) & 0xFF;  
    data[2] = config & 0xFF;         
    if (write(file, data, 3) != 3) {
        std::cerr << "Failed to write to the configuration register" << std::endl;
        close(file);
        exit(1);
    }
}

int16_t read_ads1115(int file) {
    uint8_t buf[2];
    buf[0] = ADS1115_CONVERSION_REGISTER;
    if (write(file, buf, 1) != 1) {
        std::cerr << "Failed to set conversion register" << std::endl;
        close(file);
        exit(1);
    }

    if (read(file, buf, 2) != 2) {
        std::cerr << "Failed to read conversion result" << std::endl;
        close(file);
        exit(1);
    }

    int16_t result = (buf[0] << 8) | buf[1];
    return result;
}

int getI2c() {
    const char* device =
            "/dev/i2c-1";  
    int file = open_i2c(device, ADS1115_ADDRESS);
    if (file < 0) {
    	return 5;
    }

    int channel = 1;  

    configure_ads1115(file, channel);

    usleep(10000);

    int16_t value = read_ads1115(file);


    sleep(1);

    close(file);
    return (int)value;
}
