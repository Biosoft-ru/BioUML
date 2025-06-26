package biouml.plugins.wdl;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.text.StyledEditorKit;

import com.Ostermiller.Syntax.HighlightedDocument;
import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.application.action.ActionInitializer;
import com.developmentontheedge.application.action.ActionManager;
import com.developmentontheedge.beans.annot.PropertyName;
import com.developmentontheedge.beans.swing.PropertyInspector;
import com.developmentontheedge.beans.swing.PropertyInspectorEx;
import com.developmentontheedge.log.PatternFormatter;
import com.developmentontheedge.log.TextPaneAppender;

import biouml.model.Diagram;
import biouml.model.Node;
import biouml.plugins.wdl.colorer.WDLColorer;
import biouml.plugins.wdl.diagram.WDLDiagramType;
import biouml.plugins.wdl.diagram.WDLImporter;
import biouml.plugins.wdl.parser.AstStart;
import biouml.plugins.wdl.parser.WDLParser;
import biouml.standard.diagram.DiagramUtility;
import biouml.workbench.diagram.CompositeDiagramDocument;
import biouml.workbench.diagram.DiagramDocument;
import one.util.streamex.StreamEx;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.TextFileImporter;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.TextDataElement;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.EditorPartSupport;
import ru.biosoft.gui.GUI;
import ru.biosoft.util.TempFiles;
import ru.biosoft.util.bean.BeanInfoEx2;

@SuppressWarnings ( "serial" )
public class WDLEditor extends EditorPartSupport
{
    private Logger log = Logger.getLogger( WDLEditor.class.getName() );

    private JTabbedPane tabbedPane;
    private JSplitPane splitPane;
    protected TextPaneAppender appender;
    protected String[] categoryList = {"biouml.plugins.wdl"};

    private WorkflowSettings settings;

    private WDLEditorPane wdlPane;
    private NextFlowEditorPane nextFlowPane;
    private PropertyInspector inspector = new PropertyInspectorEx();

    private Diagram diagram;

    private Action[] actions;

    private Action updateWDLAction = new UpdateWDLAction();
    private Action updateDiagramAction = new UpdateDiagramAction();
    private Action runScriptAction = new RunScriptAction();

    private WDLGenerator wdlGenerator;
    private NextFlowGenerator nextFlowGenerator;
    private WDLImporter wdlImporter;

    String outputDir = TempFiles.path( "nextflow" ).getAbsolutePath();

    public WDLEditor()
    {
        tabbedPane = new JTabbedPane( SwingConstants.LEFT );
        add( BorderLayout.CENTER, tabbedPane );
        wdlPane = new WDLEditorPane();
        nextFlowPane = new NextFlowEditorPane();
        settings = new WorkflowSettings();


        //        settingsPane = new SettingsPane( settings );
        inspector.explore( settings );

        tabbedPane.addTab( "WDL", wdlPane );
        tabbedPane.addTab( "NextFlow", nextFlowPane );
        tabbedPane.addTab( "Settings", inspector );
        appender = new TextPaneAppender( new PatternFormatter( "%4$s :  %5$s%n" ), "Application Log" );
        appender.setLevel( Level.SEVERE );
        appender.addToCategories( categoryList );

        //        JScrollPane scroll = new JScrollPane(antimonyPane);
        splitPane = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, false, tabbedPane, appender.getLogTextPanel() );
        splitPane.setResizeWeight( 0.4 );

        wdlGenerator = new WDLGenerator();
        nextFlowGenerator = new NextFlowGenerator();
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
            setDiagram( (Diagram)model );
            setWDL( wdlGenerator.generateWDL( getDiagram() ) );
            setNextFlow( nextFlowGenerator.generateNextFlow( getDiagram() ) );
        }
        catch( Exception ex )
        {
            //            setText( "" );
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
            //            document.setHighlightStyle( WDLColorer.class );
            this.setDocument( document );
            setFont( new Font( "Monospaced", Font.PLAIN, 12 ) );
        }

        public void setTextSilent(String str)
        {
            setText( str );
        }
    }

    public static class WorkflowSettings
    {
        private DataElementPath outputPath;

        @PropertyName ( "Output path" )
        public DataElementPath getOutputPath()
        {
            return outputPath;
        }

        public void setOutputPath(DataElementPath outputPath)
        {
            this.outputPath = outputPath;
        }
    }

    public class WorkflowSettingsBeanInfo extends BeanInfoEx2<WorkflowSettings>
    {
        public WorkflowSettingsBeanInfo(WorkflowSettings settings)
        {
            super( WorkflowSettings.class );
        }

        @Override
        public void initProperties()
        {
            add( "outputPath" );
        }
    }


    public void replaceDiagram(Diagram newDiagram)
    {
        Document currentDocument = GUI.getManager().getCurrentDocument();
        this.document = ( DiagramUtility.isComposite( newDiagram ) ) ? new CompositeDiagramDocument( newDiagram )
                : new DiagramDocument( newDiagram );

        this.document.update();

        if( GUI.getManager().getCurrentDocument() != null )
        {
            GUI.getManager().replaceDocument( currentDocument, this.document );
            GUI.getManager().getDocumentViewAccessProvider().enableDocumentActions( true );
        }
        else
        {
            log.info( "replacing document, but document is null" );
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
                String wdl = wdlGenerator.generateWDL( (Diagram)model );
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
            runNextFlow( diagram.getName(), getNextFlow() );
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
                AstStart start = new WDLParser().parse( new StringReader( getWDL() ) );
                diagram = wdlImporter.generateDiagram( start, diagram.getOrigin(), diagram.getName() );
                wdlImporter.layout( diagram );
                replaceDiagram( diagram );
                diagram.save();
            }
            catch( Exception ex )
            {
                ex.printStackTrace();
            }
        }
    }

    private void runNextFlow(String name, String script)
    {
        try
        {
            if( settings.getOutputPath() == null )
                log.info( "Output path not specified" );


            new File( outputDir ).mkdirs();
            DataCollectionUtils.createSubCollection( settings.getOutputPath() );

            NextFlowPreprocessor preprocessor = new NextFlowPreprocessor();
            script = preprocessor.preprocess( script );
            File f = new File( outputDir, name + ".nf" );
            ApplicationUtils.writeString( f, script );
            String parent = new File( outputDir ).getAbsolutePath().replace( "\\", "/" );
            String[] command = new String[] {"docker", "run", "-v", parent + ":/data", "nextflow/nextflow", "nextflow", "run",
                    "/data/" + f.getName()};

            executeCommand( command );
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
    }

    private void executeCommand(String[] command) throws Exception
    {
        System.out.println( "Executing command " + command );
        Process process = Runtime.getRuntime().exec( command );

        new Thread( new Runnable()
        {
            public void run()
            {
                BufferedReader input = new BufferedReader( new InputStreamReader( process.getInputStream() ) );
                String line = null;

                try
                {
                    while( ( line = input.readLine() ) != null )
                        log.info( line );
                }
                catch( IOException e )
                {
                    e.printStackTrace();
                }
                //                
                //for some reason cwl-runner outputs everything into error stream
                BufferedReader err = new BufferedReader( new InputStreamReader( process.getErrorStream() ) );
                line = null;

                try
                {
                    while( ( line = err.readLine() ) != null )
                        log.info( line );
                }
                catch( IOException e )
                {
                    e.printStackTrace();
                }
            }
        } ).start();

        process.waitFor();

        importResults();
    }

    public class NextFlowPreprocessor
    {
        private WorkflowSettings settings;
        public void NextFlowPreprocessor()
        {

        }

        public void setExportPath(WorkflowSettings settings)
        {
            this.settings = settings;
        }

        public String preprocess(String s) throws Exception
        {
            String[] lines = s.split( "\n" );
            for( int i = 0; i < lines.length; i++ )
            {
                String line = lines[i];
                if( line.contains( "biouml.get(" ) )
                {
                    line = line.replace( "\"", "" );
                    String paramName = line.substring( line.indexOf( "." ) + 1, line.indexOf( "=" ) ).trim();
                    String path = line.substring( line.indexOf( "(" ) + 1, line.lastIndexOf( ")" ) ).trim();
                    DataElement de = DataElementPath.create( path ).getDataElement();
                    if( de instanceof TextDataElement )
                    {
                        String str = ( (TextDataElement)de ).getContent();
                        File dir = new File( outputDir );
                        if( !dir.exists() && !dir.mkdirs() )
                            throw new Exception( "Failed to create directory '" + outputDir + "'." );
                        File exported = new File( dir, de.getName() );
                        ApplicationUtils.writeString( exported, str );
                        lines[i] = "params." + paramName + " = file(\"data/" + exported.getName() + "\")";
                    }

                }
            }
            return StreamEx.of( lines ).joining( "\n" );
        }


    }

    public void importResults() throws Exception
    {
        if( settings.getOutputPath() == null )
            return;
        DataCollection dc = settings.getOutputPath().getDataCollection();
        List<Node> externalOutputs = WDLUtil.getExternalOutputs( diagram );
        for( Node externalOutput : externalOutputs )
        {
            String name = WDLUtil.getName( externalOutput );
            File f = new File( outputDir, name );
            TextFileImporter importer = new TextFileImporter();
            importer.doImport( dc, f, name, null, log );
        }
    }
}