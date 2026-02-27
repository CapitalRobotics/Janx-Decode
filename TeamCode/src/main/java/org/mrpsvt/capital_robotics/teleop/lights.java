package org.mrpsvt.capital_robotics.teleop;


import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.Servo;
import com.seattlesolvers.solverslib.drivebase.MecanumDrive;
import com.seattlesolvers.solverslib.hardware.RevIMU;

import org.firstinspires.ftc.teamcode.GoBildaPrismDriver;
import org.mrpsvt.capital_robotics.control.ControlMap;
import org.mrpsvt.capital_robotics.robot_core.DriveBase;
import org.mrpsvt.capital_robotics.robot_core.DriveConstants;

import java.util.Objects;

@TeleOp(name="w_")
public class lights extends OpMode {

    // -------------------------------------------------------------------------
    // GoBilda Prism RGB LED Driver (PCA9685 over I²C)
    // -------------------------------------------------------------------------
    // The GoBilda Prism I²C driver is a PCA9685 PWM controller.
    // I²C address: 0x40 (default). Register layout per channel (4 bytes each):
    //   LEDn_ON_L  = base + 0
    //   LEDn_ON_H  = base + 1
    //   LEDn_OFF_L = base + 2
    //   LEDn_OFF_H = base + 3
    // Channel mapping (GoBilda Prism, 4 strips):
    //   CH0 = Red   (all strips tied together on this driver)
    //   CH1 = Green
    //   CH2 = Blue
    // We write a 12-bit PWM OFF count (0–4095) to set brightness per channel.
    // MODE1 register (0x00) must be written to wake the device from sleep.

    private static final byte PCA9685_MODE1         = 0x00;
    private static final byte PCA9685_LED0_ON_L     = 0x06; // CH0 base
    private static final byte PCA9685_LED1_ON_L     = 0x0A; // CH1 base
    private static final byte PCA9685_LED2_ON_L     = 0x0E; // CH2 base
    private static final int  PCA9685_MAX_PWM       = 4095;

    private GoBildaPrismDriver ledDriver;
    private boolean ledsEnabled = true;
    private boolean lastButtonB  = false; // debounce for LED toggle (gp2 B)
    private boolean lastButtonX  = false; // debounce for color cycle  (gp2 X)

    // Manual color mode cycling
    private enum LedMode { AUTO, RED, GREEN, BLUE, WHITE, OFF }
    private LedMode ledMode = LedMode.AUTO;
    private static final LedMode[] LED_CYCLE = {
            LedMode.AUTO, LedMode.RED, LedMode.GREEN,
            LedMode.BLUE, LedMode.WHITE, LedMode.OFF
    };
    private int ledModeIndex = 0;

    // -------------------------------------------------------------------------
    // Flywheels
    // -------------------------------------------------------------------------
    private DcMotorEx flywheel;
    private DcMotorEx flywheel2;
    private DcMotorEx lodewheel;

    // Servos
    private Servo claw;
    private Servo loop;

    // Constants
    private static final int    TARGET_FLYWHEEL = 5090; // target ticks/sec
    private static final int    RAMP_RATE       = 900;  // smaller step = smoother ramp

    // Servo position constants
    private static final double CLAW_CLOSED = 1.0;
    private static final double CLAW_OPEN   = 0.25;
    private static final double loop_close  = 5;

    // Track flywheel velocity for ramping
    private double currentFlywheelVelocity = 0;
    private double currentLinearPosition   = 0.5;

    private final double bob = 6000;

    // Button state tracking for debouncing (existing)
    private boolean lastLeftBumper  = false;
    private boolean lastRightBumper = false;

    // Speed control variables
    private float forwardSpeed = 0.5f;
    private float strafeSpeed  = 0.5f;
    private float turnSpeed    = 0.5f;

    private ControlMap    controls;
    private MecanumDrive  drive;
    private DriveConstants driveConstants;
    private RevIMU        imu;

    // =========================================================================
    // INIT
    // =========================================================================
    @Override
    public void init() {
        // --- LED Driver ---
        // In your robot configuration, add an "I2C Device (Synch)" named "prism_led"
        // on the I²C bus connected to the GoBilda Prism driver (default address 0x40).
        ledDriver = hardwareMap.get(GoBildaPrismDriver.class, "prism_led");
        initPCA9685();
        setLedColor(0, 255, 0); // Start green (idle)

        // --- Flywheel motors ---
        flywheel  = hardwareMap.get(DcMotorEx.class, "flywheel");
        flywheel2 = hardwareMap.get(DcMotorEx.class, "flywheel2");
        lodewheel = hardwareMap.get(DcMotorEx.class, "lode");
        flywheel.setMode(DcMotorEx.RunMode.RUN_USING_ENCODER);
        flywheel2.setMode(DcMotorEx.RunMode.RUN_USING_ENCODER);
        lodewheel.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        // Flip one flywheel so they counter-rotate
        flywheel.setDirection(DcMotorEx.Direction.FORWARD);
        flywheel2.setDirection(DcMotorEx.Direction.REVERSE);

        // --- Servos ---
        claw = hardwareMap.get(Servo.class, "claw");
        claw.setPosition(CLAW_CLOSED);
        loop = hardwareMap.get(Servo.class, "loop");
        loop.setPosition(loop_close);

        // --- Drive system ---
        DriveBase driveBase = new DriveBase(hardwareMap);
        controls      = new ControlMap(gamepad1, gamepad2);
        drive         = driveBase.mecanum;
        imu           = driveBase.imu;
        driveConstants = new DriveConstants();

        telemetry.addData("Status", "Initialized");
        telemetry.addData("LED",      "I2C Prism driver ready");
        telemetry.addData("Controls", "Gp2 X = cycle color mode | Gp2 B = toggle LEDs");
        telemetry.addData("Controls", "A=Flywheel, LB=Retract, RB=Extend");
        telemetry.addData("Controls", "LT=Close Claw, RT=Open Claw");
        telemetry.update();
    }

    // =========================================================================
    // LOOP
    // =========================================================================
    @Override
    public void loop() {

        // --- Speed control ---
        if (gamepad1.b) {
            forwardSpeed = 0.3f;
            strafeSpeed  = 0.3f;
            turnSpeed    = 0.3f;
        } else if (gamepad1.a) {
            forwardSpeed = 0.5f;
            strafeSpeed  = 0.5f;
            turnSpeed    = 0.5f;
        }

        // --- Drivetrain ---
        if (Objects.equals(driveConstants.robotDriveMode, DriveConstants.ROBOT_CENTRIC)) {
            drive.driveRobotCentric(
                    controls.driver1.getLeftX()  * -forwardSpeed,
                    controls.driver1.getLeftY()  * -strafeSpeed,
                    controls.driver1.getRightX() *  turnSpeed
            );
        } else if (Objects.equals(driveConstants.robotDriveMode, DriveConstants.FIELD_CENTRIC)) {
            drive.driveFieldCentric(
                    controls.driver1.getLeftY()  *  forwardSpeed,
                    controls.driver1.getLeftX()  *  strafeSpeed,
                    controls.driver1.getRightX() *  turnSpeed,
                    imu.getRotation2d().getDegrees()
            );
        }

        // --- Flywheel ramping ---
        if (gamepad2.a) {
            currentFlywheelVelocity = Math.min(currentFlywheelVelocity + RAMP_RATE, TARGET_FLYWHEEL);
        } else {
            currentFlywheelVelocity = Math.max(currentFlywheelVelocity - RAMP_RATE, 0);
        }
        flywheel.setVelocity(currentFlywheelVelocity);
        flywheel2.setVelocity(currentFlywheelVelocity);

        // --- Intake ---
        if (gamepad2.dpad_down) {
            lodewheel.setVelocity(bob);
        } else {
            lodewheel.setVelocity(0);
        }

        // --- Claw ---
        if (gamepad2.left_trigger > 0.1) {
            claw.setPosition(CLAW_CLOSED);
        } else if (gamepad2.right_trigger > 0.1) {
            claw.setPosition(CLAW_OPEN);
        }

        // --- Loop servo ---
        if (gamepad2.y) {
            loop.setPosition(0);
        } else if (gamepad2.dpad_right) {
            loop.setPosition(5);
        } else if (gamepad2.dpad_left) {
            loop.setPosition(0.5);
        }

        // --- LED: toggle on/off (Gamepad 2 B, debounced) ---
        if (gamepad2.b && !lastButtonB) {
            ledsEnabled = !ledsEnabled;
            if (!ledsEnabled) {
                setLedColor(0, 0, 0); // hard off
            }
        }
        lastButtonB = gamepad2.b;

        // --- LED: cycle color mode (Gamepad 2 X, debounced) ---
        if (gamepad2.x && !lastButtonX) {
            ledModeIndex = (ledModeIndex + 1) % LED_CYCLE.length;
            ledMode = LED_CYCLE[ledModeIndex];
        }
        lastButtonX = gamepad2.x;

        // --- LED: update color based on mode ---
        if (ledsEnabled) {
            updateLeds();
        }

        // --- Existing debounce state ---
        lastLeftBumper  = gamepad2.left_bumper;
        lastRightBumper = gamepad2.right_bumper;

        // --- Telemetry ---
        telemetry.addData("Status",             "Running");
        telemetry.addData("LED Mode",            ledMode.name());
        telemetry.addData("LEDs Enabled",        ledsEnabled);
        telemetry.addData("Flywheel Target",     TARGET_FLYWHEEL);
        telemetry.addData("Flywheel Commanded",  currentFlywheelVelocity);
        telemetry.addData("Flywheel1 Actual",    flywheel.getVelocity());
        telemetry.addData("Flywheel2 Actual",    flywheel2.getVelocity());
        telemetry.addData("Linear Servo",        "%.2f", currentLinearPosition);
        telemetry.addData("Claw Position",       "%.2f", claw.getPosition());
        telemetry.update();
    }

    // =========================================================================
    // LED HELPERS
    // =========================================================================

    /**
     * Decide what color to show based on the current ledMode.
     * In AUTO mode, LED color reflects robot state:
     *   - Flywheel spinning  → Blue  (ready to launch)
     *   - Claw closed        → Red   (holding game element)
     *   - Idle               → Green
     */
    private void updateLeds() {
        switch (ledMode) {
            case AUTO:
                if (currentFlywheelVelocity > 500) {
                    // Flywheel active: scale blue with speed
                    int blue = (int) ((currentFlywheelVelocity = 2580) * 255);
                    setLedColor(0, 255, 0);
                } else if (loop.getPosition() >= loop_close - 0.05)  {
                    setLedColor(0, 0, 255); // Claw closed: red
                } else if (claw.getPosition() >= CLAW_CLOSED -0.05) {
                    setLedColor(0,0,255);
                } else {
                    setLedColor(255, 0, 0); // Idle: green
                }
                break;
            case RED:
                setLedColor(255, 0, 0);
                break;
            case GREEN:
                setLedColor(0, 255, 0);
                break;
            case BLUE:
                setLedColor(0, 0, 255);
                break;
            case WHITE:
                setLedColor(255, 255, 255);
                break;
            case OFF:
                setLedColor(0, 0, 0);
                break;
        }
    }

    /**
     * Wake the PCA9685 from sleep by clearing the SLEEP bit in MODE1.
     * Also set the prescaler for ~1 kHz PWM (optional but good practice).
     */
    private void initPCA9685() {
        // Clear sleep bit (bit 4) to enable oscillator
        // 0x00 = normal mode, auto-increment enabled (bit 5 set → 0x20)
        // AI + normal mode
        // Allow oscillator to stabilize
        try { Thread.sleep(10); } catch (InterruptedException ignored) {}
    }

    /**
     * Write an RGB color (0–255 per channel) to the GoBilda Prism driver.
     * Converts 8-bit channel values to 12-bit PCA9685 OFF counts.
     *
     * @param r Red   0–255
     * @param g Green 0–255
     * @param b Blue  0–255
     */
    private void setLedColor(int r, int g, int b) {
        int rPwm = scale8to12(r);
        int gPwm = scale8to12(g);
        int bPwm = scale8to12(b);

        // CH0 = Red, CH1 = Green, CH2 = Blue
        writePwmChannel(PCA9685_LED0_ON_L, rPwm);
        writePwmChannel(PCA9685_LED1_ON_L, gPwm);
        writePwmChannel(PCA9685_LED2_ON_L, bPwm);
    }

    /**
     * Write a 12-bit PWM value to one PCA9685 channel.
     * ON count is always 0 (phase start at tick 0).
     * OFF count = pwmValue (the tick at which the signal goes low).
     *
     * Register layout for each channel (4 bytes at channelBase):
     *   [0] LEDn_ON_L  (bits 0–7 of ON count)
     *   [1] LEDn_ON_H  (bits 8–11 of ON count)
     *   [2] LEDn_OFF_L (bits 0–7 of OFF count)
     *   [3] LEDn_OFF_H (bits 8–11 of OFF count)
     */
    private void writePwmChannel(byte channelBase, int pwmValue) {
        byte[] data = new byte[4];
        data[0] = 0x00;                          // ON_L  = 0
        data[1] = 0x00;                          // ON_H  = 0
        data[2] = (byte)  (pwmValue & 0xFF);     // OFF_L = low byte
        data[3] = (byte) ((pwmValue >> 8) & 0x0F); // OFF_H = high nibble (12-bit)

    }

    /** Scale an 8-bit brightness value (0–255) to a 12-bit PCA9685 value (0–4095). */
    private int scale8to12(int value8bit) {
        return (value8bit * PCA9685_MAX_PWM) / 255;
    }
}