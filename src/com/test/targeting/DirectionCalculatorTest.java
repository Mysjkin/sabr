package com.test.targeting;

import com.sabr.targeting.DirectionCalculator;
import com.sabr.targeting.ITargetContainer;
import com.sabr.targeting.TargetContainer;
import com.sabr.exceptions.AssertException;
import com.test.NXTAssert;
import com.test.NXTTest;
import com.test.Test;

public class DirectionCalculatorTest extends Test
{
    private ITargetContainer tbi;
    private TargetContainer testContainer;

    private void setUp()
    {
        testContainer = NXTTest.getTestTargetBox();
        tbi = NXTTest.getTestTargetBox();
    }

    private void calculateDirectionTest() throws AssertException
    {
        float maxAngle = 26.725F;
        float frameMiddle = testContainer.getFrameWidth() / 2;
        float degreesPerPixel = maxAngle / frameMiddle;
        float degreesToTurn = testContainer.getTarget((byte)0).getMiddleX() * degreesPerPixel;

        NXTAssert test = new NXTAssert();
        test.assertThat(degreesToTurn, "CalculateDirection")
                .isEqualToFloat(DirectionCalculator.calculateDirection(testContainer, testContainer.getTarget((byte)0)));
    }

    public void runAllTests() throws AssertException
    {
        setUp();
        calculateDirectionTest();
    }
}

