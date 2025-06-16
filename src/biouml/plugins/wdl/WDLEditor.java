package biouml.plugins.wdl;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.StringReader;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.text.StyledEditorKit;

import com.Ostermiller.Syntax.HighlightedDocument;
import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.action.ActionInitializer;
import com.developmentontheedge.application.action.ActionManager;

import biouml.model.Diagram;
import biouml.plugins.wdl.colorer.WDLColorer;
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

@SuppressWarnings ( "serial" )
public class WDLEditor extends EditorPartSupport
{
    private Logger log = Logger.getLogger( WDLEditor.class.getName() );

    private JTabbedPane tabbedPane;

    private WDLEditorPane wdlPane;
    private NextFlowEditorPane nextFlowPane;

    private Diagram diagram;

    private Action[] actions;

    private Action updateWDLAction = new UpdateWDLAction();
    private Action updateDiagramAction = new UpdateDiagramAction();

    private WDLGenerator wdlGenerator;
    private NextFlowGenerator nextFlowGenerator;
    private WDLImporter wdlImporter;

    public WDLEditor()
    {
        tabbedPane = new JTabbedPane( SwingConstants.LEFT );
        add( BorderLayout.CENTER, tabbedPane );
        wdlPane = new WDLEditorPane();
        nextFlowPane = new NextFlowEditorPane();

        tabbedPane.addTab( "WDL", wdlPane );
        tabbedPane.addTab( "NextFlow", nextFlowPane );
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
        return tabbedPane;
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

            ActionInitializer initializer = new ActionInitializer( MessageBundle.class );

            initializer.initAction( updateWDLAction, UpdateWDLAction.KEY );
            initializer.initAction( updateDiagramAction, UpdateDiagramAction.KEY );
            actions = new Action[] {updateWDLAction, updateDiagramAction};
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
                diagram = wdlImporter.generateDiagram( start, null, diagram.getName() );
                wdlImporter.layout( diagram );
                replaceDiagram( diagram );
            }
            catch( Exception ex )
            {
                ex.printStackTrace();
            }
        }
    }
}