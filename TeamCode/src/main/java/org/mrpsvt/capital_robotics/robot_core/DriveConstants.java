package org.mrpsvt.capital_robotics.robot_core;
import com.acmerobotics.dashboard.config.Config;

@Config
public class DriveConstants {
    public static String ROBOT_CENTRIC = "robot_centric";
    public static String FIELD_CENTRIC = "field_centric";

    public float strafeSpeed = 1.0f;
    public float forwardSpeed =1.0f;
    public float turnSpeed = 1.0f;

    public final String frontLeftMotorName = "fl";
    public final String frontRightMotorName = "fr";
    public final String backLeftMotorName = "bl";
    public final String backRightMotorName = "br";

    public final String robotDriveMode = ROBOT_CENTRIC;
}
