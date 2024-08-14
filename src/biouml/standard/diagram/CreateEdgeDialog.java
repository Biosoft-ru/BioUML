package biouml.standard.diagram;

import java.awt.Point;
import java.awt.Window;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import ru.biosoft.graphics.editor.ViewEditorPane;
import biouml.model.Compartment;
import biouml.model.Module;

import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.dialog.OkCancelDialog;

/**
 *
 */
public class CreateEdgeDialog extends OkCancelDialog
{
    SemanticRelationPane content;

    ////////////////////////////////////////////////////////////////////////////
    // Constructor
    //

    public static CreateEdgeDialog getSemanticRelationDialog(Module module, Point point, ViewEditorPane viewEditor)
    {
        return new CreateEdgeDialog(point, "New relation", new SemanticRelationPane(module, viewEditor));
    }
    
    public static CreateEdgeDialog getTransitionDialog(Module module, Point point, ViewEditorPane viewEditor)
    {
        return new CreateEdgeDialog(point, "New transition", new TransitionPane(module, viewEditor));
    }

    public static CreateEdgeDialog getSimpleEdgeDialog(Module module, Point point, ViewEditorPane viewEditor, String edgeName,
            String edgeType, DynamicPropertySet initAttributes)
    {
        return new CreateEdgeDialog(point, "New edge", new SimpleEdgePane(module, viewEditor, edgeName, edgeType,
                initAttributes));
    }

    public static CreateEdgeDialog getConnectionDialog(Module module, Point point, ViewEditorPane viewEditor, Class connectionType,
            Compartment parent)
    {
        return new CreateEdgeDialog(point, "New connection", new ConnectionEdgePane(module, viewEditor, connectionType, parent));
    }
    
    public CreateEdgeDialog(Point point, String title, SemanticRelationPane relationPane)
    {
        super(Application.getApplicationFrame(), title, false);

        content = relationPane;
        setContent(content);
        pack();

        if( Application.getApplicationFrame() != null )
        {
            JComponent editor = Application.getApplicationFrame().getPanelManager().getPanel("diagram");
            if( editor != null )
            {
                Window root = SwingUtilities.getWindowAncestor(editor);

                int x = root.getLocation().x + editor.getLocation().x + ( root.getSize().width - editor.getSize().width ) / 2;
                int y = root.getLocation().y + editor.getLocation().y + editor.getSize().height;
                setLocation(x, y);
            }
        }

        relationPane.okButton = this.okButton;
        okButton.setEnabled(false);
    }

    @Override
    protected void okPressed()
    {
        content.okPressed();
        super.okPressed();
    }

    @Override
    protected void cancelPressed()
    {
        content.cancelPressed();
        super.cancelPressed();
    }
}