package com.sabr.targeting.policies;

import com.sabr.targeting.ITargetContainer;
import com.sabr.targeting.TargetBox;
import com.sabr.utilities.ArrayUtil;

import java.util.Comparator;

public class LeastRotationPolicy extends Policy
{
    @Override
    public TargetBox selectTargetBox(ITargetContainer targetContainer)
    {
        if (targetContainer.getTargetCount() == 0)
            return null;
        else if (targetContainer.getTargetCount() == 1)
            return targetContainer.getTarget((byte) 0);

        /* This policy sorts targets by their middle x-position.
           We want the lowest of such values, as that is the target requiring the least rotation. */
        TargetBox[] clonedArray = targetContainer.cloneTargets();
        ArrayUtil.sort(clonedArray, new RelativeDistanceComparator(targetContainer));

        return clonedArray[0];
    }

    /* Comparator for measuring the distance relative to the middle. */
    class RelativeDistanceComparator implements Comparator<TargetBox>
    {
        private final float _frameMiddle;

        RelativeDistanceComparator(ITargetContainer container)
        {
            this._frameMiddle = container.getFrameWidth() / 2;
        }

        @Override
        public int compare(TargetBox first, TargetBox second)
        {
            Float firstValue  = Math.abs(first.getMiddleX() - this._frameMiddle);
            Float secondValue = Math.abs(second.getMiddleX() - this._frameMiddle);

            return firstValue.compareTo(secondValue);
        }
    }
}