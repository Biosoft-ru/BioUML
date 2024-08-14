package ru.biosoft.gui;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.undo.UndoableEdit;

import com.developmentontheedge.beans.undo.TransactionEvent;
import com.developmentontheedge.application.action.ApplicationAction;

public class ViewPartSupport extends JPanel implements ViewPart
{
    public ViewPartSupport()
    {
        setLayout(new java.awt.BorderLayout());
    }

    @Override
    public JComponent getView()
    {
        return this;
    }

    protected Action action;
    @Override
    public Action getAction()
    {
        if( action == null )
            action = new ApplicationAction("ViewPart", "View part");

        return action;
    }

    @Override
    public Action[] getActions()
    {
        return null;
    }

    protected ModelValidator modelValidator;
    public ModelValidator getModelValidator()
    {
        return modelValidator;
    }

    public void setModelValidator(ModelValidator modelValidator)
    {
        this.modelValidator = modelValidator;
    }

    @Override
    public boolean canExplore(Object model)
    {
        if( modelValidator == null )
            return true;

        return modelValidator.canExplore(model, this);
    }

    @Override
    public void explore(Object model, Document document)
    {
        this.model    = model;
        this.document = document;
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
