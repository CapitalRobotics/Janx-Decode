package org.mrpsvt.capital_robotics.teleop;


import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.Servo;
import com.seattlesolvers.solverslib.drivebase.MecanumDrive;
import com.seattlesolvers.solverslib.hardware.RevIMU;

import org.mrpsvt.capital_robotics.control.ControlMap;
import org.mrpsvt.capital_robotics.robot_core.DriveBase;
import org.mrpsvt.capital_robotics.robot_core.DriveConstants;

import java.util.Objects;

@TeleOp(name="w_lode")
public class full_bot_w_load extends OpMode {
    // Flywheels
    private DcMotorEx flywheel;
    private DcMotorEx flywheel2;
    private DcMotorEx lodewheel;
    // Servos
//    private Servo nemo;
    private Servo claw;
    private Servo loop;
    // Constants
    private static final int TARGET_FLYWHEEL = 5090; // target ticks/sec
    private static final int RAMP_RATE = 900;        // smaller step = smoother ramp

    // Servo position constants

    private static final double CLAW_CLOSED = 1.0;
    private static final double CLAW_OPEN = 0.25;
    private static final double loop_close = 5;

    // Track flywheel velocity for ramping
    private double currentFlywheelVelocity = 0;
    private double currentLinearPosition = 0.5;

    private final double bob =6000;
    // Button state tracking for debouncing
    private boolean lastLeftBumper = false;
    private boolean lastRightBumper = false;

    private ControlMap controls;
    private MecanumDrive drive;
    private DriveConstants driveConstants;
    private RevIMU imu;

    @Override
    public void init() {
        // Map flywheel motors
        flywheel = hardwareMap.get(DcMotorEx.class, "flywheel");
        flywheel2 = hardwareMap.get(DcMotorEx.class, "flywheel2");
        lodewheel = hardwareMap.get(DcMotorEx.class, "lode");
        flywheel.setMode(DcMotorEx.RunMode.RUN_USING_ENCODER);
        flywheel2.setMode(DcMotorEx.RunMode.RUN_USING_ENCODER);
        lodewheel.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        // Flip one of the flywheels so they counter-rotate
        flywheel.setDirection(DcMotorEx.Direction.FORWARD);
        flywheel2.setDirection(DcMotorEx.Direction.REVERSE);

        // Initialize servos
//        nemo = hardwareMap.get(Servo.class, "li");
//        nemo.setPosition(currentLinearPosition);

        claw = hardwareMap.get(Servo.class, "claw");
        claw.setPosition(CLAW_CLOSED); // Start with claw open
        loop = hardwareMap.get(Servo.class,"loop");
        loop.setPosition(loop_close);
        // Initialize drive system
        DriveBase driveBase = new DriveBase(hardwareMap);
        controls = new ControlMap(gamepad1, gamepad2);
        drive = driveBase.mecanum;
        imu = driveBase.imu;
        driveConstants = new DriveConstants();

        telemetry.addData("Status", "Initialized");
        telemetry.addData("Controls", "A=Flywheel, LB=Retract, RB=Extend");
        telemetry.addData("Controls", "LT=Close Claw, RT=Open Claw");
        telemetry.update();
    }

    @Override
    public void loop() {
        // Drivetrain control
        if (Objects.equals(driveConstants.robotDriveMode, DriveConstants.ROBOT_CENTRIC)) {
            drive.driveRobotCentric(
                    controls.driver1.getLeftX() * -driveConstants.forwardSpeed,
                    controls.driver1.getLeftY() * -driveConstants.strafeSpeed,
                    controls.driver1.getRightX() * driveConstants.turnSpeed
            );
        } else if (Objects.equals(driveConstants.robotDriveMode, DriveConstants.FIELD_CENTRIC)) {
            drive.driveFieldCentric(
                    controls.driver1.getLeftY() * driveConstants.forwardSpeed,
                    controls.driver1.getLeftX() * driveConstants.strafeSpeed,
                    controls.driver1.getRightX() * driveConstants.turnSpeed,
                    imu.getRotation2d().getDegrees()
            );
        }

        // Flywheel control:
        // Verison with ramping (Gamepad 2, button: A)
        if (gamepad2.a) {
            // Ramp up launch motors up to target speed
            currentFlywheelVelocity = Math.min(
                    currentFlywheelVelocity + RAMP_RATE,
                    TARGET_FLYWHEEL
            );
        } else {
            // Ramp launch motors back down smoothly to 0
            currentFlywheelVelocity = Math.max(
                    currentFlywheelVelocity - RAMP_RATE,
                    0
            );
        }
        // Apply to both launch motors
        flywheel.setVelocity(currentFlywheelVelocity);
        flywheel2.setVelocity(currentFlywheelVelocity);






        // Intake motor control:
        // (Gamepad 2, button: d-pad down)

        // ON/OFF at fixed speed
        if (gamepad2.dpad_down) {
            lodewheel.setVelocity(bob); // ON
        } else {
            lodewheel.setVelocity(0); // OFF }

            // Update button states
            lastLeftBumper = gamepad2.left_bumper;
            lastRightBumper = gamepad2.right_bumper;

            // Launch loading servo control:
            // (Gamepad 2, button: back trigger)

            // Close claw: left trigger
            if (gamepad2.left_trigger > 0.1) {
                claw.setPosition(CLAW_CLOSED);
            // Open claw: right trigger
            } else if (gamepad2.right_trigger > 0.1) {
                claw.setPosition(CLAW_OPEN);
            }
            if(gamepad1.y) {
                // move to 0 degrees.
                loop.setPosition(0);
            } else if (gamepad1.dpad_right) {
                // move to 90 degrees.
                loop.setPosition(5);
            } else if (gamepad1.dpad_left) {
                // move to 180 degrees.
                loop.setPosition(.5);
            }
            // Telemetry
            telemetry.addData("Status", "Running");
            telemetry.addData("Flywheel Target", TARGET_FLYWHEEL);
            telemetry.addData("Flywheel Commanded", currentFlywheelVelocity);
            telemetry.addData("Flywheel1 Actual", flywheel.getVelocity());
            telemetry.addData("Flywheel2 Actual", flywheel2.getVelocity());
            telemetry.addData("Linear Servo", "%.2f", currentLinearPosition);
            telemetry.addData("Claw Position", "%.2f", claw.getPosition());
            telemetry.update();
        }
    }
}