package ru.biosoft.math;

import ru.biosoft.math.model.VariableResolver;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.dialog.OkCancelDialog;

public class ExpressionEditorDialog extends OkCancelDialog
{
    protected ExpressionEditorPane editorPane;

    public ExpressionEditorDialog()
    {
        this(new String[] {ExpressionEditorPane.TEXT_PANEL_NAME, ExpressionEditorPane.MATHML_PANEL_NAME}, true);
    }

    public ExpressionEditorDialog(String[] panelNames, boolean dumpAvailable)
    {
        super(Application.getApplicationFrame(), "Mathematical expression editor");

        editorPane = new ExpressionEditorPane(okButton, panelNames, dumpAvailable);
        setContent(editorPane);
        pack();
    }

    public Expression getExpression()
    {
        return editorPane.getExpression();
    }
    public void setExpression(Expression expression)
    {
        editorPane.setExpression(expression);
    }

    public void addVariableToFormula(String varName)
    {
        editorPane.addVariableToFormula(varName);
    }

    public void setVariableResolver(VariableResolver variableResolver)
    {
        editorPane.setVariableResolver(variableResolver);
    }
}
