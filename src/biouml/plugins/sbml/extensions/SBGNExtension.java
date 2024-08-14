package biouml.plugins.sbml.extensions;

import javax.annotation.Nonnull;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramType;
import biouml.plugins.sbgn.SBGNPropertyConstants;
import biouml.plugins.sbgn.SbgnCompositeDiagramType;
import biouml.plugins.sbgn.SbgnDiagramType;
import biouml.plugins.sbgn.SbgnModelReader;
import biouml.plugins.sbgn.SbgnModelWriter;
/**
 * SBML extension for read/write SBGN XML diagrams from SBML annotation.
 */
public class SBGNExtension extends SbmlExtensionSupport
{
    protected static final Logger log = Logger.getLogger(SBGNExtension.class.getName());

    public static final String SBGN_ELEMENT = "sbgn";
    @Override
    public void readElement(Element element, DiagramElement specie, @Nonnull Diagram diagram)
    {
        if( !element.getNodeName().equals(SBGN_ELEMENT) )
            return;

        try
        {
            SbgnModelReader reader = new SbgnModelReader(diagram.getOrigin(), diagram.getName(), diagram);
            Diagram sbgnDiagram = reader.read(element);
            diagram.getAttributes().add(new DynamicProperty(SBGNPropertyConstants.SBGN_ATTRIBUTE_NAME, Diagram.class, sbgnDiagram));
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not read SBGN diagram from SBML annotation", e);
        }
    }

    @Override
    public Element[] writeElement(DiagramElement specie, Document document)
    {
        if( ! ( specie instanceof Diagram ) )
        {
            return null;
        }

        Diagram diagram = (Diagram)specie;
        Object sbgnDiagramObj = diagram.getAttributes().getValue(SBGNPropertyConstants.SBGN_ATTRIBUTE_NAME);
        if( isApplicable( sbgnDiagramObj ) )
        {
            Diagram sbgnDiagram = (Diagram)sbgnDiagramObj;
            SbgnModelWriter writer = new SbgnModelWriter(sbgnDiagram, diagram);
            writer.setNewPaths( modelWriter.getNewPaths() );
            try
            {
                String stateName = sbgnDiagram.getCurrentStateName();
                sbgnDiagram.restore();
                Element result = writer.write(document);
                sbgnDiagram.setCurrentStateName(stateName);
                return new Element[] {result};
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Can not write SBGN diagram to SBML annotation", e);
            }
        }
        return null;
    }

    protected boolean isApplicable(Object sbgnDiagramObj)
    {
        if( ! ( sbgnDiagramObj instanceof Diagram ) )
            return false;
        Class<? extends DiagramType> diagramType = ( (Diagram)sbgnDiagramObj ).getType().getClass();
        return SbgnDiagramType.class.equals( diagramType ) || SbgnCompositeDiagramType.class.equals( diagramType );
    }
}
