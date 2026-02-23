package org.mrpsvt.capital_robotics.robot_core;
import com.acmerobotics.dashboard.config.Config;

@Config
public class DriveConstants {
    public static String ROBOT_CENTRIC = "robot_centric";
    public static String FIELD_CENTRIC = "field_centric";

    public float strafeSpeed = 0.5f;
    public float forwardSpeed = 0.5f;
    public float turnSpeed = 0.5f;

    public final String frontLeftMotorName = "fl";
    public final String frontRightMotorName = "fr";
    public final String backLeftMotorName = "bl";
    public final String backRightMotorName = "br";

    public final String robotDriveMode = ROBOT_CENTRIC;
}
