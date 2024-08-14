package biouml.plugins.bionetgen.diagram;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import one.util.streamex.StreamEx;

public class BionetgenToGlycanConverter
{
    private enum GlycanMoleculeName
    {
        A, AN, F, G, GN, M, NN
    }

    public static String convert(String graphString)
    {
        return convert(new BionetgenSpeciesGraph(graphString));
    }

    private static String convert(BionetgenSpeciesGraph bsg)
    {
        BionetgenMolecule startMol = null;
        boolean found = false;
        for( BionetgenMolecule mol : bsg.getMoleculesList() )
        {
            if( !isGlycanMol(mol.getName()) )
                throw new IllegalArgumentException("Incorrect BNGL representatopn of glycan");
            if( !found && "GN".equals(mol.getName()) && mol.getMoleculeComponents().toString().contains("r~1") )
            {
                startMol = mol;
                found = true;
            }
        }
        if( !found )
            throw new IllegalArgumentException("Incorrect BNGL representatopn of glycan");
        return generateGlycanString(startMol, bsg.getAdjacency(), new HashSet<MoleculeComponent>());
    }

    private static String generateGlycanString(BionetgenMolecule startMol,
            Map<MoleculeComponent, Set<MoleculeComponent>> adjacency, Set<MoleculeComponent> ignored)
    {
        List<MoleculeComponent> components = StreamEx.of( startMol.getMoleculeComponents() ).sorted( (o1, o2) -> {
            try
            {
                String name1 = o1.getName();
                String name2 = o2.getName();
                int rn1 = Integer.parseInt( name1.substring( 1 ) );
                int rn2 = Integer.parseInt( name2.substring( 1 ) );
                return rn1 > rn2 ? 1 : rn1 < rn2 ? -1 : name1.compareTo( name2 );
            }
            catch( NumberFormatException e )
            {
                return 0;
            }
        } ).toList();
        StringBuilder sb = new StringBuilder();
        Set<MoleculeComponent> bindedMCSet;
        //counter is common for all molecule components of startMol
        int counter = 0;
        for( MoleculeComponent mc : components )
        {
            if( ignored.contains(mc) )
                continue;
            ignored.add(mc);
            bindedMCSet = adjacency.get(mc);
            if( bindedMCSet == null )
                continue;
            for( MoleculeComponent keyMC : bindedMCSet )
            {
                if( ignored.contains(keyMC) )
                    continue;
                if( counter != 0 )
                    sb.append("(");
                sb.append(generateGlycanString(keyMC.getMolecule(), adjacency, ignored));
                sb.append(mc.getName());
                if( counter != 0 )
                    sb.append(")");
                ignored.add(keyMC);
                ++counter;
            }
        }
        sb.append(startMol.getName());
        return sb.toString();
    }

    public static boolean isGlycanMol(String strMol)
    {
        try
        {
            GlycanMoleculeName.valueOf(strMol);
            return true;
        }
        catch( IllegalArgumentException e )
        {
            return false;
        }
    }

}
