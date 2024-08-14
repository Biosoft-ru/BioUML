package ru.biosoft.gui;

import java.awt.Dimension;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.undo.UndoableEdit;

import ru.biosoft.gui.resources.MessageBundle;

import com.developmentontheedge.beans.swing.PropertyInspectorEx;
import com.developmentontheedge.beans.undo.TransactionEvent;
import com.developmentontheedge.application.action.ApplicationAction;


public class PropertiesEditor extends PropertyInspectorEx implements EditorPart
{
    public static final String ACTION_NAME = "Property Inspector";
    protected Action action;

    ////////////////////////////////////////////////////////////////////////////
    // Constructor
    //

    public PropertiesEditor()
    {
        action = new ApplicationAction(MessageBundle.class, ACTION_NAME);

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
        return true;
    }

    @Override
    public void explore(Object model, Document document)
    {
        this.document = document;
        explore(model);
        if( expansionLevel > 1 )
            expand(expansionLevel);
    }

    @Override
    public Object getModel()
    {
        return getBean();
    }

    protected Document document;
    @Override
    public Document getDocument()
    {
        return document;
    }

    @Override
    public void save()
    {
        updateUI();//maybe we need separate method for tab to update on changes
    }

    protected int expansionLevel = 2;
    public int getExpansionLevel()
    {
        return expansionLevel;
    }
    public void setExpansionLevel(int expansionLevel)
    {
        this.expansionLevel = expansionLevel;
    }

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
    public void modelChanged(Object model) {}
    
    @Override
    public void onClose() { }
}

