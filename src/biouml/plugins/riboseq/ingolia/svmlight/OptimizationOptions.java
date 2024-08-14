package biouml.plugins.riboseq.ingolia.svmlight;

import java.util.ArrayList;
import java.util.List;

public class OptimizationOptions implements Options
{

    /**
     * Maximum size of QP-subproblems (default 10)
     */
    private Integer QPSubproblemMaxSize;
    public Integer getQPSubproblemMaxSize()
    {
        return QPSubproblemMaxSize;
    }
    public void setQPSubproblemMaxSize(Integer qPSubproblemMaxSize)
    {
        QPSubproblemMaxSize = qPSubproblemMaxSize;
    }
    
    /**
     * Number of new variables entering the working set
     * in each iteration (default n = q). Set n<q to prevent
     * zig-zagging. 
     */
    private Integer newVariablesCount;
    public Integer getNewVariablesCount()
    {
        return newVariablesCount;
    }
    public void setNewVariablesCount(Integer newVariablesCount)
    {
        this.newVariablesCount = newVariablesCount;
    }
    
    /**
     * Size of cache for kernel evaluations in MB (default 40)
     * The larger the faster...
     */
    private Integer cacheSize;
    public Integer getCacheSize()
    {
        return cacheSize;
    }
    public void setCacheSize(Integer cacheSize)
    {
        this.cacheSize = cacheSize;
    }
    
    /**
     *  Allow that error for termination criterion
     *  [y [w*x+b] - 1] = eps (default 0.001) 
     */
    private Double epsilon;
    public Double getEpsilon()
    {
        return epsilon;
    }
    public void setEpsilon(Double epsilon)
    {
        this.epsilon = epsilon;
    }
    
    /**
     * Number of iterations a variable needs to be
     * optimal before considered for shrinking (default 100) 
     */
    private Integer iterations;
    public Integer getIterations()
    {
        return iterations;
    }
    public void setIterations(Integer iterations)
    {
        this.iterations = iterations;
    }
    
    /**
     * Do final optimality check for variables removed by
     * shrinking. Although this test is usually positive, there
     * is no guarantee that the optimum was found if the test is
     * omitted. (default true) 
     */
    private Boolean finalCheck;
    public Boolean getFinalCheck()
    {
        return finalCheck;
    }
    public void setFinalCheck(Boolean finalCheck)
    {
        this.finalCheck = finalCheck;
    }
    
    /**
     * Terminate optimization, if no progress after this
     * number of iterations. (default 100000)
     */
    private Integer maxIterations;
    public Integer getMaxIterations()
    {
        return maxIterations;
    }
    public void setMaxIterations(Integer maxIterations)
    {
        this.maxIterations = maxIterations;
    }
    
    @Override
    public List<String> getOptions()
    {
        List<String> options = new ArrayList<>();
        if(QPSubproblemMaxSize != null)
        {
            options.add( "-q" );
            options.add( QPSubproblemMaxSize.toString() );
        }
        if(newVariablesCount != null)
        {
            options.add( "-n" );
            options.add( newVariablesCount.toString() );
        }
        if(cacheSize != null)
        {
            options.add( "-m" );
            options.add( cacheSize.toString() );
        }
        if(epsilon != null)
        {
            options.add( "-e" );
            options.add( epsilon.toString() );
        }
        if(iterations != null)
        {
            options.add( "-h" );
            options.add( iterations.toString() );
        }
        if(finalCheck != null)
        {
            options.add( "-f" );
            options.add( finalCheck ? "1" : "0" );
        }
        if(maxIterations != null)
        {
            options.add( "-#" );
            options.add( maxIterations.toString() );
        }
        return options;
    }

}
