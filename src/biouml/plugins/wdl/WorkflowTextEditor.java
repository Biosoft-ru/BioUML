package biouml.plugins.wdl;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.text.StyledEditorKit;

import com.Ostermiller.Syntax.HighlightedDocument;
import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.action.ActionInitializer;
import com.developmentontheedge.application.action.ActionManager;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.swing.PropertyInspector;
import com.developmentontheedge.beans.swing.PropertyInspectorEx;
import com.developmentontheedge.log.PatternFormatter;
import com.developmentontheedge.log.TextPaneAppender;

import biouml.model.Diagram;
import biouml.plugins.wdl.colorer.WDLColorer;
import biouml.plugins.wdl.diagram.WDLConstants;
import biouml.plugins.wdl.diagram.WDLDiagramType;
import biouml.plugins.wdl.diagram.WDLImporter;
import biouml.plugins.wdl.parser.AstStart;
import biouml.plugins.wdl.parser.WDLParser;
import biouml.standard.diagram.DiagramUtility;
import biouml.workbench.diagram.CompositeDiagramDocument;
import biouml.workbench.diagram.DiagramDocument;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.EditorPartSupport;
import ru.biosoft.gui.GUI;
import ru.biosoft.util.TempFiles;

@SuppressWarnings ( "serial" )
public class WorkflowTextEditor extends EditorPartSupport
{
    private static final Logger log = Logger.getLogger( WorkflowTextEditor.class.getName() );

    private JTabbedPane tabbedPane;
    private JSplitPane splitPane;
    protected TextPaneAppender appender;

    JScrollPane scrollPane;

    protected String[] categoryList = {"biouml.plugins.wdl"};

    private WorkflowSettings settings;

    private WDLEditorPane wdlPane;
    private CWLEditorPane cwlPane;
    private NextFlowEditorPane nextFlowPane;
    private PropertyInspector settingsInspector = new PropertyInspectorEx();

    private Diagram diagram;

    private Action[] actions;

    private Action updateWDLAction = new UpdateWDLAction();
    private Action updateDiagramAction = new UpdateDiagramAction();
    private Action runScriptAction = new RunScriptAction();

    private WDLGenerator wdlGenerator;
    private CWLGenerator cwlGenerator;
    private NextFlowGenerator nextFlowGenerator;
    private WDLImporter wdlImporter;

    String outputDir = TempFiles.path( "nextflow" ).getAbsolutePath();

    public WorkflowTextEditor()
    {
        tabbedPane = new JTabbedPane( SwingConstants.LEFT );
        add( BorderLayout.CENTER, tabbedPane );
        wdlPane = new WDLEditorPane();
        cwlPane = new CWLEditorPane();
        nextFlowPane = new NextFlowEditorPane();

        tabbedPane.addTab( "WDL", new JScrollPane( wdlPane ) );
//        tabbedPane.addTab( "СWL", new JScrollPane( cwlPane ) );
        tabbedPane.addTab( "NextFlow", new JScrollPane( nextFlowPane ) );
        tabbedPane.addTab( "Settings", settingsInspector );
        appender = new TextPaneAppender( new PatternFormatter( "%4$s :  %5$s%n" ), "Application Log" );
        appender.setLevel( Level.SEVERE );
        appender.addToCategories( categoryList );

        splitPane = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, false, tabbedPane, appender.getLogTextPanel() );
        splitPane.setResizeWeight( 0.4 );

        wdlGenerator = new WDLGenerator();
        nextFlowGenerator = new NextFlowGenerator();
        cwlGenerator = new CWLGenerator();
        wdlImporter = new WDLImporter();
    }

    @Override
    public boolean canExplore(Object model)
    {
        return model instanceof Diagram && ( (Diagram)model ).getType() instanceof WDLDiagramType;
    }

    @Override
    public void explore(Object model, Document document)
    {
        try
        {
            Diagram diagram = (Diagram)model;
            setDiagram( (Diagram)model );
            setWDL( wdlGenerator.generate( diagram ) );
//            setCWL( cwlGenerator.generate( diagram ) );
            setNextFlow( nextFlowGenerator.generate( diagram ) );
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
    }

    public String getWDL()
    {
        return wdlPane.getText();
    }

    public void setWDL(String wdl)
    {
        wdlPane.setText( wdl );
    }

    public void setCWL(String cwl)
    {
        cwlPane.setText( cwl );
    }
    
    public String getCWL()
    {
        return cwlPane.getText();
    }

    public String getNextFlow()
    {
        return nextFlowPane.getText();
    }

    public void setNextFlow(String nextFlow)
    {
        nextFlowPane.setText( nextFlow );
    }

    @Override
    public void save()
    {

    }

    @Override
    public JComponent getView()
    {
        return splitPane;
    }

    public Diagram getDiagram()
    {
        return diagram;
    }
    public void setDiagram(Diagram diagram)
    {
        this.diagram = diagram;

        DynamicProperty settingsProperty = diagram.getAttributes().getProperty( WDLConstants.SETTINGS_ATTR );
        if( settingsProperty == null )
        {
            settingsProperty = new DynamicProperty( WDLConstants.SETTINGS_ATTR, WorkflowSettings.class, new WorkflowSettings() );
            diagram.getAttributes().add( settingsProperty );
        }
        settings = (WorkflowSettings)settingsProperty.getValue();
        settings.initParameters( diagram );
        settingsInspector.explore( settings );
    }

    @Override
    public Action[] getActions()
    {
        ActionManager actionManager = Application.getActionManager();
        if( actions == null )
        {
            actionManager.addAction( UpdateWDLAction.KEY, updateWDLAction );
            actionManager.addAction( UpdateDiagramAction.KEY, updateDiagramAction );
            actionManager.addAction( RunScriptAction.KEY, runScriptAction );

            ActionInitializer initializer = new ActionInitializer( MessageBundle.class );

            initializer.initAction( updateWDLAction, UpdateWDLAction.KEY );
            initializer.initAction( updateDiagramAction, UpdateDiagramAction.KEY );
            initializer.initAction( runScriptAction, RunScriptAction.KEY );
            actions = new Action[] {updateWDLAction, updateDiagramAction, runScriptAction};
        }

        return actions.clone();
    }

    public class WDLEditorPane extends JEditorPane
    {
        public WDLEditorPane()
        {
            super();
            setEditorKit( new StyledEditorKit() );
            HighlightedDocument document = new HighlightedDocument();
            document.setHighlightStyle( WDLColorer.class );
            this.setDocument( document );
            setFont( new Font( "Monospaced", Font.PLAIN, 12 ) );
        }

        public void setTextSilent(String str)
        {
            setText( str );
        }
    }

    public class NextFlowEditorPane extends JEditorPane
    {
        public NextFlowEditorPane()
        {
            super();
            setEditorKit( new StyledEditorKit() );
            HighlightedDocument document = new HighlightedDocument();
            this.setDocument( document );
            setFont( new Font( "Monospaced", Font.PLAIN, 12 ) );
        }

        public void setTextSilent(String str)
        {
            setText( str );
        }
    }

    public class CWLEditorPane extends JEditorPane
    {
        public CWLEditorPane()
        {
            super();
            setEditorKit( new StyledEditorKit() );
            HighlightedDocument document = new HighlightedDocument();
            this.setDocument( document );
            setFont( new Font( "Monospaced", Font.PLAIN, 12 ) );
        }

        public void setTextSilent(String str)
        {
            setText( str );
        }
    }

    class UpdateWDLAction extends AbstractAction
    {
        public static final String KEY = "Update WDL";

        public UpdateWDLAction()
        {
            super( KEY );
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            try
            {
                String wdl = wdlGenerator.generate( diagram );
                setWDL( wdl );
            }
            catch( Exception ex )
            {
                ex.printStackTrace();
            }
        }
    }

    class RunScriptAction extends AbstractAction
    {
        public static final String KEY = "Run Script";

        public RunScriptAction()
        {
            super( KEY );
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            try
            {
//                CWLRunner.runNextFlow( diagram, WorkflowTextEditor.this.getCWL(), settings, outputDir,
//                      System.getProperty( "os.name" ).startsWith( "Windows" ) );
                NextFlowRunner.runNextFlow( diagram, WorkflowTextEditor.this.getNextFlow(), settings, outputDir,
                        System.getProperty( "os.name" ).startsWith( "Windows" ) );
            }
            catch( Exception ex )
            {
                ex.printStackTrace();
            }
        }
    }

    class UpdateDiagramAction extends AbstractAction
    {
        public static final String KEY = "Update Diagram";

        public UpdateDiagramAction()
        {
            super( KEY );
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            try
            {
                String text = getWDL();
                text = text.replace( "<<<", "{" ).replace( ">>>", "}" );//TODO: fix parsing <<< >>>
                AstStart start = new WDLParser().parse( new StringReader( text ) );
                diagram = wdlImporter.generateDiagram( start, diagram );
                wdlImporter.layout( diagram );
                setDiagram( diagram );
                setNextFlow( nextFlowGenerator.generate( diagram ) );
                diagram.save();
            }
            catch( Exception ex )
            {
                log.info( "Error during WDL applying:" + ex.getMessage() );
                ex.printStackTrace();
            }
        }
    }
}