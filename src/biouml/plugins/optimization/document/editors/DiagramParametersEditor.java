package biouml.plugins.optimization.document.editors;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.plugins.optimization.ParameterConnection;
import biouml.standard.diagram.Util;
import one.util.streamex.StreamEx;

import com.developmentontheedge.beans.editors.StringTagEditor;

public class DiagramParametersEditor extends StringTagEditor
{
    protected static final Logger log = Logger.getLogger(DiagramParametersEditor.class.getName());

    @Override
    public String[] getTags()
    {
        Diagram diagram = ( (ParameterConnection)getBean() ).getDiagram();
        String subdiagramPath = ( (ParameterConnection)getBean() ).getSubdiagramPath();

        try
        {
            Diagram innerDiagram = Util.getInnerDiagram( diagram, subdiagramPath );
            DataCollection<?> diagramParams = innerDiagram.getRole(EModel.class).getVariables();
            if( diagramParams != null )
            {
                return StreamEx.of( diagramParams.names() ).prepend( "" ).toArray( String[]::new );
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not initialize parameters of the diagram " + diagram.getName());
        }
        return null;
    }
}
