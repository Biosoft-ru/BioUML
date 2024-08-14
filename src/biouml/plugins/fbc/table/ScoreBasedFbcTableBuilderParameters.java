package biouml.plugins.fbc.table;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

@SuppressWarnings ( "serial" )
@PropertyName ( "Score based FBC table builder parameters" )
public class ScoreBasedFbcTableBuilderParameters extends AbstractAnalysisParameters
{
    public final static String NONE = "(none)";

    private DataElementPath inputDiagram;
    private DataElementPath inputEnzymes;
    private DataElementPath objectiveTable;
    private DataElementPath outputPath;
    private String scoreColumnName;
    private String maxColumnName;
    private String objectiveColumnName;
    private boolean norm = false;
    private boolean correlation = false;

    @PropertyName ( "Enzymes table" )
    @PropertyDescription ( "Path to table with enzymes" )
    public DataElementPath getInputEnzymes()
    {
        return inputEnzymes;
    }
    public void setInputEnzymes(DataElementPath inputEnzymes)
    {
        Object oldValue = this.inputEnzymes;
        this.inputEnzymes = inputEnzymes;
        firePropertyChange( "inputEnzymes", oldValue, this.inputEnzymes );
    }

    @PropertyName ( "Diagram" )
    @PropertyDescription ( "Path to diagram" )
    public DataElementPath getInputDiagram()
    {
        return inputDiagram;
    }
    public void setInputDiagram(DataElementPath inputDiagram)
    {
        Object oldValue = this.inputDiagram;
        this.inputDiagram = inputDiagram;
        firePropertyChange( "inputDiagram", oldValue, this.inputDiagram );
    }

    @PropertyName ( "Objective function table" )
    @PropertyDescription ( "Table with objective function values" )
    public DataElementPath getObjectiveTable()
    {
        return objectiveTable;
    }
    public void setObjectiveTable(DataElementPath objectiveTable)
    {
        Object oldValue = this.objectiveTable;
        this.objectiveTable = objectiveTable;
        firePropertyChange( "objectiveTable", oldValue, this.objectiveTable );
    }

    @PropertyName ( "Output table" )
    @PropertyDescription ( "Path to output table with FBA data table" )
    public DataElementPath getOutputPath()
    {
        return outputPath;
    }
    public void setOutputPath(DataElementPath outputPath)
    {
        Object oldValue = this.outputPath;
        this.outputPath = outputPath;
        firePropertyChange( "outputPath", oldValue, this.outputPath );
    }

    @PropertyName ( "Score column" )
    @PropertyDescription ( "Name of the column with score from table with enzymes" )
    public String getScoreColumnName()
    {
        return scoreColumnName;
    }
    public void setScoreColumnName(String scoreColumnName)
    {
        Object oldValue = this.scoreColumnName;
        this.scoreColumnName = scoreColumnName;
        firePropertyChange( "scoreColumnName", oldValue, this.scoreColumnName );
    }

    @PropertyName ( "Max column" )
    @PropertyDescription ( "Name of the column with values for flux upper bound" )
    public String getMaxColumnName()
    {
        return maxColumnName;
    }
    public void setMaxColumnName(String maxColumnName)
    {
        Object oldValue = this.maxColumnName;
        this.maxColumnName = maxColumnName;
        firePropertyChange( "maxColumnName", oldValue, this.maxColumnName );
    }

    @PropertyName ( "Objective column" )
    @PropertyDescription ( "Name of the column with values of objective function" )
    public String getObjectiveColumnName()
    {
        return objectiveColumnName;
    }
    public void setObjectiveColumnName(String objectiveColumnName)
    {
        Object oldValue = this.objectiveColumnName;
        this.objectiveColumnName = objectiveColumnName;
        firePropertyChange( "objectiveColumnName", oldValue, this.objectiveColumnName );
    }

    @PropertyName ( "Is score correlation" )
    @PropertyDescription ( "Shows if enzymes score is correlation" )
    public boolean isCorrelation()
    {
        return correlation;
    }
    public void setCorrelation(boolean isCorrelation)
    {
        boolean oldValue = this.correlation;
        this.correlation = isCorrelation;
        firePropertyChange( "correlation", oldValue, this.correlation );
    }

    @PropertyName ( "Normalize bounds" )
    @PropertyDescription ( "Shows if bounds should be normalized" )
    public boolean isNorm()
    {
        return norm;
    }
    public void setNorm(boolean norm)
    {
        boolean oldValue = this.norm;
        this.norm = norm;
        firePropertyChange( "norm", oldValue, this.norm );
    }
}
