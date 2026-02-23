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
 *   - Plug the Prism I²C cable into any I²C port on the REV Control Hub
 *   - Default I²C address: 0x40
 *
 * ROBOT CONFIGURATION (Driver Hub):
 *   - Add an I²C device on the port the Prism is plugged into
 *   - Select device type: "goBILDA Prism LED Driver"
 *   - Name it: prism_led
 *   - Address: 0x40
 *
 * USAGE IN OPMODE:
 *   GoBildaPrismDriver prism = hardwareMap.get(GoBildaPrismDriver.class, "prism_led");
 *   prism.setColor(255, 0, 0);     // Red
 *   prism.setColor(0, 255, 0);     // Green
 *   prism.setColor(0, 0, 255);     // Blue
 *   prism.setColor(255, 255, 255); // White
 *   prism.setColor(0, 0, 0);       // Off
 */

@I2cDeviceType
@DeviceProperties(
        name = "goBILDA Prism LED Driver",
        description = "goBILDA Prism RGB LED Driver (PCA9685 I2C)",
        xmlTag = "GoBildaPrismDriver"
)
public class GoBildaPrismDriver extends I2cDeviceSynchDevice<I2cDeviceSynch> {

    // PCA9685 registers
    private static final int REG_MODE1     = 0x00;
    private static final int REG_MODE2     = 0x01;
    private static final int REG_LED0_ON_L = 0x06; // Red
    private static final int REG_LED1_ON_L = 0x0A; // Green
    private static final int REG_LED2_ON_L = 0x0E; // Blue
    private static final int MODE1_AI      = 0x20; // Auto-increment
    private static final int MAX_PWM       = 4095;

    public static final I2cAddr DEFAULT_ADDRESS = I2cAddr.create7bit(0x40);

    // FIX 1: Only ONE constructor (the duplicate was causing a compile error)
    public GoBildaPrismDriver(I2cDeviceSynch deviceClient, boolean deviceClientIsOwned) {
        super(deviceClient, deviceClientIsOwned);
        this.deviceClient.setI2cAddress(DEFAULT_ADDRESS);
        super.registerArmingStateCallback(false);
        this.deviceClient.engage();
    }

    @Override
    protected synchronized boolean doInitialize() {
        write8(REG_MODE1, MODE1_AI);
        try { Thread.sleep(10); } catch (InterruptedException ignored) {}
        write8(REG_MODE2, 0x04);
        setColor(0, 0, 0);
        return true;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public void setColor(int r, int g, int b) {
        writeChannel(REG_LED0_ON_L, scale(clamp(r, 0, 255)));
        writeChannel(REG_LED1_ON_L, scale(clamp(g, 0, 255)));
        writeChannel(REG_LED2_ON_L, scale(clamp(b, 0, 255)));
    }

    public void turnOff() {
        setColor(0, 0, 0);
    }

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
    // Private helpers
    // -------------------------------------------------------------------------

    private void write8(int register, int value) {
        deviceClient.write8(register, value, I2cWaitControl.NONE);
    }

    private void writeChannel(int channelBase, int pwmValue) {
        byte[] data = new byte[4];
        data[0] = 0x00;
        data[1] = 0x00;
        data[2] = (byte)  (pwmValue & 0xFF);
        data[3] = (byte) ((pwmValue >> 8) & 0x0F);
        deviceClient.write(channelBase, data, I2cWaitControl.NONE);
    }

    private int scale(int value8) {
        return (value8 * MAX_PWM) / 255;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

}