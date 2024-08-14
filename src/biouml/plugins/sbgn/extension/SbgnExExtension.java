package biouml.plugins.sbgn.extension;

import javax.annotation.Nonnull;

import java.util.logging.Level;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.plugins.sbgn.SBGNPropertyConstants;
import biouml.plugins.sbml.extensions.SBGNExtension;

public class SbgnExExtension extends SBGNExtension
{
    public static final String SBGN_EX_ELEMENT = "sbgn_ext";
    @Override
    public void readElement(Element element, DiagramElement specie, @Nonnull Diagram diagram)
    {
        if( !element.getNodeName().equals( SBGN_EX_ELEMENT ) )
            return;

        try
        {
            SbgnExModelReader reader = new SbgnExModelReader( diagram.getOrigin(), diagram.getName(), diagram );
            Diagram sbgnDiagram = reader.read( element );
            diagram.getAttributes().add( new DynamicProperty( SBGNPropertyConstants.SBGN_ATTRIBUTE_NAME, Diagram.class, sbgnDiagram ) );
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE,  "Can not read SBGN-ext diagram from SBML annotation", e );
        }
    }
    @Override
    public Element[] writeElement(DiagramElement specie, Document document)
    {
        if( ! ( specie instanceof Diagram ) )
            return null;

        Diagram diagram = (Diagram)specie;
        Object sbgnDiagramObj = diagram.getAttributes().getValue( SBGNPropertyConstants.SBGN_ATTRIBUTE_NAME );
        if( isApplicable( sbgnDiagramObj ) )
        {
            Diagram sbgnDiagram = (Diagram)sbgnDiagramObj;
            SbgnExModelWriter writer = new SbgnExModelWriter( sbgnDiagram, diagram );
            try
            {
                String stateName = sbgnDiagram.getCurrentStateName();
                sbgnDiagram.restore();
                Element result = writer.write( document );
                sbgnDiagram.setCurrentStateName( stateName );
                return new Element[] {result};
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE,  "Can not write SBGN-ext diagram to SBML annotation", e );
            }
        }
        return null;
    }

    @Override
    protected boolean isApplicable(Object sbgnDiagramObj)
    {
        return sbgnDiagramObj instanceof Diagram && SbgnExDiagramType.class.equals( ( (Diagram)sbgnDiagramObj ).getType().getClass() );
    }
}
