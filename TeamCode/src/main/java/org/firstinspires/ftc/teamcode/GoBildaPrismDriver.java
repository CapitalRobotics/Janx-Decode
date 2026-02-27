package org.firstinspires.ftc.teamcode;


import com.qualcomm.robotcore.hardware.I2cAddr;
import com.qualcomm.robotcore.hardware.I2cDeviceSynch;
import com.qualcomm.robotcore.hardware.I2cDeviceSynchDevice;
import com.qualcomm.robotcore.hardware.I2cWaitControl;
import com.qualcomm.robotcore.hardware.configuration.annotations.DeviceProperties;
import com.qualcomm.robotcore.hardware.configuration.annotations.I2cDeviceType;

/**
 * GoBilda Prism RGB LED Driver for FTC
 *
 * WIRING:
 *   - Plug Prism I²C cable into any I²C port on the REV Control Hub
 *
 * ROBOT CONFIGURATION (Driver Hub):
 *   - Add I²C device on the port the Prism is plugged into
 *   - Device type: "goBILDA Prism LED Driver"
 *   - Name: prism_led
 *   - Address: 0x38  <-- THIS IS THE CORRECT ADDRESS (NOT 0x40!)
 *
 * USAGE:
 *   GoBildaPrismDriver prism = hardwareMap.get(GoBildaPrismDriver.class, "prism_led");
 *   prism.setColor(255, 0, 0);     // Red
 *   prism.setColor(0, 255, 0);     // Green
 *   prism.setColor(0, 0, 255);     // Blue
 *   prism.setColor(255, 255, 255); // White
 *   prism.setColor(0, 0, 0);       // Off
 */

@I2cDeviceType
@DeviceProperties(
        name        = "goBILDA Prism LED Driver",
        description = "goBILDA Prism RGB LED Driver (I2C, address 0x38)",
        xmlTag      = "GoBildaPrismDriver"
)
public class GoBildaPrismDriver extends I2cDeviceSynchDevice<I2cDeviceSynch> {

    // -------------------------------------------------------------------------
    // The goBILDA Prism has its OWN firmware — NOT raw PCA9685.
    // Correct I2C address from the official goBILDA product page: 0x38
    // -------------------------------------------------------------------------
    public static final I2cAddr DEFAULT_ADDRESS = I2cAddr.create7bit(0x38);

    // Prism I2C register map (from goBILDA official documentation)
    private static final int REG_RED   = 0x00; // Red channel   (0–255)
    private static final int REG_GREEN = 0x01; // Green channel (0–255)
    private static final int REG_BLUE  = 0x02; // Blue channel  (0–255)

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------
    public GoBildaPrismDriver(I2cDeviceSynch deviceClient, boolean deviceClientIsOwned) {
        super(deviceClient, deviceClientIsOwned);
        this.deviceClient.setI2cAddress(DEFAULT_ADDRESS);
        super.registerArmingStateCallback(false);
        this.deviceClient.engage();
    }

    // -------------------------------------------------------------------------
    // Initialization
    // -------------------------------------------------------------------------
    @Override
    protected synchronized boolean doInitialize() {
        setColor(0, 0, 0); // Start with LEDs off
        return true;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Set the RGB color of the Prism LED strip.
     * @param r Red   0–255
     * @param g Green 0–255
     * @param b Blue  0–255
     */
    public void setColor(int r, int g, int b) {
        r = clamp(r, 0, 255);
        g = clamp(g, 0, 255);
        b = clamp(b, 0, 255);

        // Write all 3 channels in one I2C transaction using auto-increment
        byte[] data = { (byte) r, (byte) g, (byte) b };
        deviceClient.write(REG_RED, data, I2cWaitControl.NONE);
    }

    /** Turn off all LEDs. */
    public void turnOff() {
        setColor(0, 0, 0);
    }

    /**
     * Set color using an Android Color int.
     * Example: prism.setColorInt(Color.RED);
     */
    public void setColorInt(int color) {
        setColor((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF);
    }

    // -------------------------------------------------------------------------
    // Required overrides
    // -------------------------------------------------------------------------
    @Override
    public Manufacturer getManufacturer() {
        return Manufacturer.Other;
    }

    @Override
    public String getDeviceName() {
        return "goBILDA Prism LED Driver";
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------
    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}