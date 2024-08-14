package ru.biosoft.gui;

import javax.swing.Action;
import javax.swing.JComponent;

import com.developmentontheedge.beans.undo.TransactionListener;

/**
 * Interface to be implemented by viewer for some part of data.
 *
 * It defines component that will be used to show the data and provides TransactionListener
 * that will be notified about data changes. When transaction is completed then view should
 * be updated if necessary.
 */
public interface ViewPart extends TransactionListener

{
    /**
     * Constant used by ViewPart action to specify view parts order
     * when they are located in some tabbed pane or other container.
     */
    public static final String PRIORITY = "Priority";
    public static final String DEFAULT_ENABLE = "defaultEnable"; 
    public static float DEFAULT_PRIORITY = 1.5f;

    /**
     * The constant to indicate that view part do not depends from the data,
     * that it is not depends from model passed in explore method.
     */
    public String STATIC_VIEW = "static view";

    /** Returns the view. */
    public JComponent getView();

    /** Return action that contains information about the view part (display name, icon, etc). */
    public Action getAction();

    /** Returns actions that are specific for this view part. */
    public Action[] getActions();

    /**
     * @returns whether this view part can explore the specified data.
     * If the method return <code>false</code> then parent can disable this tab.
     */
    public boolean canExplore(Object model);

    /** Notifies the view part that it should explore new data. */
    public void explore(Object model, Document document);

    /**
     * @returns the explored data.
     * To indicate that this view part do not depends from the data the method
     * should return {@link STATIC_VIEW}
     */
    public Object getModel();

    /** Returns document that is edited. */
    public Document getDocument();
    
    /** Notifies the view part that it should explore new data 
     * even though this view part is not active at the moment 
     * @param model - new model
     */
    public void modelChanged(Object model);
    
    /** 
     * Method is called when tab is closed i.e. user selects another tab.
     */
    public void onClose();
    
}
