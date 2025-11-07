package org.mrpsvt.capital_robotics.teleop;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.Servo;

@com.qualcomm.robotcore.eventloop.opmode.TeleOp(name = "Linear Servo Example", group = "Examples")
public class linsrve extends LinearOpMode {
    private Servo linearServo; // Declare the servo

    @Override
    public void runOpMode() {
        // Initialize the servo from the hardware map
        linearServo = hardwareMap.get(Servo.class, "linear_Servo");

        // Wait for the game to start
        waitForStart();

        while (opModeIsActive()) {
            // Move the servo to a specific position based on gamepad input
            if (gamepad1.a) {
                linearServo.setPosition(0.0); // Move to minimum position
            } else if (gamepad1.b) {
                linearServo.setPosition(1.0); // Move to maximum position
            } else if (gamepad1.x) {
                linearServo.setPosition(0.5); // Move to midpoint
            }

            // Display the current servo position on the telemetry
            telemetry.addData("Servo Position", linearServo.getPosition());
            telemetry.update();
        }
    }
}