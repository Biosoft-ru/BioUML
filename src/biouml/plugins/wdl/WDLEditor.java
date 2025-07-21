package biouml.plugins.wdl;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
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
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.text.StyledEditorKit;

import com.Ostermiller.Syntax.HighlightedDocument;
import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.application.action.ActionInitializer;
import com.developmentontheedge.application.action.ActionManager;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetSupport;
import com.developmentontheedge.beans.annot.PropertyName;
import com.developmentontheedge.beans.swing.PropertyInspector;
import com.developmentontheedge.beans.swing.PropertyInspectorEx;
import com.developmentontheedge.log.PatternFormatter;
import com.developmentontheedge.log.TextPaneAppender;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Node;
import biouml.plugins.wdl.colorer.WDLColorer;
import biouml.plugins.wdl.diagram.WDLConstants;
import biouml.plugins.wdl.diagram.WDLDiagramType;
import biouml.plugins.wdl.diagram.WDLImporter;
import biouml.plugins.wdl.parser.AstStart;
import biouml.plugins.wdl.parser.WDLParser;
import biouml.standard.diagram.DiagramUtility;
import biouml.workbench.diagram.CompositeDiagramDocument;
import biouml.workbench.diagram.DiagramDocument;
import one.util.streamex.StreamEx;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.FileExporter;
import ru.biosoft.access.TextFileImporter;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.TextDataElement;
import ru.biosoft.access.file.FileDataElement;
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

    JScrollPane scrollPane;

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

        tabbedPane.addTab( "WDL", new JScrollPane( wdlPane ) );
        tabbedPane.addTab( "NextFlow", new JScrollPane( nextFlowPane ) );
        tabbedPane.addTab( "Settings", inspector );
        appender = new TextPaneAppender( new PatternFormatter( "%4$s :  %5$s%n" ), "Application Log" );
        appender.setLevel( Level.SEVERE );
        appender.addToCategories( categoryList );

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

        DynamicProperty settingsProperty = diagram.getAttributes().getProperty( WDLConstants.SETTINGS_ATTR );
        if( settingsProperty == null )
        {
            settingsProperty = new DynamicProperty( WDLConstants.SETTINGS_ATTR, WorkflowSettings.class, new WorkflowSettings() );
            diagram.getAttributes().add( settingsProperty );
        }
        settings = (WorkflowSettings)settingsProperty.getValue();
        settings.initParameters( diagram );
        inspector.explore( settings );
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
        private DynamicPropertySet parameters = new DynamicPropertySetSupport();

        public WorkflowSettings()
        {
            System.out.println( "Load" );
        }

        public void initParameters(Diagram diagram)
        {
            List<Node> externalParameters = WDLUtil.getExternalParameters( diagram );
            for( Node externalParameter : externalParameters )
            {
                String type = WDLUtil.getType( externalParameter );
                String name = WDLUtil.getName( externalParameter );
                Object value = WDLUtil.getExpression( externalParameter );
                Class clazz = String.class;
                if( type.equals( "File" ) || type.equals( "Array[File]" ) )
                {
                    if( value != null )
                        value = DataElementPath.create( value.toString() );
                    clazz = DataElementPath.class;
                }
                DynamicProperty dp = new DynamicProperty( name, clazz, value );
                parameters.add( dp );
            }
        }

        public void exportCollections(String outputDir) throws Exception
        {
            for( DynamicProperty dp : parameters )
            {
                if( dp.getValue() instanceof DataElementPath )
                {
                    DataElement de = ( (DataElementPath)dp.getValue() ).getDataElement();
                    export( de, new File( outputDir ) );
                }
            }
        }

        public File generateParametersJSON(String outputDir) throws IOException
        {
            File json = new File( outputDir, "parameters.json" );
            try (BufferedWriter bw = new BufferedWriter( new FileWriter( json ) ))
            {
                bw.write( "{\n" );
                boolean first = true;
                for( DynamicProperty dp : parameters )
                {
                    Object value = dp.getValue();
                    if( value instanceof DataElementPath dep )
                        value = "\"" + dep.getName() + "\"";
                    else
                        value = "\"" + value.toString() + "\"";
                    if( !first )
                        bw.write( "," );
                    first = false;
                    bw.write( "\"" + dp.getName() + "\"" + " : " + value + "\n" );
                }
                bw.write( "}\n" );
            }
            return json;
        }

        @PropertyName ( "Parameters" )
        public DynamicPropertySet getParameters()
        {
            return parameters;
        }

        public void setParameters(DynamicPropertySet parameters)
        {
            this.parameters = parameters;
        }

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

    public static class WorkflowSettingsBeanInfo extends BeanInfoEx2<WorkflowSettings>
    {
        public WorkflowSettingsBeanInfo(WorkflowSettings settings)
        {
            super( WorkflowSettings.class );
        }

        @Override
        public void initProperties()
        {
            add( "parameters" );
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
                String text = getWDL();
                text = text.replace( "<<<", "{" ).replace( ">>>", "}" );//TODO: fix parsing <<< >>>
                AstStart start = new WDLParser().parse( new StringReader( text ) );
                diagram = wdlImporter.generateDiagram( start, diagram.getOrigin(), diagram.getName() );
                wdlImporter.layout( diagram );
                replaceDiagram( diagram );
                setDiagram( diagram );
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

            File config = new File( outputDir, "nextflow.config" );
            ApplicationUtils.writeString( config, "docker.enabled = true" );

            File json = settings.generateParametersJSON( outputDir );

            settings.exportCollections( outputDir );

            generateFunctions( outputDir );

            for( DataElement de : StreamEx.of( WDLUtil.getImports( diagram ) ).map( f -> f.getSource().getDataElement() ) )
                export( de, new File( outputDir ) );

            NextFlowPreprocessor preprocessor = new NextFlowPreprocessor();
            script = preprocessor.preprocess( script );
            File f = new File( outputDir, name + ".nf" );
            ApplicationUtils.writeString( f, script );
            String parent = new File( outputDir ).getAbsolutePath().replace( "\\", "/" );

            String[] command = new String[] {"wsl", "--cd", parent, "nextflow", f.getName(), "-c", "nextflow.config", "-params-file",
                    json.getName()};
            //            String[] command = new String[] {"docker", "run", "-v", parent + ":/data", "nextflow/nextflow", "nextflow", "run",
            //                    "/data/" + f.getName()};

            executeCommand( command );

            importResults();
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
    }

    private void executeCommand(String[] command) throws Exception
    {
        System.out.println( "Executing command " + StreamEx.of( command ).joining( " " ) );
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
            return s.replace( "~{", "${" );
            //            String[] lines = s.split( "\n" );
            //            for( int i = 0; i < lines.length; i++ )
            //            {
            //                String line = lines[i];
            //                if( line.contains( "biouml.get(" ) )
            //                {
            //                    line = line.replace( "\"", "" );
            //                    String paramName = line.substring( line.indexOf( "." ) + 1, line.indexOf( "=" ) ).trim();
            //                    String path = line.substring( line.indexOf( "(" ) + 1, line.lastIndexOf( ")" ) ).trim();
            //                    DataElement de = DataElementPath.create( path ).getDataElement();
            //                    export( de, new File( outputDir ) );
            //                    lines[i] = "params." + paramName + " = file(\"" + de.getName() + "\")";
            //                }
            //            }
            //            return StreamEx.of( lines ).joining( "\n" );
        }


    }

    public void importResults() throws Exception
    {
        if( settings.getOutputPath() == null )
            return;
        DataCollection dc = settings.getOutputPath().getDataCollection();

        for( Compartment n : WDLUtil.getAllCalls( diagram ) )
        {
            String taskRef = WDLUtil.getCallName( n );
            String folderName = ( taskRef );
            File folder = new File( outputDir, folderName );
            if( !folder.exists() || !folder.isDirectory() )
            {
                log.info( "No results for " + n.getName() );
                continue;
            }
            DataCollection nested = DataCollectionUtils.createSubCollection( dc.getCompletePath().getChildPath( folderName ) );
            for( File f : folder.listFiles() )
            {
                TextFileImporter importer = new TextFileImporter();
                importer.doImport( nested, f, f.getName(), null, log );
            }
        }
    }

    public static void export(DataElement de, File dir) throws Exception
    {
        if( !dir.exists() && !dir.mkdirs() )
            throw new Exception( "Failed to create directory '" + dir.getName() + "'." );
        if( de instanceof TextDataElement )
        {
            String str = ( (TextDataElement)de ).getContent();
            File exported = new File( dir, de.getName() );
            ApplicationUtils.writeString( exported, str );
        }
        else if( de instanceof FileDataElement )
        {
            File exported = new File( dir, de.getName() );
            FileExporter exporter = new FileExporter();
            exporter.doExport( de, exported );
        }
        else if( de instanceof Diagram )
        {
            NextFlowGenerator generator = new NextFlowGenerator();
            String nextFlow = generator.generateNextFlow( (Diagram)de );
            File exported = new File( dir, de.getName() );
            ApplicationUtils.writeString( exported, nextFlow );
        }
        else if( de instanceof DataCollection )
        {
            File exportedDir = new File( dir, de.getName() );
            exportedDir.mkdirs();
            for( Object innerDe : ( (DataCollection<?>)de ) )
                export( (DataElement)innerDe, new File( dir, de.getName() ) );
        }
    }

    public static File generateFunctions(String outputDir) throws IOException
    {
        File result = new File( outputDir, "biouml_function.nf" );
        try (BufferedWriter bw = new BufferedWriter( new FileWriter( result ) ))
        {
            String baseName = """
                    def basename(filePath) {
                        def fname = filePath instanceof Path ? filePath.getFileName().toString() : filePath.toString()
                        return fname.replaceFirst(/\\.[^\\.]+$/, '')
                    }""";
            bw.write( baseName );
        }
        return result;

    }
}