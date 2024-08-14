package biouml.plugins.bionetgen.diagram;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.Highlighter.Highlight;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollectionEvent;
import ru.biosoft.access.core.DataCollectionListener;
import ru.biosoft.access.core.DataCollectionVetoException;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.graphics.editor.ViewPaneEvent;
import ru.biosoft.graphics.editor.ViewPaneListener;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.EditorPartSupport;
import ru.biosoft.gui.GUI;
import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.plugins.bionetgen.bnglparser.BNGStart;
import biouml.workbench.diagram.DiagramDocument;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.application.action.ActionInitializer;
import com.developmentontheedge.application.action.ActionManager;

@SuppressWarnings ( "serial" )
public class BionetgenEditor extends EditorPartSupport implements ViewPaneListener, DataCollectionListener, PropertyChangeListener
{
    protected Logger log = Logger.getLogger(BionetgenEditor.class.getName());

    protected JEditorPane bionetgenPane;
    protected JComponent view;
    protected Diagram diagram;
    protected Bionetgen bionetgen;

    /**
     * Flag indicating that we are in the middle of BioNetGen application
     * (during that process new document is created, tabs are closed and open again,
     * but this should not cause new BioNetGen applying).
     */
    private boolean bionetgenIsApplying = false;

    public BionetgenEditor()
    {
        bionetgenPane = new JEditorPane("text/plain", "");
        bionetgenPane.setFont(new Font("Monospaced", Font.PLAIN, 12));

        view = new JScrollPane(bionetgenPane);
        view.setEnabled(false);
    }

    @Override
    public boolean canExplore(Object model)
    {
        return model instanceof Diagram && BionetgenUtils.checkDiagramType((Diagram)model);
    }

    @Override
    public void explore(Object model, Document document)
    {
        if( bionetgenIsApplying )
            return;

        boolean isNewModel = ( model != this.model );
        if( isNewModel && this.model != null )
        {
            String bngText = BionetgenUtils.getBionetgenAttr(diagram);
            if( bngText != null && !bngText.equals(getText()) )
                applyBionetgenOnClose(this.document);
        }
        if( diagram != null )
            release(diagram);

        super.explore(model, document);
        register((Diagram)model);

        String bngText = BionetgenUtils.getBionetgenAttr(diagram);
        if( bngText == null )
        {
            bionetgen = new Bionetgen( diagram );
            bngText = bionetgen.generateText();
            bionetgenPane.setText(bngText);
            BionetgenUtils.setBionetgenAttr(diagram, bngText);
        }
        else if( isNewModel )
        {
            if( !getText().equals( bngText ) )
            {
                bionetgen = new Bionetgen( diagram );
                bionetgenPane.setText( bngText );
                applyBionetgen( true );
            }
        }
    }

    private void register(Diagram diagram)
    {
        model = diagram;
        this.diagram = (Diagram)model;
        diagram.addDataCollectionListener(this);
        diagram.addPropertyChangeListener(this);
        if( diagram.getRole() != null )
            diagram.getRole(EModel.class).getVariables().addDataCollectionListener(this);
        if( document != null )
            ( (DiagramDocument)document ).getViewPane().addViewPaneListener(this);
    }

    private void release(Diagram diagram)
    {
        if( diagram != null )
        {
            diagram.removePropertyChangeListener(this);
            diagram.removeDataCollectionListener(this);
            if( diagram.getRole() != null )
                diagram.getRole(EModel.class).getVariables().removeDataCollectionListener(this);
        }
        if( document != null )
            ( (DiagramDocument)document ).getViewPane().removeViewPaneListener(this);
    }

    @Override
    public void save()
    {
        if( diagram == null )
            return;
        String newText = getText().replaceAll( BionetgenTextGenerator.HIGHLIGHT_START, "" ).replaceAll(
                BionetgenTextGenerator.HIGHLIGHT_END, "" );
        if( newText.equals( BionetgenUtils.getBionetgenAttr( diagram ) ) )
            return;

        BionetgenUtils.setBionetgenAttr( diagram, newText );
    }

    public String getText()
    {
        return bionetgenPane.getText();
    }

    public void setText(String text)
    {
        Highlighter highlighter = bionetgenPane.getHighlighter();
        Highlighter.Highlight[] highlights = highlighter.getHighlights();

        for( Highlight highlight : highlights )
        {
            if( highlight.getPainter() instanceof DefaultHighlighter.DefaultHighlightPainter )
                highlighter.removeHighlight(highlight);
        }

        class HighlightArea
        {
            int start;
            int end;
            public HighlightArea(int start, int end)
            {
                this.start = start;
                this.end = end;
            }
        }

        ArrayList<HighlightArea> areas = new ArrayList<>();
        String textIteration = text;
        String highlightStart = BionetgenTextGenerator.HIGHLIGHT_START;
        String highlightEnd = BionetgenTextGenerator.HIGHLIGHT_END;
        while( textIteration.contains(highlightStart) )
        {
            int start = textIteration.indexOf(highlightStart);
            textIteration = textIteration.replaceFirst(highlightStart, "");
            areas.add(new HighlightArea(start, textIteration.indexOf(highlightEnd)));
            textIteration = textIteration.replaceFirst(highlightEnd, "");
        }
        text = text.replaceAll(highlightStart, "").replaceAll(highlightEnd, "");

        int position = bionetgenPane.getCaretPosition();
        bionetgenPane.setText(text);
        if( position < bionetgenPane.getDocument().getLength() )
            bionetgenPane.setCaretPosition( position );

        try
        {
            for( HighlightArea area : areas )
                highlighter.addHighlight(area.start, area.end, new DefaultHighlighter.DefaultHighlightPainter(Color.GRAY));
        }
        catch( BadLocationException e )
        {
            log.log(Level.SEVERE, "Can't highlight text");
        }
    }

    @Override
    public JComponent getView()
    {
        return view;
    }

    public Bionetgen getBionetgen()
    {
        return bionetgen;
    }

    protected Action[] actions;
    protected Action applyBionetgen = new ApplyBionetgen();
    protected Action deployBionetgen = new DeployBionetgen();
    protected Action recreateText = new RecreateText();

    @Override
    public Action[] getActions()
    {
        ActionManager actionManager = Application.getActionManager();
        if( actions == null )
        {
            actionManager.addAction(ApplyBionetgen.KEY, applyBionetgen);
            actionManager.addAction(DeployBionetgen.KEY, deployBionetgen);
            actionManager.addAction(RecreateText.KEY, recreateText);

            ActionInitializer initializer = new ActionInitializer(MessageBundle.class);

            initializer.initAction(applyBionetgen, ApplyBionetgen.KEY);
            initializer.initAction(deployBionetgen, DeployBionetgen.KEY);
            initializer.initAction(recreateText, RecreateText.KEY);

            actions = new Action[] {applyBionetgen, deployBionetgen, recreateText};
        }

        return actions.clone();
    }

    class ApplyBionetgen extends AbstractAction
    {
        public static final String KEY = "Apply";

        public ApplyBionetgen()
        {
            super(KEY);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            applyBionetgen(true);
        }
    }

    class DeployBionetgen extends AbstractAction
    {
        public static final String KEY = "Deploy";

        public DeployBionetgen()
        {
            super(KEY);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            deployBionetgen();
        }
    }

    class RecreateText extends AbstractAction
    {
        public static final String KEY = "Recreate text";

        public RecreateText()
        {
            super(KEY);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            recreateText();
        }
    }

    protected void recreateText()
    {
        diagram.getAttributes().remove(Bionetgen.BIONETGEN_ATTR);
        bionetgen = new Bionetgen(diagram);
        setText(bionetgen.generateText());
        save();
    }

    protected void deployBionetgen()
    {
        try
        {
            BNGStart start = BionetgenAstCreator.generateAstFromText(getText());
            String newName = diagram.getName() + "_deployed";
            start.setName(newName);
            Diagram newDiagram = BionetgenDiagramGenerator.generateDiagram( start, diagram.getOrigin(), newName, false );
            newDiagram = BionetgenDiagramDeployer.deployBNGDiagram(newDiagram);
            Document doc = new DiagramDocument(newDiagram);
            GUI.getManager().addDocument(doc);
        }
        catch( Exception ex )
        {
            showErrorMessage(ex);
        }
    }

    protected void applyBionetgenOnClose(Document closedDocument)
    {
        bionetgenIsApplying = true;
        try
        {
            Diagram newDiagram = bionetgen.generateDiagram(getText());
            BionetgenUtils.setBionetgenAttr(newDiagram, getText());
            release(diagram);
            Document document = new DiagramDocument(newDiagram);
            Document currentDoc = GUI.getManager().getCurrentDocument();
            GUI.getManager().replaceDocument(closedDocument, document);
            GUI.getManager().addDocument(currentDoc);
        }
        catch( Exception ex )
        {
            showErrorMessage(ex);
        }
        finally
        {
            bionetgenIsApplying = false;
        }
    }

    protected void applyBionetgen(boolean selectBNGTab)
    {
        bionetgenIsApplying = true;
        try
        {
            Diagram newDiagram = bionetgen.generateDiagram(getText());
            this.document = new DiagramDocument(newDiagram);
            release(diagram);
            register(newDiagram);
            BionetgenUtils.setBionetgenAttr(newDiagram, getText());
            BNGStart oldAstStart = bionetgen.bngStart;
            String oldText = getText();

            GUI.getManager().replaceDocument(GUI.getManager().getCurrentDocument(), this.document);

            bionetgen.bngStart = oldAstStart;
            setText(oldText);
            bionetgen.diagram = newDiagram;
            this.document.update();

            if( selectBNGTab )
            {
                GUI.getManager().showViewPart( BionetgenEditor.this );
            }
        }
        catch( Exception ex )
        {
            showErrorMessage(ex);
        }
        finally
        {
            bionetgenIsApplying = false;
        }
    }

    private void showErrorMessage(Exception ex)
    {
        if( ex instanceof BionetgenUtils.BionetgenException )
        {
            log.warning( ex.getMessage() );
            ApplicationUtils.errorBox( ex.getMessage() );
        }
        else
        {
            String error = ExceptionRegistry.log( ex );
            log.log(Level.SEVERE,  error );
            ApplicationUtils.errorBox( "Incorrect bionetgen text: " + error );
        }
    }

    @Override
    public void elementAdded(DataCollectionEvent e) throws Exception
    {
        updateText( e );
    }

    @Override
    public void elementWillAdd(DataCollectionEvent e) throws DataCollectionVetoException, Exception
    {
    }

    @Override
    public void elementChanged(DataCollectionEvent e) throws Exception
    {
        while( e.getPrimaryEvent() != null )
            e = e.getPrimaryEvent();

        updateText( e );
    }

    @Override
    public void elementWillChange(DataCollectionEvent e) throws DataCollectionVetoException, Exception
    {
        while( e.getPrimaryEvent() != null )
            e = e.getPrimaryEvent();

        if( e.getType() == DataCollectionEvent.ELEMENT_WILL_REMOVE )
        {
            updateText( e );
        }
    }

    @Override
    public void elementRemoved(DataCollectionEvent e) throws Exception
    {
        updateText( e );
    }

    @Override
    public void elementWillRemove(DataCollectionEvent e) throws DataCollectionVetoException, Exception
    {
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if( isIgnoringProperty(evt.getPropertyName()) )
            return;
        updateText( evt );
    }

    private void updateText(DataCollectionEvent e)
    {
        String currentText = getText();
        String text = currentText;
        try
        {
            text = bionetgen.updateText( e );
        }
        catch( Exception ex )
        {
            showErrorMessage( ex );
        }
        if( !currentText.equals( text ) )
        {
            setText( text );
            save();
        }
    }

    private void updateText(PropertyChangeEvent evt)
    {
        String currentText = getText();
        String text = currentText;
        try
        {
            text = bionetgen.updateText( evt );
        }
        catch( Exception ex )
        {
            showErrorMessage( ex );
        }
        if( !currentText.equals( text ) )
        {
            setText( text );
            save();
        }
    }

    private boolean isIgnoringProperty(String propertyName)
    {
        return propertyName.equals("location") || propertyName.equals("variables") || propertyName.endsWith(Bionetgen.BIONETGEN_ATTR)
                || propertyName.equals( "path" ) || propertyName.endsWith( Bionetgen.BIONETGEN_LINK );
    }

    @Override
    public void modelChanged(Object model)
    {
        if( bionetgenIsApplying )
            return;
        if( diagram != null )
        {
            String bngText = BionetgenUtils.getBionetgenAttr(diagram);
            if( bngText != null && !bngText.equals(getText()) )
                applyBionetgenOnClose(document);
            release(diagram);

            this.model = null;
            this.diagram = null;
        }

        if( ! ( model instanceof Diagram ) )
            return;

        String bngText = BionetgenUtils.getBionetgenAttr((Diagram)model);
        if( bngText == null )
            return;

        register((Diagram)model);
        bionetgen = new Bionetgen(diagram);
        bionetgenPane.setText(bngText);

        applyBionetgen(false);
    }

    @Override
    public void onClose()
    {
        if( bionetgenIsApplying || diagram == null )
            return;

        String bngText = BionetgenUtils.getBionetgenAttr(diagram);
        if( bngText == null || bngText.equals(getText()) )
            return;

        applyBionetgen(false);
    }

    @Override
    public void mousePressed(ViewPaneEvent e)
    {
        String text = bionetgen.highlight(e);
        if( text != null )
            setText(text);
    }
    @Override
    public void mouseClicked(ViewPaneEvent e)
    {
    }
    @Override
    public void mouseReleased(ViewPaneEvent e)
    {
    }
    @Override
    public void mouseEntered(ViewPaneEvent e)
    {
    }
    @Override
    public void mouseExited(ViewPaneEvent e)
    {
    }
    @Override
    public void mouseDragged(ViewPaneEvent e)
    {
    }
    @Override
    public void mouseMoved(ViewPaneEvent e)
    {
    }
}
