package biouml.plugins.antimony;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.plugins.sbml.extensions.SbmlExtensionSupport;

public class AntimonySbmlExtension extends SbmlExtensionSupport
{
    protected static final Logger log = Logger.getLogger(AntimonySbmlExtension.class.getName());
    public static final String ANTIMONY_TEXT_ATTR = "text";
    public static final String ANTIMONY_VERSION_ATTR = "version";
    public static final String ANTIMONY_NAMESPACE = "antimony";

    @Override
    public void readElement(Element element, DiagramElement de, @Nonnull
    Diagram diagram)
    {
        if( de instanceof Diagram )
        {
            try
            {
                String antimonyText = element.getAttribute(ANTIMONY_TEXT_ATTR);
                String antimonyVersion = element.getAttribute(ANTIMONY_VERSION_ATTR);

                if( antimonyVersion == null )
                    antimonyVersion = "1.0";

                AntimonyUtility.setAntimonyAttribute(diagram, antimonyText, AntimonyConstants.ANTIMONY_TEXT_ATTR);
                AntimonyUtility.setAntimonyAttribute(diagram, antimonyVersion, AntimonyConstants.ANTIMONY_VERSION_ATTR);
            }
            catch( Exception ex )
            {
                log.log(Level.SEVERE, "Error during antimony attribute reading " + ex.getMessage());
            }
        }
    }

    @Override
    public Element[] writeElement(DiagramElement de, Document document)
    {
        //antimony is BioUML specific
        if( !modelWriter.isWriteBioUMLAnnotation() || ! ( de instanceof Diagram ) )
            return null;

        Diagram diagram = (Diagram)de;

        String antimonyText = AntimonyUtility.getAntimonyAttribute(diagram, AntimonyConstants.ANTIMONY_TEXT_ATTR);
        if( antimonyText == null )
            return null;

        String antimonyVersion = AntimonyUtility.getAntimonyAttribute(diagram, AntimonyConstants.ANTIMONY_VERSION_ATTR);
        if( antimonyVersion == null )
            return null;

        Element elem = document.createElement(ANTIMONY_NAMESPACE);// .class new Element();
        elem.setAttribute(ANTIMONY_TEXT_ATTR, antimonyText);
        elem.setAttribute(ANTIMONY_VERSION_ATTR, antimonyVersion);

        return new Element[] {elem};
    }
}
