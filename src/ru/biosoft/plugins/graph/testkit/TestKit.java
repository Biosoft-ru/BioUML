package ru.biosoft.plugins.graph.testkit;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.util.logging.Level;
import java.util.logging.LogManager;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JTextField;

import ru.biosoft.graph.Edge;
import ru.biosoft.graph.Graph;
import ru.biosoft.graph.Layouter;
import ru.biosoft.util.ApplicationUtils;

import com.developmentontheedge.beans.swing.PropertyInspector;
import com.developmentontheedge.beans.web.HtmlPropertyInspector;
import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.PanelInfo;
import com.developmentontheedge.application.action.ActionManager;

public class TestKit
        extends GraphViewer
{
    public static final String OPTIONS_PANE = "OPTIONS_PANE";

    protected JTextField addPane;

    protected PropertyInspector optionsPane;

    protected HtmlPropertyInspector infoPane;

    protected Layouter layouter = null;

    public Layouter getLayouter ( )
    {
        return layouter;
    }

    public void setLayouter ( Layouter layouter )
    {
        this.layouter = layouter;
        LayoutOptions options = new LayoutOptions ( this );
        optionsPane.explore ( options );
        optionsPane.expandAll ( true );
    }

    // ///////////////////////////////////////////////////////////////////////////
    // initialisation
    //

    public TestKit ( )
    {
        File configFile = new File( "./ru/biosoft/graph/testkit/log.lcf" );
        try( FileInputStream fis = new FileInputStream( configFile ) )
        {
            LogManager.getLogManager().readConfiguration( fis );
        }
        catch( Exception e1 )
        {
            System.err.println( "Error init logging: " + e1.getMessage() );
        }
    }

    @Override
    protected void initPanels ( )
    {
        super.initPanels ( );

        LayoutOptions options = new LayoutOptions ( this );
        optionsPane = new PropertyInspector ( );
        optionsPane.explore ( options );
        optionsPane.expandAll ( true );

        PanelInfo optionsPaneInfo = new PanelInfo ( OPTIONS_PANE, optionsPane, true, null );
        panelManager.addPanel ( optionsPaneInfo, GRAPH_PANE, PanelInfo.RIGHT, 750 );

        addPane = new JTextField ( "add path" );
        editingPane.add ( addPane, BorderLayout.NORTH );
        infoPane = new HtmlPropertyInspector ( );
        tabPane.addTab ( "info", infoPane );
    }

    @Override
    protected void initActions ( )
    {
        super.initActions ( );

        ActionManager actionManager = Application.getActionManager ( );

        actionManager.addAction ( ApplyLayoutAction.KEY, new ApplyLayoutAction ( ) );
        actionManager.addAction ( AddGraphAction.KEY, new AddGraphAction ( ) );
        actionManager.addAction ( GenerateGraphTextAction.KEY, new GenerateGraphTextAction ( ) );

        actionManager.initActions ( ru.biosoft.plugins.graph.testkit.MessageBundle.class );
    }

    @Override
    protected void initToolbar ( )
    {
        super.initToolbar ( );

        ActionManager actionManager = Application.getActionManager ( );
        Action action = null;

        toolBar.addSeparator ( GROUP_LAYOUT );
        action = actionManager.getAction ( AddGraphAction.KEY );
        toolBar.addAction ( action );
        action = actionManager.getAction ( ApplyLayoutAction.KEY );
        toolBar.addAction ( action );
        action = actionManager.getAction ( GenerateGraphTextAction.KEY );
        toolBar.addAction ( action );
        /*
         * toolBar.addSeparator(GROUP_PANE);
         *
         * action = actionManager.getAction(GRAPH_PANE);
         * toolBar.addToggleButtonAt(action, GROUP_PANE, true);
         *
         * action = actionManager.getAction(OPTIONS_PANE);
         * toolBar.addToggleButtonAt(action, GROUP_PANE, true);
         *
         * action = actionManager.getAction(TEXT_PANE);
         * toolBar.addToggleButtonAt(action, GROUP_PANE, true);
         *
         * action = actionManager.getAction(INFO_PANE);
         * toolBar.addToggleButtonAt(action, GROUP_PANE, true);
         */
    }

    @Override
    protected void enableActions ( )
    {
        super.enableActions ( );

        ActionManager actionManager = Application.getActionManager ( );
        actionManager.enableActions( true, AddGraphAction.KEY, ApplyLayoutAction.KEY, GenerateGraphTextAction.KEY  );
    }

    class ApplyLayoutAction
            extends AbstractAction
    {
        public static final String KEY = "Apply layout";

        public ApplyLayoutAction ( )
        {
            super ( KEY );
            setEnabled ( false );
        }

        @Override
        public void actionPerformed ( ActionEvent e )
        {
            layouter.doLayout ( graph, null );
            updateGraphView ( );
        }
    }

    class AddGraphAction
            extends AbstractAction
    {
        public static final String KEY = "Add graph";

        public AddGraphAction ( )
        {
            super ( KEY );
            setEnabled ( false );
        }

        @Override
        public void actionPerformed ( ActionEvent e )
        {
            try
            {
                Edge edge = Graph.parseEdge ( addPane.getText ( ), graph );
                if ( edge != null )
                {
                    graph.addEdge ( edge );

                    layouter.layoutPath ( graph, edge, null );
                }
                updateGraphView ( );
            }
            catch ( Throwable t )
            {
                t.printStackTrace ( );
                log.log( Level.SEVERE, "Adding path error: " + t.getClass() + t.getMessage() );
            }
        }
    }

    class GenerateGraphTextAction
            extends AbstractAction
    {
        public static final String KEY = "generate graph";

        public GenerateGraphTextAction ( )
        {
            super ( KEY );
            setEnabled ( false );
        }

        @Override
        public void actionPerformed ( ActionEvent e )
        {
            textPane.setText ( graph.generateText ( ) );
            updateGraphView ( );
        }
    }

    // ///////////////////////////////////////////////////

    public static void main ( String[] args )
    {
        TestKit frame = new TestKit ( );

        Dimension parentSize = Toolkit.getDefaultToolkit ( ).getScreenSize ( );
        frame.setSize ( parentSize.width - 50, parentSize.height - 50 );
        ApplicationUtils.moveToCenter ( frame );
        frame.setVisible ( true );
    }
}
