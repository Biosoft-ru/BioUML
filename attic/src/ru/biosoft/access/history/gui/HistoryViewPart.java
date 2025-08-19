package ru.biosoft.access.history.gui;

import java.awt.BorderLayout;

import javax.swing.Action;
import javax.swing.JComponent;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.history.HistoryFacade;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.ViewPartSupport;

public class HistoryViewPart extends ViewPartSupport
{
    protected HistoryPane historyPane;

    public HistoryViewPart()
    {
        historyPane = new HistoryPane();
        add(historyPane, BorderLayout.CENTER);
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
        if( model instanceof DataElement )
        {
            historyPane.explore((DataElement)model, document);
        }
    }

    @Override
    public boolean canExplore(Object model)
    {
        return ( ( model instanceof DataElement ) && HistoryFacade.hasHistory((DataElement)model) );
    }

    @Override
    public Action[] getActions()
    {
        return historyPane.getActions();
    }
}
