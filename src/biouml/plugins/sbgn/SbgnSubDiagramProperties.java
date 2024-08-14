package biouml.plugins.sbgn;

import java.awt.Point;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.plugins.sbml.SbmlSupport;
import biouml.standard.diagram.SubDiagramProperties;
import ru.biosoft.graphics.editor.ViewEditorPane;

public class SbgnSubDiagramProperties extends SubDiagramProperties
{
    public SbgnSubDiagramProperties(Diagram diagram)
    {
        super(diagram);
    }

    @Override
    public DiagramElementGroup doCreateElements(Compartment compartment, Point location, ViewEditorPane viewPane) throws Exception
    {
        DiagramElementGroup elements = super.doCreateElements( compartment, location, viewPane );
        for( DiagramElement de : elements.getElements() )
            SbgnSemanticController.setNeccessaryAttributes(de);
        return elements;
    }

    @Override
    protected String validateName(String name)
    {
        return SbmlSupport.castStringToSId(name);
    }
}
