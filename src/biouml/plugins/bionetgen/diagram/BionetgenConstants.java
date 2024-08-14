package biouml.plugins.bionetgen.diagram;

import java.beans.PropertyDescriptor;

import javax.annotation.Nonnull;

import ru.biosoft.util.bean.StaticDescriptor;
import com.developmentontheedge.beans.editors.StringTagEditor;

public class BionetgenConstants
{
    //----------------Reserved words------------------------------------------
    public static final String END = "end";
    public static final String PARAMETERS = "parameters";
    public static final String BEGIN = "begin";
    public static final String SEED_SPECIES = "seed species";
    public static final String MOLECULE_TYPES = "molecule types";
    public static final String MODEL = "model";
    public static final String OBSERVABLES = "observables";
    public static final String REACTION_RULES = "reaction rules";
    public static final String EDGE_NAME = "bound";
    public static final String OBSERVABLE = "observable";
    public static final @Nonnull String EQUATION_NAME = "math_equation";
    public static final @Nonnull String MOLECULE_TYPE_NAME = "Molecule_Type";

    //----------------Possible xml types--------------------------------------
    public static final String TYPE_SPECIES = "species";
    public static final String TYPE_MOLECULE = "molecule";
    public static final String TYPE_MOLECULE_COMPONENT = "molecule component";
    public static final String TYPE_OBSERVABLE = "observable";
    public static final String TYPE_EDGE = "notelink";
    public static final String TYPE_REACTION = "reaction";
    public static final String TYPE_EQUATION = "equation";
    public static final String TYPE_MOLECULETYPE = "molecule type";

    //----------------Reserved attributes' names------------------------------
    public static final String BACKWARD_RATE_ATTR = "BackwardRate";
    public static final String FORWARD_RATE_ATTR = "ForwardRate";
    public static final String RATE_LAW_TYPE_ATTR = "RateLawType";
    public static final String ADDITION_ATTR = "Addition";
    public static final String REVERSIBLE_ATTR = "reversible";
    public static final String CONTENT_ATTR = "Content";
    public static final String MATCH_ONCE_ATTR = "MatchOnce";
    public static final String IS_SEED_SPECIES_ATTR = "isSeedSpecies";
    public static final String GRAPH_ATTR = "Graph";
    public static final String MOLECULE_ATTR = "Molecule";
    public static final String MOLECULE_TYPE_ATTR = "MoleculeType";
    public static final String REACTANT_NUMBER_ATTR = "ReactantNumber";
    public static final String GENERATE_NETWORK_ATTR = "generate_network";
    public static final String SIMULATE_ODE_ATTR = "simulate_ode";
    public static final String SIMULATE_SSA_ATTR = "simulate_ssa";
    public static final String SIMULATE_ATTR = "simulate";
    public static final String LABEL_ATTR = "Label";

    //----------------Reserved bionetgen action parameters' names-------------
    public static final String ATOL_PARAM = "atol";
    public static final String RTOL_PARAM = "rtol";
    public static final String N_OUTPUT_STEPS_PARAM = "n_output_steps";
    public static final String N_STEPS_PARAM = "n_steps";
    public static final String T_END_PARAM = "t_end";
    public static final String T_START_PARAM = "t_start";
    public static final String MAX_STOICH_PARAM = "max_stoich";
    public static final String MAX_ITER_PARAM = "max_iter";
    public static final String MAX_AGG_PARAM = "max_agg";
    public static final String SAMPLE_TIMES_PARAM = "sample_times";
    public static final String PREFIX_PARAM = "prefix";
    public static final String SUFFIX_PARAM = "suffix";

    //----------------Bionetgen rate law types--------------------------------
    public static final String DEFAULT = "Default";
    public static final String MM = "MM";
    public static final String SATURATION = "Sat";

    //----------------Pre-defined bionetgen kinetic law formulas--------------
    public static final String MM_FORMULA = "function MM(s1,s2,kcat,Km) = kcat*sFree(s1,s2,Km)*s2/(Km + sFree(s1,s2,Km))";
    public static final String MM_SFREE_FORMULA = "function sFree(s1,s2,Km) = 0.5*(term(s1,s2,Km)+sqrt((term(s1,s2,Km))^2+4*Km*s1))";
    public static final String MM_TERM_FORMULA = "function term(s1,s2,Km) = s1-s2-Km";
    public static final String SATURATION_FORMULA_1REACTANT = "function Sat_1(s1,kcat,Km) = kcat*s1/(Km+s1)";
    public static final String SATURATION_FORMULA_2REACTANTS = "function Sat_2(s1,s2,kcat,Km) = s2*Sat_1(s1,kcat,Km)";

    //----------------Static property descriptors-----------------------------
    public static final PropertyDescriptor RATE_LAW_TYPE_PD = StaticDescriptor.create( RATE_LAW_TYPE_ATTR, RateLawTypeEditor.class );
    public static final PropertyDescriptor IS_SEED_SPECIES_PD = StaticDescriptor.create( IS_SEED_SPECIES_ATTR, "Is seed species" );
    
    public static class RateLawTypeEditor extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            return new String[] {DEFAULT, MM, SATURATION};
        }
    }
}
