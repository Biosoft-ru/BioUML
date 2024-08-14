package ru.biosoft.gui;

import java.awt.Dimension;
import java.net.URL;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.undo.UndoableEdit;

import com.developmentontheedge.beans.undo.TransactionEvent;
import com.developmentontheedge.beans.web.HtmlPane;
import com.developmentontheedge.application.action.ApplicationAction;

public class HtmlView extends HtmlPane implements ViewPart
{
    public static final String ACTION_NAME = "Html view";
    protected Action action;

    public HtmlView()
    {
        action = new ApplicationAction(ru.biosoft.gui.resources.MessageBundle.class, ACTION_NAME);

        setPreferredSize(new Dimension(300, 400));
    }

    ////////////////////////////////////////////////////////////////////////////
    // ViewPart interface implimentation
    //

    @Override
    public JComponent getView()
    {
        return scrollPane;
    }

    @Override
    public Action getAction()
    {
        return action;
    }

    @Override
    public boolean canExplore(Object model)
    {
        return model instanceof String || model instanceof URL;
    }

    @Override
    public void explore(Object model, Document document)
    {
        this.document = document;
        this.model    = model;

        setInitialText(model);
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
