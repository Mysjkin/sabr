package com.sabr.movement.shooting;

import java.lang.Math;

import com.sabr.exceptions.OutOfRangeException;

import com.sabr.movement.MotorController;
import lejos.nxt.*;
import lejos.robotics.RegulatedMotor;

public class Shooter extends MotorController implements IShooter
{
    private RegulatedMotor regMotor;

    /* DEBUGGING */
    public float rawPower = 0;
    public float compPower = 0;
    public float compFactor = 0;

    private final boolean _direction = false;

    public Shooter(MotorPort[] motors)
    {
        super(new NXTMotor(motors[0]), new NXTMotor(motors[1]), 4.630f);
        regMotor = new NXTRegulatedMotor(motors[0]);
    }

    private int getPowerLinear(float distance)
    {
        /* distance=  78.102 + 0.807 * power
         * giving...
         * power = (distance - 78.102) / 0.802
         * */

        float correctedDistance = distance + 11.5f;
        float rawPower = (correctedDistance - 78.102f) / 0.802f;

        int theoreticalMaxSpeed = 900; /* 9V * approx. 100 */
        float compensationFactor = theoreticalMaxSpeed / regMotor.getMaxSpeed();

        return (int) Math.round(rawPower * compensationFactor);
    }

    public void shootDistance(float distance)throws OutOfRangeException
    {
        int power = getPowerLinear(distance);

        // Check if target is out of range
        if (power > 100)
            throw new OutOfRangeException("Target too far.");
        else if (power < 45)
            throw new OutOfRangeException("Target too close.");

        // Run motors
        int degrees = (int) (180 / getGearRatio());

        super.startMotors(power, _direction);
        super.waitWhileTurning(degrees);
        super.resetTacho();

        resetMotors();
    }

    private void resetMotors()
    {
        /* Move in opposite direction */
        super.startMotors(15, !_direction);

        /* 180 degrees should be enough */
        waitWhileTurning((int)(180 / getGearRatio()));

        /* Stop motors, reset tacho count */
        super.stopMotors();
        super.resetTacho();
    }

    /* ONLY FOR DEBUGGING -- REMOVE REMOVE REMOVE */
    public void shootAtPower(int power)
    {
        rawPower = power;

        float compensationFactor = 900 / regMotor.getMaxSpeed();
        compFactor = compensationFactor;

        int realPower = (int) Math.round(power * compensationFactor);
        compPower = power * compensationFactor;

        int degrees = (int) Math.round(180 / getGearRatio());

        super.startMotors(power, _direction);
        super.waitWhileTurning(degrees);
        super.resetTacho();

        resetMotors();
    }
}
