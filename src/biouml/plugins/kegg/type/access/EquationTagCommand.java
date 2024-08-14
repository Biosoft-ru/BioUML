package biouml.plugins.kegg.type.access;

import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.support.TagCommand;
import biouml.model.Module;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;

public class EquationTagCommand implements TagCommand
{
    public static final int REACTANT = 1;
    public static final int COEFF = 2;
    public static final int PLUS = 3;
    public static final int EQ = 4;

    protected static final Logger log = Logger.getLogger(EquationTagCommand.class.getName());
    private boolean isReactant;
    private int coeff;
    private String tag;
    private ReactionTransformer transformer;

    public EquationTagCommand(String tag, ReactionTransformer transformer)
    {
        this.tag = tag;
        this.transformer = transformer;
    }

    @Override
    public void start(String tag)
    {
        isReactant = true;
        coeff = 1;
    }

    @Override
    public void addValue(String value)
    {
        StringTokenizer st = new StringTokenizer(value, " ");
        while( st.hasMoreTokens() )
        {
            String token = st.nextToken();
            switch( tokenType(token) )
            {
                case REACTANT:
                    parseReactant(token);
                    break;
                case COEFF:
                    parseCoeff(token);
                    break;
                case PLUS:
                    parsePlus();
                    break;
                case EQ:
                    parseEquality();
                    break;
                default:
                    log.log(Level.SEVERE, "Error undefined token type.");
            }
        }
    }

    private int tokenType(String token)
    {
        if( isInt(token) )
            return COEFF;
        else if( token.equals("+") )
            return PLUS;
        else if( token.equals("<=>") )
            return EQ;
        else
            return REACTANT;
    }

    private boolean isInt(String token)
    {
        boolean isInt = false;
        try
        {
            if( Integer.parseInt(token) > 0 )
                isInt = true;
        }
        catch( NumberFormatException ex )
        {
            isInt = false;
        }
        return isInt;
    }

    private void parseReactant(String token)
    {
        Reaction reaction = transformer.getProcessedObject();
        String role = null;
        if( isReactant )
        {
            role = SpecieReference.REACTANT;
        }
        else
        {
            role = SpecieReference.PRODUCT;
        }
        SpecieReference reference = new SpecieReference(reaction, token + " as " + role);
        reference.setSpecie(Module.DATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + "compound" + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName(token));
        reference.setRole(role);

        if( coeff > 1 )
        {
            reference.setStoichiometry("" + coeff);
            coeff = 1;
        }
        try
        {
            reaction.put(reference);
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "cannot add reactant to reaction: " + t.getMessage());
        }
    }

    private void parseCoeff(String token)
    {
        try
        {
            coeff = Integer.parseInt(token);
            if( coeff < 1 )
                log.log(Level.SEVERE, "wrong stoichiometry coefficient.");
        }
        catch( NumberFormatException ex )
        {
            log.log(Level.SEVERE, "error parsing stoichiometry coefficient");
        }
    }

    private void parsePlus()
    {
        //...
        //System.err.println("plus");
    }

    private void parseEquality()
    {
        if( !isReactant )
            log.log(Level.SEVERE, "duplicate equality sign!");

        isReactant = false;
        //System.err.println("equality");
    }

    @Override
    public void complete(String tag)
    {
        Reaction reaction = transformer.getProcessedObject();
        //At this time reactions in KEGG are always reversible.
        reaction.setReversible(true);
    }

    @Override
    public String getTag()
    {
        return tag;
    }

    @Override
    public String getTaggedValue()
    {
        StringBuffer reactants = new StringBuffer();
        StringBuffer products = new StringBuffer();
        Reaction reaction = transformer.getProcessedObject();
        SpecieReference[] refs = reaction.getSpecieReferences();
        for( SpecieReference ref : refs )
        {
            if( ref.getRole().equals(SpecieReference.REACTANT) )
            {
                appendSpecieReference(reactants, ref);
            }
            else if( ref.getRole().equals(SpecieReference.PRODUCT) )
            {
                appendSpecieReference(products, ref);
            }
        }
        return tag + "\t" + reactants.toString() + " <=> " + products.toString();
    }

    private void appendSpecieReference(StringBuffer sb, SpecieReference ref)
    {
        if( sb.length() > 0 )
            sb.append(" + ");

        int coeff = 1;
        try
        {
            coeff = Integer.parseInt(ref.getStoichiometry());
        }
        catch( Exception t )
        {
            log
                    .log( Level.SEVERE, "Stoichiometry coefficient should be integer, specie= " + ref.getName() + ", stoichiometry="
                            + ref.getStoichiometry());
        }

        if( coeff > 1 )
            sb.append(coeff).append(" ");

        sb.append(ref.getName());
    }

    @Override
    public String getTaggedValue(String value)
    {
        throw new UnsupportedOperationException();
    }
}
