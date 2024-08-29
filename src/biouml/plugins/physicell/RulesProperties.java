package biouml.plugins.physicell;

import java.util.ArrayList;
import java.util.List;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Diagram;
import biouml.model.DiagramElement;
import ru.biosoft.physicell.core.CellDefinition;
import ru.biosoft.physicell.core.HypothesisRule;
import ru.biosoft.physicell.core.HypothesisRuleset;
import ru.biosoft.physicell.core.Model;
import ru.biosoft.physicell.core.Rules;

/**
 * Bean corresponds to HypothesisRuleset in Physicell.
 * Is used for user interface in BioUML, it is a part of CellDefinitionProperties role
 * @see CellDefinitionProperties 
 */
public class RulesProperties extends Option
{
    private CellDefinitionProperties cellDefinition;
    private MulticellEModel emodel;
    private RuleProperties[] rules = new RuleProperties[0];

    /**
     * It is called when parent CellDefinitionProperties is assigned to given DiagramElement as role
     * Initialize bean with properties of given DiagramElement 
     */
    public void setDiagramElement(DiagramElement de)
    {
        cellDefinition = de.getRole( CellDefinitionProperties.class );
        emodel = Diagram.getDiagram( de ).getRole( MulticellEModel.class );
        for( RuleProperties ruleProperties : rules )
        {
            ruleProperties.setCellDefinition( cellDefinition );
            ruleProperties.setEModel( emodel );
        }
    }

    /**
     * Create Physicell HypothesisRuleset object from BioUML RulesProperties
     * First is for numerical simulation, second - for user interface and model setting 
     */
    public void createRules(CellDefinition cd, Model model) throws Exception
    {
        if( rules.length == 0 )
            return;
        model.setRulesEnabled( true );
        Rules.setupRules( model );
        model.getSignals().setupDictionaries( model );
        for( RuleProperties rule : rules )
        {
            Rules.addRule( model, cd.name, rule.getSignal(), rule.getBehavior(), rule.getDirection(), rule.isApplyToDead() );
            model.getRules().set_hypothesis_parameters( model, cd.name, rule.getSignal(), rule.getBehavior(), rule.getHalfMax(), rule.getHillPower() );
//            rules.set_hypothesis_parameters( model, cell_type, signal, behavior, half_max, hill_power );
            // compare to base behavior value in cell def for discrepancies 
            double ref_base_value = model.getSignals().getSingleBaseBehavior( model, cd, rule.getBehavior() );
            Rules.setBehaviorBaseValue( model, cd.name, rule.getBehavior(), ref_base_value );
            if(  rule.getDirection().equals( "increases" ) )
                Rules.set_behavior_max_value( model, cd.name, rule.getBehavior(), rule.getSaturationValue() );
            else
                Rules.setBehaviorMinValue( model, cd.name, rule.getBehavior(),  rule.getSaturationValue() );
        }
    }

    /**
     * Init BioUML RulesProperties from Physicell HypotheisRuleset object 
     * First is used for user interface and model setting, second - for numerical simulation
     */
    public void init(HypothesisRuleset ruleset)
    {
        List<RuleProperties> list = new ArrayList<>();
        if (ruleset == null)
            return;
        for( HypothesisRule rule : ruleset.getRules() )
        {
            for( int i = 0; i < rule.getResponses().size(); i++ )
            {
                RuleProperties ruleProperties = new RuleProperties();
                ruleProperties.setDirection(
                        rule.getResponses().get( i ) ? RuleProperties.DIRECTION_INCREASES : RuleProperties.DIRECTION_DECREASES );
                ruleProperties.setHalfMax( rule.getHalfMaxes().get( i ) );
                ruleProperties.setHillPower( rule.getHillPowers().get( i ) );
                ruleProperties.setBehavior( rule.getBehavior() );
                ruleProperties.setSaturationValue( rule.getMaxValue() );
                ruleProperties.setSignal( rule.getSignals().get( i ) );
                ruleProperties.setApplyToDead( rule.isAppliesToDead().get( i ) );
                list.add( ruleProperties );
            }
        }
        this.setRules( list.toArray( new RuleProperties[list.size()] ) );
    }

    @PropertyName ( "Rules" )
    public RuleProperties[] getRules()
    {
        return rules;
    }
    public void setRules(RuleProperties[] rules)
    {
        Object oldValue = this.rules;
        this.rules = rules;
        for( RuleProperties ruleProperties : rules )
        {
            ruleProperties.setCellDefinition( cellDefinition );
            ruleProperties.setEModel( emodel );
        }
        firePropertyChange( "rules", oldValue, rules );
    }

    /**
     * Add single rule to ruleset
     */
    public void addRule()
    {
        int l = rules.length;
        RuleProperties[] newRules = new RuleProperties[l + 1];
        newRules[l] = new RuleProperties();
        System.arraycopy( rules, 0, newRules, 0, l );
        this.setRules( newRules );
    }

    /**
     * Remove single rule with given index from ruleset
     */
    public void removeRule(int index)
    {
        int l = rules.length;
        RuleProperties[] newRules = new RuleProperties[l - 1];
        if( index == 0 )
            System.arraycopy( rules, 1, newRules, 0, l - 1 );
        else if( index == l - 1 )
            System.arraycopy( rules, 0, newRules, 0, l - 1 );
        else
        {
            System.arraycopy( rules, 0, newRules, 0, index );
            System.arraycopy( rules, index + 1, newRules, index, l - index - 1 );
        }
        this.setRules( newRules );
    }

    public RulesProperties clone(DiagramElement de)
    {
        RulesProperties result = new RulesProperties();
        result.setDiagramElement( de );
        RuleProperties[] newRules = new RuleProperties[rules.length];
        for( int i = 0; i < rules.length; i++ )
            newRules[i] = rules[i].clone();
        result.setRules( newRules );
        return result;
    }
}