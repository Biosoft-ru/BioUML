package biouml.plugins.research.workflow.items;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.json.JSONException;
import org.json.JSONObject;

import ru.biosoft.access.repository.JSONSerializable;

import biouml.model.Compartment;
import com.developmentontheedge.beans.editors.CustomEditorSupport;
import com.developmentontheedge.application.Application;

public class ExpressionEditor extends CustomEditorSupport implements JSONSerializable
{
    private JTextField textField = new JTextField();
    private JButton editButton = new JButton("...");
    private JPanel panel;
    private Component parent;
    
    public ExpressionEditor()
    {
        panel = new JPanel(new BorderLayout());
        panel.add(textField);
        panel.add(editButton, BorderLayout.EAST);

        editButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                editButtonAction();
            }
        });
    }

    protected void editButtonAction()
    {
        Compartment d = null;
        try
        {
            d = (Compartment)((WorkflowItem)getBean()).getNode().getOrigin();
        }
        catch( Exception e )
        {
        }
        int pos = textField.getCaretPosition();
        if(d == null)
        {
            JOptionPane.showMessageDialog(parent, "Unable to access Diagram", "Error", JOptionPane.ERROR_MESSAGE);
        }
        while(!(parent instanceof JFrame) && parent != null) parent = parent.getParent();
        if(parent == null) parent = Application.getApplicationFrame();
        VariableSelectorDialog dialog = new VariableSelectorDialog((JFrame)parent, d);
        if(dialog.doModal())
        {
            String text = textField.getText();
            textField.setText(text.substring(0, pos)+"$"+WorkflowExpression.escape( dialog.getSelectedVariable() )+"$"+text.substring(pos));
            textField.requestFocus();
            int newPos = pos+2+dialog.getSelectedVariable().length();
            textField.setSelectionStart(pos);
            textField.setSelectionEnd(newPos);
        }
    }

    @Override
    public Object getValue()
    {
        return textField.getText();
    }

    @Override
    public void setValue(Object text)
    {
        textField.setText(text==null?null:text.toString());
    }

    @Override
    public Component getCustomEditor(Component parent, boolean isSelected)
    {
        this.parent = parent;
        textField.addActionListener(this);
        return panel;
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
        result.put("type", "workflow-expression");
        result.put("value", getValue().toString());
        return result;
    }
}
