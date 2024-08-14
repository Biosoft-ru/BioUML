package biouml.plugins.fbc.table;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

@SuppressWarnings ( "serial" )
public class FbcBuilderDataTableAnalysisParameters extends AbstractAnalysisParameters
{
    private DataElementPath diagramPath;
    private DataElementPath fbcResultPath;
    private String lowerBoundDefault = "-100.0";
    private String equalsDefault = "";
    private String upperBoundDefault = "100.0";
    private double fbcObjective = 1.0;

    @PropertyName ( "Diagram" )
    @PropertyDescription ( "Path to input diagram" )
    public DataElementPath getDiagramPath()
    {
        return diagramPath;
    }
    public void setDiagramPath(DataElementPath modelPath)
    {
        Object oldValue = this.diagramPath;
        this.diagramPath = modelPath;
        firePropertyChange("diagramPath", oldValue, modelPath);
    }

    @PropertyName ( "Output path" )
    @PropertyDescription ( "Path to table with FBC data" )
    public DataElementPath getFbcResultPath()
    {
        return fbcResultPath;
    }
    public void setFbcResultPath(DataElementPath fbcResultPath)
    {
        Object oldValue = this.fbcResultPath;
        this.fbcResultPath = fbcResultPath;
        firePropertyChange("fbcResultPath", oldValue, fbcResultPath);
    }

    @PropertyName ( "Lower bound" )
    @PropertyDescription ( "Default value of lower bound" )
    public String getLowerBoundDefault()
    {
        return lowerBoundDefault;
    }
    public void setLowerBoundDefault(String lowerBoundDefault)
    {
        Object oldValue = this.lowerBoundDefault;
        this.lowerBoundDefault = lowerBoundDefault;
        firePropertyChange( "lowerBoundDefault", oldValue, lowerBoundDefault );
    }

    @PropertyName ( "Equals" )
    @PropertyDescription ( "Default value of equals" )
    public String getEqualsDefault()
    {
        return equalsDefault;
    }
    public void setEqualsDefault(String equalsDefault)
    {
        Object oldValue = this.equalsDefault;
        this.equalsDefault = equalsDefault;
        firePropertyChange( "equalsDefault", oldValue, equalsDefault );
    }

    @PropertyName ( "Upper bound" )
    @PropertyDescription ( "Default value of upper bound" )
    public String getUpperBoundDefault()
    {
        return upperBoundDefault;
    }
    public void setUpperBoundDefault(String upperBoundDefault)
    {
        Object oldValue = this.upperBoundDefault;
        this.upperBoundDefault = upperBoundDefault;
        firePropertyChange( "upperBoundDefault", oldValue, upperBoundDefault );
    }

    @PropertyName ( "Objective function coefficient" )
    @PropertyDescription ( "Default objective function coefficient" )
    public double getFbcObjective()
    {
        return fbcObjective;
    }
    public void setFbcObjective(double fbcObjective)
    {
        double oldValue = this.fbcObjective;
        this.fbcObjective = fbcObjective;
        firePropertyChange( "fbcObjective", oldValue, fbcObjective );
    }
}
