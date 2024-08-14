package biouml.standard.diagram;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.ResourceBundle;

import javax.annotation.Nonnull;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import one.util.streamex.StreamEx;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.support.NewDataElementDialog;
import ru.biosoft.gui.ExplorerPane;
import biouml.model.CollectionDescription;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Module;
import biouml.model.Node;
import biouml.standard.type.Base;
import biouml.standard.type.GenericEntity;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.dialog.OkCancelDialog;

/**
 * Create new diagram element dialog
 *
 * @pending high filling kernel list (use model instead)
 * @pending String initialisation from MessageBundle
 */
public class CreateDiagramElementDialog extends OkCancelDialog implements ElementChooserPaneListener
{

    protected Logger log = Logger.getLogger( CreateDiagramElementDialog.class.getName() );

    private static MessageBundle resources = (MessageBundle)ResourceBundle.getBundle(MessageBundle.class.getName());

    @Nonnull
    private Compartment parent;

    private Node node;

    private Class<? extends DataElement> type;

    protected ExplorerPane explorerPane;

    protected ElementChooserPane elementChooser;

    protected JButton newButton;

    protected Module module;
    protected CollectionDescription[] extendedModules;
    protected DataCollection elementDC;

    ////////////////////////////////////////////////////////////////////////////
    // Constructor
    //

    /**
     * Create dialog for new diagram element creation,
     * which correctly create new diagram element with kernel
     * of selected type
     */
    public CreateDiagramElementDialog(@Nonnull Compartment parent, Class<? extends DataElement> type) throws Exception
    {
        this(Application.getApplicationFrame(), resources.getResourceString("NEW_ELEMENT_DIALOG"), parent, type);
    }

    protected CreateDiagramElementDialog(JFrame frame, String titlePrototype, @Nonnull Compartment parent, Class<? extends DataElement> type) throws Exception
    {
        super(frame, "");

        this.parent = parent;
        this.type = type;

        module = Module.getModule(parent);
        elementDC = module.getCategory(type);
        extendedModules = module.getExternalCategories(type);

        if( elementDC == null && (extendedModules == null || extendedModules.length == 0) )
        {
            setContent(new JLabel(resources.getResourceString("NEW_ELEMENT_DIALOG_ERROR")));
            return;
        }

        if( elementDC == null )
        {
            elementDC = extendedModules[0].getDc();
        }

        setTitle(MessageFormat.format(titlePrototype, new Object[] {elementDC.getName()}));

        newButton = new JButton(resources.getResourceString("NEW_BUTTON"));
        newButton.setPreferredSize(cancelButton.getPreferredSize());
        buttonPanel.add(newButton, 0);

        newButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                newPressed();
            }
        });

        newButton.setEnabled( ( elementDC != null && Base.class.isAssignableFrom(type) ) ? elementDC.isMutable() : false);

        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(new EmptyBorder(10, 10, 10, 10));

        explorerPane = new ExplorerPane();
        explorerPane.setPreferredSize(new Dimension(400, 300));

        String borderTitle = MessageFormat.format(resources.getResourceString("EXPLORER_BORDER_TITLE"), new Object[] {elementDC != null
                ? elementDC.getName() : ""});
        explorerPane.setBorder(new javax.swing.border.TitledBorder(new javax.swing.border.EtchedBorder(), borderTitle));

        content.add(explorerPane, BorderLayout.CENTER);

        elementChooser = new ElementChooserPane(Module.getModule(parent), explorerPane, extendedModules, this)
        {
            @Override
            protected void setControlsEnabled(boolean enable)
            {
                super.setControlsEnabled(enable);
                okButton.setEnabled(enable);
            }
        };
        elementChooser.init(type);
        content.add(elementChooser, BorderLayout.NORTH);

        setContent(content);
    }

    /**
     * @pending move resource strings to ru.biosoft.access.support.MessageBundle
     */
    protected void newPressed()
    {
        try
        {
            DataCollection category = Module.getModule(parent).getCategory(type);
            if( category != null )
            {
                NewDataElementDialog newDataElementDialog = new NewDataElementDialog(this, "New data element", category);
                if( newDataElementDialog.doModal() )
                {
                    GenericEntity entity = (GenericEntity)newDataElementDialog.getNewDataElement();
                    if( entity != null )
                    {
                        elementChooser.addNew(entity);
                    }
                }
            }
        }
        catch( Throwable e )
        {
            log.log(Level.SEVERE, "Error during creation new data element", e);
        }
    }

    @Override
    protected void okPressed()
    {
        if( elementChooser != null )
        {
            elementChooser.close();
            node = createDiagramElement();
        }
        if( node != null )
        {
            super.okPressed();
        }
    }

    @Override
    protected void cancelPressed()
    {
        if( elementChooser != null )
        {
            elementChooser.close();
        }
        super.cancelPressed();
    }

    protected Node createDiagramElement()
    {
        Node node = null;
        try
        {
            String name = getKernelName();
            if( name == null )
                return null;
            if( Diagram.class == type )
            {
                Diagram selectedDiagram = (Diagram)elementDC.get(name);
                if( selectedDiagram.getRole() == null )
                {
                    JOptionPane
                            .showMessageDialog(this, resources.getResourceString("NULL_MODEL_ERROR"), "Error", JOptionPane.ERROR_MESSAGE);
                    return null;
                }
                else if( Diagram.optDiagram(parent) == selectedDiagram )
                {
                    JOptionPane.showMessageDialog(this, resources.getResourceString("INCORRECT_ELEMENT_ERROR"), "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return null;
                }
                else
                {
                    return selectedDiagram;
                }
            }

            Base base = (Base)Module.getModule(parent).getKernel(elementDC.getCompletePath().getChildPath(name).toString());
            if( biouml.standard.type.Compartment.class.isAssignableFrom(type) )
            {
                node = new Compartment(parent, getKernelName(), base);
            }
            else
            {
                node = new Node(parent, getKernelName(), base);
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Error during creation of a diagram element", e);
        }
        return node;
    }
    protected String getKernelName()
    {
        return elementChooser.getKernelName();
    }

    /**
     * return created Node
     */
    public Node getNode()
    {
        return node;
    }

    /**
     * Select requered data element in ElementChooserPane
     */
    public void selectItem(String name, String moduleName)
    {
        elementChooser.selectItem(name, moduleName);
    }

    @Override
    public void moduleChanged(String moduleName, DataCollection targetDC)
    {
        this.elementDC = targetDC;
        if( !module.getName().equals(moduleName) && extendedModules != null )
        {
            StreamEx.of( extendedModules ).findFirst( et -> et.getModuleName().equals( moduleName ) )
                .ifPresent( et -> newButton.setEnabled( !et.isReadOnly() && elementDC.isMutable()) );
        }
        else
        {
            newButton.setEnabled(elementDC.isMutable() && Base.class.isAssignableFrom(type));
        }
    }
}