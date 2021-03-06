package com.sabr;

import com.sabr.abortion.AbortCode;
import com.sabr.abortion.IAbortable;
import com.sabr.communication.Connection;
import com.sabr.communication.ConnectionFactory;
import com.sabr.communication.packets.DebugPacket;
import com.sabr.communication.packets.Packet;
import com.sabr.communication.packets.PacketIds;
import com.sabr.communication.packets.TargetInfoRequestPacket;
import com.sabr.exceptions.OutOfRangeException;
import com.sabr.listeners.ExitButtonListener;
import com.sabr.listeners.ShootButtonListener;
import com.sabr.movement.aiming.IRotator;
import com.sabr.movement.aiming.Rotator;
import com.sabr.movement.shooting.IShooter;
import com.sabr.movement.shooting.Shooter;
import com.sabr.targeting.DirectionCalculator;
import com.sabr.targeting.DistanceCalculator;
import com.sabr.targeting.ITargetContainer;
import com.sabr.targeting.TargetBox;
import com.sabr.targeting.policies.Policy;
import com.sabr.targeting.policies.PolicyFactory;
import lejos.nxt.Button;
import lejos.nxt.LCD;
import lejos.nxt.MotorPort;
import lejos.nxt.Sound;
import java.util.Random;

import java.io.File;

// The Robot class uses the singleton pattern, since only one robot can be used.
public class Robot implements IAbortable
{
    private static final String CONNECTED_SOUND = "connected.wav";
    private static final String ERROR_SOUND = "error.wav";

    private static Robot _robotInstance = new Robot();

    private static final float TARGET_ANGLE_MAX_DEVIATION = 0.70f;

    private static final Button EXIT_BUTTON = Button.ESCAPE;
    private static final Button SHOOT_BUTTON = Button.ENTER;

    private final IShooter _shooter;
    private final IRotator _rotator;

    private PolicyFactory.TargetingPolicyType _targetingPolicyType = PolicyFactory.TargetingPolicyType.Nearest;

    private Connection _connection;
    private ConnectionFactory.ConnectionType _connectionType = ConnectionFactory.ConnectionType.Bluetooth;

    private boolean _debug = false;

    public static Robot getInstance()
    {
        return Robot._robotInstance;
    }

    public final Random Random = new Random();

    private Robot()
    {
        /* Set up movement controllers with desired motors. */
        this._rotator = new Rotator(MotorPort.C);
        this._shooter = new Shooter(new MotorPort[]{MotorPort.A, MotorPort.B});
    }

    public boolean isDebug()
    {
        return this._debug;
    }

    public void setDebug(boolean debug)
    {
        this._debug = debug;
    }

    public void addButtonListeners()
    {
        EXIT_BUTTON.addButtonListener(new ExitButtonListener());
        SHOOT_BUTTON.addButtonListener(new ShootButtonListener());
    }

    public void locateAndShoot()
    {
        /* Do not send packets if we are not connected. */
        if (_connection == null || !_connection.isConnected())
            return;

        /* Choose a policy using the policy factory. */
        Policy chosenPolicy = PolicyFactory.getPolicy(_targetingPolicyType);

        int numRotations = 0;
        while (true)
        {
            ITargetContainer targetContainer = receiveTargetInformation();

            /* If there are no targets, we cannot proceed. */
            if (targetContainer.getTargetCount() == 0)
            {
                this.warn("No targets found.");
                return;
            }

            /* Get suggested target using the chosen policy. */
            TargetBox target = chosenPolicy.selectTargetBox(targetContainer);

            // Calculate the angle to the target object.
            float directionAngle = DirectionCalculator.calculateDirection(targetContainer, target);
            if (Math.abs(directionAngle) > TARGET_ANGLE_MAX_DEVIATION)
            {
                /* We are not facing the target, so we must rotate towards it first. */
                _rotator.turnDegrees(directionAngle);
                numRotations++;
            }
            else
            {
                try
                {
                    float distance = DistanceCalculator.calculateDistance(target);
                    _shooter.shootDistance(distance);

                    /* If debugging, output final departure angle and number of rotations. */
                    if (this._debug)
                        this.sendDebugMessage("r: " + numRotations + ", a: " + directionAngle + ", d: " + distance);
                }
                catch (OutOfRangeException ex)
                {
                    this.warn(ex.getMessage());
                }

                return;
            }
        }
    }

    private ITargetContainer receiveTargetInformation()
    {
        /* Request target information. */
        this._connection.sendPacket(new TargetInfoRequestPacket());

        /* Receive packet with target information. */
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

    public void sendDebugMessage(String message)
    {
        if (_connection == null || !_connection.isConnected())
            return;

        this._connection.sendPacket(new DebugPacket(message));
    }

    public void awaitConnection(ConnectionFactory connectionFactory)
    {
        LCD.drawString("Awaiting...", 0, 0);

        /* Close any existing connection. */
        this.closeConnection();

        /* Instantiate the connection and await the connection from host. */
        this._connection = connectionFactory.createInstance(_connectionType, this);
        this._connection.awaitConnection();

        LCD.clear();
        LCD.drawString("Connected", 0, 0);

        if (_debug)
            this.sendDebugMessage("Connection established! Debugging is enabled.");

        Sound.playSample(new File(CONNECTED_SOUND));
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
            Sound.playSample(new File(ERROR_SOUND));

            LCD.clear();
            LCD.drawString("Robot abortion", 0, 0);
            LCD.drawString("Code: " + code, 0, 1);
            if (message != null && !message.isEmpty())
                LCD.drawString(message, 0, 2);

            /* Send error to host. */
            if (message == null)
                this.sendDebugMessage(code.toString());
            else
                this.sendDebugMessage(message);

            /* Await key press and exit system fully. */
            Button.waitForAnyPress();
        }

        System.exit(code.ordinal());
    }

    public void warn(String message)
    {
        Sound.beepSequence();

        LCD.clear();
        LCD.drawString("Robot warning", 0, 0);
        LCD.drawString(message, 0, 1);

        /* Send warning to host. */
        this.sendDebugMessage(message);

        Button.waitForAnyPress();
        LCD.clear();
    }
}
