package org.mrpsvt.capital_robotics.control;

import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.seattlesolvers.solverslib.gamepad.GamepadEx;

public class ControlMap {

    public GamepadEx driver1;
    public GamepadEx driver2;

    public ControlMap(Gamepad gamepad1, Gamepad gamepad2) {
        driver1 = new GamepadEx(gamepad1);
        driver2 = new GamepadEx(gamepad2);
    }
}
