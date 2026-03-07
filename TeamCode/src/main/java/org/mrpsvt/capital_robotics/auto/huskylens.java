package org.mrpsvt.capital_robotics.auto;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.hardware.dfrobot.HuskyLens;

@Autonomous(name="HuskyLens AprilTag Movement")
public class huskylens extends LinearOpMode {

    private DcMotor leftFront;
    private DcMotor rightFront;
    private DcMotor leftBack;
    private DcMotor rightBack;
    private HuskyLens huskyLens;

    // Target AprilTag ID you want to follow
    private final int TARGET_TAG_ID = 1; // Change this to your target tag

    @Override
    public void runOpMode()
    {
        // Initialize motors
        leftFront = hardwareMap.get(DcMotor.class, "leftFront");
        rightFront = hardwareMap.get(DcMotor.class, "rightFront");
        leftBack = hardwareMap.get(DcMotor.class, "leftBack");
        rightBack = hardwareMap.get(DcMotor.class, "rightBack");

        // Initialize HuskyLens
        huskyLens = hardwareMap.get(HuskyLens.class, "huskyLens");

        // Reverse right motors
        rightFront.setDirection(DcMotor.Direction.REVERSE);
        rightBack.setDirection(DcMotor.Direction.REVERSE);

        // Set HuskyLens to Tag Recognition algorithm
        huskyLens.selectAlgorithm(HuskyLens.Algorithm.TAG_RECOGNITION);

        telemetry.addData("Status", "Initialized");
        telemetry.addData("Algorithm", "Tag Recognition");
        telemetry.update();

        waitForStart();

        while (opModeIsActive())
        {
            // Get blocks (AprilTags) detected by HuskyLens
            HuskyLens.Block[] blocks = huskyLens.blocks();

            telemetry.addData("Tags Detected", blocks.length);

            boolean targetFound = false;

            // Look for our target tag
            for (HuskyLens.Block block : blocks)
            {
                if (block.id == TARGET_TAG_ID)
                {
                    targetFound = true;

                    int tagX = block.x; // X position (0-320, center is ~160)
                    int tagY = block.y; // YY position (0-240, center is ~120)
                    // upper left of screen is (0, 0)
                    int tagWidth = block.width; // Size of tag from side to side (bigger = closer)
                    int tagHeight = block.height; // height of tag from top to bottom

                    telemetry.addData("Target Tag ID", block.id);
                    telemetry.addData("Tag X", tagX);
                    telemetry.addData("Tag Width", tagWidth);
                    telemetry.addData("Tag Y", tagY);
                    telemetry.addData("Tag Height", tagHeight);
                    telemetry.update();

                    int x = 160;
                    int y = 120;
                    int h = 41;
                    int w = 41;
                    int change = 0;

                    while (tagX > (x - 3) || tagX < (x + 3))
                    {
                        telemetry.addData("action", "triangulating x");
                        if (tagX > x && change == 0)
                        {
                            driveDistance(0, 0.5, 0, 10);
                            tagX = block.x;
                            if (tagX < x)
                            {
                                change++;
                            }
                        }
                        else if (tagX > x && change > 1)
                        {
                            driveDistance(0, 0.5, 0, (long)(10 * Math.pow(.75, change)));
                            tagX = block.x;
                            if (tagX < x)
                            {
                                change++;
                            }
                        }
                        else if (tagX < x && change == 0)
                        {
                            driveDistance(0, -0.5, 0, 10);
                            tagX = block.x;
                            if (tagX > x)
                            {
                                change++;
                            }
                        }
                        else if (tagX < x && change > 1)
                        {
                            driveDistance(0, -0.5, 0, (long)(10 * Math.pow(.75, change)));
                            tagX = block.x;
                            if (tagX > x)
                            {
                                change++;
                            }
                        }
                    }



                    /*
                    // Move based on tag position
                    if (tagX < 140)
                    {
                        // Tag is on left - turn left
                        turnLeft(0.25);
                        telemetry.addData("Action", "Turning Left");

                    }
                    else if (tagX > 180)
                    {
                        // Tag is on right - turn right
                        turnRight(0.25);
                        telemetry.addData("Action", "Turning Right");

                    }
                    else if (tagWidth < 80)
                    {
                        // Tag is centered but far - move forward
                        moveForward(0.3);
                        telemetry.addData("Action", "Moving Forward");

                    } else {
                        // Tag is centered and close - stop
                        stopMotors();
                        telemetry.addData("Action", "Arrived at Tag!");
                    }
                    */
                    break; // Found our tag, stop searching
                }
            }

            if (!targetFound)
            {
                // Target tag not found - stop
                stopMotors();
                telemetry.addData("Action", "Stopped - Tag Not Found");
            }

            telemetry.update();
        }
    }

    private void moveForward(double power)
    {
        leftFront.setPower(power);
        rightFront.setPower(power);
        leftBack.setPower(power);
        rightBack.setPower(power);
    }

    private void turnLeft(double power)
    {
        leftFront.setPower(-power);
        rightFront.setPower(power);
        leftBack.setPower(-power);
        rightBack.setPower(power);
    }

    private void turnRight(double power)
    {
        leftFront.setPower(power);
        rightFront.setPower(-power);
        leftBack.setPower(power);
        rightBack.setPower(-power);
    }

    private void stopMotors()
    {
        leftFront.setPower(0);
        rightFront.setPower(0);
        leftBack.setPower(0);
        rightBack.setPower(0);
    }
    private void driveDistance(double forward, double strafe, double turn, long milliseconds) {
        // Mecanum drive calculations
        double frontLeftPower = forward + strafe + turn;
        double frontRightPower = forward - strafe - turn;
        double backLeftPower = forward - strafe + turn;
        double backRightPower = forward + strafe - turn;

        // Normalize powers if any exceed 1.0
        double maxPower = Math.max
        (
                Math.max(Math.abs(frontLeftPower), Math.abs(frontRightPower)),
                Math.max(Math.abs(backLeftPower), Math.abs(backRightPower))
        );

        if (maxPower > 1.0)
        {
            frontLeftPower /= maxPower;
            frontRightPower /= maxPower;
            backLeftPower /= maxPower;
            backRightPower /= maxPower;
        }

        // Set motor powers
        leftFront.setPower(frontLeftPower);
        rightFront.setPower(frontRightPower);
        leftBack.setPower(backLeftPower);
        rightBack.setPower(backRightPower);

        sleep(milliseconds);

        // Stop motors
        leftFront.setPower(0);
        rightFront.setPower(0);
        leftBack.setPower(0);
        rightBack.setPower(0);
    }
}