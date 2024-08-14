package biouml.plugins.research.workflow;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.beans.IntrospectionException;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.util.PropertiesDialog;
import biouml.model.Diagram;
import biouml.plugins.research.workflow.items.WorkflowItemFactory;

import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class WorkflowPropertiesDialog extends PropertiesDialog
{
    private static final long serialVersionUID = 1L;
    private Diagram diagram;
    private DataElementPathEditor editor;
    public WorkflowPropertiesDialog(JFrame frame, Diagram diagram) throws Exception
    {
        super(frame, "Workflow parameters for "+diagram.getName(), WorkflowItemFactory.getWorkflowParameters(diagram));
        this.diagram = diagram;
        //editor.setValue(DataElementPath.createPath(diagram).getSiblingPath(diagram.getName()+" research"));
    }

    @Override
    public boolean doModal()
    {
        try
        {
            if( ( (DynamicPropertySet)getProperties() ).size() == 0 )
                return true;
        }
        catch( Exception e )
        {
        }
        return super.doModal();
    }

    public Diagram getDiagram()
    {
        return diagram;
    }

    @Override
    public Component getDialogContent()
    {
        Component dpi = super.getDialogContent();
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(dpi);
        try
        {
            editor = new DataElementPathEditor();
            PropertyDescriptorEx pde = DataElementPathEditor.registerOutput(new PropertyDescriptorEx("research", null, null), Diagram.class, true);
            editor.setDescriptor(pde);
            JPanel panel = new JPanel(new GridLayout());
            panel.setBackground(Color.WHITE);
            panel.setBorder(BorderFactory.createEtchedBorder());
            JLabel label = new JLabel("Generate research diagram");
            Component pathEditor = editor.getCustomEditor(panel, true);
            label.setBackground(pathEditor.getBackground());
            panel.add(label);
            panel.add(pathEditor);
            content.add(panel);
        }
        catch( IntrospectionException e )
        {
        }
        return content;
    }
    
    public DataElementPath getResearchDiagramPath()
    {
        try
        {
            return (DataElementPath)editor.getValue();
        }
        catch( Exception e )
        {
            return null;
        }
    }
}
