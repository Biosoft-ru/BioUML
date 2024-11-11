package biouml.plugins.sbol;

import java.awt.Point;

import org.sbolstandard.core2.Component;
import org.sbolstandard.core2.ComponentDefinition;
import org.sbolstandard.core2.Identified;
import org.sbolstandard.core2.SBOLDocument;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElementGroup;
import biouml.model.InitialElementProperties;
import ru.biosoft.graphics.editor.ViewEditorPane;

public class SequenceFeature extends SbolBase implements InitialElementProperties
{
    public SequenceFeature(Identified so)
    {
        super( so );
    }

    private String[] strandTypes = new String[] {"Single-stranded", "Double-stranded"};
    private String[] topologyTypes = new String[] {"Linear", "Circular"};
    private String strandType = "Single-stranded";
    private String topologyType = "Linear";

    @Override
    public DiagramElementGroup createElements(Compartment compartment, Point location, ViewEditorPane viewPane) throws Exception
    {

        return null;
    }

}
