package biouml.plugins.bionetgen.diagram;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.util.List;

import javax.annotation.CheckForNull;

import ru.biosoft.util.TextUtil;

public class BionetgenReactionRecord
{
    private String name = "";
    private final TIntList reactantsIndexes;
    private final TIntList productsIndexes;
    private final String forwardRate;
    private final String rateLawType;
    private boolean needMultipliers = false;
    private double multiplier = 1.0;

    public BionetgenReactionRecord(String templateName, TIntList reactIndexes, TIntList prodIndexes, String rate, String rateType)
    {
        name = templateName;
        reactantsIndexes = reactIndexes == null ? new TIntArrayList() : reactIndexes;
        productsIndexes = prodIndexes == null ? new TIntArrayList() : prodIndexes;
        forwardRate = rate;
        rateLawType = rateType == null ? BionetgenConstants.DEFAULT : rateType;
        if( BionetgenConstants.DEFAULT.equals(rateLawType) )
        {
            needMultipliers = true;
        }
        else if( forwardRate == null || forwardRate.isEmpty() )
        {
            throw new IllegalArgumentException("Empty rate law.");
        }
        if( reactantsIndexes.isEmpty() && productsIndexes.isEmpty() )
            throw new IllegalArgumentException("Trying to create empty reaction record.");

        int reactantsNumber = reactantsIndexes.size();
        if( needMultipliers )
        {
            TIntList ignoreIndexes = new TIntArrayList();
            for( int i = 0; i < reactantsNumber - 1; i++ )
            {
                int anotherMultiplier = 1;
                if( ignoreIndexes.contains(reactantsIndexes.get(i)) )
                    continue;
                for( int j = i + 1; j < reactantsNumber; j++ )
                {
                    if( reactantsIndexes.get(i) == reactantsIndexes.get(j) )
                    {
                        anotherMultiplier *= ( anotherMultiplier + 1 );
                        ignoreIndexes.add(reactantsIndexes.get(j));
                    }
                }
                multiplier /= anotherMultiplier;
            }
        }
        else if( isMMType() && ( reactantsNumber != 2 || reactantsIndexes.get(0) == reactantsIndexes.get(1) ) )
        {
            throw new IllegalArgumentException("Wrong number of reactants: " + reactantsNumber + ". Should be 2.");
        }
        else if( isSaturationType() && reactantsNumber > 2 )
        {
            throw new IllegalArgumentException("Wrong number of reactants: " + reactantsNumber + ". Should be 2 or less.");
        }
    }

    public static @CheckForNull TIntList getIndexes(List<BionetgenSpeciesGraph> targets, List<BionetgenSpeciesGraph> allItems)
    {
        TIntList result = new TIntArrayList();
        for( BionetgenSpeciesGraph target : targets )
        {
            int index = allItems.indexOf(target);
            if( index == -1 )
                return null;
            result.add(index);
        }
        return result;
    }

    public void addMultiplier(double someMoreMultiplier)
    {
        if( someMoreMultiplier != 1.0 )
        {
            multiplier *= someMoreMultiplier;
            TIntSet ignoreIndexes = new TIntHashSet();
            int size = productsIndexes.size();
            for( int i = 0; i < size - 1; i++ )
            {
                int anotherMultiplier = 1;
                if( ignoreIndexes.contains(productsIndexes.get(i)) )
                    continue;
                for( int j = i + 1; j < size; j++ )
                {
                    if( productsIndexes.get(i) == productsIndexes.get(j) )
                    {
                        anotherMultiplier *= ( anotherMultiplier + 1 );
                        ignoreIndexes.add(productsIndexes.get(j));
                    }
                }
                multiplier /= anotherMultiplier;
            }
        }
    }

    public String getName()
    {
        return name;
    }

    public boolean needMultipliers()
    {
        return needMultipliers;
    }

    public TIntList getReactantsIndexes()
    {
        return reactantsIndexes;
    }

    public TIntList getProductsIndexes()
    {
        return productsIndexes;
    }

    public String getForwardRate()
    {
        if( multiplier == 1 )
            return forwardRate;
        return String.valueOf(multiplier) + "*" + forwardRate;
    }

    public boolean isDefaultType()
    {
        return needMultipliers;
    }

    public boolean isMMType()
    {
        return BionetgenConstants.MM.equals(rateLawType);
    }

    public boolean isSaturationType()
    {
        return BionetgenConstants.SATURATION.equals(rateLawType);
    }

    public String generateFormula()
    {
        String formula = "";
        if( isDefaultType() )
        {
            formula = generateDefaultFormula();
        }
        else if( isMMType() )
        {
            formula = generateMMFormula();
        }
        else if( isSaturationType() )
        {
            formula = generateSaturationFormula();
        }
        return formula;
    }

    private String generateDefaultFormula()
    {
        StringBuilder result = new StringBuilder(getForwardRate());
        for( TIntIterator iterator = reactantsIndexes.iterator(); iterator.hasNext(); )
        {
            result.append("*$").append(BionetgenDiagramDeployer.SPECIES_NAME_FORMAT + iterator.next());
        }
        return result.toString();
    }

    private String generateMMFormula()
    {
        String[] constants = prepareConstants(forwardRate);
        String kcat = constants[0];
        String Km = constants[1];
        StringBuilder rateLaw = new StringBuilder("MM(");
        String name0 = "$" + BionetgenDiagramDeployer.SPECIES_NAME_FORMAT + reactantsIndexes.get(0);
        String name1 = "$" + BionetgenDiagramDeployer.SPECIES_NAME_FORMAT + reactantsIndexes.get(1);
        rateLaw.append(name0).append(",").append(name1).append(",");
        rateLaw.append(kcat).append(",").append(Km).append(")");
        return rateLaw.toString();
    }

    private String generateSaturationFormula()
    {
        String[] constants = prepareConstants(forwardRate);
        String kcat = constants[0];
        String Km = constants[1];
        StringBuilder rateLaw = new StringBuilder("Sat");
        String name0 = "$" + BionetgenDiagramDeployer.SPECIES_NAME_FORMAT + reactantsIndexes.get(0);
        if( reactantsIndexes.size() == 2 )
        {
            rateLaw.append("_2(").append(name0).append(",");
            String name1 = "$" + BionetgenDiagramDeployer.SPECIES_NAME_FORMAT + reactantsIndexes.get(1);
            rateLaw.append(name1).append(",");
        }
        else
        {
            rateLaw.append("_1(").append(name0).append(",");
        }
        rateLaw.append(kcat).append(",").append(Km).append(")");
        return rateLaw.toString();
    }

    private String[] prepareConstants(String rateLaw)
    {
        String[] result = new String[2];
        String consts = rateLaw.substring(rateLaw.indexOf('(') + 1, rateLaw.lastIndexOf(')'));
        String[] parts = TextUtil.split(consts, ',');
        result[0] = parts[0];
        result[1] = parts[1];
        return result;
    }
}
