package biouml.plugins.research.workflow.yaml;

import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import ru.biosoft.gui.Document;
import biouml.model.Diagram;

/**
 * Links text(yaml) representation to diagram representation. 
 */
public class TextToWorkflowLink implements DocumentListener
{
    private JTextArea textEditor;
    private WorkflowToTextLink workflowToTextLink;
    private WorkflowUpdater workflowUpdater;

    public TextToWorkflowLink(Diagram diagram, JTextArea textEditor, Document document)
    {
        this.textEditor = textEditor;
        this.workflowUpdater = new WorkflowUpdater( diagram, document );
    }

    public void setWorkflowToTextLink(WorkflowToTextLink workflowToTextLink)
    {
        this.workflowToTextLink = workflowToTextLink;
    }

    public void enable()
    {
        textEditor.getDocument().addDocumentListener( this );
    }

    public void disable()
    {
        textEditor.getDocument().removeDocumentListener( this );
    }

    @Override
    public void insertUpdate(DocumentEvent e)
    {
        textChanged();
    }

    @Override
    public void removeUpdate(DocumentEvent e)
    {
        textChanged();
    }

    @Override
    public void changedUpdate(DocumentEvent e)
    {
        textChanged();
    }

    private void textChanged()
    {
        workflowToTextLink.disable();
        try
        {
            String text = textEditor.getText();
            workflowUpdater.updateWorkflow( text );
        }
        catch(Exception e)
        {
            //Ignore, may be due to intermediate changes
        }
        finally
        {
            workflowToTextLink.enable();
        }

    }
}