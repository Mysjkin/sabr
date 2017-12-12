package com.ballthrower;

import com.ballthrower.abortion.AbortCode;
import com.ballthrower.abortion.IAbortable;
import com.ballthrower.communication.Connection;
import com.ballthrower.communication.ConnectionFactory;
import com.ballthrower.communication.packets.Packet;
import com.ballthrower.communication.packets.PacketIds;
import com.ballthrower.communication.packets.TargetInfoRequestPacket;
import com.ballthrower.exceptions.OutOfRangeException;
import com.ballthrower.listeners.ExitButtonListener;
import com.ballthrower.listeners.ShootButtonListener;
import com.ballthrower.movement.aiming.IRotator;
import com.ballthrower.movement.aiming.Rotator;
import com.ballthrower.movement.shooting.IShooter;
import com.ballthrower.movement.shooting.Shooter;
import com.ballthrower.targeting.DirectionCalculator;
import com.ballthrower.targeting.DistanceCalculator;
import com.ballthrower.targeting.ITargetContainer;
import com.ballthrower.targeting.TargetBox;
import com.ballthrower.targeting.policies.Policy;
import com.ballthrower.targeting.policies.PolicyFactory;
import lejos.nxt.Button;
import lejos.nxt.LCD;
import lejos.nxt.MotorPort;
import lejos.nxt.Sound;

// The Robot class uses the singleton pattern, since only one robot can be used.
public class Robot implements IAbortable
{
    private static Robot _robotInstance = new Robot();

    private static final float TARGET_ANGLE_THRESHOLD = 2.5f;

    private static final Button EXIT_BUTTON = Button.ESCAPE;
    private static final Button SHOOT_BUTTON = Button.ENTER;

    private DistanceCalculator _distanceCalculator;
    private DirectionCalculator _directionCalculator;

    private final IShooter _shooter;
    private final IRotator _rotator;

    private PolicyFactory.TargetingPolicyType _targetingPolicyType = PolicyFactory.TargetingPolicyType.Nearest;

    private Connection _connection;
    private ConnectionFactory.ConnectionType _connectionType = ConnectionFactory.ConnectionType.Bluetooth;

    public static Robot getInstance()
    {
        return Robot._robotInstance;
    }

    private Robot()
    {
        // Set up movement controllers with desired motors.
        this._rotator = new Rotator(MotorPort.C);
        this._shooter = new Shooter(new MotorPort[]{MotorPort.A, MotorPort.B});
    }

    public void addButtonListeners()
    {
        EXIT_BUTTON.addButtonListener(new ExitButtonListener());
        SHOOT_BUTTON.addButtonListener(new ShootButtonListener());
    }

    public void locateAndShoot()
    {
        /* Choose a policy using the policy factory. */
        Policy chosenPolicy = PolicyFactory.getPolicy(_targetingPolicyType);

        while (true)
        {
            ITargetContainer targetContainer = receiveTargetInformation();

            /* If there are no targets, we cannot proceed. */
            if (targetContainer == null || targetContainer.getTargetCount() == 0)
            {
                this.warn("No targets found.");
                return;
            }

            // Set up distance and direction calculator instances.
            /* TODO: Bad for memory to do this all the time. Unless GC works well. But we need to init direction calc. */
            this._distanceCalculator = new DistanceCalculator();
            this._directionCalculator = new DirectionCalculator(targetContainer);

            /* Get suggested target using the chosen policy. */
            TargetBox target = chosenPolicy.selectTargetBox(targetContainer);

            // Calculate the angle to the target object.
            float directionAngle = _directionCalculator.calculateDirection(target);
            if (Math.abs(directionAngle) > TARGET_ANGLE_THRESHOLD)
            {
                // We are not facing the target, so we must rotate towards it first.
                _rotator.turnDegrees(directionAngle);
            }
            else
            {
                try
                {
                    _shooter.shootDistance(_distanceCalculator.calculateDistance(target));
                }
                catch (OutOfRangeException ex)
                {
                    Sound.buzz();
                    //this.warn(ex.getMessage());
                }

                return;
            }
        }
    }

    private ITargetContainer receiveTargetInformation()
    {
        // Request target information
        this._connection.sendPacket(new TargetInfoRequestPacket());

        // Receive packet with target information
        Packet receivedPacket = this._connection.receivePacket();
        if (receivedPacket.getId() != PacketIds.TargetDirectionRequest)
            this.abort(AbortCode.UNKNOWN_PACKET, "Expected target information.");

        return ((TargetInfoRequestPacket)receivedPacket).getTargetBoxInfo();
    }

    private void closeConnection()
    {
        if (this._connection != null)
        {
            this._connection.closeConnection();
            this._connection = null;
        }
    }

    public void awaitConnection(ConnectionFactory connectionFactory)
    {
        // Close any existing connection
        this.closeConnection();

        // Instantiate the connection and await the connection from
        this._connection = connectionFactory.createInstance(_connectionType, this);
        this._connection.awaitConnection();
    }

    public void setTargetingPolicyType(PolicyFactory.TargetingPolicyType policyType)
    {
        this._targetingPolicyType = policyType;
    }

    public void setConnectionType(ConnectionFactory.ConnectionType connectionType)
    {
        this._connectionType = connectionType;
    }

    public void abort(AbortCode code)
    {
        abort(code, null);
    }

    public void abort(AbortCode code, String message)
    {
        if (code != AbortCode.MANUAL)
        {
            Sound.buzz();

            // Draw abort message
            LCD.clear();

            LCD.drawString("Robot abortion", 0, 0);
            LCD.drawString("Code: " + code, 0, 1);
            if (message != null && !message.isEmpty())
                LCD.drawString(message, 0, 2);

            // Await key press and exit system fully
            Button.waitForAnyPress();
        }

        System.exit(code.ordinal());
    }

    public void warn(String message)
    {
        Sound.beep();

        // Draw warning message
        LCD.clear();

        LCD.drawString("Robot warning", 0, 0);
        LCD.drawString(message, 0, 1);

        Button.waitForAnyPress();
        LCD.clear();
    }
}
