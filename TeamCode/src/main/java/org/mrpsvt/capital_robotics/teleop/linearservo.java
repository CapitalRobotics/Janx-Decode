package org.mrpsvt.capital_robotics.teleop;



import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.Servo;

@TeleOp(name="Linear Servo Control", group="Linear Opmode")
public class linearservo extends LinearOpMode {

    // Declare servo
    private Servo linearServo;

    // Servo position constants (0.0 to 1.0)
    private static final double RETRACTED_POSITION = 0.0;
    private static final double EXTENDED_POSITION = 1.0;



    private double currentPosition = 0.5;

    @Override
    public void runOpMode() {

        // Initialize the servo
        linearServo = hardwareMap.get(Servo.class, "linear_servo");

        // Set initial position
        linearServo.setPosition(currentPosition);

        telemetry.addData("Status", "Initialized");
        telemetry.addData("Controls", "left bumper=Retract, right bumper=Extend");
        telemetry.addData("Controls", "DPad Up/Down for fine control");
        telemetry.update();

        waitForStart();

        while (opModeIsActive()) {

            // Button controls for preset positions
            if (gamepad2.left_bumper) {
                linearServo.setPosition(RETRACTED_POSITION);
                currentPosition = RETRACTED_POSITION;
            } else if (gamepad2.right_bumper) {
                linearServo.setPosition(EXTENDED_POSITION);
                currentPosition = EXTENDED_POSITION;
            }


            // Clamp position between 0.0 and 1.0
            currentPosition = Math.max(0.0, Math.min(1.0, currentPosition));

            // Update servo position
            linearServo.setPosition(currentPosition);

            // Display telemetry
            telemetry.addData("Status", "Running");
            telemetry.addData("Current Position", "%.2f", currentPosition);
            telemetry.addData("Servo Position", "%.2f", linearServo.getPosition());
            telemetry.update();

            // Small delay to prevent excessive updates
            sleep(20);
        }
    }
}