package ru.biosoft.analysis.optimization.methods.solution;

import java.util.Comparator;

public class SolutionComparator implements Comparator<Solution>
{
    public SolutionComparator(Status status)
    {
        this.status = status;
    }

    private Status status = Status.OBJECTIVE;

    @Override
    public int compare(Solution solution1, Solution solution2)
    {
        if( solution1 == null )
            return 1;
        else if( solution2 == null )
            return -1;

        double value1 = 0;
        double value2 = 0;

        if( status.equals(Status.OBJECTIVE) )
        {
            //Sorting in increasing order
            value1 = solution1.getDistance();
            value2 = solution2.getDistance();
        }
        else if( status.equals(Status.PENALTY) )
        {
            //Sorting in increasing order
            value1 = solution1.getPenalty();
            value2 = solution2.getPenalty();
        }
        else if( status.equals(Status.CROWDING_DISTANCE) )
        {
            //Sorting in decreasing order
            value2 = solution1.getCrowdingDistance();
            value1 = solution2.getCrowdingDistance();
        }

        return Double.compare( value1, value2 );
    }

    public enum Status
    {
        PENALTY, OBJECTIVE, CROWDING_DISTANCE;
    }
}