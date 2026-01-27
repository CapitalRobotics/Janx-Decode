package org.mrpsvt.capital_robotics.auto;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.Servo;

@Autonomous(name="blue close")
public class blue_close extends LinearOpMode {
    // Flywheels
    private DcMotorEx flywheel;
    private DcMotorEx flywheel2;
    private DcMotorEx lodewheel;

    // Drive motors
    private DcMotor frontLeft;
    private DcMotor frontRight;
    private DcMotor backLeft;
    private DcMotor backRight;

    // Servos
    private Servo claw;

    // Constants
    private static final int TARGET_FLYWHEEL = 6000;
    private static final int RAMP_RATE = 900;
    private static final double CLAW_CLOSED = 1.0;
    private static final double CLAW_OPEN = 0.25;
    private static final double LODEWHEEL_SPEED = 8;

    @Override
    public void runOpMode() throws InterruptedException {
        // Initialize hardware
        initializeHardware();

        telemetry.addData("Status", "Initialized");
        telemetry.addData("Ready", "Waiting for start");
        telemetry.update();

        waitForStart();

        if (opModeIsActive()) {
            // Execute autonomous sequence
            autonomousSequence();
        }
    }

    private void initializeHardware() {
        // Map flywheel motors
        flywheel = hardwareMap.get(DcMotorEx.class, "flywheel");
        flywheel2 = hardwareMap.get(DcMotorEx.class, "flywheel2");
        lodewheel = hardwareMap.get(DcMotorEx.class, "lode");

        flywheel.setMode(DcMotorEx.RunMode.RUN_USING_ENCODER);
        flywheel2.setMode(DcMotorEx.RunMode.RUN_USING_ENCODER);
        lodewheel.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        flywheel.setDirection(DcMotorEx.Direction.FORWARD);
        flywheel2.setDirection(DcMotorEx.Direction.REVERSE);

        // Initialize servos
        claw = hardwareMap.get(Servo.class, "claw");
//        claw.setPosition(CLAW_OPEN);

        // Initialize drive motors - adjust these names to match your robot configuration
        frontLeft = hardwareMap.get(DcMotor.class, "fl");
        frontRight = hardwareMap.get(DcMotor.class, "fr");
        backLeft = hardwareMap.get(DcMotor.class, "bl");
        backRight = hardwareMap.get(DcMotor.class, "br");

        // Set motor directions for mecanum drive
        frontLeft.setDirection(DcMotor.Direction.FORWARD);
        backLeft.setDirection(DcMotor.Direction.FORWARD);
        frontRight.setDirection(DcMotor.Direction.REVERSE);
        backRight.setDirection(DcMotor.Direction.REVERSE);

        // Set zero power behavior
        frontLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        frontRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        backLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        backRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
    }

    private void autonomousSequence() throws InterruptedException {
        // Example autonomous sequence - customize as needed

        // Step 1: Close claw to grab preloaded game element


        // Step 2: Drive forward to position
        telemetry.addData("Step", "2: Driving forward");
        telemetry.update();
        driveDistance(-1, 0, 0, 1500);
//-forwers +back weras
        telemetry.addData("Step", "2: Driving forward");
        telemetry.update();
        driveDistance(0, 0, -1, 250);
//+ to the rigth - to the lefft
        // Step 3: Spin up flywheels
        telemetry.addData("Step", "3: Spinning up flywheels");
        telemetry.update();
        rampFlywheels(TARGET_FLYWHEEL, 3000);


        telemetry.addData("Step", "1: Closing claw");
        telemetry.update();
        setClaw(CLAW_OPEN);
        sleep(500);
        // Step 4: Run lodewheel to push ball into flywheels and launch
//        telemetry.addData("Step", "4: Pushing ball and launching");
//        telemetry.update();
//        lodewheel.setVelocity(LODEWHEEL_SPEED);
//        sleep(2000);
//        lodewheel.setVelocity(0);

        // Step 5: Spin down flywheels
        telemetry.addData("Step", "5: Spinning down");
        telemetry.update();
        rampFlywheels(0, 2000);

        // Step 6: Strafe right
//        telemetry.addData("Step", "6: Strafing right");
//        telemetry.update();
//        driveDistance(0, 0.5, 0, 1000);

        // Step 7: Open claw
        telemetry.addData("Step", "7: Opening claw");
        telemetry.update();
        setClaw(CLAW_OPEN);
        sleep(500);

        // Step 8: Drive backward
//        telemetry.addData("Step", "8: Driving backward");
//        telemetry.update();
//        driveDistance(-0.3, 0, 0, 800);

        telemetry.addData("Status", "Autonomous Complete");
        telemetry.update();
    }

    private void driveDistance(double forward, double strafe, double turn, long milliseconds) {
        // Mecanum drive calculations
        double frontLeftPower = forward + strafe + turn;
        double frontRightPower = forward - strafe - turn;
        double backLeftPower = forward - strafe + turn;
        double backRightPower = forward + strafe - turn;

        // Normalize powers if any exceed 1.0
        double maxPower = Math.max(
                Math.max(Math.abs(frontLeftPower), Math.abs(frontRightPower)),
                Math.max(Math.abs(backLeftPower), Math.abs(backRightPower))
        );

        if (maxPower > 1.0) {
            frontLeftPower /= maxPower;
            frontRightPower /= maxPower;
            backLeftPower /= maxPower;
            backRightPower /= maxPower;
        }

        // Set motor powers
        frontLeft.setPower(frontLeftPower);
        frontRight.setPower(frontRightPower);
        backLeft.setPower(backLeftPower);
        backRight.setPower(backRightPower);

        sleep(milliseconds);

        // Stop motors
        frontLeft.setPower(0);
        frontRight.setPower(0);
        backLeft.setPower(0);
        backRight.setPower(0);
    }

    private void rampFlywheels(int targetVelocity, long rampTimeMs) {
        long startTime = System.currentTimeMillis();
        double currentVelocity = flywheel.getVelocity();

        while (System.currentTimeMillis() - startTime < rampTimeMs && opModeIsActive()) {
            double progress = (double)(System.currentTimeMillis() - startTime) / rampTimeMs;
            double velocity = currentVelocity + (targetVelocity - currentVelocity) * progress;

            flywheel.setVelocity(velocity);
            flywheel2.setVelocity(velocity);

            telemetry.addData("Flywheel Target", targetVelocity);
            telemetry.addData("Flywheel Current", velocity);
            telemetry.addData("Flywheel1 Actual", flywheel.getVelocity());
            telemetry.addData("Flywheel2 Actual", flywheel2.getVelocity());
            telemetry.update();

            sleep(50);
        }

        flywheel.setVelocity(targetVelocity);
        flywheel2.setVelocity(targetVelocity);
    }

    private void setClaw(double position) {
        claw.setPosition(position);
    }
}
