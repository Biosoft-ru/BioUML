package biouml.plugins.research.workflow.items;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JOptionPane;

import org.json.JSONException;
import org.json.JSONObject;

import ru.biosoft.access.repository.JSONSerializable;
import ru.biosoft.gui.Document;
import biouml.model.Node;
import biouml.plugins.research.workflow.engine.ScriptElement;
import biouml.plugins.research.workflow.engine.TextScriptParameters;
import biouml.workbench.diagram.DiagramDocument;

import com.developmentontheedge.beans.editors.TextButtonEditor;
import com.developmentontheedge.beans.undo.PropertyChangeUndo;
import com.developmentontheedge.beans.undo.TransactionEvent;
import com.developmentontheedge.beans.undo.TransactionListener;
import com.developmentontheedge.application.Application;

/**
 * Editor for Script elements in workflow
 */
public class TextScriptEditor extends TextButtonEditor implements JSONSerializable
{

    protected JLabel titledTextField = new JLabel();
    protected String scriptText = "";

    public TextScriptEditor()
    {
        super();
        editor.remove(textField);
        editor.add(titledTextField, BorderLayout.CENTER);
    }

    @Override
    protected void buttonPressed()
    {
        final TextScriptEditorDialog dialog = new TextScriptEditorDialog(scriptText);

        if( ! ( getBean() instanceof TextScriptParameters ) )
        {
            JOptionPane.showMessageDialog(Application.getApplicationFrame(), "Bean is not compatible with java script editor",
                    "Editor unavailable", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if( dialog.doModal() )
        {
            setValue(dialog.getScript());

            Document document = Document.getCurrentDocument();
            if( document instanceof DiagramDocument )
            {
                document.update();
            }
            TextScriptParameters params = (TextScriptParameters)getBean();
            Node node = params.getNode();
            String oldValue = params.getScript();
            TransactionListener listener = document != null ? document.getUndoManager() : null;
            if( listener != null )
                listener.startTransaction(new TransactionEvent(document, "Change"));
            params.setScript(scriptText);
            if( listener != null )
            {
                listener.addEdit(new PropertyChangeUndo(node, "attributes/" + ScriptElement.SCRIPT_SOURCE, oldValue, scriptText));
                listener.completeTransaction();
            }
        }
    }

    @Override
    public void setValue(Object value)
    {
        super.setValue(value);
        scriptText = (String)value;
        titledTextField.setText(scriptText);
    }

    @Override
    public Object getValue()
    {
        return scriptText;
    }

    @Override
    public void fromJSON(JSONObject input) throws JSONException
    {
        String expression = input.getString("value");
        setValue(expression);
    }

    @Override
    public JSONObject toJSON() throws JSONException
    {
        JSONObject result = new JSONObject();
        result.put("type", "text-script");
        result.put("value", getValue());
        return result;
    }
}
