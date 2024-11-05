package biouml.plugins.sbol;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

public class SbolImportProperties extends Option
{
    private String diagramName;

    @PropertyName ( "Diagram name" )
    public String getDiagramName()
    {
        return diagramName;
    }
    public void setDiagramName(String diagramName)
    {
        this.diagramName = diagramName;
    }
}