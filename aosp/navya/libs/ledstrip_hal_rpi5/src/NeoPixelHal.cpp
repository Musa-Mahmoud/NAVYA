/*
#include "NeoPixelHal.hpp"
#include <cstdio>
#include <fcntl.h>
#include <unistd.h>
#include <linux/spi/spidev.h>
#include <sys/ioctl.h>
#include <cstring>
#include <vector>
#include <string>
#include <algorithm>
#include <cmath>
#include <ctime>


NeoPixelHal::NeoPixelHal(const std::string& spiDevice, uint32_t numLeds)
    : spiDevice(spiDevice), numLeds(numLeds), ledData(numLeds * 24, 0), spiFd(-1) {
    if (!openSpi()) {
        fprintf(stderr, "Error: Failed to open SPI device: %s\n", spiDevice.c_str());
    }
}

NeoPixelHal::~NeoPixelHal() {
    if (spiFd >= 0) {
        close(spiFd);
    }
}

bool NeoPixelHal::openSpi() {
    spiFd = open(spiDevice.c_str(), O_WRONLY);
    if (spiFd < 0) {
        perror("Error opening SPI device");
        return false;
    }

    uint8_t mode = SPI_MODE_0;
    uint32_t speed = 2400000; // 2.4 MHz for WS2812  2400000
    uint8_t bits = 8;

    if (ioctl(spiFd, SPI_IOC_WR_MODE, &mode) < 0 ||
        ioctl(spiFd, SPI_IOC_WR_MAX_SPEED_HZ, &speed) < 0 ||
        ioctl(spiFd, SPI_IOC_WR_BITS_PER_WORD, &bits) < 0) {
        perror("Error configuring SPI device");
        close(spiFd);
        spiFd = -1;
        return false;
    }

    return true;
}


bool NeoPixelHal::setColor(uint32_t index, uint8_t red, uint8_t green, uint8_t blue) {
    if (index >= numLeds) {
        fprintf(stderr, "Error: LED index out of range\n");
        return false;
    }

    // Scale colors by brightness percentage
    float scale = brightnessPercentage / 100.0f;
    red = static_cast<uint8_t>(std::round(red * scale));
    green = static_cast<uint8_t>(std::round(green * scale));
    blue = static_cast<uint8_t>(std::round(blue * scale));

    auto encoded = encodeColor(red, green, blue);
    std::copy(encoded.begin(), encoded.end(), ledData.begin() + index * 24);

    return true;
}




void NeoPixelHal::clear() {
    std::fill(ledData.begin(), ledData.end(), 0);
    usleep(50);
    show();
}

bool NeoPixelHal::show() {
    if (write(spiFd, ledData.data(), ledData.size()) < 0) {
        perror("Error writing to SPI device");
        return false;
    }

    // WS2812 reset signal: Pause for 50 µs (at least)
    usleep(50); // Microseconds
    return true;
}

std::vector<uint8_t> NeoPixelHal::encodeColor(uint8_t red, uint8_t green, uint8_t blue) {
    std::vector<uint8_t> encoded(24);
    uint32_t color = (green << 16) | (red << 8) | blue; // GRB order for WS2812

    for (int i = 0; i < 24; ++i) {
        if (color & (1 << (23 - i))) {
            // High bit (1): ~800ns high, ~450ns low
            encoded[i] = 0b11100000; 
        } else {
            // Low bit (0): ~400ns high, ~850ns low
            encoded[i] = 0b10000000; 
        }
    }

    return encoded;
}

// the setBrightness method
void NeoPixelHal::setBrightness(int percentage) {
    if (percentage < 0 || percentage > 100) {
        fprintf(stderr, "Error: Brightness percentage out of range. Must be between 0 and 100\n");
        return;
    }
    this->brightnessPercentage = percentage;

    // Re-apply brightness to existing LED data
    for (uint32_t i = 0; i < numLeds; ++i) {
        uint8_t red = ledData[i * 24 + 0];
        uint8_t green = ledData[i * 24 + 1];
        uint8_t blue = ledData[i * 24 + 2];
        setColor(i, red, green, blue);
    }
}

// Method to activate random mode
void NeoPixelHal::setRandomMode() {
    std::lock_guard<std::mutex> lock(threadMutex); // Protect thread management

    if (randomModeRunning) {
        // Stop the random mode if it's already running
        randomModeRunning = false;
        if (randomModeThread.joinable()) {
            randomModeThread.join();
        }
        return;
    }

    // Start random mode
    randomModeRunning = true;
    randomModeThread = std::thread([this]() {
        std::srand(std::time(nullptr)); // Seed the random generator

        while (randomModeRunning) {
            for (uint32_t i = 0; i < numLeds; ++i) {
                uint8_t red = std::rand() % 256;
                uint8_t green = std::rand() % 256;
                uint8_t blue = std::rand() % 256;
                setColor(i, red, green, blue);
            }
            show();

            // Delay between updates
            usleep(100000); // 100 ms
        }
    });
}


void NeoPixelHal::setGlobalFadeMode() {
    static std::atomic<bool> running{false}; // Static to persist across calls
    std::lock_guard<std::mutex> lock(threadMutex);
    
    if (running) {
        running = false; // Stop the current thread if already running
        return;
    }

     // Stop random mode if running
    if (randomModeRunning) {
        randomModeRunning = false;
        if (randomModeThread.joinable()) {
            randomModeThread.join();
        }
    }

    running = true;       // Mark as running
    fadeModeRunning = true; // Enable fade mode

    // Start the fade effect in a new thread
    std::thread([this](std::atomic<bool>& runFlag) {
        std::srand(std::time(nullptr)); // Seed the random generator

        bool increasing = false; // Tracks whether brightness is increasing or decreasing
        int brightness = 70;     // Initial brightness

        while (runFlag && fadeModeRunning) { // Check both flags
            // Generate a random target color within the range of 20 to 255
            uint8_t targetRed = 20 + (std::rand() % 236);
            uint8_t targetGreen = 20 + (std::rand() % 236);
            uint8_t targetBlue = 20 + (std::rand() % 236);

            // Brightness loop: Decrease or increase based on the current direction
            int start = increasing ? 10 : 70;
            int end = increasing ? 70 : 10;
            int step = increasing ? 1 : -1;

            for (brightness = start; brightness != end + step && runFlag && fadeModeRunning; brightness += step) {
                setBrightness(brightness);
                for (uint32_t i = 0; i < numLeds; ++i) {
                    setColor(i, targetRed, targetGreen, targetBlue); // Apply the same color to all LEDs
                }
                show();
                usleep(50000); // 50ms delay for smoother brightness transition
            }

            // Toggle the direction for the next cycle
            increasing = !increasing;

            // Interpolate to a new random color
            uint8_t startRed = targetRed;
            uint8_t startGreen = targetGreen;
            uint8_t startBlue = targetBlue;

            targetRed = 20 + (std::rand() % 236); // Generate new random color
            targetGreen = 20 + (std::rand() % 236);
            targetBlue = 20 + (std::rand() % 236);

            for (float t = 0; t <= 1 && runFlag && fadeModeRunning; t += 0.01) {
                uint8_t currentRed = startRed + t * (targetRed - startRed);
                uint8_t currentGreen = startGreen + t * (targetGreen - startGreen);
                uint8_t currentBlue = startBlue + t * (targetBlue - startBlue);

                // Maintain the same brightness level during color interpolation
                for (uint32_t i = 0; i < numLeds; ++i) {
                    setColor(i, currentRed, currentGreen, currentBlue); // Gradually change color
                }
                show();
                usleep(50000); // 50ms delay for smoother color transition
            }
        }
    }, std::ref(running)).detach(); // Pass running by reference
}


void NeoPixelHal::stopThreads() {
    std::lock_guard<std::mutex> lock(threadMutex); // Protect thread management

    // Stop the fade mode thread if running
    if (fadeModeRunning) {
        fadeModeRunning = false;
    }
     // Stop random mode if running
    if (randomModeRunning) {
        randomModeRunning = false;
        if (randomModeThread.joinable()) {
            randomModeThread.join();
        }
    }
}
*/

#include "NeoPixelHal.hpp"
#include <cstdio>
#include <fcntl.h>
#include <unistd.h>
#include <linux/spi/spidev.h>
#include <sys/ioctl.h>
#include <cstring>
#include <vector>
#include <string>
#include <algorithm>
#include <cmath>
#include <ctime>

NeoPixelHal::NeoPixelHal(const std::string& spiDevice, uint32_t numLeds)
    : spiDevice(spiDevice), numLeds(numLeds), ledData(numLeds * 24, 0), spiFd(-1) {
    if (!openSpi()) {
        fprintf(stderr, "Error: Failed to open SPI device: %s\n", spiDevice.c_str());
    }
}

NeoPixelHal::~NeoPixelHal() {
    if (spiFd >= 0) {
        close(spiFd);
    }
}

bool NeoPixelHal::openSpi() {
    spiFd = open(spiDevice.c_str(), O_WRONLY);
    if (spiFd < 0) {
        perror("Error opening SPI device");
        return false;
    }

    uint8_t mode = SPI_MODE_0;
    uint32_t speed = 2400000; // 2.4 MHz for WS2812
    uint8_t bits = 8;

    if (ioctl(spiFd, SPI_IOC_WR_MODE, &mode) < 0 ||
        ioctl(spiFd, SPI_IOC_WR_MAX_SPEED_HZ, &speed) < 0 ||
        ioctl(spiFd, SPI_IOC_WR_BITS_PER_WORD, &bits) < 0) {
        perror("Error configuring SPI device");
        close(spiFd);
        spiFd = -1;
        return false;
    }

    return true;
}

bool NeoPixelHal::setColor(uint32_t index, uint8_t red, uint8_t green, uint8_t blue) {
    if (index >= numLeds) {
        fprintf(stderr, "Error: LED index out of range\n");
        return false;
    }

    // Scale colors by brightness percentage
    float scale = brightnessPercentage / 100.0f;
    red = static_cast<uint8_t>(std::round(red * scale));
    green = static_cast<uint8_t>(std::round(green * scale));
    blue = static_cast<uint8_t>(std::round(blue * scale));

    auto encoded = encodeColor(red, green, blue);
    std::copy(encoded.begin(), encoded.end(), ledData.begin() + index * 24);

    return true;
}

void NeoPixelHal::clear() {
    std::fill(ledData.begin(), ledData.end(), 0);
    usleep(50);
    show();
}

bool NeoPixelHal::show() {
    if (write(spiFd, ledData.data(), ledData.size()) < 0) {
        perror("Error writing to SPI device");
        return false;
    }

    // WS2812 reset signal: Pause for 50 µs (at least)
    usleep(50); // Microseconds
    return true;
}

std::vector<uint8_t> NeoPixelHal::encodeColor(uint8_t red, uint8_t green, uint8_t blue) {
    std::vector<uint8_t> encoded(24);
    uint32_t color = (green << 16) | (red << 8) | blue; // GRB order for WS2812

    for (int i = 0; i < 24; ++i) {
        if (color & (1 << (23 - i))) {
            // High bit (1): ~800ns high, ~450ns low
            encoded[i] = 0b11100000;
        } else {
            // Low bit (0): ~400ns high, ~850ns low
            encoded[i] = 0b10000000;
        }
    }

    return encoded;
}

void NeoPixelHal::setBrightness(int percentage) {
    if (percentage < 0 || percentage > 100) {
        fprintf(stderr, "Error: Brightness percentage out of range. Must be between 0 and 100\n");
        return;
    }
    this->brightnessPercentage = percentage;

    // Re-apply brightness to existing LED data
    for (uint32_t i = 0; i < numLeds; ++i) {
        uint8_t red = ledData[i * 24 + 0];
        uint8_t green = ledData[i * 24 + 1];
        uint8_t blue = ledData[i * 24 + 2];
        setColor(i, red, green, blue);
    }
}

void NeoPixelHal::setRandomMode() {
    std::lock_guard<std::mutex> lock(threadMutex); // Protect thread management

    if (randomModeRunning) {
        // Stop the random mode if it's already running
        randomModeRunning = false;
        if (randomModeThread.joinable()) {
            randomModeThread.join();
        }
        return;
    }

    // Start random mode
    randomModeRunning = true;
    randomModeThread = std::thread([this]() {
        std::srand(std::time(nullptr)); // Seed the random generator

        while (randomModeRunning) {
            for (uint32_t i = 0; i < numLeds; ++i) {
                uint8_t red = std::rand() % 256;
                uint8_t green = std::rand() % 256;
                uint8_t blue = std::rand() % 256;
                setColor(i, red, green, blue);
            }
            show();

            // Delay between updates
            usleep(100000); // 100 ms
        }
    });
}

bool NeoPixelHal::getColor(uint32_t index, uint8_t* red, uint8_t* green, uint8_t* blue) const {
    if (index >= numLeds) {
        fprintf(stderr, "Error: LED index out of range\n");
        return false;
    }
    // Decode from ledData (assuming GRB order from encodeColor)
    *green = ledData[index * 24 + 0]; // First byte is green (adjust based on encoding)
    *red = ledData[index * 24 + 1];   // Second byte is red
    *blue = ledData[index * 24 + 2];  // Third byte is blue
    return true;
}

void NeoPixelHal::setGlobalFadeMode() {
    static std::atomic<bool> running{false}; // Static to persist across calls
    std::lock_guard<std::mutex> lock(threadMutex);

    if (running) {
        running = false; // Stop the current thread if already running
        return;
    }

    // Stop random mode if running
    if (randomModeRunning) {
        randomModeRunning = false;
        if (randomModeThread.joinable()) {
            randomModeThread.join();
        }
    }

    running = true;       // Mark as running
    fadeModeRunning = true; // Enable fade mode

    // Start the fade effect in a new thread
    std::thread([this](std::atomic<bool>& runFlag) {
        std::srand(std::time(nullptr)); // Seed the random generator

        bool increasing = false; // Tracks whether brightness is increasing or decreasing
        int brightness = 70;     // Initial brightness

        while (runFlag && fadeModeRunning) { // Check both flags
            // Generate a random target color within the range of 20 to 255
            uint8_t targetRed = 20 + (std::rand() % 236);
            uint8_t targetGreen = 20 + (std::rand() % 236);
            uint8_t targetBlue = 20 + (std::rand() % 236);

            // Brightness loop: Decrease or increase based on the current direction
            int start = increasing ? 10 : 70;
            int end = increasing ? 70 : 10;
            int step = increasing ? 1 : -1;

            for (brightness = start; brightness != end + step && runFlag && fadeModeRunning; brightness += step) {
                setBrightness(brightness);
                for (uint32_t i = 0; i < numLeds; ++i) {
                    setColor(i, targetRed, targetGreen, targetBlue); // Apply the same color to all LEDs
                }
                show();
                usleep(50000); // 50ms delay for smoother brightness transition
            }

            // Toggle the direction for the next cycle
            increasing = !increasing;

            // Interpolate to a new random color
            uint8_t startRed = targetRed;
            uint8_t startGreen = targetGreen;
            uint8_t startBlue = targetBlue;

            targetRed = 20 + (std::rand() % 236); // Generate new random color
            targetGreen = 20 + (std::rand() % 236);
            targetBlue = 20 + (std::rand() % 236);

            for (float t = 0; t <= 1 && runFlag && fadeModeRunning; t += 0.01) {
                uint8_t currentRed = startRed + t * (targetRed - startRed);
                uint8_t currentGreen = startGreen + t * (targetGreen - startGreen);
                uint8_t currentBlue = startBlue + t * (targetBlue - startBlue);

                // Maintain the same brightness level during color interpolation
                for (uint32_t i = 0; i < numLeds; ++i) {
                    setColor(i, currentRed, currentGreen, currentBlue); // Gradually change color
                }
                show();
                usleep(50000); // 50ms delay for smoother color transition
            }
        }
    }, std::ref(running)).detach(); // Pass running by reference
}

void NeoPixelHal::stopThreads() {
    std::lock_guard<std::mutex> lock(threadMutex); // Protect thread management

    // Stop the fade mode thread if running
    if (fadeModeRunning) {
        fadeModeRunning = false;
    }

    // Stop random mode if running
    if (randomModeRunning) {
        randomModeRunning = false;
        if (randomModeThread.joinable()) {
            randomModeThread.join();
        }
    }
}

