package biouml.plugins.chebi;

import java.awt.Dimension;
import java.awt.Graphics2D;

import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.graphics.CompositeView;
import biouml.standard.type.CDKRenderer;
import biouml.standard.type.Structure;
import biouml.standard.type.Substance;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

@SuppressWarnings ( "serial" )
@PropertyName ( "ChEBI molecule" )
@PropertyDescription ( "ChEBI molecule" )
public class ChebiMolecule extends Substance
{
    private final String structurePath;
    public ChebiMolecule(DataCollection origin, String name)
    {
        super( origin, name );
        if( origin != null )
            structurePath = origin.getOrigin().getCompletePath().toString() + "/";
        else
            structurePath = "databases/ChEBI/Data/";
    }

    private CompositeView structureView = null;
    @PropertyName ( "Structure" )
    @PropertyDescription ( "View of associated structures" )
    public CompositeView getStructureView()
    {
        return structureView;
    }
    private void createStructureView()
    {
        String[] refs = super.getStructureReferences();
        if( structureView == null && refs.length != 0 )
        {
            try
            {
                Dimension d = new Dimension( 150, 150 );
                Graphics2D g = ApplicationUtils.getGraphics();
                structureView = new CompositeView();
                for( int i = 0; i < refs.length; i++ )
                {
                    CompositeView innerView = CDKRenderer.createStructureView( DataElementPath.create( structurePath + refs[i] )
                            .getDataElement( Structure.class ), d, g );
                    innerView.setLocation( 0, 160 * i );
                    structureView.add( innerView );
                }
            }
            catch( Throwable t )
            {
            }
        }
    }

    @Override
    public void setStructureReferences(String[] structureReferences)
    {
        super.setStructureReferences( structureReferences );
        createStructureView();
    }
    @Override
    public String[] getStructureReferences()
    {
        String[] structureRefs = super.getStructureReferences();
        return structureRefs == null || structureRefs.length == 0 ? structureRefs : StreamEx.of( structureRefs )
                .map( sr -> "<a target='_blank' href=\"de:" + structurePath + sr + "\">" + sr + "</a>" ).toArray( String[]::new );
    }

}
