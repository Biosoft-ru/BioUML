package biouml.plugins.wdl;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.FocusListener;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.text.StyledEditorKit;

import com.Ostermiller.Syntax.HighlightedDocument;
import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.action.ActionInitializer;
import com.developmentontheedge.application.action.ActionManager;
import com.developmentontheedge.log.PatternFormatter;
import com.developmentontheedge.log.TextPaneAppender;

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
    protected Logger log = Logger.getLogger( WDLEditor.class.getName() );

    protected WDLTab wdlTab;
    protected Diagram diagram;

    protected Action[] actions;

    protected Action updateWDLAction = new UpdateWDLAction();
    protected Action updateDiagramAction = new UpdateDiagramAction();

    protected WDLGenerator wdlGenerator;

    public WDLEditor()
    {
        wdlTab = new WDLTab();
        wdlGenerator = new WDLGenerator();
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
            String wdl = wdlGenerator.generateWDL( (Diagram)model );
            setText( wdl );
        }
        catch( Exception ex )
        {
            setText( "" );
        }
    }

    @Override
    public void save()
    {

    }

    @Override
    public JComponent getView()
    {
        return wdlTab;
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

    public void setText(String text)
    {
        wdlTab.setText( text );
    }
    
    public String getText()
    {
        return wdlTab.getText();
    }

    public class WDLTab extends EditorPartSupport
    {
        protected Logger log = Logger.getLogger( WDLTab.class.getName() );

        protected TextPaneAppender appender;
        protected WDLEditorPane wdlPane;

        protected String[] categoryList = {"biouml.plugins.wdl"};

        private JSplitPane splitPane = new JSplitPane();

        @Override
        public void addFocusListener(FocusListener listener)
        {
            this.wdlPane.addFocusListener( listener );
        }

        public WDLTab()
        {
            initSplitPane();
            add( splitPane );
        }

        private JSplitPane initSplitPane()
        {
            wdlPane = new WDLEditorPane();
            appender = new TextPaneAppender( new PatternFormatter( "%4$s :  %5$s%n" ), "Application Log" );
            appender.setLevel( Level.SEVERE );
            appender.addToCategories( categoryList );
            JScrollPane scroll = new JScrollPane( wdlPane );
            splitPane = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, false, scroll, appender.getLogTextPanel() );
            splitPane.setResizeWeight( 0.4 );
            return splitPane;
        }

        private void setText(String text)
        {
            wdlPane.setTextSilent( text );
        }
        
        private String getText()
        {
            return wdlPane.getText();
        }

        public JEditorPane getEditorPane()
        {
            return wdlPane;
        }

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

        /** Method to set text without notifying listeners */
        public void setTextSilent(String str)
        {
            setText( str );
        }

        /**
         * Sets the position of the text insertion caret for the
         * <code>AntimonyEditorPane</code>.  The position
         * must be greater than 0 or else an exception is thrown. 
         * If the position is greater than the length of the component's text, 
         * it will be reset to the length of the document.
         */
        //        @Override
        //        public void setCaretPosition(int position)
        //        {
        //            javax.swing.text.Document doc = getDocument();
        //            if( doc != null )
        //            {
        //                if( position < 0 )
        //                {
        //                    throw new IllegalArgumentException( "bad position: " + position );
        //                }
        //                else if( position > doc.getLength() )
        //                    position = doc.getLength();
        //
        //                getCaret().setDot( position );
        //            }
        //        }
    }
    
    public void updateDiagram(Diagram newDiagram)
    {
        Document currentDocument = GUI.getManager().getCurrentDocument();
        this.document = ( DiagramUtility.isComposite(newDiagram) ) ? new CompositeDiagramDocument(newDiagram)
                : new DiagramDocument(newDiagram);

        this.document.update();

        if( GUI.getManager().getCurrentDocument() != null )
        {
            GUI.getManager().replaceDocument(currentDocument, this.document);
            GUI.getManager().getDocumentViewAccessProvider().enableDocumentActions(true);
        }
        else
        {
            log.info("replacing document, but document is null");
        }
//    }
//    catch( Exception ex )
//    {
//        ExceptionRegistry.log(ex);
//        ApplicationUtils.errorBox("Incorrect antimony text: " + ex.getMessage());
//        if( antimony.astStart == null )
//            removeAntimony();
//    }
//    finally
//    {
//        antimonyIsApplying = false;
//    }
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
                setText( wdl );
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
                AstStart start = new WDLParser().parse( new StringReader(getText()) );
                diagram = new WDLImporter().generateDiagram( start, null, diagram.getName() );
                updateDiagram(diagram);
            }
            catch( Exception ex )
            {
                ex.printStackTrace();
            }
        }
    }
}