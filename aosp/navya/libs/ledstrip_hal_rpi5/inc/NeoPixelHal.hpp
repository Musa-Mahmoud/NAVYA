#ifndef NEOPIXEL_HAL_HPP
#define NEOPIXEL_HAL_HPP

#include <vector>
#include <cstdint>
#include <string>
#include <thread>
#include <atomic>
#include <ctime>
#include <unistd.h>
#include <vector>
#include <mutex>

/**
 * @class NeoPixelHal
 * @brief A HAL to control NeoPixel (WS2812) LEDs using SPI.
 */
class NeoPixelHal {
public:

    //a member variable to store brightness
    int brightnessPercentage = 100;

    /**
     * @brief Constructor for NeoPixelHal.
     * @param spiDevice Path to the SPI device (e.g., "/dev/spidev0.0").
     * @param numLeds Number of LEDs in the NeoPixel stick.
     */
    NeoPixelHal(const std::string& spiDevice, uint32_t numLeds);

    /**
     * @brief Destructor for NeoPixelHal.
     */
    ~NeoPixelHal();

    /**
     * @brief Sets the color of an individual LED.
     * @param index Index of the LED (0-based).
     * @param red Red component (0-255).
     * @param green Green component (0-255).
     * @param blue Blue component (0-255).
     * @return True if the color was successfully set, false otherwise.
     */
    bool setColor(uint32_t index, uint8_t red, uint8_t green, uint8_t blue);

    /**
     * @brief Clears all LEDs (turns them off).
     */
    void clear();

    /**
     * @brief Sends the color data to the NeoPixel stick.
     * @return True if the data was successfully sent, false otherwise.
     */
    bool show();

    /**
     * @brief Sets the brightness of all LEDs as a percentage.
     * @param percentage Brightness percentage (0-100).
     */
    void setBrightness(int percentage);

    /**
     * @brief Activates random mode, assigning random colors to all LEDs.
     * @param continuous If true, continuously update with random colors.
     */
    void setRandomMode();

    void setGlobalFadeMode();

    void stopThreads();
    
    bool getColor(uint32_t index, uint8_t* red, uint8_t* green, uint8_t* blue) const;

private:
    std::atomic<bool> randomModeRunning{false}; // Flag to control the random mode thread
    std::atomic<bool> fadeModeRunning{false}; // Flag to control fade mode thread
    std::string spiDevice; ///< Path to the SPI device.
    uint32_t numLeds;      ///< Number of LEDs in the NeoPixel stick.
    std::vector<uint8_t> ledData; ///< Encoded LED data buffer.
    int spiFd;             ///< File descriptor for the SPI device.
    std::thread randomModeThread;               // Thread for random mode
    std::mutex threadMutex;                     // Mutex to protect thread lifecycle

    /**
     * @brief Opens the SPI device.
     * @return True if successful, false otherwise.
     */
    bool openSpi();

    /**
     * @brief Encodes a single LED's color data.
     * @param red Red component.
     * @param green Green component.
     * @param blue Blue component.
     * @return Encoded SPI data for the LED.
     */
    std::vector<uint8_t> encodeColor(uint8_t red, uint8_t green, uint8_t blue);


};

#endif // NEOPIXEL_HAL_HPP
