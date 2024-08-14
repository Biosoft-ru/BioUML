package biouml.plugins.keynodes.customhub;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

@SuppressWarnings ( "serial" )
@PropertyName ( "Parameters" )
public class DiagramToPairsConverterParameters extends AbstractAnalysisParameters
{
    private DataElementPath diagramPath, tablePath;
    private double weight = 1.0;



    @PropertyName ( "Diagram" )
    @PropertyDescription ( "Diagram to convert to list of interaction pairs." )
    public DataElementPath getDiagramPath()
    {
        return diagramPath;
    }

    public void setDiagramPath(DataElementPath diagramPath)
    {
        Object oldValue = this.diagramPath;
        this.diagramPath = diagramPath;
        firePropertyChange( "outputTable", oldValue, this.diagramPath );
    }

    @PropertyName ( "Output name" )
    @PropertyDescription ( "Output table name." )
    public DataElementPath getTablePath()
    {
        return tablePath;
    }

    public void setTablePath(DataElementPath tablePath)
    {
        Object oldValue = this.tablePath;
        this.tablePath = tablePath;
        firePropertyChange( "tablePath", oldValue, this.tablePath );
    }

    @PropertyName ( "Weight" )
    @PropertyDescription ( "Interaction weight common for all pairs from diagram." )
    public double getWeight()
    {
        return weight;
    }

    public void setWeight(double weight)
    {
        this.weight = weight;
    }

}
