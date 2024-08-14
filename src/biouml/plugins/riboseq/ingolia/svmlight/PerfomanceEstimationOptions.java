package biouml.plugins.riboseq.ingolia.svmlight;

import java.util.ArrayList;
import java.util.List;

public class PerfomanceEstimationOptions implements Options
{
    /**
     * Compute leave-one-out estimates (default false)
     */
    private Boolean computeLeaveOneOutEstimates;
    public Boolean getComputeLeaveOneOutEstimates()
    {
        return computeLeaveOneOutEstimates;
    }
    public void setComputeLeaveOneOutEstimates(Boolean computeLeaveOneOutEstimates)
    {
        this.computeLeaveOneOutEstimates = computeLeaveOneOutEstimates;
    }

    /**
     * Value of rho for XiAlpha-estimator and for pruning
     * leave-one-out computation (default 1.0) 
     */
    private Double rho;
    public Double getRho()
    {
        return rho;
    }
    public void setRho(Double rho)
    {
        this.rho = rho;
    }
    
    /**
     * Search depth for extended XiAlpha-estimator (default 0)
     */
    private Integer searchDepth;
    public Integer getSearchDepth()
    {
        return searchDepth;
    }
    public void setSearchDepth(Integer searchDepth)
    {
        this.searchDepth = searchDepth;
    }
    
    @Override
    public List<String> getOptions()
    {
        List<String> options = new ArrayList<>();
        if(computeLeaveOneOutEstimates != null)
        {
            options.add( "-x" );
            options.add( computeLeaveOneOutEstimates ? "1" : "0" );
        }
        if(rho != null)
        {
            options.add( "-o" );
            options.add( String.valueOf( rho ) );
        }
        if(searchDepth != null)
        {
            options.add( "-k" );
            options.add( String.valueOf( searchDepth ) );
        }   
        return options;
    }
}
