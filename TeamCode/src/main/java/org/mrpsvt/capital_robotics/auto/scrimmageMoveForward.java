package org.mrpsvt.capital_robotics.auto;

import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.BezierLine;
import com.pedropathing.geometry.Pose;
import com.pedropathing.paths.PathChain;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.seattlesolvers.solverslib.command.CommandOpMode;
import com.seattlesolvers.solverslib.command.SequentialCommandGroup;
import com.seattlesolvers.solverslib.pedroCommand.FollowPathCommand;

import org.firstinspires.ftc.teamcode.pedroPathing.Constants;
import org.mrpsvt.capital_robotics.robot_core.DriveBase;

@Autonomous
public abstract class scrimmageMoveForward extends CommandOpMode
{
    private Follower followerElement;
    private PathChain pathElement;
    private boolean stop;
    private double speed;
    private HardwareMap hardwareImport;

    //start and end points
    private Pose startPose = new Pose(0, 0, Math.toRadians(90));
    private Pose endPose = new Pose(48, 48, Math.toRadians(90));

    //gives context to the follower via hardwareMap, and makes the linear pathElement
    public void compilePathChain()
    {
        followerElement = Constants.createFollower(DriveBase.getHardwareMap());
        pathElement = followerElement.pathBuilder()
                .addPath(new BezierLine(startPose, endPose))
                .setLinearHeadingInterpolation(startPose.getHeading(), endPose.getHeading())
                .build();
    }

    @Override
    public void initialize()
    {
        //super.reset()
        followerElement.setStartingPose(startPose);
        compilePathChain();

        SequentialCommandGroup order = new SequentialCommandGroup
        (
                new FollowPathCommand(followerElement, pathElement).setGlobalMaxPower(0.5)
        );
    }

    @Override
    public void run()
    {
        super.run();
    }
}
