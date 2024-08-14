package biouml.standard.diagram;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Node;
import biouml.standard.type.Base;
import biouml.standard.type.Gene;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Substance;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;

public class ReactionUtility
{
    private static final String PHOSPHORYLATION = "Posphorylation";
    public static final String NEDDYLATION = "Neddylation";
    public static final String SUMOYLATION = "Sumoylation";
    public static final String MODULATED = "Modulated";
    private static final String EXPRESSION = "Expression";
    private static final String GENE_REGULATION = "Gene regulation";
    private static final String NON_ENZYME = "Enzyme";

    public static final String ONE_PRODUCT = "One product";
    public static final String ONE_REACTANT = "One reactant";
    public static final String ONE_MODIFIER = "One modifier";

    public static final String NO_PRODUCT = "No product";
    public static final String NO_REACTANT = "No reactant";
    public static final String NO_MODIFIER = "No modifier";

    public static final String SEMANTIC = "Semantic";

    private static final String ENZYMATIC = "Enzymatic";

    //    public static final String UNI_UNI = "Uni-uni";
    //    public static final String UNI_UNI_UNI = "Uni-uni-uni";
    public static final String HAS_GENE = "Has genes";
    public static final String GENE_REACTANTS = "Only gene reactants";
    public static final String GENE_PRODUCTS = "Only gene products";
    public static final String HAS_ENZYME = "Has enzyme";
    public static final String ALL_ENZYME = "Has only enzymes";
    public static final String MANY_ENZYMES = "Many enzymes";
    //    public static final String ONE_MODIFIER = "One modifier";
    public static final String TRANSLOCATION = "Translocation";
    public static final String UBIQUITINATION = "Ubiquitination";
    public static final String INHIBITION_UNI_UNI = "UNI-UNI inhibition";
    public static final String INHIBITION = "Inhibition";
    public static final String HAS_INHIBITORS = "Has inhibitors";

    public static final String SIMILAR_PARTICIPANTS = "Similar participants";

    public static final String YLATION_TYPE = "Has ylation";
    public static final String HYDROLYSIS = "Hydrolysis";

    //    private static final String NO_REACTANTS = "No reactants";
    //    private static final String NO_PRODUCTS = "No products";
    //    private static final String ASSOCIATION = "Association";
    //    private static final String DISSOCIATION = "Dissociation";
    public static final String IRREVERSIBLE = "Irreversible";
    public static final String REVERSIBLE = "Reversible";

    public static class KinetikLawDescription
    {
        private Map<String, Integer> requiredTags = new HashMap<>();
        private Map<String, Integer> forbiddenTags = new HashMap<>();
        private Map<String, Integer> optionalTags = new HashMap<>();

        protected List<VariableDescription> variables = new ArrayList<>();
        private String name;
        private String description;
        protected String formula;

        public List<String> getParameters(Reaction reaction, Set<String> tags)
        {
            return new ArrayList<>();
        }

        public KinetikLawDescription(String name, String description, String formula)
        {
            this.name = name;
            this.description = description;
            this.formula = formula;
        }

        public void apply(Reaction reaction, Set<String> tags)
        {
            for( String s : getParameters(reaction, tags) )
                variables.add(new VariableDescription(s, "", VariableDescription.PARAMETER));

            for( SpecieReference ref : reaction.getSpecieReferences() )
            {
                String type = ref.isReactant() ? VariableDescription.REACTANT
                        : ref.isProduct() ? VariableDescription.PRODUCT : VariableDescription.MODIFIER;
                variables.add(new VariableDescription(ref.getSpecieVariable(), ref.getSpecieName(), "", type, ref.getStoichiometry()));
            }
        }

        public void addRequiredTag(String tag, int score)
        {
            requiredTags.put(tag, score);
        }

        public void addForbiddenTag(String tag, int score)
        {
            forbiddenTags.put(tag, score);
        }

        public void addOptionalTags(String tag, int score)
        {
            optionalTags.put(tag, score);
        }

        public int accept(Set<String> tags)
        {
            int result = 1;
            for( String tag : tags )
            {
                if( forbiddenTags.containsKey(tag) )
                    return 0;

                if( optionalTags.containsKey(tag) )
                    result += optionalTags.get(tag);
            }

            for( Map.Entry<String, Integer> requiredTagEntry : requiredTags.entrySet() )
            {
                String tag = requiredTagEntry.getKey();
                if( !tags.contains(tag) )
                    return 0;
                else
                    result += requiredTagEntry.getValue();
            }
            return result;
        }
    }

    public static Set<String> getReactionType(Reaction reaction, DataCollection molecules, DataCollection genes) throws Exception
    {
        Set<String> result = new HashSet<>();

        //special terminal types
        if( isSemantic(reaction) )
        {
            result.add(SEMANTIC);
            return result;
        }

        if( isInhibition(reaction) )
        {
            result.add(INHIBITION);
            return result;
        }

        List<SpecieReference> reactants = new ArrayList<>();
        List<SpecieReference> products = new ArrayList<>();
        List<SpecieReference> modifiers = new ArrayList<>();
        List<SpecieReference> inhibitors = new ArrayList<>();

        for( SpecieReference ref :reaction.getSpecieReferences() )
        {
            if (ref.getSpecieName().equals("ATP") || ref.getSpecieName().equals("ADP")) //ATP and ADP ignored
                continue;

            if( ref.isReactant() )
                reactants.add(ref);
            else if( ref.isProduct())
                products.add(ref);
            else
            {
                if( ref.getModifierAction().equals(SpecieReference.ACTION_INHIBITOR) )
                    inhibitors.add(ref);
                else
                    modifiers.add(ref);
            }
        }

        result.add(reaction.isReversible() ? REVERSIBLE : IRREVERSIBLE);

        //first - number of participants
        if( modifiers.isEmpty() )
            result.add(NO_MODIFIER);
        else if( modifiers.size() == 1 )
            result.add(ONE_MODIFIER);

        if( products.isEmpty() )
            result.add(NO_PRODUCT);
        else if( products.size() == 1 )
            result.add(ONE_PRODUCT);

        if( reactants.isEmpty() )
            result.add(NO_REACTANT);
        else if( reactants.size() == 1 )
            result.add(ONE_REACTANT);

        if (!inhibitors.isEmpty())
            result.add(HAS_INHIBITORS);

        //check participant types
        if (genes != null)
        result.addAll(checkGenes(reaction, genes));
        
        if (molecules != null)
        result.addAll(checkEnzymes(reaction, molecules));

        if (productMatchReactant(reactants, products))
        result.add(SIMILAR_PARTICIPANTS);

        if( hasEffect(reaction, "translocation") )
            result.add(TRANSLOCATION);

        //special enzymatic types
        if( hasEffect(reaction, "phosphorylation") )
        {
            result.add(ENZYMATIC);
            result.add(PHOSPHORYLATION);
        }

        if( hasEffect(reaction, "ubiquitination") )
        {
            result.add(ENZYMATIC);
            result.add(UBIQUITINATION);
        }

        if( hasEffect(reaction, "neddylation") )
        {
            result.add(ENZYMATIC);
            result.add(NEDDYLATION);
        }

        if( hasEffect(reaction, "sumoylation") )
        {
            result.add(ENZYMATIC);
            result.add(SUMOYLATION);
        }

        if( hasEffect(reaction, "ylation") )
        {
            result.add(ENZYMATIC);
            result.add(YLATION_TYPE);
        }

        if( hasEffect(reaction, "hydrolysis") )
        {
            result.add(ENZYMATIC);
            result.add(HYDROLYSIS);
        }

        return result;
    }

    private final static String[] enzymaticTypes = new String[]{NEDDYLATION, SUMOYLATION, YLATION_TYPE, HYDROLYSIS, HAS_ENZYME};

    private static Set<String> checkGenes(Reaction reaction, DataCollection genes)
    {
        Set<String> result = new HashSet<>();
        boolean notGeneReactant = false;
        boolean notGeneProduct = false;
        for( SpecieReference sr : StreamEx.of(reaction.getSpecieReferences()) )
        {
            String specie = sr.getSpecieName();

            if( genes.contains(specie) )
                result.add(HAS_GENE);

            if( !genes.contains(specie) )
            {
                if( sr.isProduct() )
                    notGeneProduct = true;
                else if( sr.isReactant() )
                    notGeneReactant = true;
            }
        }
        if( !notGeneProduct )
            result.add(GENE_PRODUCTS);
        if( !notGeneReactant )
            result.add(GENE_REACTANTS);

        return result;
    }

    //TODO: move all this to Transpath plugin and reuse their constants
    public static final Set<String> STOP_LIST = StreamEx.of( "MO000000072",
            "MO000000328", "MO000000006", "MO000016761", "MO000000327", "MO000000007", "MO000000329", "MO000000216", "MO000020185",
            "MO000020184", "MO000019479", "MO000021841", "MO000044086", "MO000000223", "MO000044168", "MO000021828", "MO000000111",
            "MO000000229", "MO000102541", "MO000000152", "MO000000244", "MO000044087", "MO000021822", "MO000000314", "MO000099583",
            "MO000021833", "MO000019387", "MO000044146", "MO000099582", "MO000099584", "MO000099581", "MO000021837", "MO000021894",
            "MO000017082", "MO000099597", "MO000021835", "MO000000081", "MO000021834", "MO000099592", "MO000018134", "MO000019000",
            "MO000099652", "MO000000149", "MO000021878", "MO000099640", "MO000044112", "MO000000222", "MO000021944", "MO000044095",
            "MO000044165", "MO000099588", "MO000044154", "MO000044133", "MO000044091", "MO000000127", "MO000044697", "MO000019596",
            "MO000099594", "MO000102544", "MO000044109", "MO000044033", "MO000044701", "MO000099589", "MO000000013", "MO000016839",
            "MO000021914", "MO000017930", "MO000044861", "MO000000002", "MO000021840", "MO000044166", "MO000019158", "MO000017079",
            "MO000099645", "MO000019585", "MO000016762", "MO000000286", "MO000106960", "MO000100741", "MO000021930", "MO000106075",
            "MO000000031", "MO000000024", "MO000044863", "MO000044005", "MO000044695", "MO000044004", "MO000099620", "MO000043335",
            "MO000021855", "MO000102540", "MO000018454", "MO000099609", "MO000021838", "MO000016957", "MO000044089", "MO000000225",
            "MO000099590", "MO000099601", "MO000021853", "MO000021329", "MO000000265", "MO000099599", "MO000083580", "MO000017297",
            "MO000000266", "MO000000075", "MO000099619", "MO000098444", "MO000102542", "MO000084420", "MO000017629", "MO000106076",
            "MO000099729", "MO000017052", "MO000017242", "MO000021539", "MO000021442", "MO000000077", "MO000099653", "MO000095528",
            "MO000099639", "MO000099603", "MO000044130", "MO000021891", "MO000016810", "MO000000338", "MO000099605", "MO000044172",
            "MO000044092", "MO000097366", "MO000151816", "MO000099635", "MO000000003", "MO000098683", "MO000083524", "MO000035234",
            "MO000017928", "MO000016920", "MO000018269", "MO000099734", "MO000017009", "MO000016930", "MO000000197", "MO000045753",
            "MO000041943", "MO000019802", "MO000100440", "MO000099663", "MO000000097", "MO000044823", "MO000023571", "MO000017735",
            "MO000099598", "MO000042416", "MO000131768", "MO000100452", "MO000099733", "MO000017137", "MO000016927", "MO000100422",
            "MO000099637", "MO000099606", "MO000044150", "MO000020148", "MO000033518", "MO000017683", "MO000017258", "MO000016812",
            "MO000000104", "MO000122838", "MO000122837", "MO000100451", "MO000097970", "MO000019750", "MO000044100", "MO000016791" )
            .toSetAndThen( Collections::unmodifiableSet );

    private static Set<String> checkEnzymes(Reaction reaction, DataCollection<Base> molecules) throws Exception
    {
        Set<String> result = new HashSet<>();
        boolean notEnzyme = false;
        boolean enzyme = false;
        boolean manyEnzymes = false;
        for( SpecieReference sr : StreamEx.of(reaction.getSpecieReferences()).filter(sr -> !sr.isReactantOrProduct()) )
        {
            if( STOP_LIST.contains(sr.getSpecieName() ))
                continue;

            String specie = sr.getSpecieName();

            Base molecule = molecules.get(specie);
            if( molecule == null )
                continue;

            if( isEnzyme(molecule) )
            {
                if( enzyme )
                    manyEnzymes = true;
                else
                    enzyme = true;
            }
            else
                notEnzyme = true;
        }

        if( enzyme )
        {
            result.add(HAS_ENZYME);
            if( !notEnzyme )
                result.add(ALL_ENZYME);
            if (manyEnzymes)
                result.add(MANY_ENZYMES);
        }

        return result;
    }

    public static boolean productMatchReactant(Reaction r)
    {
        return productMatchReactant(getReactants(r).toList(), getProducts(r).toList());
    }

    public static StreamEx<SpecieReference> getReactants(Reaction r)
    {
        return StreamEx.of(r.getSpecieReferences()).filter(sr -> sr.isReactant());
    }

    public static StreamEx<SpecieReference> getModifiers(Reaction r)
    {
        return StreamEx.of(r.getSpecieReferences()).filter(sr -> !sr.isReactantOrProduct());
    }

    public static StreamEx<SpecieReference> getProducts(Reaction r)
    {
        return StreamEx.of(r.getSpecieReferences()).filter(sr -> sr.isProduct());
    }

    public static boolean productMatchReactant(Collection<SpecieReference> reactants, Collection<SpecieReference> products)
    {
        Set<String> reactantsSet = StreamEx.of(reactants).map(sr -> removeBrackets(sr.getTitle())).toSet();
        Set<String> productsSet = StreamEx.of(products).map(sr -> removeBrackets(sr.getTitle())).toSet();
        for( String s : reactantsSet )
            if( productsSet.contains(s) )
                return true;
        return false;
    }

    public static String removeBrackets(String input)
    {
        return input.replaceAll("(\\{.+?\\})", "");
    }

    public static boolean hasInhibitors(Reaction reaction)
    {
        for( SpecieReference sr : StreamEx.of(reaction.getSpecieReferences()).filter(sr -> !sr.isReactantOrProduct()) )
        {
            if( sr.getModifierAction().equals(SpecieReference.ACTION_INHIBITOR) )
                return true;

        }
        return false;
    }

    public static boolean hasEnzyme(Reaction reaction, DataCollection<Substance> molecules)
    {
        try
        {
            for( SpecieReference sr : StreamEx.of(reaction.getSpecieReferences()).filter(sr -> !sr.isReactantOrProduct()) )
            {
                String specie = sr.getSpecieName();

                Substance molecule = molecules.get(specie);
                if( molecule == null )
                    continue;

                if( isEnzyme(molecule) )
                    return true;

            }
        }
        catch( Exception ex )
        {

        }
        return false;
    }

    public static boolean hasGenes(Reaction reaction, DataCollection<Gene> genes)
    {
        for( SpecieReference sr : StreamEx.of(reaction.getSpecieReferences()).filter(sr -> sr.isReactantOrProduct()) )
        {
            String specie = sr.getSpecieName();

            if( genes.contains(specie) )
                return true;
        }
        return false;
    }

    private static boolean isEnzyme(Base base)
    {
        if( base.getTitle().contains("ase ") )
            return true;
        DynamicProperty dp = base.getAttributes().getProperty("classifications");
        return dp != null && dp.getValue().toString().contains("enzymes");
    }

    private static boolean isGene(Node node)
    {
        return node.getKernel() instanceof Gene;
    }

    /**
     * Reaction has only one reactant and one product
     */
    public static boolean isUniUni(Reaction reaction)
    {
        SpecieReference[] reference = reaction.getSpecieReferences();
        if( reference.length != 2 )
            return false;
        return ( reference[0].isReactant() && reference[1].isProduct() ) || ( reference[0].isProduct() && reference[1].isReactant() );
    }

    private static boolean isUniUniMany(Reaction reaction)
    {
        SpecieReference[] reference = reaction.getSpecieReferences();
        return StreamEx.of(reference).filter(sr -> sr.isReactant()).count() == 1
                && StreamEx.of(reference).filter(sr -> sr.isReactant()).count() == 1;
    }


    private static boolean isUniUniUni(Reaction reaction)
    {
        SpecieReference[] reference = reaction.getSpecieReferences();
        if( reference.length != 3 )
            return false;
        return StreamEx.of(reference).map(sr -> sr.getRole()).toSet()
                .containsAll(Arrays.asList(SpecieReference.REACTANT, SpecieReference.PRODUCT, SpecieReference.MODIFIER));
    }

    public static boolean isSemantic(Reaction reaction)
    {
        DynamicProperty dp = reaction.getAttributes().getProperty("reactionType");
        return dp != null && dp.getValue().toString().contains("semantic");
    }

    private static boolean hasEffect(Reaction reaction, String effect)
    {
        DynamicProperty dp = reaction.getAttributes().getProperty("effect");
        return dp != null && dp.getValue().toString().contains(effect);
    }


    private static boolean isUbiquitination(Reaction reaction)
    {
        DynamicProperty dp = reaction.getAttributes().getProperty("effect");
        return dp != null && dp.getValue().toString().contains("ubiquitination");
    }

    private static boolean isYlation(Reaction reaction)
    {
        DynamicProperty dp = reaction.getAttributes().getProperty("effect");
        return dp != null && dp.getValue().toString().contains("ylation");
    }

    private static boolean isHydrolysis(Reaction reaction)
    {
        DynamicProperty dp = reaction.getAttributes().getProperty("effect");
        return dp != null && dp.getValue().toString().contains("hydrolysis");
    }

    public static boolean isInhibition(Reaction reaction)
    {
        return reaction.getTitle().contains("--/");
    }

    public static String generateKinetikLaw(Reaction reaction, Set<String> tags)
    {
        List<KinetikLawDescription> laws = new ArrayList<>();
        laws.add(new MichaelisMenten());
        laws.add(new MassAction());
        laws.add(new ReversedMassAction());

        int priority = Integer.MIN_VALUE;
        KinetikLawDescription law = null;
        for( KinetikLawDescription nextLaw : laws )
        {
            int nextP = nextLaw.accept(tags);
            if( nextP > priority )
            {
                law = nextLaw;
                priority = nextP;
            }
        }
        if( law == null )
            law = new MassAction();

        law.apply(reaction, tags);
        reaction.setFormula(law.formula);
        return law.formula;
    }

    public static String generateKinetikLaw(Reaction reaction, DataCollection molecules, DataCollection genes) throws Exception
    {
        return generateKinetikLaw(reaction, getReactionType(reaction, molecules, genes));
    }

    public static class MichaelisMenten extends KinetikLawDescription
    {
        public MichaelisMenten()
        {
            super("Michaelis-Menten", "", "");
            addRequiredTag(MODULATED, 1);
            addRequiredTag(ONE_MODIFIER, 1);
            addRequiredTag(ENZYMATIC, 1);
        }

        @Override
        public void apply(Reaction reaction, Set<String> tags)
        {
            super.apply(reaction, tags);

            String reactant = null;
            String modifier = null;
            for( VariableDescription v : variables )
            {
                if( v.type.equals(VariableDescription.REACTANT) )
                    reactant = v.value;
                else if( v.type.equals(VariableDescription.ENYZYME) || v.type.equals(VariableDescription.MODIFIER) )
                    modifier = v.value;
            }

            if( modifier == null )
                this.formula = variables.get(0).getName() +"*"+ reactant + "/(" + reactant + "+ " + variables.get(1).getName() + ")";
            else
                this.formula = variables.get(0).getName() + " * " + modifier + "*" + reactant + "/(" + reactant + "+ "
                        + variables.get(1).getName() + ")";
        }

        @Override
        public List<String> getParameters(Reaction reaction, Set<String> tags)
        {
            String suffix = "";
            if( tags.contains(UBIQUITINATION) )
                suffix += "_ubiq";
            else if( tags.contains(SUMOYLATION) )
                suffix += "_sumo";
            else if( tags.contains(HYDROLYSIS) )
                suffix += "_hydro";
            else if( tags.contains(PHOSPHORYLATION) )
                suffix += "_phosopho";
            else if( tags.contains(NEDDYLATION) )
                suffix += "_nedd";
            return StreamEx.of("k_cat"+suffix, "k_M"+suffix).toList();
        }
    }


    public static class ConstantLaw extends KinetikLawDescription
    {
        public ConstantLaw()
        {
            super("Constant", "", "");
            addRequiredTag(SEMANTIC, 1);
        }

        @Override
        public void apply(Reaction reaction, Set<String> tags)
        {
            super.apply(reaction, tags);
            this.formula = variables.get(0).getName();
        }

        @Override
        public List<String> getParameters(Reaction reaction, Set<String> tags)
        {
            return StreamEx.of("k_"+reaction.getName()).toList();
        }
    }

    public static class MassAction extends KinetikLawDescription
    {
        public MassAction()
        {
            super("Mass Action", "", "");
            addForbiddenTag(REVERSIBLE, 1);
            addForbiddenTag(ENZYMATIC, 1);
        }

        @Override
        public List<String> getParameters(Reaction reaction, Set<String> tags)
        {
            return StreamEx.of("k_"+reaction.getName()).toList();
        }

        @Override
        public void apply(Reaction reaction, Set<String> tags)
        {
            super.apply(reaction, tags);
            formula = StreamEx.of(variables)
                    .filter(v -> v.type.equals(VariableDescription.REACTANT) || v.type.equals(VariableDescription.MODIFIER))
                    .map(v -> powered(v)).prepend(variables.get(0).getName()).joining("*");
        }
    }

    public static class ReversedMassAction extends KinetikLawDescription
    {
        public ReversedMassAction()
        {
            super("Mass Action", "", "");
            addRequiredTag(REVERSIBLE, 1);
            addForbiddenTag(ENZYMATIC, 1);
        }

        @Override
        public List<String> getParameters(Reaction reaction, Set<String> tags)
        {
            return StreamEx.of("k_"+reaction.getName(), "r_"+reaction.getName()).toList();
        }

        @Override
        public void apply(Reaction reaction, Set<String> tags)
        {
            super.apply(reaction, tags);
            String reactants = StreamEx.of(variables).filter(v -> v.type.equals(VariableDescription.REACTANT)).map(v -> powered(v))
                    .joining("*");
            String products = StreamEx.of(variables).filter(v -> v.type.equals(VariableDescription.PRODUCT)).map(v -> powered(v))
                    .joining("*");
            this.formula = variables.get(0) +"*" + reactants + " - " + variables.get(1)+" * " + products;
        }
    }

    public static String powered(VariableDescription v)
    {
        if( v.stochiometry > 1 )
            return v.getValue() + "^" + v.stochiometry;
        return v.getValue();
    }

    public static class SBOKinetikLaw extends KinetikLawDescription
    {

        public SBOKinetikLaw(String name, String description, String formula)
        {
            super(name, description, formula);
        }
    }

    public static class VariableDescription
    {
        public static final String REACTANT = "Reactant";
        public static final String PRODUCT = "Product";
        public static final String STIMUALTOR = "Stimulator";
        public static final String MODIFIER = "Modifier";
        public static final String INHIBITOR = "Inhibitor";
        public static final String ENYZYME = "Enzyme";
        public static final String PARAMETER = "Parameter";

        private String name;
        private String value;
        private String description;
        private String type;
        private int stochiometry;

        public VariableDescription(String name, String value, String description, String type, String stochiometry)
        {
            this.name = name;
            this.value = value;
            this.description = description;
            this.type = type;
            this.stochiometry = Integer.parseInt(stochiometry);
        }

        public VariableDescription(String name, String description, String type)
        {
            this.name = name;
            this.value = name;
            this.description = description;
            this.type = type;
        }

        public String getDescription()
        {
            return description;
        }
        public void setDescription(String description)
        {
            this.description = description;
        }
        public String getName()
        {
            return name;
        }
        public void setName(String name)
        {
            this.name = name;
        }
        public String getValue()
        {
            return value;
        }
        public void setValue(String value)
        {
            this.value = value;
        }
    }

}
