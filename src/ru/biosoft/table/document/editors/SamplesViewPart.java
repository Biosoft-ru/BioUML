package ru.biosoft.table.document.editors;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Action;
import javax.swing.JComponent;

import ru.biosoft.gui.Document;
import ru.biosoft.gui.ViewPartSupport;
import ru.biosoft.table.TableDataCollection;

public class SamplesViewPart extends ViewPartSupport implements PropertyChangeListener
{
    private SamplesViewPane samplesViewPane;

    public SamplesViewPart()
    {
        samplesViewPane = new SamplesViewPane();
        add(samplesViewPane, BorderLayout.CENTER);
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
        
        this.model = model;
        if( !(model instanceof TableDataCollection))
            return;
        ((TableDataCollection)model).addPropertyChangeListener(this);
        
        this.document = document;
        
        this.samplesViewPane.explore((TableDataCollection)model, this.document);
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
        return this.samplesViewPane.getActions();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getPropertyName().indexOf("columnInfo") > -1)
        {
            this.samplesViewPane.explore((TableDataCollection)model, this.document);
        }
    }
}
