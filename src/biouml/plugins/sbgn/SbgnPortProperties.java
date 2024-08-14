package biouml.plugins.sbgn;

import java.awt.Point;
import java.util.List;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.diagram.PortProperties;
import biouml.standard.type.Base;
import one.util.streamex.StreamEx;
import ru.biosoft.graphics.editor.ViewEditorPane;

public class SbgnPortProperties extends PortProperties
{
    @Override
    public DiagramElementGroup doCreateElements(Compartment c, Point location, ViewEditorPane viewPane) throws Exception
    {
        List<DiagramElement> elements =  DiagramUtility.createPortNode(c, this, viewPane, location);
        StreamEx.of(elements).forEach(de->SbgnSemanticController.setNeccessaryAttributes(de));
        return new DiagramElementGroup( elements );
    }

    public SbgnPortProperties(Diagram diagram, Class<? extends Base> type)
    {
        super(diagram, type);
    }
}
