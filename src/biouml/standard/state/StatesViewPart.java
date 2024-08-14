package biouml.standard.state;

import java.awt.BorderLayout;

import javax.swing.Action;
import javax.swing.JComponent;

import biouml.model.Diagram;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.ViewPartSupport;

/**
 * Microarray view part
 */
public class StatesViewPart extends ViewPartSupport
{
    protected StatesPane stPane;

    public StatesViewPart()
    {
        stPane = new StatesPane();
        add(stPane, BorderLayout.CENTER);
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
        if( model instanceof Diagram )
        {
            stPane.explore((Diagram)model, document);
        }
    }

    @Override
    public boolean canExplore(Object model)
    {
        getActions();
        if( model instanceof Diagram && ( (Diagram)model ).getRole() != null )
            return true;
        return false;
    }

    public void refreshDiagramPane()
    {
        getDocument().update();
    }

    @Override
    public Action[] getActions()
    {
        if( stPane != null )
            return stPane.getActions();
        return new Action[0];
    }
}