package biouml.plugins.expression;

import java.util.Properties;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

/**
 * Parameters for {@link ExpressionFilterAnalysis} analysis
 * @author lan
 */
@SuppressWarnings ( "serial" )
public class ExpressionFilterAnalysisParameters extends AbstractAnalysisParameters
{
    private DataElementPath inputDiagram, outputDiagram;
    private ExpressionFilterProperties filterProperties;

    public ExpressionFilterAnalysisParameters()
    {
        filterProperties = new ExpressionFilter().getProperties();
        filterProperties.setLoading( false );
        filterProperties.setParent(this);
    }

    /**
     * @return the inputDiagram
     */
    public DataElementPath getInputDiagram()
    {
        return inputDiagram;
    }

    /**
     * @param inputDiagram the inputDiagram to set
     */
    public void setInputDiagram(DataElementPath inputDiagram)
    {
        Object oldValue = this.inputDiagram;
        this.inputDiagram = inputDiagram;
        firePropertyChange("inputDiagram", oldValue, inputDiagram);
    }

    /**
     * @return the outputDiagram
     */
    public DataElementPath getOutputDiagram()
    {
        return outputDiagram;
    }

    /**
     * @param outputDiagram the outputDiagram to set
     */
    public void setOutputDiagram(DataElementPath outputDiagram)
    {
        Object oldValue = this.outputDiagram;
        this.outputDiagram = outputDiagram;
        firePropertyChange("outputDiagram", oldValue, outputDiagram);
    }

    /**
     * @return the filterProperties
     */
    public ExpressionFilterProperties getFilterProperties()
    {
        return filterProperties;
    }

    /**
     * @param filterProperties the filterProperties to set
     */
    public void setFilterProperties(ExpressionFilterProperties filterProperties)
    {
        Object oldValue = this.filterProperties;
        this.filterProperties = filterProperties;
        this.filterProperties.setParent( this );
        firePropertyChange("filterProperties", oldValue, filterProperties);
    }

    @Override
    public void read(Properties properties, String prefix)
    {
        super.read( properties, prefix );
        this.filterProperties.setLoading( false );
    }
}
