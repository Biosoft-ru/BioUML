package biouml.standard.diagram;

import java.awt.BorderLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import com.developmentontheedge.beans.swing.table.DefaultRowModel;

import biouml.model.Diagram;
import biouml.model.Role;
import biouml.model.dynamics.EModel;
import biouml.standard.type.Reaction;
import ru.biosoft.math.ExpressionEditorPane;
import ru.biosoft.math.model.AstStart;

/**
 * Edit pane for reaction kinetic law. Is used by kinetic law editor and create reaction pane.
 */
@SuppressWarnings ( "serial" )
public class ReactionEditPane extends JPanel
{
    protected static final Logger log = Logger.getLogger(ReactionEditPane.class.getName());
    protected static ResourceBundle resources = ResourceBundle.getBundle(MessageBundle.class.getName()); 
    
    protected Diagram diagram;
    protected ReactionFormulaPanel formulaPanel;
    protected DefaultRowModel components;
    protected JTextField reactionRate;
    protected FormulaTemplatePane specialPanel;
    protected Reaction reactionTemplate;
    protected JTextField titleField;

    public static final String RATE_SIMPLE_TAB_NAME = "Simple";
    
    public ReactionEditPane(Reaction reaction, Role reactionRole, Diagram diagram, DefaultRowModel components, boolean newReaction, boolean canModifyReaction)
    {
        super(new BorderLayout());
        this.diagram = diagram;
        this.components = components;
        JTabbedPane tabbedPane = new JTabbedPane();
        reactionRate = new JTextField();
        reactionRate.setText( "0" );
        formulaPanel = new ReactionFormulaPanel(diagram.getRole( EModel.class ), reaction, reactionRole);
        add(formulaPanel, BorderLayout.NORTH);
        
        if( newReaction )
        {
            reactionRate.getDocument().addDocumentListener( new DocumentListener()
            {
                @Override
                public void changedUpdate(DocumentEvent e)
                {
                    formulaPanel.processFormulaEvent(reactionRate.getText());
                }
                @Override
                public void insertUpdate(DocumentEvent e)
                {
                    formulaPanel.processFormulaEvent(reactionRate.getText());
                }
                @Override
                public void removeUpdate(DocumentEvent e)
                {
                    formulaPanel.processFormulaEvent(reactionRate.getText());
                }
            } );
        }
        JPanel simplePanel = new JPanel(new BorderLayout());
        simplePanel.add(reactionRate, BorderLayout.NORTH);
       
        specialPanel = new FormulaTemplatePane(reactionRate, components, diagram);
        tabbedPane.add(FormulaTemplatePane.PANE_NAME, specialPanel);
        tabbedPane.add(RATE_SIMPLE_TAB_NAME, simplePanel);
        add(tabbedPane, BorderLayout.CENTER);

        formulaPanel.addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e)
            {
                formulaPanel.processFormulaEvent(reactionRate.getText());
            }
        });

        initReactionInfo( reaction, reactionRole, canModifyReaction );
    }
    
    /**
     * Get kinetic law formula
     */
    public String getFormula()
    {
        String formula = reactionRate.getText();
        if( formula.trim().isEmpty() )
            return "0";

        Role role = diagram.getRole();
        if( role instanceof EModel)
        {
            AstStart astStart = ( (EModel)role ).readMath(formula, null, EModel.VARIABLE_NAME_BY_ID);
            return ExpressionEditorPane.linearFormatter.format(astStart)[1];
        }
        return formula;
    }
 
    protected void initReactionInfo(Reaction reaction, Role reactionRole, boolean canModifyReaction)
    {
        if( reaction != null )
        {
            reactionTemplate = reaction;

            reaction.forEach( s -> {
                s.setInitialized( false );
                components.add( s );
            } );
            specialPanel.changeComponents();
            String rateText = getTitledFormula(reaction.getFormula(), reaction, reactionRole);
            reactionRate.setText(rateText);
            formulaPanel.processFormulaEvent(rateText);
        }
    }

    protected String getTitledFormula(String formula, Reaction reaction, Role reactionRole)
    {
        Role role = diagram.getRole();
        if( role instanceof EModel )
        {
            int mode = diagram.getViewOptions().getVarNameCode();
            AstStart astStart = ( (EModel)role ).readMath( formula, reactionRole, mode );
            return ExpressionEditorPane.linearFormatter.format(astStart)[1];
        }
        return formula;
    }
    
    protected void changeComponents()
    {
        this.specialPanel.changeComponents();
    }    
}
