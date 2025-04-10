package biouml.plugins.bionetgen.diagram;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import javax.annotation.Nonnull;
import one.util.streamex.StreamEx;

import ru.biosoft.util.TextUtil2;
import biouml.plugins.bionetgen.bnglparser.BNGList;
import biouml.plugins.bionetgen.bnglparser.BNGMoleculeType;
import biouml.plugins.bionetgen.bnglparser.BNGSpecies;
import biouml.plugins.bionetgen.bnglparser.Node;

public class BionetgenMoleculeType
{
    private String name = "";
    private final Set<String> possibleStates = new TreeSet<>();
    private final List<MoleculeTypeComponent> components = new ArrayList<>();

    public BionetgenMoleculeType(BNGSpecies species)
    {
        String speciesStr = species.getName();
        if( speciesStr.contains(".") )
            throw new IllegalArgumentException("Invalid molecule type format. It mustn't be a species, only a single molecule: "
                    + speciesStr);
        if( speciesStr.contains("(") )
        {
            String[] parts = TextUtil2.split(speciesStr, '(');
            String nameAndState = parts[0];
            parts = TextUtil2.split(parts[1], ')');
            setNameAndState(nameAndState + parts[1]);
            generateComponents(parts[0]);
        }
        else
            setNameAndState(speciesStr);
    }
    private void setNameAndState(String str)
    {
        if( str.isEmpty() )
            return;
        String delimiters = "%!~";
        boolean isName = true;
        boolean addState = false;
        StringTokenizer tokens = new StringTokenizer(str, delimiters, true);
        while( tokens.hasMoreTokens() )
        {
            String token = tokens.nextToken();
            if( isName )
            {
                name = token;
                isName = false;
            }
            else if( addState )
            {
                possibleStates.add(token);
                addState = false;
            }
            else if( token.equals("~") )
                addState = true;
        }
    }
    private void generateComponents(String str)
    {
        if( str.isEmpty() )
            return;
        String[] components = TextUtil2.split(str, ',');
        try
        {
            for( String component : components )
                this.components.add(new MoleculeTypeComponent(component));
        }
        catch( IllegalArgumentException e )
        {
            throw new IllegalArgumentException(e.getMessage() + str, e);
        }
    }

    public boolean check(BionetgenMolecule molecule, boolean isTemplate)
    {
        if( !name.equals(molecule.getName()) )
            return false;

        String state = molecule.getState();
        if( state.isEmpty() )
        {
            if( possibleStates.size() != 0 && !isTemplate )
                return false;
        }
        else
        {
            if( !possibleStates.contains(state) )
                return false;
        }
        if( components.size() < molecule.getMoleculeComponents().size() )
            return false;
        else if( !isTemplate && components.size() > molecule.getMoleculeComponents().size() )
            return false;
        List<MoleculeTypeComponent> usedComponents = new ArrayList<>();
        for( MoleculeComponent mc : molecule.getMoleculeComponents() )
        {
            boolean found = false;
            for( MoleculeTypeComponent component : components )
            {
                if( usedComponents.contains(component) )
                    continue;
                if( component.check(mc, isTemplate) )
                {
                    found = true;
                    usedComponents.add(component);
                    break;
                }
            }
            if( !found )
                return false;
        }
        return true;
    }

    public static boolean checkAllowability(@Nonnull List<BionetgenMoleculeType> typeList, BionetgenMolecule molecule, boolean isTemplate)
    {
        return StreamEx.of( typeList ).anyMatch( t -> t.check( molecule, isTemplate ) );
    }

    public static @Nonnull List<BionetgenMoleculeType> createMoleculeTypesList(@Nonnull BNGList molTypesList)
    {
        List<BionetgenMoleculeType> result = new ArrayList<>();
        for( int i = 0; i < molTypesList.jjtGetNumChildren(); i++ )
        {
            Node listChild = molTypesList.jjtGetChild(i);
            if( listChild instanceof BNGMoleculeType )
            {
                for( int j = 0; j < listChild.jjtGetNumChildren(); j++ )
                {
                    if( listChild.jjtGetChild(j) instanceof BNGSpecies )
                        result.add(new BionetgenMoleculeType((BNGSpecies)listChild.jjtGetChild(j)));
                }
            }
        }
        return result;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(name);
        for( String state : possibleStates )
            sb.append("~").append(state);
        int size = components.size();
        if( size != 0 )
            sb.append("(");
        for( int i = 0; i < size; i++ )
        {
            if( i != 0 )
                sb.append(",");
            sb.append(components.get(i).toString());
        }
        if( size != 0 )
            sb.append(")");
        return sb.toString();
    }

    static class MoleculeTypeComponent
    {
        String name = "";
        Set<String> possibleStates = new TreeSet<>();

        public MoleculeTypeComponent(String fullString)
        {
            if( fullString.isEmpty() )
                throw new IllegalArgumentException("Invalid molecule type format. It mustn't have empty components: ");
            String delimiters = "%!~";
            boolean isName = true;
            boolean addState = false;
            StringTokenizer tokens = new StringTokenizer(fullString, delimiters, true);
            while( tokens.hasMoreTokens() )
            {
                String token = tokens.nextToken();
                if( isName )
                {
                    name = token;
                    isName = false;
                }
                else if( addState )
                {
                    possibleStates.add(token);
                    addState = false;
                }
                else if( token.equals("~") )
                    addState = true;
            }
        }

        public boolean check(MoleculeComponent mc, boolean isTemplate)
        {
            if( !name.equals(mc.getName()) )
                return false;

            String state = mc.getState();
            if( state.isEmpty() )
            {
                if( possibleStates.size() != 0 && !isTemplate )
                    return false;
            }
            else
            {
                if( !possibleStates.contains(state) )
                    return false;
            }
            return true;
        }

        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder(name);
            for( String state : possibleStates )
                sb.append("~").append(state);
            return sb.toString();
        }
    }

}
