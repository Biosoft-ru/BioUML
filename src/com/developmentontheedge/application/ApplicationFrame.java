package com.developmentontheedge.application;

import java.awt.BorderLayout;

import javax.swing.JFrame;

import com.developmentontheedge.application.action.ActionManager;

@SuppressWarnings ( "serial" )
public class ApplicationFrame extends JFrame
{
    /*
     * Panel name constants
     */
    public static final String REPOSITORY_PANE_NAME = "repository";
    public static final String EXPLORER_PANE_NAME = "explorer";
    public static final String DOCUMENT_PANE_NAME = "document";
    public static final String EDITOR_PANE_NAME = "editor";
    
    /**
     * Creates and registers JFrame
     *
     * @param title - JFrame title
     * @param name - frame name for registration by Application.
     * If name is NULL, then the frame is main application frame.
     */
    protected ApplicationFrame(String title, String name)
    {
        super(title);

        if( name == null )
            Application.registerApplicationFrame(this);
        else
            Application.registerApplicationFrame(name, this);

        getContentPane().setLayout(new BorderLayout());

        toolBar = new ApplicationToolBar();
        getContentPane().add(toolBar, BorderLayout.NORTH);

        statusBar = new ApplicationStatusBar();
        getContentPane().add(statusBar, BorderLayout.SOUTH);

        menuBar = new ApplicationMenuBar();
        setJMenuBar(menuBar);
    }

    /**
     * Creates and registers main application frame.
     */
    public ApplicationFrame(ActionManager actionManager, DocumentManager documentManager, PanelManager panelManager, String title)
    {
        this(title, null);

        this.documentManager = documentManager;
        this.panelManager = panelManager;
        getContentPane().add(panelManager, BorderLayout.CENTER);
        Application.registerActionManager(actionManager);
    }

    /**
     * Creates and registers named application frame.
     */
    public ApplicationFrame(ActionManager actionManager, DocumentManager documentManager, PanelManager panelManager, String title,
            String name)
    {
        this(title, name);

        this.documentManager = documentManager;
        this.panelManager = panelManager;
        getContentPane().add(panelManager, BorderLayout.CENTER);
        Application.registerActionManager(actionManager, this);
    }

    // //////////////////////////////////////////////////////////////////////////
    // Properties
    //

    private DocumentManager documentManager;
    public DocumentManager getDocumentManager()
    {
        return documentManager;
    }

    protected PanelManager panelManager;
    public PanelManager getPanelManager()
    {
        return panelManager;
    }

    protected ApplicationStatusBar statusBar;
    public ApplicationStatusBar getStatusBar()
    {
        return statusBar;
    }

    protected ApplicationToolBar toolBar;
    public ApplicationToolBar getToolBar()
    {
        return toolBar;
    }

    protected ApplicationMenuBar menuBar;
}
