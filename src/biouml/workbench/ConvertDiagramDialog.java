package biouml.workbench;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import biouml.model.Diagram;
import biouml.model.DiagramTypeConverter;
import biouml.model.Role;
import biouml.model.dynamics.EModel;
import biouml.workbench.diagram.DiagramDocument;
import biouml.workbench.diagram.DiagramTypeConverterRegistry;
import biouml.workbench.diagram.DiagramTypeConverterRegistry.Conversion;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.GUI;

@SuppressWarnings ( "serial" )
public class ConvertDiagramDialog extends ProcessElementDialog
{
    // Logging issues
    protected static final Logger log = Logger.getLogger(ConvertDiagramDialog.class.getName());
    protected JLabel currentType = new JLabel();
    protected JTextField diagramName = new JTextField();
    protected JComboBox<String> diagramType = new JComboBox<>();
    protected DiagramTypeConverterRegistry.Conversion[] conversions;
    protected Diagram diagram;

    ///////////////////////////////////////////////////////////////////
    // Constructor
    //

    public ConvertDiagramDialog(Diagram diagram)
    {
        super("CONVERT_DIAGRAM_DIALOG_TITLE");

        this.diagram = diagram;

        JPanel contentPane = new JPanel(new GridBagLayout());
        contentPane.setBorder(new EmptyBorder(10, 10, 10, 10));

        //--- diagram name ---
        contentPane.add(new JLabel(messageBundle.getResourceString("DIAGRAM_DIALOG_DIAGRAM")), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 0, 0, 0), 0, 0));
        contentPane.add(new JLabel(diagram.getName() + " (" + diagram.getTitle() + ")"), new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 0), 0, 0));

        //--- diagram type ---
        contentPane.add(new JLabel(messageBundle.getResourceString("CONVERT_DIAGRAM_DIALOG_TYPE")), new GridBagConstraints(0, 1, 1, 1, 0.0,
                0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 0, 0, 0), 0, 0));
        contentPane.add(currentType, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(5, 5, 0, 0), 0, 0));

        //--- new name ---
        contentPane.add(new JLabel(messageBundle.getResourceString("CONVERT_DIAGRAM_DIALOG_NEW_NAME")), new GridBagConstraints(0, 2, 1, 1,
                0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 0, 0, 0), 0, 0));
        contentPane.add(diagramName, new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(5, 5, 0, 0), 0, 0));
        diagramName.setText(diagram.getName()+"(converted)");
        
        //--- new type ---
        contentPane.add(new JLabel(messageBundle.getResourceString("CONVERT_DIAGRAM_DIALOG_NEW_TYPE")), new GridBagConstraints(0, 3, 1, 1,
                0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 0, 0, 0), 0, 0));
        contentPane.add(diagramType, new GridBagConstraints(1, 3, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(5, 5, 0, 0), 0, 0));

        //--- logging settings ---
        initAppender("Converter log", "");

        contentPane.add(new JLabel(messageBundle.getResourceString("DIAGRAM_DIALOG_INFO")), new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 0, 0, 0), 0, 0));

        contentPane.add(appender.getLogTextPanel(), new GridBagConstraints(0, 5, 2, 2, 1.0, 1.0, GridBagConstraints.WEST,
                GridBagConstraints.BOTH, new Insets(5, 0, 0, 0), 0, 0));

        //--- dialog settings ---
        init(true);

        setContent(contentPane);

        okButton.setText(messageBundle.getResourceString("CONVERT_DIAGRAM_DIALOG_CONVERT"));
        okButton.setPreferredSize(okButton.getMinimumSize());
        cancelButton.setText(messageBundle.getResourceString("DIAGRAM_DIALOG_CLOSE"));

        okButton.setEnabled(true);
    }


    protected void init(boolean isFirst)
    {
        String type = diagram.getType().getClass().getName();
        try
        {
            BeanInfo info = Introspector.getBeanInfo(diagram.getType().getClass());
            type = info.getBeanDescriptor().getDisplayName();
        }
        catch( Exception e )
        {
        }
        currentType.setText(type);

        if( !isFirst )
            diagramType.removeAllItems();

        conversions = DiagramTypeConverterRegistry.getPossibleConversions(diagram.getType().getClass().getName());
        if( conversions == null || conversions.length == 0 )
        {
            if( isFirst )
                log.info(messageBundle.getResourceString("CONVERT_DIAGRAM_DIALOG_NO_CONVERSIONS"));
            else
                log.info(messageBundle.getResourceString("CONVERT_DIAGRAM_DIALOG_NO_FURTHER_CONVERSIONS"));

            okButton.setEnabled(false);
        }
        else
        {
            for( Conversion conversion : conversions )
            {
                diagramType.addItem(conversion.getDiagramTypeDisplayName());
            }
            diagramType.setSelectedIndex(0);
        }
    }

    @Override
    protected void okPressed()
    {
        DiagramTypeConverterRegistry.Conversion conversion = conversions[diagramType.getSelectedIndex()];
        log.info(MessageFormat.format(messageBundle.getResourceString("CONVERT_DIAGRAM_DIALOG_START"), new Object[] {diagram.getName(),
                diagram.getTitle(), currentType.getText(), conversion.getDiagramTypeDisplayName()}));
        // try to change diagram type
        try
        {
            DataElementPath path = DataElementPath.create(diagram.getOrigin(), diagramName.getText());
            Diagram newDiagram = diagram.clone(path.getParentCollection(), path.getName());
            DiagramTypeConverter converter = conversion.getConverter().newInstance();
            startTransaction();
            newDiagram = converter.convert(newDiagram, conversion.getDiagramType());
            CollectionFactoryUtils.save(newDiagram);
            completeTransaction();
            updateDiagramPane();
            log.info(messageBundle.getResourceString("CONVERT_DIAGRAM_DIALOG_SUCCESS"));

            init(false);
        }
        catch( IllegalArgumentException iae )
        {
            log.info(MessageFormat.format(messageBundle.getResourceString("CONVERT_DIAGRAM_DIALOG_ILLEGAL"),
                    new Object[] {iae.getMessage()}));
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Creating new diagram ", e);
        }
    }

    protected void updateDiagramPane()
    {
        if( document != null )
        {
            document.update();
            document.updateViewPane();
        }
    }

    private Document document;
    private boolean notification;
    protected void startTransaction()
    {
        for( Document document : GUI.getManager().getDocuments() )
        {
            if( document instanceof DiagramDocument )
            {
                if( ( (DiagramDocument)document ).getDiagram() == diagram )
                {
                    this.document = document;
                    ( (ViewEditorPane)document.getViewPane() ).startTransaction("Convert");
                    break;
                }
            }
        }
        Role role = diagram.getRole();
        if( role instanceof EModel )
        {
            EModel emodel = ( (EModel)role );
            notification = emodel.isNotificationEnabled();
            emodel.setNotificationEnabled(false);
            ( (ViewEditorPane)document.getViewPane() ).addEdit(new NotificationUndo((EModel)role, notification, false));
        }
    }

    protected void completeTransaction()
    {
        Role role = diagram.getRole();
        if( role instanceof EModel )
        {
            EModel emodel = ( (EModel)role );
            boolean oldNotification = emodel.isNotificationEnabled();
            emodel.setNotificationEnabled(notification);
            ( (ViewEditorPane)document.getViewPane() ).addEdit( new NotificationUndo( emodel, oldNotification, notification ) );
        }

        if( document != null )
            ( (ViewEditorPane)document.getViewPane() ).completeTransaction();
    }

    public static class NotificationUndo extends AbstractUndoableEdit
    {
        protected boolean oldNotification;
        protected boolean newNotification;
        protected EModel emodel = null;

        public NotificationUndo(EModel emodel, boolean oldNotification, boolean newNotification)
        {
            this.emodel = emodel;
            this.oldNotification = oldNotification;
            this.newNotification = newNotification;
        }

        @Override
        public void undo() throws CannotUndoException
        {
            emodel.setNotificationEnabled(oldNotification);
        }

        @Override
        public void redo() throws CannotRedoException
        {
            emodel.setNotificationEnabled(newNotification);
        }

        @Override
        public String getPresentationName()
        {
            return "Change executable model notification";
        }
    }
}
