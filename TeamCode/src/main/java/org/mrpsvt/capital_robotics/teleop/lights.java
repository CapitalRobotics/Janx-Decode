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
    // LED
    // -------------------------------------------------------------------------
    private GoBildaPrismDriver ledDriver;
    private boolean ledsEnabled  = true;
    private boolean lastButtonB  = false;
    private boolean lastButtonX  = false;

    private enum LedMode { AUTO, RED, GREEN, BLUE, WHITE, OFF }
    private LedMode ledMode = LedMode.AUTO;
    private static final LedMode[] LED_CYCLE = {
            LedMode.AUTO, LedMode.RED, LedMode.GREEN,
            LedMode.BLUE, LedMode.WHITE, LedMode.OFF
    };
    private int ledModeIndex = 0;

    // -------------------------------------------------------------------------
    // Motors & Servos
    // -------------------------------------------------------------------------
    private DcMotorEx flywheel;
    private DcMotorEx flywheel2;
    private DcMotorEx lodewheel;
    private Servo claw;
    private Servo loop;

    // Constants
    private static final int    TARGET_FLYWHEEL = 5090;
    private static final int    RAMP_RATE       = 900;
    private static final double CLAW_CLOSED     = 1.0;
    private static final double CLAW_OPEN       = 0.25;
    private static final double LOOP_CLOSE      = 5.0;
    private static final double BOB             = 6000;

    private double currentFlywheelVelocity = 0;
    private double currentLinearPosition   = 0.5;

    private boolean lastLeftBumper  = false;
    private boolean lastRightBumper = false;

    private float forwardSpeed = 0.5f;
    private float strafeSpeed  = 0.5f;
    private float turnSpeed    = 0.5f;

    private ControlMap     controls;
    private MecanumDrive   drive;
    private DriveConstants driveConstants;
    private RevIMU         imu;

    // =========================================================================
    // INIT
    // =========================================================================
    @Override
    public void init() {
        // --- LED Driver ---
        ledDriver = hardwareMap.get(GoBildaPrismDriver.class, "prism_led");
        ledDriver.setColor(0, 255, 0); // Start green (idle)

        // --- Flywheel motors ---
        flywheel  = hardwareMap.get(DcMotorEx.class, "flywheel");
        flywheel2 = hardwareMap.get(DcMotorEx.class, "flywheel2");
        lodewheel = hardwareMap.get(DcMotorEx.class, "lode");
        flywheel.setMode(DcMotorEx.RunMode.RUN_USING_ENCODER);
        flywheel2.setMode(DcMotorEx.RunMode.RUN_USING_ENCODER);
        lodewheel.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        flywheel.setDirection(DcMotorEx.Direction.FORWARD);
        flywheel2.setDirection(DcMotorEx.Direction.REVERSE);

        // --- Servos ---
        claw = hardwareMap.get(Servo.class, "claw");
        claw.setPosition(CLAW_CLOSED);
        loop = hardwareMap.get(Servo.class, "loop");
        loop.setPosition(LOOP_CLOSE);

        // --- Drive system ---
        DriveBase driveBase = new DriveBase(hardwareMap);
        controls       = new ControlMap(gamepad1, gamepad2);
        drive          = driveBase.mecanum;
        imu            = driveBase.imu;
        driveConstants = new DriveConstants();

        telemetry.addData("Status",    "Initialized");
        telemetry.addData("Controls",  "Gp2 X = cycle color | Gp2 B = toggle LEDs");
        telemetry.addData("Controls",  "A=Flywheel | LT=Close Claw | RT=Open Claw");
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
            // FIX 1: Flywheels ramp DOWN when A is released instead of staying on
            currentFlywheelVelocity = Math.max(currentFlywheelVelocity - RAMP_RATE, 0);
        }
        flywheel.setVelocity(currentFlywheelVelocity);
        flywheel2.setVelocity(currentFlywheelVelocity);

        // --- Intake ---
        if (gamepad2.dpad_down) {
            lodewheel.setVelocity(BOB);
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
            loop.setPosition(LOOP_CLOSE);
        } else if (gamepad2.dpad_left) {
            loop.setPosition(0.5);
        }

        // --- LED toggle on/off (Gp2 B, debounced) ---
        if (gamepad2.b && !lastButtonB) {
            ledsEnabled = !ledsEnabled;
            if (!ledsEnabled) {
                ledDriver.setColor(0, 0, 0); // hard off
            }
        }
        lastButtonB = gamepad2.b;

        // --- LED cycle color mode (Gp2 X, debounced) ---
        if (gamepad2.x && !lastButtonX) {
            ledModeIndex = (ledModeIndex + 1) % LED_CYCLE.length;
            ledMode = LED_CYCLE[ledModeIndex];
        }
        lastButtonX = gamepad2.x;

        // --- LED update ---
        if (ledsEnabled) {
            updateLeds();
        }

        lastLeftBumper  = gamepad2.left_bumper;
        lastRightBumper = gamepad2.right_bumper;

        // --- Telemetry ---
        telemetry.addData("Status",            "Running");
        telemetry.addData("LED Mode",           ledMode.name());
        telemetry.addData("LEDs Enabled",       ledsEnabled);
        telemetry.addData("Flywheel Target",    TARGET_FLYWHEEL);
        telemetry.addData("Flywheel Commanded", currentFlywheelVelocity);
        telemetry.addData("Flywheel1 Actual",   flywheel.getVelocity());
        telemetry.addData("Flywheel2 Actual",   flywheel2.getVelocity());
        telemetry.addData("Claw Position",      "%.2f", claw.getPosition());
        telemetry.update();
    }

    // =========================================================================
    // LED HELPERS
    // =========================================================================

    /**
     * AUTO mode:
     *   Flywheel spinning → Blue  (ready to launch)
     *   Loop closed       → Red   (loop engaged)
     *   Claw closed       → Red   (holding element)
     *   Idle              → Green
     */
    private void updateLeds() {
        switch (ledMode) {
            case AUTO:
                if (currentFlywheelVelocity > 500) {
                    // FIX 2: Was "currentFlywheelVelocity = 2580" (assignment bug), now just sets blue
                    ledDriver.setColor(0, 0, 255); // Blue = flywheel spinning
                } else if (loop.getPosition() >= LOOP_CLOSE - 0.05) {
                    ledDriver.setColor(255, 0, 0); // Red = loop closed
                } else if (claw.getPosition() >= CLAW_CLOSED - 0.05) {
                    ledDriver.setColor(255, 0, 0); // Red = claw closed
                } else {
                    ledDriver.setColor(0, 255, 0); // Green = idle
                }
                break;
            case RED:   ledDriver.setColor(255, 0, 0);     break;
            case GREEN: ledDriver.setColor(0, 255, 0);     break;
            case BLUE:  ledDriver.setColor(0, 0, 255);     break;
            case WHITE: ledDriver.setColor(255, 255, 255); break;
            case OFF:   ledDriver.setColor(0, 0, 0);       break;
        }
    }
}