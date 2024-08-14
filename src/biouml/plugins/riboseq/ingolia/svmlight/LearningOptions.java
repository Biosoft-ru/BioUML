package biouml.plugins.riboseq.ingolia.svmlight;

import java.util.ArrayList;
import java.util.List;


public class LearningOptions implements Options
{
    public enum Mode
    {
        CLASSIFICATION ( 'c' ), REGRESSION ( 'r' ), PREFERENCE_RANKING ( 'p' );
        public final char optionValue;
        private Mode(char optionValue)
        {
            this.optionValue = optionValue;
        }
    }
    private Mode mode;
    public Mode getMode()
    {
        return mode;
    }
    public void setMode(Mode mode)
    {
        this.mode = mode;
    }

    /**
     * Trade-off between training error
     * and margin (default [avg. x*x]^-1)
     */
    private Double tradeOff;
    public Double getTradeOff()
    {
        return tradeOff;
    }

    public void setTradeOff(Double tradeOff)
    {
        this.tradeOff = tradeOff;
    }

    /**
     * Epsilon width of tube for regression
     */
    private Double epsilonWidth;
    public Double getEpsilonWidth()
    {
        return epsilonWidth;
    }
    public void setEpsilonWidth(Double epsilonWidth)
    {
        this.epsilonWidth = epsilonWidth;
    }

    /**
     * Cost-factor, by which training errors on
     * positive examples outweight errors on negative
     * examples (default 1)
     */
    private Double costFactor;
    public Double getCostFactor()
    {
        return costFactor;
    }
    public void setCostFactor(Double costFactor)
    {
        this.costFactor = costFactor;
    }

    /**
     * Use biased hyperplane (i.e. x*w+b0) instead
     * of unbiased hyperplane (i.e. x*w0) (default true)
     */
    private Boolean useBiasedHiperplane;
    public Boolean getUseBiasedHiperplane()
    {
        return useBiasedHiperplane;
    }
    public void setUseBiasedHiperplane(Boolean useBiasedHiperplane)
    {
        this.useBiasedHiperplane = useBiasedHiperplane;
    }
    
    /**
     * Remove inconsistent training examples
     * and retrain (default false)
     */
    private Boolean removeAndRetrain;
    public Boolean getRemoveAndRetrain()
    {
        return removeAndRetrain;
    }
    public void setRemoveAndRetrain(Boolean removeAndRetrain)
    {
        this.removeAndRetrain = removeAndRetrain;
    }

    @Override
    public List<String> getOptions()
    {
        List<String> options = new ArrayList<>();
        if(mode != null)
        {
            options.add( "-z" );
            options.add( String.valueOf( mode.optionValue ) );
        }
        if(tradeOff != null)
        {
            options.add( "-c" );
            options.add( String.valueOf( tradeOff ) );
        }
        if(epsilonWidth != null)
        {
            options.add( "-w" );
            options.add( String.valueOf( epsilonWidth ) );
        }
        if(costFactor != null)
        {
            options.add( "-j" );
            options.add( String.valueOf( costFactor ) );
        }
        if(useBiasedHiperplane != null)
        {
            options.add( "-b" );
            options.add( useBiasedHiperplane ? "1" : "0" );
        }
        if(removeAndRetrain != null)
        {
            options.add( "-i" );
            options.add( removeAndRetrain ? "1" : "0" );
        }
        return options;
    }
}
