package Movement.Shooting;
import java.lang.Math;
import lejos.nxt.

/**
 * Created by Thomas Buhl on 17/10/2017.
 */
public class Shooter implements IShooter
{
    private static final double g = 9.8;
    private static final int departureAngle = 45;
    private double factor = 1;
    private static NXTMotor motorA = new NXTMotor(MotorPort.A);
    private static NXTMotor motorB = new NXTMotor(MotorPort.B);
    private byte Gears = 3;
    private int[] gearSizes = {40, 24};


    public Shooter()
    {

    }

    private double getInitialVelocity(Float distance)
    {
        // Calculate and return the required initial velocity given the target distance, gravity and departure angle.
        return Math.sqrt((distance * g)/Math.sin(2*Math.toRadians(departureAngle)));
    }

    private int getPower(double velocity)
    {
        float maxDistance = 4;
        double maxVelocity = getInitialVelocity(maxDistance);

        // Calculate power as a direct linear function.
        int power = (velocity < maxVelocity)? 100 * (int)(velocity / maxVelocity): 100;

        return power;
    }

    private double getGearFactor()
    {
        return Math.pow(gearSizes[0]/gearSizes[1], Gears);
    }


    @Override
    public void Shoot(Float distance)
    {
        double initialVelocity = getInitialVelocity(distance);
        int power = getPower(initialVelocity);

        // Debug.Log(This, factor, power);


        // ready motors
        motorA.setPower(power);
        motorB.setPower(power);

        int degrees = (int)(360 / getGearFactor());

        // start motors
        if (evenGears)
        {
            motorA.forward();
            motorA.forward();
        }
        else
        {
            motorA.backward();
            motorA.backward();
        }

        while( Math.abs(motorA.getTachoCount()) < degrees ){}

        LCD.drawString("Power: " + power + ".", 0, 0);
        LCD.drawString("InitialVel: " + initialVelocity + ", ", 0, 5);
    }
}
