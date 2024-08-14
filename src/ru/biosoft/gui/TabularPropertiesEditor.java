package ru.biosoft.gui;

import java.awt.Dimension;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Iterator;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.undo.UndoableEdit;

import com.developmentontheedge.beans.swing.TabularPropertyInspector;
import com.developmentontheedge.beans.undo.TransactionEvent;
import com.developmentontheedge.application.action.ApplicationAction;

public class TabularPropertiesEditor extends TabularPropertyInspector implements EditorPart
{
    public static final String ACTION_NAME = "Tabular Property Editor";
    protected Action action;

    ////////////////////////////////////////////////////////////////////////////
    // Constructor
    //

    public TabularPropertiesEditor()
    {
        action = new ApplicationAction(ru.biosoft.gui.resources.MessageBundle.class, ACTION_NAME);

        setPreferredSize(new Dimension(300, 400));

        addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusGained(FocusEvent e)
            {
                if( getDocument() != null )
                    DocumentManager.setActiveDocument(getDocument(), getView());
            }
        });

    }

    ////////////////////////////////////////////////////////////////////////////
    // EditorPart implementation
    //

    @Override
    public JComponent getView()
    {
        return this;
    }

    @Override
    public Action getAction()
    {
        return action;
    }

    @Override
    public Action[] getActions()
    {
        return null;
    }

    @Override
    public boolean canExplore(Object bean)
    {
        return bean instanceof Iterator || bean instanceof Object[];
    }

    @Override
    public void explore(Object model, Document document)
    {
        this.model = model;
        this.document = document;

        if( model instanceof Iterator )
            explore((Iterator)model);
        if( model instanceof Object[] )
            explore((Object[])model);
    }

    protected Object model;
    @Override
    public Object getModel()
    {
        return model;
    }

    protected Document document;
    @Override
    public Document getDocument()
    {
        return document;
    }

    @Override
    public void save()
    {}

    ////////////////////////////////////////////////////////////////////////////
    // TransactionListener - do nothing
    //

    @Override
    public void     startTransaction(TransactionEvent te)   {}
    @Override
    public boolean  addEdit(UndoableEdit ue)                {return false;}
    @Override
    public void     completeTransaction()                   {}
    @Override
    public void modelChanged(Object model) { }
    
    @Override
    public void onClose() { }
}

