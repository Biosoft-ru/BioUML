package biouml.plugins.riboseq.ingolia.svmlight;

import java.util.ArrayList;
import java.util.List;

public class TransductionOptions implements Options
{
    /**
     * Fraction of unlabeled examples to be classified
     * into the positive class (default is the ratio of
     * positive and negative examples in the training data)
     */
    private Double positiveClassFraction;
    public Double getPositiveClassFraction()
    {
        return positiveClassFraction;
    }
    public void setPositiveClassFraction(Double positiveClassFraction)
    {
        this.positiveClassFraction = positiveClassFraction;
    }

    @Override
    public List<String> getOptions()
    {
        List<String> options = new ArrayList<>();
        if( positiveClassFraction != null )
        {
            options.add( "-p" );
            options.add( String.valueOf( positiveClassFraction ) );
        }
        return options;
    }
}
