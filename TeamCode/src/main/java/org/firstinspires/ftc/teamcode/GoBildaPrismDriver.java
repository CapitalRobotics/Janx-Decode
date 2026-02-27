package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.hardware.I2cAddr;
import com.qualcomm.robotcore.hardware.I2cDeviceSynch;
import com.qualcomm.robotcore.hardware.I2cDeviceSynchDevice;
import com.qualcomm.robotcore.hardware.I2cWaitControl;
import com.qualcomm.robotcore.hardware.configuration.annotations.DeviceProperties;
import com.qualcomm.robotcore.hardware.configuration.annotations.I2cDeviceType;

/**
 * GoBilda Prism RGB LED Driver for FTC
 * Based on official goBILDA user guide (3118-2855-0001)
 *
 * ROBOT CONFIGURATION (Driver Hub):
 *   - Device type: "goBILDA Prism LED Driver"
 *   - Name: prism_led
 *   - Address: 0x38
 *
 * USAGE:
 *   GoBildaPrismDriver prism = hardwareMap.get(GoBildaPrismDriver.class, "prism_led");
 *   prism.setColor(255, 0, 0);      // Red solid color
 *   prism.setColor(0, 255, 0);      // Green solid color
 *   prism.setColor(0, 0, 255);      // Blue solid color
 *   prism.setColor(255, 255, 255);  // White solid color
 *   prism.turnOff();                // Off
 */

@I2cDeviceType
@DeviceProperties(
        name        = "goBILDA Prism LED Driver",
        description = "goBILDA Prism RGB LED Driver (I2C 0x38)",
        xmlTag      = "GoBildaPrismDriver"
)
public class GoBildaPrismDriver extends I2cDeviceSynchDevice<I2cDeviceSynch> {

    // -------------------------------------------------------------------------
    // Official register map from goBILDA user guide
    // -------------------------------------------------------------------------
    public  static final I2cAddr DEFAULT_ADDRESS   = I2cAddr.create7bit(0x38);

    // Control register — write 32-bit value to clear all animations
    private static final int REG_CONTROL           = 0x06;
    private static final int CONTROL_CLEAR_ANIM    = (1 << 25); // bit 25 = clear animations

    // Layer 0 register (0x08) — this is where we write our animation
    private static final int REG_LAYER_0           = 0x08;

    // Sub-registers for any layer
    private static final int SUB_SELECTED_ANIM     = 0x00; // animation type
    private static final int SUB_BRIGHTNESS        = 0x01; // 0–100
    private static final int SUB_START_INDEX       = 0x02; // start LED index
    private static final int SUB_STOP_INDEX        = 0x03; // stop LED index (255 = all)
    private static final int SUB_PRIMARY_COLOR     = 0x04; // 3 bytes: R, G, B

    // Animation type IDs (from official docs)
    private static final int ANIM_SOLID_COLOR      = 0x01;
    private static final int ANIM_NONE             = 0x00;

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
        turnOff();
        return true;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Set a solid RGB color across all LEDs.
     * @param r Red   0–255
     * @param g Green 0–255
     * @param b Blue  0–255
     */
    public void setColor(int r, int g, int b) {
        r = clamp(r, 0, 255);
        g = clamp(g, 0, 255);
        b = clamp(b, 0, 255);

        // Step 1: Select "Solid Color" animation on Layer 0
        writeLayerSubReg(REG_LAYER_0, SUB_SELECTED_ANIM, new byte[]{ (byte) ANIM_SOLID_COLOR });

        // Step 2: Set brightness to 100%
        writeLayerSubReg(REG_LAYER_0, SUB_BRIGHTNESS, new byte[]{ (byte) 100 });

        // Step 3: Start at LED 0
        writeLayerSubReg(REG_LAYER_0, SUB_START_INDEX, new byte[]{ (byte) 0 });

        // Step 4: Stop at LED 255 (all LEDs)
        writeLayerSubReg(REG_LAYER_0, SUB_STOP_INDEX, new byte[]{ (byte) 255 });

        // Step 5: Write RGB color (3 bytes)
        writeLayerSubReg(REG_LAYER_0, SUB_PRIMARY_COLOR, new byte[]{
                (byte) r, (byte) g, (byte) b
        });
    }

    /**
     * Turn off all LEDs by clearing all animations.
     */
    public void turnOff() {
        // Write CLEAR_ANIMATIONS bit to the control register
        // Control register is 32-bit
        byte[] clearCmd = intToBytes(CONTROL_CLEAR_ANIM);
        deviceClient.write(REG_CONTROL, clearCmd, I2cWaitControl.NONE);
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
    public Manufacturer getManufacturer() { return Manufacturer.Other; }

    @Override
    public String getDeviceName() { return "goBILDA Prism LED Driver"; }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Write to a layer sub-register.
     * Protocol: [device_addr, layer_reg, sub_reg, data...]
     * We write: [layer_reg, sub_reg, data...] (device addr handled by I2C bus)
     */
    private void writeLayerSubReg(int layerReg, int subReg, byte[] data) {
        // Build packet: sub-register byte followed by data bytes
        byte[] packet = new byte[1 + data.length];
        packet[0] = (byte) subReg;
        System.arraycopy(data, 0, packet, 1, data.length);
        deviceClient.write(layerReg, packet, I2cWaitControl.NONE);
    }

    /** Convert 32-bit int to 4 bytes, little-endian */
    private byte[] intToBytes(int value) {
        return new byte[]{
                (byte)  (value        & 0xFF),
                (byte) ((value >>  8) & 0xFF),
                (byte) ((value >> 16) & 0xFF),
                (byte) ((value >> 24) & 0xFF)
        };
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}