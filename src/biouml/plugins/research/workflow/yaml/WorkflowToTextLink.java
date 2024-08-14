package biouml.plugins.research.workflow.yaml;

import java.util.Map;

import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.yaml.snakeyaml.Yaml;

import ru.biosoft.access.core.DataCollectionEvent;
import ru.biosoft.access.DataCollectionListenerSupport;
import ru.biosoft.access.core.DataCollectionVetoException;
import biouml.model.Diagram;

/**
 * Links workflow to text(yaml) representation.
 * Listen for workflow changes and update text representation accordingly
 */
public class WorkflowToTextLink extends DataCollectionListenerSupport
{
    private final Diagram workflow;
    private final JTextArea textEditor;
    private TextToWorkflowLink textToWorkflowLink;

    public WorkflowToTextLink(Diagram workflow, JTextArea text)
    {
        this.workflow = workflow;
        this.textEditor = text;
    }
    
    public void setTextToWorkflowLink(TextToWorkflowLink textToWorkflowLink)
    {
        this.textToWorkflowLink = textToWorkflowLink;
    }

    public void enable()
    {
        workflow.addDataCollectionListener( this );
    }
    
    public void disable()
    {
        workflow.removeDataCollectionListener( this );
    }
    
    @Override
    public void elementAdded(DataCollectionEvent e) throws Exception
    {
        workflowChanged();
    }

    @Override
    public void elementRemoved(DataCollectionEvent e) throws DataCollectionVetoException, Exception
    {
        workflowChanged();
    }

    @Override
    public void elementChanged(DataCollectionEvent e) throws Exception
    {
        workflowChanged();
    }

    public void workflowChanged()
    {
        WorkflowToYamlConverter converter = new WorkflowToYamlConverter();
        Map<String, Object> yamlModel = converter.convert( workflow );
        final String text = new Yaml().dump( yamlModel );
        if(textEditor.getText().equals( text ))
            return;
        SwingUtilities.invokeLater( new Runnable()
        {
            @Override
            public void run()
            {
                textToWorkflowLink.disable();
                try
                {
                    textEditor.setText( text );
                }
                finally
                {
                    textToWorkflowLink.enable();
                }
            }
        } );
    }
}
