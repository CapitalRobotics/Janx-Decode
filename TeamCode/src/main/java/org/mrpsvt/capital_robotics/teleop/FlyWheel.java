package org.mrpsvt.capital_robotics.teleop;


import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.seattlesolvers.solverslib.drivebase.MecanumDrive;
import com.seattlesolvers.solverslib.hardware.RevIMU;

import org.mrpsvt.capital_robotics.control.ControlMap;
import org.mrpsvt.capital_robotics.robot_core.DriveBase;
import org.mrpsvt.capital_robotics.robot_core.DriveConstants;

import java.util.Objects;

@TeleOp(name = "fiy wheal")
public class FlyWheel extends OpMode {

    // Flywheels
    private DcMotorEx flywheel;
    private DcMotorEx flywheel2;

    // Constants
    private static final int TARGET_FLYWHEEL = 5090; // target ticks/sec
    private static final int RAMP_RATE = 900;        // smaller step = smoother ramp

    // Track flywheel velocity for ramping
    private double currentFlywheelVelocity = 0;
    private ControlMap controls;
    private MecanumDrive drive;
    private DriveConstants driveConstants;
    private RevIMU imu; // Assuming you have an IMU class for field-centric driving

    @Override
    public void init() {


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
        DriveBase driveBase = new DriveBase(hardwareMap);
        controls = new ControlMap(gamepad1, gamepad2);
        drive = driveBase.mecanum;
        imu = driveBase.imu;
        driveConstants = new DriveConstants();
    }

    @Override
    public void loop() {
        // Drivetrain control


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


        if (Objects.equals(driveConstants.robotDriveMode, DriveConstants.ROBOT_CENTRIC)) {
            drive.driveRobotCentric(
                    controls.driver1.getLeftX() * driveConstants.forwardSpeed,
                    controls.driver1.getLeftY() * -driveConstants.strafeSpeed,
                    controls.driver1.getRightX() * driveConstants.turnSpeed
            );
        } else if (Objects.equals(driveConstants.robotDriveMode, DriveConstants.FIELD_CENTRIC)) {
            drive.driveFieldCentric(
                    controls.driver1.getLeftY() * driveConstants.forwardSpeed,
                    controls.driver1.getLeftX() * driveConstants.strafeSpeed,
                    controls.driver1.getRightX() * driveConstants.turnSpeed,
                    imu.getRotation2d().getDegrees()  // IMU heading would go here
            );
        }
    }

}


