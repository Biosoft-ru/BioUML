package ru.biosoft.table.document.editors;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Action;
import javax.swing.JComponent;

import ru.biosoft.gui.Document;
import ru.biosoft.gui.ViewPartSupport;
import ru.biosoft.table.TableDataCollection;

/**
 * Microarray view part
 */
public class ColumnsViewPart extends ViewPartSupport implements PropertyChangeListener
{
    private final ColumnsViewPane columnsViewPane;

    public ColumnsViewPart()
    {
        columnsViewPane = new ColumnsViewPane();
        add(columnsViewPane, BorderLayout.CENTER);
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
        
        this.columnsViewPane.explore((TableDataCollection)model, this.document);
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
        return this.columnsViewPane.getActions();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getPropertyName().indexOf("columnInfo") > -1)
        {
            this.columnsViewPane.explore((TableDataCollection)model, this.document);
        }
    }
}
