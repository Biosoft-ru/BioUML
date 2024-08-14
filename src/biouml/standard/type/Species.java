package biouml.standard.type;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringEscapeUtils;

import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.plugins.jsconsistent.JavaScriptConsistent;

/**
 * General definition for 'Species' concept.
 *
 * Generally, species Latin name is its identifier as {@link ru.biosoft.access.core.DataElement},
 * thus methods {@link getName()} and {@link getLatinName()} should returns the same value.
 *
 * @pending ru.biosoft.access.core.DataElement.origin should refer to species classification.
 */
@ClassIcon( "resources/species.gif" )
public class Species extends BaseSupport implements JavaScriptConsistent
{
    private static final long serialVersionUID = 1L;
    public static final DataElementPath SPECIES_PATH = DataElementPath.create("databases/Utils/Species");
    public static final String ANY_SPECIES = "Any species";

    public Species(DataCollection<?> parent, String name)
    {
        super(parent, name);
    }

    ////////////////////////////////////////////////////////////////////////////
    // Properties
    //

    /** Returns species Latin name, for example 'Mus musculus'. */
    public String getLatinName()
    {
        return getName();
    }

    private String commonName;
    /** Returns species common name (generally English), for example 'mouse'. */
    public String getCommonName()
    {
        return commonName;
    }
    public void setCommonName(String commonName)
    {
        String oldValue = commonName;
        this.commonName = commonName;
        firePropertyChange("commonName", oldValue, commonName);
    }

    private String abbreviation;
    /**
     * Returns species abbreviation used in gene and protein identifiers.
     * Generally it is two letter species abbreviations, for example 'Mm' for 'Mus musculus'.
     */
    public String getAbbreviation()
    {
        return abbreviation;
    }
    public void setAbbreviation(String abbreviation)
    {
        String oldValue = abbreviation;
        this.abbreviation = abbreviation;
        firePropertyChange("abbreviation", oldValue, abbreviation);
    }

    private String description;
    /**
     * Returns species description.
     * Generally it is some short comment.
     */
    public String getDescription()
    {
        return description;
    }
    public void setDescription(String description)
    {
        String oldValue = description;
        this.description = description;
        firePropertyChange("description", oldValue, description);
    }

    @Override
    public String toString()
    {
        return getCommonName() + " (" + getName() + ")";
    }

    public static Species getDefaultSpecies(DataCollection<?> collection)
    {
        if( collection != null )
        {
            String speciesStr = collection.getInfo().getProperty(DataCollectionUtils.SPECIES_PROPERTY);
            if( speciesStr != null )
            {
                Species species = getSpecies(speciesStr);
                if( species != null )
                    return species;
            }
        }
        return getSpecies("Homo sapiens");
    }

    public static StreamEx<Species> allSpecies()
    {
        return StreamEx.of( SPECIES_PATH.getDataCollection( Species.class ).stream() );
    }

    private static Map<String, Species> speciesCache = new ConcurrentHashMap<>();

    public static Species getSpecies(String latinName)
    {
        Species result = speciesCache.get(latinName);
        if(result != null) return result;
        Species de = SPECIES_PATH.getChildPath(latinName).optDataElement(Species.class);
        if(de == null) return null;
        synchronized( speciesCache )
        {
            speciesCache.put(latinName, de);
        }
        return de;
    }

    /**
     * Instantiate species by TextUtil.fromString
     */
    public static Species createInstance(String data)
    {
        //try to get Species directly from path
        Species speciesElement = DataElementPath.create(data).optDataElement(Species.class);
        if(speciesElement != null)
            return speciesElement;
        for(Species species: SPECIES_PATH.getDataCollection(Species.class))
        {
            if( species.getLatinName().equals(data) || species.toString().equals(data) )
                return species;
        }
        return null;
    }

    /**
     * @return string like "human, mouse or rat"
     */
    public static String formatSpeciesNames()
    {
        if(!SPECIES_PATH.exists())
            return "";
        StringBuilder sb = new StringBuilder();
        List<String> allSpecies = allSpecies().map( s -> s.getCommonName().toLowerCase( Locale.ENGLISH ) ).toList();
        for(int i=0; i<allSpecies.size(); i++)
        {
            sb.append(allSpecies.get(i));
            if(i<allSpecies.size()-2) sb.append(", ");
            else if(i == allSpecies.size()-2) sb.append(" or ");
        }
        return sb.toString();
    }

    private static class SpeciesProxy implements Serializable
    {
        private static final long serialVersionUID = 1L;
        private final String latinName;

        public SpeciesProxy(String latinName)
        {
            this.latinName = latinName;
        }

        private Object readResolve()
        {
            return getSpecies(latinName);
        }
    }

    private Object writeReplace()
    {
        return new SpeciesProxy( getLatinName() );
    }

    private void readObject(ObjectInputStream s) throws InvalidObjectException
    {
        throw new InvalidObjectException( "Proxy required" );
    }

    @Override
    public String toJaveScriptString()
    {
        return "'" + StringEscapeUtils.escapeJava( getName() ) + "'";
    }
}
