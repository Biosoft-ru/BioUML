package biouml.standard.diagram;

import java.awt.BorderLayout;

import javax.swing.Action;
import javax.swing.JComponent;

import biouml.model.Diagram;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.ViewPartSupport;

/**
 * Microarray view part
 */
public class DatabaseReferencesViewPart extends ViewPartSupport
{
    protected DatabaseReferencesPane drPane;

    public DatabaseReferencesViewPart()
    {
        drPane = new DatabaseReferencesPane();
        add(drPane, BorderLayout.CENTER);
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
        if ( document != null )
        {
            drPane.explore(document.getViewPane());
        }
    }

    @Override
    public boolean canExplore(Object model)
    {
        getActions();
        if( model instanceof Diagram )
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
        if( drPane != null )
            return drPane.getActions();
        return new Action[0];
    }
}