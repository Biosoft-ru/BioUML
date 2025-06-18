package biouml.plugins.antimony;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.Highlighter.Highlight;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationFrame;
import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.application.action.ActionInitializer;
import com.developmentontheedge.application.action.ActionManager;
import com.developmentontheedge.application.dialog.OkCancelDialog;
import com.developmentontheedge.log.PatternFormatter;
import com.developmentontheedge.log.TextPaneAppender;

import biouml.model.Diagram;
import biouml.model.ModelDefinition;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.plot.PlotsInfo;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.type.DiagramInfo;
import biouml.workbench.diagram.CompositeDiagramDocument;
import biouml.workbench.diagram.DiagramDocument;
import ru.biosoft.access.core.DataCollectionEvent;
import ru.biosoft.access.core.DataCollectionListener;
import ru.biosoft.access.core.DataCollectionVetoException;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.graphics.editor.ViewPane;
import ru.biosoft.graphics.editor.ViewPaneEvent;
import ru.biosoft.graphics.editor.ViewPaneListener;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.EditorPartSupport;
import ru.biosoft.gui.EditorsTabbedPane;
import ru.biosoft.gui.GUI;
import ru.biosoft.gui.ViewPart;
import ru.biosoft.util.TextUtil2;


@SuppressWarnings ( "serial" )
public class AntimonyEditor extends EditorPartSupport implements ViewPaneListener, DataCollectionListener, PropertyChangeListener
{
    protected Logger log = Logger.getLogger(AntimonyEditor.class.getName());

    protected AntimonyTab antimonyTab;
    protected Diagram diagram;
    protected Antimony antimony = new Antimony(null);
    private Timer timer;

    protected Action[] actions;

    protected Action clearLogAction = new ClearLogAction();
    private Action enableAntimony = new EnableAntimony();
    private Action enableAutoUpdate = new EnableAutoupdate();
    protected Action applyAntimony = new ApplyAntimony();

    private boolean enabledAuto;

    /**
     * Flag indicating that we are in the middle of antimony application
     * (during that process new document is created, tabs are closed and open again,
     * but this should not cause new antimony applying).
     */
    private boolean antimonyIsApplying = false;

    public AntimonyEditor()
    {
        antimonyTab = new AntimonyTab();


        antimonyTab.addFocusListener(new FocusListener()
        {

            @Override
            public void focusGained(FocusEvent e)
            {
                if( enabledAuto )
                    setTimer(1000);
            }

            @Override
            public void focusLost(FocusEvent e)
            {
                stopTimer();
            }
        });
    }

    @Override
    public boolean canExplore(Object model)
    {
        return model instanceof Diagram && AntimonyUtility.checkDiagramType((Diagram)model);
    }

    @Override
    public void explore(Object model, Document document)
    {
        //don't generate antimony ASTree, if needGenerateNewModel == false
        if( antimonyIsApplying )
            return;

        //view.setEnabled(true);

        //don't apply antimony if model is old
        boolean isNewModel;

        //don't generate antimony ASTree, if this is subdiagram
        if( ( (Diagram)model ).getParent() instanceof ModelDefinition )
        {
            ModelDefinition modelDefinition = (ModelDefinition) ( (Diagram)model ).getParent();

            removeListeners((Diagram)model, document);
            addListeners((Diagram)model, document, this);


            isNewModel = ( modelDefinition.getParent() != this.model );
            super.explore(modelDefinition.getParent(), Document.getCurrentDocument());
            if( diagram != null )
                release(diagram);

            register((Diagram)modelDefinition.getParent());
        }
        else
        {
            DiagramDocument doc = (DiagramDocument)Document.getCurrentDocument();

            if( doc != null && doc.getModel() != model )//model was changed?
            {
                removeListeners((Diagram)model, document);
                addListeners((Diagram)model, document, this);
                isNewModel = ( doc.getModel() != this.model );
                super.explore(doc.getModel(), doc);
                if( diagram != null )
                    release(diagram);

                register((Diagram)doc.getModel());
            }
            else
            {
                isNewModel = ( model != this.model );
                if( isNewModel && this.model != null ) //model was changed, we need apply antimony to old model
                {
                    String antimonyText = AntimonyUtility.getAntimonyAttribute(diagram, AntimonyConstants.ANTIMONY_TEXT_ATTR);
                    if( antimonyText != null && !antimonyText.equals(getText()) )
                        applyAntimonyOnClose(this.document);
                }

                if( diagram != null )
                    release(diagram);

                super.explore(model, document);
                register((Diagram)model);
            }

        }

        String antimonyText = AntimonyUtility.getAntimonyAttribute(diagram, AntimonyConstants.ANTIMONY_TEXT_ATTR);
        antimony.init(diagram);

        if( antimonyText == null )
        {
            antimonyTab.createNewAntimonyText();
        }
        else if( isNewModel )
        {
            if( !getText().equals(antimonyText) )
            {
                setText(antimonyText);
                applyAntimony();
            }
        }
    }

    @Override
    public void save()
    {
        if( diagram == null )
            return;

        String antimonyText = AntimonyUtility.getAntimonyAttribute(diagram, AntimonyConstants.ANTIMONY_TEXT_ATTR);
        if( antimonyText != null )
        {
            try
            {
                //String text = antimony.generateText();
                String text = getText();
                text = text.replaceAll(AntimonyConstants.HIGHLIGHT_START, "").replaceAll(AntimonyConstants.HIGHLIGHT_END, "");
                AntimonyUtility.setAntimonyAttribute(diagram, text, AntimonyConstants.ANTIMONY_TEXT_ATTR);
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Can't add antimony attribute");
            }
        }
    }

    //
    //
    //    private String saveHighlightArea(ArrayList<Integer> leftHighlightersIndex, ArrayList<Integer> rightHighlightersIndex,
    //            String textIteration)
    //    {
    //        int start = textIteration.indexOf(AntimonyConstants.HIGHLIGHT_START);
    //        textIteration = textIteration.replaceFirst(AntimonyConstants.HIGHLIGHT_START, "");
    //
    //        int nextStart = textIteration.indexOf(AntimonyConstants.HIGHLIGHT_START);
    //        int end = textIteration.indexOf(AntimonyConstants.HIGHLIGHT_END);
    //        if( nextStart != -1 && nextStart < end )
    //            textIteration = saveHighlightArea(leftHighlightersIndex, rightHighlightersIndex, textIteration);
    //
    //        end = textIteration.indexOf(AntimonyConstants.HIGHLIGHT_END);
    //        leftHighlightersIndex.add(start);
    //        rightHighlightersIndex.add(end);
    //        textIteration = textIteration.replaceFirst(AntimonyConstants.HIGHLIGHT_END, "");
    //        return textIteration;
    //    }

    @Override
    public JComponent getView()
    {
        return antimonyTab;
    }

    public void setDiagram(Diagram diagram)
    {
        this.diagram = diagram;
    }

    public Antimony getAntimony()
    {
        return antimony;
    }

    @Override
    public Action[] getActions()
    {
        ActionManager actionManager = Application.getActionManager();
        if( actions == null )
        {
            actionManager.addAction(EnableAntimony.KEY, enableAntimony);
            actionManager.addAction(EnableAutoupdate.KEY, enableAutoUpdate);
            actionManager.addAction(ApplyAntimony.KEY, applyAntimony);
            actionManager.addAction(ClearLogAction.KEY, clearLogAction);
            ActionInitializer initializer = new ActionInitializer(MessageBundle.class);

            initializer.initAction(enableAntimony, EnableAntimony.KEY);
            initializer.initAction(enableAutoUpdate, EnableAutoupdate.KEY);
            initializer.initAction(applyAntimony, ApplyAntimony.KEY);
            initializer.initAction(clearLogAction, ClearLogAction.KEY);
            actions = new Action[] {enableAntimony, enableAutoUpdate, applyAntimony, clearLogAction};
            ( (EnableAntimony)enableAntimony ).setPressed(true);
        }

        return actions.clone();
    }

    class EnableAntimony extends AbstractAction
    {
        public static final String KEY = "Enable Antimony";

        public EnableAntimony()
        {
            super(KEY);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {

            if( enabled )
            {
                JPanel panel = new JPanel();
                panel.setLayout(new BorderLayout());
                panel.add(new JLabel("Antimony will be reset and all text formatting will be lost. \n Are you sure?"), BorderLayout.CENTER);
                panel.setBorder(new EmptyBorder(10, 10, 10, 10));
                OkCancelDialog dialog = new OkCancelDialog(Application.getApplicationFrame(), "Warning", panel);
                if( !dialog.doModal() )
                    return;
            }

            enabled = !enabled;
            if( enabled )
            {
                applyAntimony();
            }
            else
            {
                removeAntimony();
            }
            setPressed(enabled);
        }

        public void setPressed(boolean pressed)
        {
            String iconPath = pressed ? "disableAntimony.gif" : "enableAntimony.jpg";
            URL url = getClass().getResource("resources/" + iconPath);
            if( url != null )
                putValue(Action.SMALL_ICON, new javax.swing.ImageIcon(url));
            antimonyTab.getEditorPane().setEditable(pressed);
            enableAutoUpdate.setEnabled(pressed);
            applyAntimony.setEnabled(pressed);
        }
    }

    class EnableAutoupdate extends AbstractAction
    {
        public static final String KEY = "Enable Autoupdate";

        public EnableAutoupdate()
        {
            super(KEY);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            enabledAuto = !enabledAuto;
            setPressed(enabledAuto);
        }

        public void setPressed(boolean pressed)
        {
            String iconPath = pressed ? "disableAuto.png" : "enableAuto.gif";

            URL url = getClass().getResource("resources/" + iconPath);
            if( url != null )
                putValue(Action.SMALL_ICON, new javax.swing.ImageIcon(url));
        }
    }

    class ApplyAntimony extends AbstractAction
    {
        public static final String KEY = "Apply";

        public ApplyAntimony()
        {
            super(KEY);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            applyAntimony();
        }
    }

    class ClearLogAction extends AbstractAction
    {
        public static final String KEY = "Clear log";

        public ClearLogAction()
        {
            super(KEY);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            antimonyTab.appender.getLogTextPanel().setText("");
        }
    }


    /**
     * Method for antimony application in the case when user goes to other diagram (document is changed)
     * In that case we apply antimony to closed diagram automatically (to preserve antimony text)
     */
    protected void applyAntimonyOnClose(Document closedDocument)
    {
        try
        {
            antimonyIsApplying = true;
            Diagram newDiagram = antimony.generateDiagram(getText());
            AntimonyUtility.setAntimonyAttribute(newDiagram, getText(), AntimonyConstants.ANTIMONY_TEXT_ATTR);
            release(diagram);
            Document document = DiagramUtility.isComposite(newDiagram) ? new CompositeDiagramDocument(newDiagram)
                    : new DiagramDocument(newDiagram);
            Document currentDoc = GUI.getManager().getCurrentDocument();
            GUI.getManager().replaceDocument(closedDocument, document);
            GUI.getManager().addDocument(currentDoc); //hack to actually change document to one selected by user
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
        finally
        {
            antimonyIsApplying = false;
        }
    }

    protected void applyAntimony()
    {
        applyAntimony(true);
    }

    protected void applyAntimony(boolean setAntimonyTab)
    {
        antimonyIsApplying = true;

        try
        {
            int caretPosition = antimonyTab.getEditorPane().getCaretPosition();
            Diagram newDiagram;
            try
            {
                newDiagram = antimony.generateDiagram(getText());
            }
            catch( Exception ex )
            {
                ExceptionRegistry.log(ex);
                return;
            }
            Document currentDocument = GUI.getManager().getCurrentDocument();
            this.document = ( DiagramUtility.isComposite(newDiagram) ) ? new CompositeDiagramDocument(newDiagram)
                    : new DiagramDocument(newDiagram);

            PlotsInfo plotsInfo = DiagramUtility.getPlotsInfo(diagram);
            if( plotsInfo != null )
                DiagramUtility.setPlotsInfo(newDiagram, plotsInfo.clone(newDiagram.getRole(EModel.class)));

            //String text = antimony.generateText();
            //setText(text);
            AntimonyUtility.setAntimonyAttribute(newDiagram, getText(), AntimonyConstants.ANTIMONY_TEXT_ATTR);
            AntimonyUtility.setAntimonyAttribute(newDiagram, "2.0", AntimonyConstants.ANTIMONY_VERSION_ATTR);

            release(diagram);
            register(newDiagram);

            this.document.update();

            if( GUI.getManager().getCurrentDocument() != null )
            {
                GUI.getManager().replaceDocument(currentDocument, this.document);
                if( this.document instanceof CompositeDiagramDocument && currentDocument instanceof CompositeDiagramDocument )
                {
                    int dividerLocation = ( (CompositeDiagramDocument)currentDocument ).getDividerLocation();
                    ( (CompositeDiagramDocument)document ).setDividerLocation(dividerLocation);
                }
                GUI.getManager().getDocumentViewAccessProvider().enableDocumentActions(true);

                if( setAntimonyTab )
                {
                    GUI.getManager().showViewPart(AntimonyEditor.this);
                    EditorsTabbedPane editorsPane = (EditorsTabbedPane)Application.getApplicationFrame().getPanelManager()
                            .getPanel(ApplicationFrame.EDITOR_PANE_NAME);
                    ViewPart part = editorsPane.getSelectedTab();
                    ( (AntimonyEditor)part ).antimonyTab.getEditorPane().requestFocus();
                    ( (AntimonyEditor)part ).antimonyTab.getEditorPane().setCaretPosition(caretPosition);
                }
            }
            else
            {
                log.info("replacing document, but document is null");
            }
        }
        catch( Exception ex )
        {
            ExceptionRegistry.log(ex);
            ApplicationUtils.errorBox("Incorrect antimony text: " + ex.getMessage());
            if( antimony.astStart == null )
                removeAntimony();
        }
        finally
        {
            antimonyIsApplying = false;
        }
    }

    private void removeAntimony()
    {
        diagram.getAttributes().remove(AntimonyConstants.ANTIMONY_TEXT_ATTR);
        antimony.init(diagram);
        antimony.createAst();
        try
        {
            setText(antimony.generateText());
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, "Error during antimony generating: " + ex.getMessage());
        }
    }

    private void register(Diagram diagram)
    {
        model = diagram;
        this.diagram = (Diagram)model;
        addListeners(diagram, this.document, this);
    }

    private void release(Diagram diagram)
    {
        removeListeners(diagram, this.document);
    }

    private void removeListeners(Diagram diagram, Document document)
    {
        diagram.removePropertyChangeListener(this);
        diagram.removeDataCollectionListener(this);
        diagram.getRole(EModel.class).getVariables().removeDataCollectionListener(this);
        if( document instanceof DiagramDocument )
        {
            ViewPane viewPane = ( (DiagramDocument)document ).getDiagramViewPane();
            viewPane.removeViewPaneListener(this);
        }
    }

    public static void addListeners(Diagram diagram, Document document, AntimonyEditor editor)
    {
        diagram.addDataCollectionListener(editor);
        diagram.addPropertyChangeListener(editor);
        diagram.getRole(EModel.class).getVariables().addDataCollectionListener(editor);
        if( document instanceof DiagramDocument )
        {
            ViewPane viewPane = ( (DiagramDocument)document ).getDiagramViewPane();
            viewPane.addViewPaneListener(editor);
        }
    }

    @Override
    public void elementAdded(DataCollectionEvent e) throws Exception
    {
        String text;
        try
        {
            text = antimony.updateText(e);
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            text = ex.getMessage();
        }
        if( text == null )
            text = "";
        setText(text);
        save();
    }

    @Override
    public void elementWillAdd(DataCollectionEvent e) throws DataCollectionVetoException, Exception
    {
    }

    @Override
    public void elementChanged(DataCollectionEvent e) throws Exception
    {
        if( e.getDataElement() instanceof ModelDefinition )
            return;
        while( e.getPrimaryEvent() != null )
            e = e.getPrimaryEvent();

        String text;
        try
        {
            text = antimony.updateText(e);
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            text = ex.getMessage();
        }
        if( text == null )
            text = "";
        setText(text);
        save();
    }

    @Override
    public void elementWillChange(DataCollectionEvent e) throws DataCollectionVetoException, Exception
    {
    }

    @Override
    public void elementRemoved(DataCollectionEvent e) throws Exception
    {
        String text;
        try
        {
            text = antimony.updateText(e);
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            text = ex.getMessage();
        }
        if( text == null )
            text = "";
        setText(text);
        save();
    }

    @Override
    public void elementWillRemove(DataCollectionEvent e) throws DataCollectionVetoException, Exception
    {
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if( evt.getPropertyName().equals("attributes/" + AntimonyConstants.ANTIMONY_TEXT_ATTR)
                || evt.getPropertyName().equals("attributes/" + AntimonyConstants.ANTIMONY_LINK)
                || evt.getPropertyName().equals("location") )
            return;

        String text;
        try
        {
            text = antimony.updateText(evt);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, e.getMessage(), e);
            text = e.getMessage();
        }
        if( text == null )
            text = "";
        setText(text);
        save();
    }

    @Override
    public void modelChanged(Object model)
    {
        //don't generate antimony ASTree, if needGenerateNewModel == false
        if( antimonyIsApplying )
            return;

        if( diagram != null )
        {
            String antimonyText = AntimonyUtility.getAntimonyAttribute(diagram, AntimonyConstants.ANTIMONY_TEXT_ATTR);
            //if we have some unsaved changes in antimony text - apply them to old diagram before opening new one
            if( antimonyText != null && !antimonyText.equals(getText()) )
                applyAntimonyOnClose(this.document);
            release(diagram);

            this.model = null;
            this.diagram = null;
        }

        if( ! ( model instanceof Diagram ) )
            return;

        String antimonyText = AntimonyUtility.getAntimonyAttribute((Diagram)model, AntimonyConstants.ANTIMONY_TEXT_ATTR);
        String antimonyVersion = AntimonyUtility.getAntimonyAttribute((Diagram)model, AntimonyConstants.ANTIMONY_VERSION_ATTR);
        if( antimonyText == null )
            return;

        if( antimonyVersion.equals("1.0") )
        {
            // version changed and text cleared
            AntimonyUtility.setAntimonyAttribute((Diagram)model, "2.0", AntimonyConstants.ANTIMONY_VERSION_ATTR);
            ( (Diagram)model ).getAttributes().remove(AntimonyConstants.ANTIMONY_TEXT_ATTR);
        }

        register((Diagram)model);
        antimony.init(diagram);

        if( antimonyVersion.equals("1.0") )
        {
            antimonyText = antimony.generateText(antimony.generateAstFromDiagram((Diagram)model));
        }


        setText(antimonyText);

        applyAntimony();
    }

    public void setText(String text)
    {
        antimonyTab.setText(text);
    }

    public String getText()
    {
        return antimonyTab.getText();
    }

    @Override
    public void onClose()
    {
        stopTimer();
        doUpdatePane(false);
    }

    @Override
    public void mouseClicked(ViewPaneEvent e)
    {
    }

    @Override
    public void mousePressed(ViewPaneEvent e)
    {
        String text = antimony.highlight(e);
        if( text != null )
            setText(text);
    }

    @Override
    public void mouseReleased(ViewPaneEvent e)
    {
    }

    @Override
    public void mouseEntered(ViewPaneEvent e)
    {
        doUpdatePane(true);
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

    private void doUpdatePane(boolean keepAntimonyTab)
    {
        if( antimonyIsApplying )
            return;

        if( diagram == null )
            return;

        if( GUI.getManager().getCurrentDocument() == null )
            return;

        //if diagram is not synchronized with antimony or antimony text was not changed since last sync do nothing
        String antimonyText = AntimonyUtility.getAntimonyAttribute(diagram, AntimonyConstants.ANTIMONY_TEXT_ATTR);
        if( antimonyText == null || antimonyText.equals(getText()) )
            return;

        applyAntimony(keepAntimonyTab);
    }

    /**Tasks to automatically update diagram from antimony*/
    class UpdateTask extends TimerTask
    {
        @Override
        public void run()
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    doUpdatePane(true);
                }
            });
        }
    }

    /**Creates timer for diagram update*/
    private void setTimer(int delay)
    {
        stopTimer();
        timer = new Timer();
        timer.schedule(new UpdateTask(), 0, delay);
    }

    /**Stops timer for diagram update*/
    private void stopTimer()
    {
        if( this.timer == null )
            return;
        this.timer.cancel();
        this.timer.purge();
    }

    public class AntimonyTab extends EditorPartSupport
    {
        protected Logger log = Logger.getLogger(AntimonyTab.class.getName());

        protected TextPaneAppender appender;
        protected AntimonyEditorPane antimonyPane;

        protected String[] categoryList = {"biouml.plugins.antimony"};

        private JSplitPane splitPane = new JSplitPane();

        @Override
        public void addFocusListener(FocusListener listener)
        {
            this.antimonyPane.addFocusListener(listener);
        }

        public AntimonyTab()
        {
            initSplitPane();
            add(splitPane);
        }

        private JSplitPane initSplitPane()
        {
            antimonyPane = new AntimonyEditorPane();
            appender = new TextPaneAppender(new PatternFormatter("%4$s :  %5$s%n"), "Application Log");
            appender.setLevel(Level.SEVERE);
            appender.addToCategories(categoryList);
            antimonyPane.setFont(new Font("Monospaced", Font.PLAIN, 12));
            LineNumbersView lineNumbers = new LineNumbersView(antimonyPane);
            JScrollPane scroll = new JScrollPane(antimonyPane);
            scroll.setRowHeaderView(lineNumbers);
            splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, scroll, appender.getLogTextPanel());
            splitPane.setResizeWeight(0.4);
            return splitPane;
        }

        private void createNewAntimonyText()
        {
            antimony.createAst();

            String text = "";
            if( diagram.getKernel() instanceof DiagramInfo )
            {
                try
                {
                    text = TextUtil2.nullToEmpty(antimony.generateText());
                }
                catch( Exception e )
                {
                    log.log(Level.SEVERE, "Error during antimony generating: " + e.getMessage());
                }
            }

            setText(text);
        }

        private String getText()
        {
            return antimonyPane.getText();
        }

        private void setText(String text)
        {
            Highlighter hilite = antimonyPane.getHighlighter();
            Highlighter.Highlight[] hilites = hilite.getHighlights();

            for( Highlight hiliteEntry : hilites )
            {
                if( hiliteEntry.getPainter() instanceof DefaultHighlighter.DefaultHighlightPainter )
                    hilite.removeHighlight(hiliteEntry);
            }

            ArrayList<Integer> leftHighlightersIndex = new ArrayList<>();
            ArrayList<Integer> rightHighlightersIndex = new ArrayList<>();
            String textIteration = text.replaceAll("\r\n", "\n");
            while( textIteration.contains(AntimonyConstants.HIGHLIGHT_START) )
            {
                textIteration = saveHighlightArea(leftHighlightersIndex, rightHighlightersIndex, textIteration);
            }

            text = text.replaceAll(AntimonyConstants.HIGHLIGHT_START, "").replaceAll(AntimonyConstants.HIGHLIGHT_END, "");

            antimonyPane.setTextSilent(text);

            try
            {
                for( int i = 0; i < leftHighlightersIndex.size(); i++ )
                {
                    hilite.addHighlight(leftHighlightersIndex.get(i), rightHighlightersIndex.get(i),
                            new DefaultHighlighter.DefaultHighlightPainter(Color.GRAY));
                }
            }
            catch( BadLocationException e )
            {
                log.log(Level.SEVERE, "Can't highlight text");
            }
        }

        private String saveHighlightArea(ArrayList<Integer> leftHighlightersIndex, ArrayList<Integer> rightHighlightersIndex,
                String textIteration)
        {
            int start = textIteration.indexOf(AntimonyConstants.HIGHLIGHT_START);
            textIteration = textIteration.replaceFirst(AntimonyConstants.HIGHLIGHT_START, "");

            int nextStart = textIteration.indexOf(AntimonyConstants.HIGHLIGHT_START);
            int end = textIteration.indexOf(AntimonyConstants.HIGHLIGHT_END);
            if( nextStart != -1 && nextStart < end )
                textIteration = saveHighlightArea(leftHighlightersIndex, rightHighlightersIndex, textIteration);

            end = textIteration.indexOf(AntimonyConstants.HIGHLIGHT_END);
            leftHighlightersIndex.add(start);
            rightHighlightersIndex.add(end);
            textIteration = textIteration.replaceFirst(AntimonyConstants.HIGHLIGHT_END, "");
            return textIteration;
        }


        public JEditorPane getEditorPane()
        {
            return antimonyPane;
        }

    }
}
