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
    public void runOpMode() {
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

                    int tagX = block.x;      // X position (0-320, center is ~160)
                    int tagWidth = block.width;  // Size of tag from side to side (bigger = closer)

                    //int tagY = block.y; y position (0-240, center is ~120)
                    //int tagHeight = block.height; size of tag from top to bottom (bigger = closer)

                    telemetry.addData("Target Tag ID", block.id);
                    telemetry.addData("Tag X", tagX);
                    telemetry.addData("Tag Width", tagWidth);
                    //telemetry.addData("Tag Y", tagY);
                    //telemetry.addData("Tag Height", tagHeight);
                    /*
                    soooo, logic for strafing based on the size of the april tag on the husky lens
                    pixel size:

                    when the april tag is y pixels tall, and x pixels wide, it is a certain
                    distance away from the camera. So ill create a while-loop with a nested if
                    statement logic, to have the bot drive foreward/bakward/strafe to have the april
                    tag positioned perfectly to shoot. this is meant to curcumvent the issue of not
                    having depth perception.

                    while (TagX =! [fill this in/x] && Tagheight =! [fill this in/h])
                    {
                        //first fit the x
                        if (tagX < x)
                        {
                            driveDistance(0, (x - tagX), 0, (time, i dunno \(0-0)/);
                            telemetry.addData("Action", "Strafing left");
                        }
                        else if (tagX > x)
                        {
                            driveDistance(0, (x - tagX), 0, (time));
                            telemetry.addData("Action", "Strafing right");
                        }
                        else {}

                        if (tagHeight < y)
                        {
                            driveDistance((h - tagHeight), 0, 0, (time));
                            telemetry.addData(
                     */

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

                    break; // Found our tag, stop searching
                }
            }

            if (!targetFound) {
                // Target tag not found - stop
                stopMotors();
                telemetry.addData("Action", "Stopped - Tag Not Found");
            }

            telemetry.update();
        }
    }

    private void moveForward(double power) {
        leftFront.setPower(power);
        rightFront.setPower(power);
        leftBack.setPower(power);
        rightBack.setPower(power);
    }

    private void turnLeft(double power) {
        leftFront.setPower(-power);
        rightFront.setPower(power);
        leftBack.setPower(-power);
        rightBack.setPower(power);
    }

    private void turnRight(double power) {
        leftFront.setPower(power);
        rightFront.setPower(-power);
        leftBack.setPower(power);
        rightBack.setPower(-power);
    }

    private void stopMotors() {
        leftFront.setPower(0);
        rightFront.setPower(0);
        leftBack.setPower(0);
        rightBack.setPower(0);
    }
}