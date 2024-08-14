package biouml.plugins.fbc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.math3.exception.TooManyIterationsException;
import org.apache.commons.math3.optim.MaxIter;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.linear.LinearConstraint;
import org.apache.commons.math3.optim.linear.LinearConstraintSet;
import org.apache.commons.math3.optim.linear.LinearObjectiveFunction;
import org.apache.commons.math3.optim.linear.NoFeasibleSolutionException;
import org.apache.commons.math3.optim.linear.NonNegativeConstraint;
import org.apache.commons.math3.optim.linear.SimplexSolver;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ApacheModel implements FbcModel
{
    /**
     * f is linear objective function, it contains coefficients for reaction and constant term;
     * constraints are all conditions for reactions (set of upper and lower bounds) and
     * the system of mass balance equations at steady state. Linear constraint contains coefficients,
     * sign and constant term;
     * GoalType is type FBC (min or max);
     * nonNegative is conditions when all flaxes are non negative;
     * optimValues is solution of problem;
     */
    private final int maxIter;
    private final String[] reactionNames;
    private PointValuePair solution;
    private final HashMap<String, Double> optimValues;
    private final LinearObjectiveFunction f;
    private final Collection<LinearConstraint> constraints;
    private final GoalType type;
    private final boolean nonNegative;

    protected Logger log = Logger.getLogger( ApacheModel.class.getName() );

    protected ApacheModel(LinearObjectiveFunction f, Collection<LinearConstraint> constraints, String[] reactionNames, GoalType type,
            int maxIter, boolean nonNegative)
    {
        int lengthVector = f.getCoefficients().toArray().length;
        for( LinearConstraint linearConstraint : constraints )
        {
            if( linearConstraint.getCoefficients().toArray().length != lengthVector )
                throw new IllegalArgumentException();
        }
        this.reactionNames = reactionNames;
        optimValues = new HashMap<>();

        this.f = f;
        this.constraints = constraints;
        this.type = type;
        this.maxIter = maxIter;
        this.nonNegative = nonNegative;
    }

    public Collection<LinearConstraint> getConstraints()
    {
        return constraints;
    }

    public void setLogLevel(Level level)
    {
        log.setLevel( level );
    }

    @Override
    public void optimize()
    {
        try
        {
            SimplexSolver solver = new SimplexSolver();
            long time = System.currentTimeMillis();
            solution = solver.optimize( new MaxIter( maxIter ), f, new LinearConstraintSet( constraints ), type,
                    new NonNegativeConstraint( nonNegative ) );
            log.info( "Optimal value are calculated(Flux balance analysis), time: " + ( System.currentTimeMillis() - time ) + "ms" );
            for( int i = 0; i < reactionNames.length; i++ )
                optimValues.put( reactionNames[i], solution.getPoint()[i] );
        }
        catch( TooManyIterationsException e )
        {
            for( int i = 0; i < reactionNames.length; i++ )
                optimValues.put( reactionNames[i], Double.NaN );
            log.log( Level.SEVERE, "Iterations limit exceeded, solution was not found." );
        }
        catch( NoFeasibleSolutionException e )
        {
            for( int i = 0; i < reactionNames.length; i++ )
                optimValues.put( reactionNames[i], Double.NaN );
            log.log( Level.SEVERE, "No feasible solution for given problem." );
        }
    }

    @Override
    public double getOptimValue(String reactionName)
    {
        return optimValues.get( reactionName );
    }

    @Override
    public double getValueObjFunc()
    {
        try
        {
            if( solution == null )
                return Double.NaN;
            return solution.getValue();
        }
        catch( Exception e )
        {
            return Double.NaN;
        }
    }

    @Override
    public String[] getReactionNames()
    {
        return reactionNames;
    }

    @Override
    public ApacheModel clone()
    {
        LinearObjectiveFunction newF = new LinearObjectiveFunction( f.getCoefficients().copy(), 0 );
        List<LinearConstraint> newConstraints = new ArrayList<>();
        for (LinearConstraint lc: constraints)
            newConstraints.add( new LinearConstraint( lc.getCoefficients().copy(), lc.getRelationship(), lc.getValue() ) );
        return new ApacheModel( newF, newConstraints, reactionNames.clone(), type, maxIter, nonNegative );
    }
}
