package biouml.plugins.microarray;

import java.awt.BorderLayout;

import javax.swing.Action;
import javax.swing.JComponent;

import biouml.model.Diagram;
import biouml.workbench.diagram.DiagramDocument;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.ViewPartSupport;

/**
 * Microarray view part
 */
@SuppressWarnings ( "serial" )
public class MicroarrayViewPart extends ViewPartSupport
{
    private final MicroarrayPane microarrayPane;

    public MicroarrayViewPart()
    {
        microarrayPane = new MicroarrayPane();
        add(microarrayPane, BorderLayout.CENTER);
    }

    @Override
    public JComponent getView()
    {
        return this;
    }

    @Override
    public void explore(Object model, Document document)
    {
        this.model = model;
        this.document = document;
        microarrayPane.setDocument((DiagramDocument)document);
    }

    @Override
    public boolean canExplore(Object model)
    {
        if( model instanceof Diagram )
            return true;
        return false;
    }

    @Override
    public Action[] getActions()
    {
        if( microarrayPane != null )
            return microarrayPane.getActions();
        return new Action[0];
    }
}