 package org.firstinspires.ftc.teamcode.decod;

import static com.qualcomm.robotcore.util.Range.clip;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotorEx;

import org.firstinspires.ftc.teamcode.TemplateJanx;

@TeleOp(name = "fiy wheal")
public class FlyWheel extends OpMode {
    // Wheel motors
    private DcMotorEx frontRight;
    private DcMotorEx backRight;
    private DcMotorEx frontLeft;
    private DcMotorEx backLeft;

    private final int Speed = 18000;

    // Flywheels
    private DcMotorEx flywheel;
    private DcMotorEx flywheel2;

    // Constants
    private static final int TARGET_FLYWHEEL = 90000; // target ticks/sec
    private static final int RAMP_RATE = 9000;        // smaller step = smoother ramp

    // Track flywheel velocity for ramping
    private double currentFlywheelVelocity = 0;

    @Override
    public void init() {
        TemplateJanx janx = new TemplateJanx(hardwareMap);
        janx.wheelInit("fr", "br", "bl", "fl");
        frontLeft = janx.fl;
        frontRight = janx.fr;
        backRight = janx.br;
        backLeft = janx.bl;

        // Map flywheel motors
        flywheel = hardwareMap.get(DcMotorEx.class, "flywheel");
        flywheel2 = hardwareMap.get(DcMotorEx.class, "flywheel2");

        flywheel.setMode(DcMotorEx.RunMode.RUN_USING_ENCODER);
        flywheel2.setMode(DcMotorEx.RunMode.RUN_USING_ENCODER);

        // Flip one of the flywheels so they counter-rotate
        flywheel.setDirection(DcMotorEx.Direction.FORWARD);
        flywheel2.setDirection(DcMotorEx.Direction.REVERSE);

        telemetry.addData("Status", "Initialized");
        telemetry.update();
    }

    @Override
    public void loop() {
        // Drivetrain control
        mecanum(gamepad1.left_stick_y, gamepad1.left_stick_x, gamepad1.right_stick_x);

        // Flywheel control with ramping (on gamepad2 A)
        if (gamepad2.a) {
            // Ramp up to target speed
            currentFlywheelVelocity = Math.min(
                    currentFlywheelVelocity + RAMP_RATE,
                    TARGET_FLYWHEEL
            );
        } else {
            // Ramp down smoothly to 0
            currentFlywheelVelocity = Math.max(
                    currentFlywheelVelocity - RAMP_RATE,
                    0
            );
        }

        // Apply to both flywheels
        flywheel.setVelocity(currentFlywheelVelocity);
        flywheel2.setVelocity(currentFlywheelVelocity);

        // Telemetry
        telemetry.addData("Flywheel Target", TARGET_FLYWHEEL);
        telemetry.addData("Flywheel Commanded", currentFlywheelVelocity);
        telemetry.addData("Flywheel1 Actual", flywheel.getVelocity());
        telemetry.addData("Flywheel2 Actual", flywheel2.getVelocity());
        telemetry.update();
    }

    // Mecanum drive math
    private void mecanum(double LSY, double LSX, double RSX) {
        // Cube inputs for finer control
        double ly = Math.pow(LSY, 3);   // forward/back
        double lx = -Math.pow(LSX, 3);  // strafe
        double rx = -Math.pow(RSX, 3);  // rotation

        if (Math.abs(LSY) > 0.01 || Math.abs(LSX) > 0.01 || Math.abs(RSX) > 0.01) {
            frontRight.setVelocity(Speed * (clip((ly - lx), -1, 1) - rx));
            frontLeft.setVelocity(Speed * (clip((ly + lx), -1, 1) + rx));
            backRight.setVelocity(Speed * (clip((ly + lx), -1, 1) - rx));
            backLeft.setVelocity(Speed * (clip((ly - lx), -1, 1) + rx));
        } else {
            stopMotors();
        }
    }

    // Stop all motors
    private void stopMotors() {
        frontLeft.setVelocity(0);
        backLeft.setVelocity(0);
        frontRight.setVelocity(0);
        backRight.setVelocity(0);
        flywheel.setVelocity(0);
        flywheel2.setVelocity(0);
    }
}

