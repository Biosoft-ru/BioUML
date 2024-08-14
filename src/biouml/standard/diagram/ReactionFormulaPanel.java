package biouml.standard.diagram;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Role;
import biouml.model.dynamics.EModel;
import biouml.standard.type.Reaction;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.editor.ViewPane;
import ru.biosoft.math.Expression;
import ru.biosoft.math.ExpressionEditorPane;
import ru.biosoft.math.model.AbstractParser;
import ru.biosoft.math.model.AstStart;
import ru.biosoft.math.model.Parser;
import ru.biosoft.math.view.FormulaViewBuilder;

public class ReactionFormulaPanel extends JPanel
{        
    private final AbstractParser linearParser = new ru.biosoft.math.parser.Parser();
    protected FormulaViewBuilder viewBuilder = new FormulaViewBuilder();
    protected Expression expression;
    protected ViewPane reactionView;
    private int status = Parser.STATUS_OK;
    
    public ReactionFormulaPanel(EModel emodel, Reaction reaction, Role reactionRole)
    {
        super(new BorderLayout(10, 10));
        setBorder(new TitledBorder(new EmptyBorder(10, 10, 10, 10), ReactionEditPane.resources.getString("REACTION_RATE_PANEL")));
        reactionView = new ViewPane();
        reactionView.setPreferredSize(new Dimension(0, 100));
        initExpression(emodel, reaction, reactionRole);
        add(reactionView, BorderLayout.CENTER);
        viewBuilder.setMultiplySign( emodel.getDiagramElement().getViewOptions().getMultiplySign() );
    }
    
    protected void processFormulaEvent(String text)
    {
        if( expression == null )
            return;

        linearParser.setDeclareUndefinedVariables( false );
        linearParser.setContext( expression.getParserContext() );
        status = Parser.STATUS_OK;
        try
        {
            status = linearParser.parse( text );
        }
        catch( Exception ex )
        {
            status |= Parser.STATUS_FATAL_ERROR;
        }

        if( ( status & Parser.STATUS_FATAL_ERROR ) == 0 )
        {
            expression.setAstStart( linearParser.getStartNode() );
            if( isVisible() )
            {
                CompositeView expressionView = viewBuilder.createView( expression.getAstStart(), ApplicationUtils.getGraphics() );
                int x = 0;
                if( reactionView.getWidth() > expressionView.getBounds().getWidth() )
                    x = (int) ( reactionView.getWidth() - expressionView.getBounds().getWidth() ) / 2;

                int y = 0;
                if( reactionView.getHeight() > expressionView.getBounds().getHeight() )
                    y = (int) ( reactionView.getHeight() - expressionView.getBounds().getHeight() ) / 2;

                reactionView.setView( expressionView, new java.awt.Point( x, y ) );
            }
        }
    }
    
    private void initExpression(EModel emodel, Reaction reaction, Role reactionRole)
    {
        if( reaction != null )
        {
            int mode = emodel.getDiagramElement().getViewOptions().getVarNameCode();
            AstStart astStart = emodel.readMath( reaction.getKineticLaw().getFormula(), reactionRole, mode );
            expression = new Expression(null, ( ExpressionEditorPane.linearFormatter.format(astStart) )[1]);
            expression.setAstStart(astStart);
        }
        else
        {
            expression = new Expression(null, "0");
        }
        expression.setParserContext(emodel);

        linearParser.setVariableResolver(emodel.getVariableResolver(EModel.VARIABLE_NAME_BY_ID));
    }
    
    public int getStatus()
    {
        return status;
    }
}