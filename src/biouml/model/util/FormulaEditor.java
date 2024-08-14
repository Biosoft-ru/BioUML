package biouml.model.util;

import java.awt.BorderLayout;
import java.net.URL;

import javax.swing.JLabel;
import javax.swing.JOptionPane;

import ru.biosoft.graphics.editor.ViewPaneAdapter;
import ru.biosoft.graphics.editor.ViewPaneEvent;
import ru.biosoft.gui.Document;
import ru.biosoft.math.Expression;
import ru.biosoft.math.ExpressionEditorDialog;
import ru.biosoft.math.ExpressionEditorPane;
import ru.biosoft.math.model.AstStart;
import ru.biosoft.math.model.LinearFormatter;
import ru.biosoft.util.IconUtils;
import biouml.model.Node;
import biouml.model.Role;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.ExpressionOwner;
import biouml.model.dynamics.VariableRole;
import biouml.standard.type.Reaction;
import biouml.workbench.diagram.DiagramDocument;

import com.developmentontheedge.beans.editors.TextButtonEditor;
import com.developmentontheedge.application.Application;

/**
 * Math expression editor
 */
public class FormulaEditor extends TextButtonEditor
{
    protected DiagramDocument diagramDocument;
    protected JLabel titledTextField = new JLabel();

    public FormulaEditor()
    {
        super();

        editor.remove(textField);
        editor.add(titledTextField, BorderLayout.CENTER);

        URL url = FormulaEditor.class.getResource("resources/edit.gif");
        getButton().setText("");
        getButton().setIcon( IconUtils.getImageIcon( url ) );
    }

    @Override
    protected void buttonPressed()
    {
        final ExpressionEditorDialog dialog = new ExpressionEditorDialog(new String[] {ExpressionEditorPane.TEXT_PANEL_NAME}, false);

        if( ! ( getBean() instanceof ExpressionOwner ) )
        {
            JOptionPane.showMessageDialog(Application.getApplicationFrame(), "Bean is not compatible with expression editor",
                    "Editor unavailable", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if( getEModel() == null )
        {
            JOptionPane.showMessageDialog(Application.getApplicationFrame(), "Diagram model is not defined", "Editor unavailable",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        final ViewPaneAdapter adapter = ( new ViewPaneAdapter()
        {
            @Override
            public void mousePressed(ViewPaneEvent e)
            {
                Object model = e.getViewSource().getModel();

                if( model instanceof Role )
                    model = ( (Role)model ).getDiagramElement();

                if( model instanceof Node )
                {
                    Node node = (Node)model;
                    if( node.getKernel() instanceof Reaction )
                        return;

                    Role role = node.getRole();
                    if( role instanceof VariableRole )
                    {
                        int mode = emodel.getDiagramElement().getViewOptions().getVarNameCode();
                        String varName = emodel.getQualifiedName( ( (VariableRole)role ).getName(), node, mode );
                        dialog.addVariableToFormula(varName);
                    }
                }
            }
        } );

        Expression expression;
        try
        {
            emodel.getParser().setDeclareUndefinedVariables( false );
            int mode = emodel.getDiagramElement().getViewOptions().getVarNameCode();
            AstStart astStart = emodel.readMath( (String)getValue(), getRole(), mode );
            expression = new Expression(null, ( ExpressionEditorPane.linearFormatter.format(astStart) )[1]);
            expression.setParserContext(emodel);
            expression.setAstStart(astStart);
            emodel.getParser().setDeclareUndefinedVariables( true );
        }
        catch( Exception ex )
        {
            expression = new Expression(null, "");
        }
        dialog.setVariableResolver(emodel.getVariableResolver(EModel.VARIABLE_NAME_BY_ID));
        dialog.setExpression(expression);
        dialog.getOkButton().addActionListener(e ->
        {
            String text = new LinearFormatter().format( dialog.getExpression().getAstStart() )[1];
            setValue(text);
            diagramDocument.getViewPane().removeViewPaneListener(adapter);
        });

        diagramDocument.getViewPane().addViewPaneListener(adapter);

        dialog.doModal();
    }

    @Override
    public void setValue(Object value)
    {
        super.setValue(value);
        String titledValue = value.toString();
        titledTextField.setText(titledValue);
    }

    protected Role getRole()
    {
        return getBean() instanceof ExpressionOwner? ( (ExpressionOwner)getBean() ).getRole() : null;
    }

    protected EModel emodel = null;
    protected EModel getEModel()
    {
        if( emodel == null )
        {
            Document document = Document.getCurrentDocument();
            if( document instanceof DiagramDocument )
                diagramDocument = ( (DiagramDocument)document );

            if( diagramDocument != null )
            {
                Role diagramRole = this.diagramDocument.getDiagram().getRole();
                if( diagramRole instanceof EModel )
                    emodel = (EModel)diagramRole;
            }
        }
        return emodel;
    }
}
