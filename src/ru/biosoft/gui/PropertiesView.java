package ru.biosoft.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.html.HTMLDocument;
import javax.swing.undo.UndoableEdit;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.application.action.ApplicationAction;
import com.developmentontheedge.application.action.SeparatorAction;
import com.developmentontheedge.application.dialog.OkCancelDialog;
import com.developmentontheedge.beans.undo.TransactionEvent;
import com.developmentontheedge.beans.web.HtmlPropertyInspector;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.math.xml.MathMLEditorKit;
import ru.biosoft.access.HtmlDescribedElement;
import ru.biosoft.templates.TemplateInfo;
import ru.biosoft.templates.TemplateRegistry;
import ru.biosoft.templates.TemplateUtils;

@SuppressWarnings ( "serial" )
public class PropertiesView extends HtmlPropertyInspector implements ViewPart
{
    public static final String ACTION_NAME = "Properties view";

    public static final int TOOLBAR_BUTTON_SIZE = 20;

    protected Action action;
    protected Object modelObj;
    private MathMLEditorKit kit = new MathMLEditorKit();

    public PropertiesView()
    {
        this.remove(super.getView());

        action = new ApplicationAction(ru.biosoft.gui.resources.MessageBundle.class, ACTION_NAME);

        setPreferredSize(new Dimension(300, 400));
        this.getEditor().setEditorKit( kit );
    }

    // //////////////////////////////////////////////////////////////////////////
    // ViewPart interface implimentation
    //

    @Override
    public Action getAction()
    {
        return action;
    }

    @Override
    public boolean canExplore(Object model)
    {
        return true;
    }

    @Override
    public void explore(Object model, Document document)
    {
        this.document = document;
        this.modelObj = model;

        TemplateUtils.applyStyle((HTMLDocument)getEditor().getDocument());

        if( model instanceof HtmlDescribedElement )
        {
            URL base = ( (HtmlDescribedElement)model ).getBase();
            ( (HTMLDocument)getEditor().getDocument() ).setBase(base);
        }
        explore(model);
    }

    @Override
    public void hyperlinkUpdate(HyperlinkEvent e)
    {
        if( e.getEventType() == HyperlinkEvent.EventType.ACTIVATED )
        {
            if( e.getURL() == null && e.getDescription().startsWith( "de:" ) )
            {
                DataElementPath path = DataElementPath.create( e.getDescription().substring( "de:".length() ) );
                GUI.getManager().getRepositoryTabs().selectElement( path );
                if( path.exists() )
                    DocumentManager.getDocumentManager().openDocument( path.getDataElement() );
                return;
            }
            if( e.getURL() != null && e.getDescription().startsWith("#") )
            {
                if(e.getDescription().startsWith( "#de=" ))
                {
                    DataElementPath path = DataElementPath.create( e.getDescription().substring( "#de=".length() ) );
                    GUI.getManager().getRepositoryTabs().selectElement( path );
                    if( path.exists() )
                        DocumentManager.getDocumentManager().openDocument( path.getDataElement() );
                } else
                {
                    getEditor().scrollToReference(e.getDescription().substring(1));
                }
                return;
            }
        }
        super.hyperlinkUpdate(e);
    }

    @Override
    protected void setText(Object data)
    {
        if( data instanceof URL && ( (URL)data ).getProtocol().equals("http") )
        {
            TemplateUtils.generateBrowserView((URL)data);
            return;
        }
        String text = data.toString();
        if( text.startsWith( "<html>" ) )
            text = replaceSelfClosingTags( data.toString() );
        else
            text = "<pre>" + text;//non html text should keep its formatting
        super.setText( text );
    }

    public static String replaceSelfClosingTags(String text)
    {
        Pattern pattern = Pattern.compile( "<(\\w+)(.+?)/>", Pattern.CASE_INSENSITIVE );
        Matcher matcher = pattern.matcher(text);
        String result = matcher.replaceAll( "<$1$2></$1$2>" );
        return result;
    }

    /**
     * Add templates menu to the toolbar
     */
    public void initTemplatesPanel(JToolBar toolbar)
    {
        if( toolbar != null )
        {
            TemplateInfo[] templates = TemplateRegistry.getSuitableTemplates(modelObj);
            if( templates == null || templates.length == 0 )
                return;

            JPanel templatesPanel = new JPanel(new BorderLayout());

            final JComboBox<String> templatesBox = new JComboBox<>();
            templatesBox.setPreferredSize(new Dimension(200, TOOLBAR_BUTTON_SIZE));
            templatesBox.addActionListener(e -> {
                String templateName = (String)templatesBox.getSelectedItem();
                StringBuffer result = modelObj == null ? new StringBuffer("Select an element in the repository to view its description") : TemplateRegistry.mergeTemplate( modelObj, templateName );
                setInitialText(result.toString());
            });
            for( TemplateInfo ti : templates )
            {
                templatesBox.addItem(ti.getName());
            }
            templatesPanel.add(templatesBox, BorderLayout.CENTER);

            URL url = PropertiesView.class.getResource("resources/view.gif");
            JButton templateButton = new JButton(new ImageIcon(url));
            configureButton(templateButton, null);
            templateButton.addActionListener(new TemplateActionListener(templatesBox, modelObj));

            url = PropertiesView.class.getResource("resources/viewBrowser.gif");
            JButton templateBrowserButton = new JButton(new ImageIcon(url));
            configureButton(templateBrowserButton, null);
            templateBrowserButton.addActionListener(new TemplateInBrowserActionListener(templatesBox, modelObj));

            JPanel buttons = new JPanel(new BorderLayout());
            buttons.add(templateButton, BorderLayout.WEST);
            buttons.add(templateBrowserButton, BorderLayout.EAST);
            templatesPanel.add(buttons, BorderLayout.EAST);

            toolbar.add(templatesPanel);
        }
    }

    @Override
    public Object getModel()
    {
        return getExploredBean();
    }

    protected Document document;

    @Override
    public Document getDocument()
    {
        return document;
    }

    // //////////////////////////////////////////////////////////////////////////
    // TransactionListener - do nothing
    //

    @Override
    public void startTransaction(TransactionEvent te)
    {
    }

    @Override
    public boolean addEdit(UndoableEdit ue)
    {
        return false;
    }

    @Override
    public void completeTransaction()
    {
    }

    protected static void configureButton(AbstractButton button, String text)
    {
        button.setAlignmentY(0.5f);

        Dimension btnSize = new Dimension(TOOLBAR_BUTTON_SIZE, TOOLBAR_BUTTON_SIZE);
        button.setSize(btnSize);
        button.setPreferredSize(btnSize);
        button.setMinimumSize(btnSize);
        button.setMaximumSize(btnSize);

        if( button.getIcon() != null )
            button.setText(null);
        else
            button.setText(text);
    }

    //classes
    public static class TemplateActionListener implements ActionListener
    {
        protected JComboBox<String> templatesBox;
        protected Object model;

        public TemplateActionListener(JComboBox<String> templatesBox, Object model)
        {
            this.templatesBox = templatesBox;
            this.model = model;
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            String templateName = (String)templatesBox.getSelectedItem();
            generateDialogView( model, templateName );
        }
    }

    public static class TemplateInBrowserActionListener implements ActionListener
    {
        protected JComboBox<String> templatesBox;
        protected Object model;

        public TemplateInBrowserActionListener(JComboBox<String> templatesBox, Object model)
        {
            this.templatesBox = templatesBox;
            this.model = model;
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            String templateName = (String)templatesBox.getSelectedItem();
            TemplateUtils.generateBrowserView(model, templateName);
        }
    }
    @Override
    public void modelChanged(Object model) { }
    
    @Override
    public void onClose()  { };

    public static void generateDialogView(Object model, String templateName)
    {
        StringBuffer result = TemplateRegistry.mergeTemplate( model, templateName );
        OkCancelDialog infoDialog = new OkCancelDialog( Application.getApplicationFrame(), templateName, null, null, "Close" );
        infoDialog.setSize( 400, 500 );
        ApplicationUtils.moveToCenter( infoDialog );

        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable( false );

        PropertiesView htmlPane = new PropertiesView();
        if( model instanceof HtmlDescribedElement )
        {
            URL base = ( (HtmlDescribedElement)model ).getBase();
            ( (HTMLDocument)htmlPane.getEditor().getDocument() ).setBase( base );
        }

        htmlPane.setText( result.toString().replaceAll( "href=\"de:([^\"]+)\"", "" ) );

        JScrollPane scrollPane = new JScrollPane( htmlPane.getEditor() );
        JPanel panel = new JPanel( new BorderLayout() );
        panel.add( toolbar, BorderLayout.NORTH );
        panel.add( scrollPane, BorderLayout.CENTER );
        infoDialog.setContent( panel );
        infoDialog.setVisible( true );
    }

    public static void addActions(JToolBar toolbar, Action[] actions)
    {
        if( actions != null )
        {
            for( int i = 0; i < actions.length; i++ )
            {
                Action action = actions[i];
                if( action instanceof SeparatorAction )
                {
                    JToolBar.Separator separator = new JToolBar.Separator( (Dimension)action.getValue( SeparatorAction.DIMENSION ) );
                    toolbar.add( separator, i );
                }
                else
                {
                    JButton button = new JButton( action );
                    configureButton( button, (String)action.getValue( Action.NAME ) );
                    toolbar.add( button, i );
                }
            }
        }
    }
}
