package biouml.plugins.microarray.editors;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Action;
import javax.swing.JComponent;

import ru.biosoft.gui.Document;
import ru.biosoft.gui.ViewPartSupport;
import ru.biosoft.table.TableDataCollection;

public class FilterViewPart extends ViewPartSupport implements PropertyChangeListener
{
    private final FilterViewPane filterViewPane;

    public FilterViewPart()
    {
        filterViewPane = new FilterViewPane();
        add(filterViewPane, BorderLayout.CENTER);
    }

    @Override
    public JComponent getView()
    {
        return this;
    }

    @Override
    public void explore(Object model, Document document)
    {
        if (this.model != null)
        {
            ((TableDataCollection)this.model).removePropertyChangeListener(this);
        }
        
        if( !(model instanceof TableDataCollection))
            return;
        ((TableDataCollection)model).addPropertyChangeListener(this);
        this.model = model;
        this.document = document;
        
//        if (filterViewPane != null)
//        {
//            remove(filterViewPane);
//        }
        
        this.filterViewPane.explore((TableDataCollection)model, this.document);
    }

    @Override
    public boolean canExplore(Object model)
    {
        if( model instanceof TableDataCollection )
            return true;
        return false;
    }

    @Override
    public Action[] getActions()
    {
        return this.filterViewPane.getActions();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getPropertyName().indexOf("columnInfo") > -1)
        {
            this.filterViewPane.explore((TableDataCollection)model, this.document);
        }
    }
}
