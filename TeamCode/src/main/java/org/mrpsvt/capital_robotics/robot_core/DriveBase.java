package org.mrpsvt.capital_robotics.robot_core;
import org.mrpsvt.capital_robotics.robot_core.DriveConstants;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.IMU;
import com.seattlesolvers.solverslib.drivebase.MecanumDrive;
import com.seattlesolvers.solverslib.hardware.RevIMU;
import com.seattlesolvers.solverslib.hardware.motors.Motor;


public class DriveBase {
    HardwareMap hardwareMap;
    Motor frontLeft;
    Motor frontRight;
    Motor backLeft;
    Motor backRight;

    public MecanumDrive mecanum;
    public RevIMU imu;

    public DriveBase(HardwareMap hardwareMap) {
        this.hardwareMap = hardwareMap;
        this.mecanum = initDriveBase();
    }

    public MecanumDrive initDriveBase() {
        DriveConstants constants = new DriveConstants();
        frontLeft = new Motor(hardwareMap, constants.frontLeftMotorName);
        frontRight = new Motor(hardwareMap, constants.frontRightMotorName);
        backLeft = new Motor(hardwareMap, constants.backLeftMotorName);
        backRight = new Motor(hardwareMap, constants.backRightMotorName);

        mecanum = new MecanumDrive(frontLeft, frontRight, backLeft, backRight);
        return mecanum;
    }

    public RevIMU initIMU() {
        RevIMU imu = new RevIMU(hardwareMap);
        imu.init();
        return imu;
    }
}
