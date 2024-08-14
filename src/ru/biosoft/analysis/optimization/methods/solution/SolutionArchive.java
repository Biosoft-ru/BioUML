package ru.biosoft.analysis.optimization.methods.solution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class SolutionArchive
{
    public int ARCHIVE_SIZE = 20;
    private List<Solution> solutionArchive = new ArrayList<>();

    public SolutionArchive()
    {
    }

    public SolutionArchive(int size)
    {
        ARCHIVE_SIZE = size;
    }

    public Solution get(int i)
    {
        if( solutionArchive.size() < i + 1 )
            return null;
        return solutionArchive.get(i);
    }

    public void add(Solution solution)
    {
        solutionArchive.add(solution);
    }

    public void remove(int i)
    {
        if( solutionArchive.size() < i + 1 )
            return;
        solutionArchive.remove(i);
    }

    public int size()
    {
        return solutionArchive.size();
    }

    public void sort(SolutionComparator.Status status)
    {
        Collections.sort(solutionArchive, new SolutionComparator(status));
    }

    public Solution getRandomSolution(Random random)
    {
        if( solutionArchive.size() == 1 )
            return solutionArchive.get(0);

        int ind1 = random.nextInt(solutionArchive.size() - 1);
        int ind2 = random.nextInt(solutionArchive.size() - 1);

        Solution one = solutionArchive.get(ind1);
        Solution two = solutionArchive.get(ind2);

        if( one.dominates(two) )
            return one;
        else if( two.dominates(one) )
            return two;
        else if( random.nextDouble() < 0.5 )
            return one;
        else
            return two;
    }

    public void refresh(Solution solution)
    {
        if( !isDominated(solution) )
        {
            solutionArchive.add(solution);
            if( solutionArchive.size() > ARCHIVE_SIZE )
            {
                distanceDistributionCrowding();
                sort(SolutionComparator.Status.CROWDING_DISTANCE);
                solutionArchive.remove(ARCHIVE_SIZE);
            }
        }
    }

    private boolean isDominated(Solution solution)
    {
        for( int i = 0; i < solutionArchive.size(); ++i )
        {
            Solution next = solutionArchive.get(i);

            if( next.getDistance() <= solution.getDistance() && next.getPenalty() <= solution.getPenalty() )
                return true;
            else if( solution.getDistance() <= next.getDistance() && solution.getPenalty() <= next.getPenalty() )
                solutionArchive.remove(i);
        }
        return false;
    }

    public void distanceDistributionCrowding()
    {
        if( solutionArchive != null && solutionArchive.size() != 0 )
        {
            for( int i = 0; i < solutionArchive.size(); ++i )
            {
                solutionArchive.get(i).setCrowdingDistance(0);
            }

            doCrowding(SolutionComparator.Status.OBJECTIVE);
            doCrowding(SolutionComparator.Status.PENALTY);
        }
    }

    private void doCrowding(SolutionComparator.Status status)
    {
        sort(status);

        Solution firstSolution = solutionArchive.get(0);
        Solution lastSolution = solutionArchive.get(solutionArchive.size() - 1);

        double minValue = firstSolution.getValue(status);
        double maxValue = lastSolution.getValue(status);

        firstSolution.setCrowdingDistance(Double.POSITIVE_INFINITY);
        lastSolution.setCrowdingDistance(Double.POSITIVE_INFINITY);

        for( int j = 1; j < solutionArchive.size() - 1; j++ )
        {
            if( solutionArchive.get(j).getCrowdingDistance() != Double.POSITIVE_INFINITY && maxValue != minValue )
            {
                double distance = solutionArchive.get(j + 1).getValue(status) - solutionArchive.get(j - 1).getValue(status);
                distance = distance / ( maxValue - minValue );
                distance += solutionArchive.get(j).getCrowdingDistance();
                solutionArchive.get(j).setCrowdingDistance(distance);
            }
        }
    }
}