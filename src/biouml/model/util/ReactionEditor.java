package biouml.model.util;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.beans.editors.CustomEditorSupport;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Node;
import biouml.standard.diagram.CreateReactionDialog;
import biouml.standard.diagram.ReactionPane;
import biouml.standard.type.Base;
import biouml.standard.type.Reaction;
import biouml.workbench.diagram.DiagramDocument;
import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.graphics.editor.ViewPane;
import ru.biosoft.gui.Document;

public class ReactionEditor extends CustomEditorSupport
{
    protected JPanel panel;
    protected JButton editButton;
    protected ReactionPane editorPane = null;
    protected ViewEditorPane viewEditor;

    private final static String HEADER = "Edit Reaction";
    
    public ReactionEditor()
    {
        panel = new JPanel( new BorderLayout( 3, 0 ) );
        panel.setOpaque( false );
        editButton = new JButton( HEADER );
        panel.add( editButton, BorderLayout.EAST );
        editButton.addActionListener( e -> editButtonAction() );
    }

    public void editButtonAction()
    {
        try
        {
            init();
            new CreateReactionDialog( Application.getApplicationFrame(), editorPane, HEADER );
        }
        catch( Throwable t )
        {
        }
    }

    /**
     * Find node in diagram by kernel
     */
    protected Node findNode(Compartment comp, Base kernel)
    {
        return comp.recursiveStream().select( Compartment.class ).flatMap( c -> c.getKernelNodes( kernel ) ).findAny().orElse( null );
    }

    @Override
    public Component getCustomRenderer(Component parent, boolean isSelected, boolean hasFocus)
    {
        return panel;
    }

    @Override
    public Component getCustomEditor(Component parent, boolean isSelected)
    {
        return getCustomRenderer( parent, isSelected, true );
    }


    private void init()
    {
      //set current document
        Document document = Document.getCurrentDocument();
        Diagram diagram = null;
        if( document instanceof DiagramDocument )
        {
            diagram = ( (DiagramDocument)document ).getDiagram();
            ViewPane vp = ( (DiagramDocument)document ).getDiagramViewPane();
            if( vp instanceof ViewEditorPane )
                viewEditor = (ViewEditorPane)vp;
        }
        //set reaction
        Object bean = getBean();
        if( ! ( bean instanceof Node && ((Node)bean).getKernel() instanceof Reaction ) )
        {
            JOptionPane.showMessageDialog( Application.getApplicationFrame(), "Unsupported object type", "Editor unavailable",
                    JOptionPane.ERROR_MESSAGE );
            return;
        }

        if( diagram == null )
        {
            JOptionPane.showMessageDialog( Application.getApplicationFrame(), "Opened diagran document is necessary for reaction editor",
                    "Editor unavailable", JOptionPane.ERROR_MESSAGE );
            return;
        }
        
        Node reactionNode = ((Node)bean);
        Reaction reaction = (Reaction)reactionNode.getKernel();
        editorPane = new ReactionPane( reaction, diagram, reactionNode.getCompartment(), reactionNode.getLocation(), viewEditor );
    }
}
