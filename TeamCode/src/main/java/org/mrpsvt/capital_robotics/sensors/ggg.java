package org.mrpsvt.capital_robotics.sensors;

import android.annotation.SuppressLint;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;
import org.firstinspires.ftc.vision.apriltag.AprilTagLibrary;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

import java.util.List;

    @TeleOp(name = "AprilTag Detection", group = "Vision")
    public class ggg extends LinearOpMode {

        private AprilTagProcessor aprilTag;
        private VisionPortal visionPortal;

        @Override
        public void runOpMode() {
            // Initialize AprilTag processor
            initAprilTag();

            // Wait for the DS start button to be touched
            telemetry.addData("Status", "Initialized");
            telemetry.addData(">", "Touch Play to start");
            telemetry.update();

            waitForStart();

            if (opModeIsActive()) {
                while (opModeIsActive()) {
                    // Get the current list of detected AprilTags
                    telemetryAprilTag();

                    // Push telemetry to the Driver Station
                    telemetry.update();

                    // Save CPU resources; can resume streaming when needed
                    if (gamepad1.dpad_down) {
                        visionPortal.stopStreaming();
                    } else if (gamepad1.dpad_up) {
                        visionPortal.resumeStreaming();
                    }

                    sleep(20);
                }
            }

            // Save more CPU resources when camera is no longer needed
            visionPortal.close();
        }


        private void initAprilTag() {
            // Create the AprilTag processor with custom tag library
            aprilTag = new AprilTagProcessor.Builder()
                    .setTagLibrary(buildTagLibrary())
                    .build();

            // Create the vision portal using the camera
            visionPortal = new VisionPortal.Builder()
                    .setCamera(hardwareMap.get(WebcamName.class, "webcam"))
                    .addProcessor(aprilTag)
                    .build();
        }


        private AprilTagLibrary buildTagLibrary() {
            return new AprilTagLibrary.Builder()
                    // Add tags with: ID, name, size (in inches), size unit
                    .addTag(5, "gpp", 4.0, DistanceUnit.INCH)
                    .addTag(4, "ppg", 4.0, DistanceUnit.INCH)
                    .addTag(3, "pgp", 4.0, DistanceUnit.INCH)
                    .addTag(2, "Blue", 4.0, DistanceUnit.INCH)
                    .addTag(1, "Red", 4.0, DistanceUnit.INCH)
                    // Add more tags as needed
                    .build();
        }


        @SuppressLint("DefaultLocale")
        private void telemetryAprilTag() {
            List<AprilTagDetection> currentDetections = aprilTag.getDetections();
            telemetry.addData("# AprilTags Detected", currentDetections.size());

            // Step through the list of detections and display info for each one
            for (AprilTagDetection detection : currentDetections) {
                if (detection.metadata != null) {
                    telemetry.addLine(String.format("\n==== (ID %d) %s",
                            detection.id, detection.metadata.name));
                    telemetry.addLine(String.format("XYZ %6.1f %6.1f %6.1f  (inch)",
                            detection.ftcPose.x, detection.ftcPose.y, detection.ftcPose.z));
                    telemetry.addLine(String.format("PRY %6.1f %6.1f %6.1f  (deg)",
                            detection.ftcPose.pitch, detection.ftcPose.roll, detection.ftcPose.yaw));
                    telemetry.addLine(String.format("RBE %6.1f %6.1f %6.1f  (inch, deg, deg)",
                            detection.ftcPose.range, detection.ftcPose.bearing, detection.ftcPose.elevation));
                } else {
                    telemetry.addLine(String.format("\n==== (ID %d) Unknown", detection.id));
                    telemetry.addLine(String.format("Center %6.0f %6.0f   (pixels)",
                            detection.center.x, detection.center.y));
                }
            }

            // Add "key" legend to telemetry
            telemetry.addLine("\nKey:");
            telemetry.addLine("Dpad Up - Resume camera streaming");
            telemetry.addLine("Dpad Down - Stop camera streaming");
        }
    }

