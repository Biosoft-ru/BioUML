package biouml.plugins.research.workflow.yaml;

import java.awt.Font;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import ru.biosoft.gui.Document;
import ru.biosoft.gui.ViewPartSupport;
import biouml.model.Diagram;
import biouml.plugins.research.workflow.WorkflowDiagramType;

public class YamlViewPart extends ViewPartSupport
{
    private JTextArea textEditor;
    private WorkflowToTextLink workflowToTextLink;
    private TextToWorkflowLink textToWorkflowLink;

    @Override
    public boolean canExplore(Object model)
    {
        return model instanceof Diagram && ( (Diagram)model ).getType() instanceof WorkflowDiagramType;
    }

    @Override
    public void explore(Object model, Document document)
    {
        super.explore( model, document );
        initUI();

        Diagram diagram = (Diagram)model;

        if( workflowToTextLink != null )
            workflowToTextLink.disable();
        if( textToWorkflowLink != null )
            textToWorkflowLink.disable();
        
        workflowToTextLink = new WorkflowToTextLink( diagram, textEditor );
        textToWorkflowLink = new TextToWorkflowLink( diagram, textEditor, getDocument() );
        workflowToTextLink.setTextToWorkflowLink( textToWorkflowLink );
        textToWorkflowLink.setWorkflowToTextLink( workflowToTextLink );
        workflowToTextLink.enable();
        textToWorkflowLink.enable();
        workflowToTextLink.workflowChanged();
    }

    private void initUI()
    {
        if( textEditor == null )
        {
            textEditor = new JTextArea();
            textEditor.setFont( new Font( "Monospaced", Font.PLAIN, 12 ) );
            add( new JScrollPane( textEditor ) );
        }
    }

}
