package biouml.workbench.diagram;

import java.awt.Point;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import com.developmentontheedge.application.ApplicationUtils;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Node;
import biouml.model.SemanticController;
import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.graphics.editor.ViewPaneAdapter;
import ru.biosoft.graphics.editor.ViewPaneEvent;

@SuppressWarnings ( "serial" )
public class CopyNodeAction extends AbstractAction
{
    public static final String KEY = "Copy";
    public static final String NODE = "node";
    public static final String VIEWPANE = "viewPane";

    public CopyNodeAction()
    {
        this(true);
    }

    public CopyNodeAction(Boolean enabled)
    {
        super(KEY);
        setEnabled(enabled);
    }
    
    @Override
    public void actionPerformed(ActionEvent arg0)
    {
        Node node = (Node)getValue(NODE);
        ViewEditorPane viewPane = (ViewEditorPane)getValue(VIEWPANE);
        ViewPaneAdapter adapter = ( new ViewPaneAdapter()
        {
            @Override
            public void mousePressed(ViewPaneEvent e)
            {
                Object model = e.getViewSource().getModel();
                Compartment parent = null;
                if( model instanceof Compartment )
                    parent = (Compartment)model;
                else if( model instanceof DiagramElement )
                {
                    if( ( (DiagramElement)model ).getOrigin() instanceof Compartment )
                        parent = (Compartment) ( (DiagramElement)model ).getOrigin();
                }
                if( parent == null )
                    return;
                try
                {
                    createCopyElement(parent, node, e.getPoint(), ( (ViewEditorPane)viewPane ));
                }
                catch( Exception ex )
                {
                    String message = "Error copying " + node.getName() + ": " + ex.getMessage();
                    ApplicationUtils.errorBox("Error", message);
                }
                viewPane.removeViewPaneListener(this);
                viewPane.getSelectionManager().clearSelection();
            }
        } );
        viewPane.addViewPaneListener(adapter);
    }

    private DiagramElement createCopyElement(Compartment compartment, Node node, Point point, ViewEditorPane viewEditor) throws Exception
    {
        SemanticController controller = Diagram.getDiagram( compartment ).getType().getSemanticController();
        String newName = generateName( node.getName(), compartment );
        Node result = controller.copyNode( node, newName, compartment, point );
        if( result != null && controller.canAccept( compartment, result ) )
            viewEditor.add( result, point );
        return null;
    }

    private String generateName(String oldName, Compartment compartment)
    {
        String baseName = oldName;
        int index = 1;
        if( oldName.matches(".*_\\d\\d*") )
        {
            int i = oldName.lastIndexOf("_");
            index = Integer.parseInt(oldName.substring(i + 1));
            baseName = oldName.substring(0, i);
        }

        String newName = baseName + "_" + index;
        while( compartment.contains(newName) )
            newName = baseName + "_" + ++index;
        return newName;
    }
}