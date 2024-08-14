package biouml.plugins.optimization.analysis;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.analysis.optimization.OptimizationMethod;
import ru.biosoft.analysis.optimization.methods.ASAOptMethod;
import ru.biosoft.analysis.optimization.methods.GlbSolveOptMethod;
import ru.biosoft.analysis.optimization.methods.GoldfeldOptMethod;
import ru.biosoft.analysis.optimization.methods.MOCellOptMethod;
import ru.biosoft.analysis.optimization.methods.MOPSOOptMethod;
import ru.biosoft.analysis.optimization.methods.SRESOptMethod;

@SuppressWarnings ( "serial" )
@PropertyName ( "Optimization method parameters" )
public class OptMethodWrapper extends Option
{
    private static final String ASA = "Adaptive simulated annealing";
    private static final String MOCELL = "Cellular genetic algorithm";
    private static final String SRES = "Evolution strategy (SRES)";
    private static final String GLBSOLVE = "GLBSOLVE";
    private static final String MOPSO = "Particle swarm optimization";
    private static final String GOLDFELD = "Quadratic Hill-climbing";

    public OptMethodWrapper(Option parent)
    {
        super( parent );
    }

    public static String[] getAvailableMethodNames()
    {
        return new String[] {ASA, MOCELL, SRES, GLBSOLVE, MOPSO, GOLDFELD};
    }

    public OptimizationMethod<?> getOptMethod()
    {
        switch( optMethodName )
        {
            case ASA:
                ASAOptMethod asa = new ASAOptMethod( null, "asa" );
                asa.getParameters().setDelta( delta );
                return asa;
            case MOCELL:
                MOCellOptMethod mocell = new MOCellOptMethod( null, "mocell" );
                mocell.getParameters().setMaxIterations( optimizationIterations );
                mocell.getParameters().setGridLength( gridLength );
                mocell.getParameters().setGridWidth( gridWidth );
                return mocell;
            case SRES:
                SRESOptMethod sres = new SRESOptMethod( null, "sres" );
                sres.getParameters().setNumOfIterations( optimizationIterations );
                sres.getParameters().setSurvivalSize( populationSize );
                return sres;
            case GLBSOLVE:
                GlbSolveOptMethod glb = new GlbSolveOptMethod( null, "glb" );
                glb.getParameters().setNumOfIterations( optimizationIterations );
                return glb;
            case MOPSO:
                MOPSOOptMethod mopso = new MOPSOOptMethod( null, "mopso" );
                mopso.getParameters().setNumberOfIterations( optimizationIterations );
                mopso.getParameters().setParticleNumber( particleNumber );
                return mopso;
            case GOLDFELD:
                GoldfeldOptMethod goldfeld = new GoldfeldOptMethod( null, "goldfeld" );
                goldfeld.getParameters().setDeltaOutside( delta );
                return goldfeld;
            default:
                return new SRESOptMethod( null, "sres" );
        }
    }

    private String optMethodName = SRES;
    private int optimizationIterations = 500;
    private int populationSize = 20;
    private double delta = 1.0E-9;
    private int gridLength = 5;
    private int gridWidth = 4;
    private int particleNumber = 50;

    @PropertyName ( "Optimization method name" )
    @PropertyDescription ( "Name of optimization method to use" )
    public String getOptMethodName()
    {
        return optMethodName;
    }
    public void setOptMethodName(String optMethodName)
    {
        Object oldValue = this.optMethodName;
        this.optMethodName = optMethodName;
        resetDefaults( optMethodName );
        firePropertyChange( "optMethodName", oldValue, optMethodName );
        firePropertyChange( "*", null, null );
    }
    private void resetDefaults(String optMethodName)
    {
        switch( optMethodName )
        {
            case ASA:
                delta = 1.0E-9;
                break;
            case MOCELL:
                optimizationIterations = 500;
                gridLength = 5;
                gridWidth = 4;
                break;
            case SRES:
                optimizationIterations = 500;
                populationSize = 20;
                break;
            case GLBSOLVE:
                optimizationIterations = 300;
                break;
            case MOPSO:
                optimizationIterations = 1000;
                particleNumber = 50;
                break;
            case GOLDFELD:
                delta = 1.0E-4;
                break;
            default:
                break;
        }
    }

    @PropertyName ( "Number of iterations" )
    @PropertyDescription ( "Number of iterations during optimization" )
    public int getOptimizationIterations()
    {
        return optimizationIterations;
    }
    public void setOptimizationIterations(int optimizationIterations)
    {
        int oldValue = this.optimizationIterations;
        this.optimizationIterations = optimizationIterations;
        firePropertyChange( "optimizationIterations", oldValue, this.optimizationIterations );
    }
    public boolean isOptItersHidden()
    {
        return ! ( MOCELL.equals( optMethodName ) || SRES.equals( optMethodName ) || GLBSOLVE.equals( optMethodName )
                || MOPSO.equals( optMethodName ) );
    }

    @PropertyName ( "Populations size" )
    public int getPopulationSize()
    {
        return populationSize;
    }
    public void setPopulationSize(int populationSize)
    {
        int oldValue = this.populationSize;
        this.populationSize = populationSize;
        firePropertyChange( "populationSize", oldValue, this.populationSize );
    }
    public boolean isPopulationSizeHidden()
    {
        return !SRES.equals( optMethodName );
    }

    @PropertyName ( "Calculation accuracy" )
    public double getDelta()
    {
        return delta;
    }
    public void setDelta(double delta)
    {
        double oldValue = this.delta;
        this.delta = delta;
        firePropertyChange( "delta", oldValue, this.delta );
    }
    public boolean isDeltaHidden()
    {
        return ! (ASA.equals( optMethodName ) || GOLDFELD.equals( optMethodName ) );
    }

    @PropertyName ( "Grid length" )
    public int getGridLength()
    {
        return gridLength;
    }
    public void setGridLength(int gridLength)
    {
        int oldValue = this.gridLength;
        this.gridLength = gridLength;
        this.particleNumber = gridLength * gridWidth;
        firePropertyChange( "gridLength", oldValue, gridLength );
    }
    @PropertyName ( "Grid width" )
    public int getGridWidth()
    {
        return gridWidth;
    }
    public void setGridWidth(int gridWidth)
    {
        int oldValue = this.gridWidth;
        this.gridWidth = gridWidth;
        this.particleNumber = gridLength * gridWidth;
        firePropertyChange( "gridWidth", oldValue, gridWidth );
    }
    public boolean isGridHidden()
    {
        return !MOCELL.equals( optMethodName );
    }

    @PropertyName ( "Number of particles" )
    @PropertyDescription ( "The total number of particles" )
    public int getParticleNumber()
    {
        return this.particleNumber;
    }
    public void setParticleNumber(int particleNumber)
    {
        int oldValue = this.particleNumber;
        this.particleNumber = particleNumber;
        firePropertyChange( "particleNumber", oldValue, particleNumber );
    }
    public boolean isParticlesHidden()
    {
        return !MOPSO.equals( optMethodName );
    }
}
