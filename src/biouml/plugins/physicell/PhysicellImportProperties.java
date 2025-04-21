package biouml.plugins.physicell;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

public class PhysicellImportProperties extends Option
{
    private String diagramName = "Model";
    private boolean importDefaultDefinition = false;

    @PropertyName ( "Diagram name" )
    public String getDiagramName()
    {
        return diagramName;
    }
    public void setDiagramName(String diagramName)
    {
        this.diagramName = diagramName;
    }

    @PropertyName ( "Import default Cell Definition" )
    public boolean isImportDefaultDefinition()
    {
        return importDefaultDefinition;
    }
    public void setImportDefaultDefinition(boolean importDefaultDefinition)
    {
        this.importDefaultDefinition = importDefaultDefinition;
    }
}