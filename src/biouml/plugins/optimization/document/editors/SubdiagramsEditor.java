package biouml.plugins.optimization.document.editors;

import java.util.logging.Level;
import java.util.logging.Logger;

import biouml.model.Diagram;

import biouml.standard.diagram.Util;
import one.util.streamex.StreamEx;

import com.developmentontheedge.beans.editors.StringTagEditor;

public class SubdiagramsEditor extends StringTagEditor
{
    protected static final Logger log = Logger.getLogger(SubdiagramsEditor.class.getName());

    @Override
    public String[] getTags()
    {
        try
        {
            Diagram diagram = (Diagram)getBean().getClass().getMethod( "getDiagram" ).invoke( getBean() );
            return StreamEx.of( Util.getSubDiagrams( diagram ) ).map( s -> Util.getPath( s ) ).prepend( "" ).toArray( String[]::new );
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, getClass() + " : " + e);
        }
        return null;
    }
}
