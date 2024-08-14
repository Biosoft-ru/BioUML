package ru.biosoft.analysis.optimization.methods.solution;

public class Solution
{
    public Solution(double[] values, double distance, double penalty)
    {
        this.values = values;
        this.distance = distance;
        this.penalty = penalty;
    }

    private double[] values;
    public double[] getValues()
    {
        return this.values;
    }

    private double distance;
    public double getDistance()
    {
        return this.distance;
    }

    private double penalty;
    public double getPenalty()
    {
        return this.penalty;
    }

    private double crowdingDistance = 0.0;
    public double getCrowdingDistance()
    {
        return this.crowdingDistance;
    }
    public void setCrowdingDistance(double distance)
    {
        this.crowdingDistance = distance;
    }

    public double getValue(SolutionComparator.Status status)
    {
        switch( status )
        {
            case OBJECTIVE:
                return distance;
            case PENALTY:
                return penalty;
            default:
                return 0;
        }
    }

    public boolean dominates(Solution solution)
    {
        if( ( penalty < solution.getPenalty() && distance < solution.getDistance() )
                || ( penalty == 0.0 && solution.getPenalty() == 0.0 && distance < solution.getDistance() ) )
            return true;
        return false;
    }
}
