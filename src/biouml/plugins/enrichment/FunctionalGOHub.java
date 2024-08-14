package biouml.plugins.enrichment;

import java.beans.PropertyDescriptor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.biohub.BioHub;
import ru.biosoft.access.biohub.Element;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.exception.BiosoftSQLException;
import ru.biosoft.access.sql.Query;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.util.bean.StaticDescriptor;
import biouml.plugins.ensembl.tabletype.GeneSymbolTableType;
import biouml.plugins.go.GOHub;
import biouml.plugins.go.GOTableType;
import biouml.standard.type.Species;

import com.developmentontheedge.beans.DynamicProperty;

/**
 * @author lan
 *
 */
public class FunctionalGOHub extends SqlCachedFunctionalHubSupport
{
    public static final String TYPE_PROPERTY = "type";
    private String subType = "all";

    public FunctionalGOHub(Properties properties)
    {
        super(properties);
        if(properties.containsKey(TYPE_PROPERTY))
            subType = properties.getProperty(TYPE_PROPERTY);
        if( !properties.containsKey( DataCollectionConfigConstants.COMPLETE_NAME_PROPERTY ) )
            properties.setProperty( DataCollectionConfigConstants.COMPLETE_NAME_PROPERTY, GOHub.GO_PATH.toString() );
        if(!properties.containsKey(MODULE_NAME_PROPERTY))
            properties.setProperty(MODULE_NAME_PROPERTY, GOHub.GO_PATH.toString());
    }

    private List<String> getSpeciesList()
    {
        List<String> species = new ArrayList<>();
        try( PreparedStatement ps = getConnection().prepareStatement( "SELECT common_name FROM species WHERE genus=? AND species=?" ) )
        {
            for(Species s: Species.allSpecies())
            {
                try
                {
                    String latinName = s.getName();
                    int spacePos = latinName.indexOf(' ');
                    String genusName = latinName.substring(0, spacePos);
                    String speciesName = latinName.substring(spacePos+1);
                    ps.setString(1, genusName);
                    ps.setString(2, speciesName);
                    try( ResultSet rs = ps.executeQuery() )
                    {
                        if( rs.next() )
                            species.add( rs.getString( 1 ) );
                    }
                }
                catch( Exception e )
                {
                    log.log(Level.SEVERE, "While getting common name for species "+s+":", e);
                }
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Unable to get species list:", e);
            species.add("human");
        }
        return species;
    }

    @Override
    protected Iterable<Group> getGroups() throws Exception
    {
        String speciesList = "'"+String.join("','", getSpeciesList())+"'";
        String query = "select distinct g.symbol,t.acc,t.name from gene_product g,species s,term t,association a "
                + "where s.id=g.species_id and s.common_name in(" + speciesList + ") and a.term_id=t.id and gene_product_id=g.id"
                + ( subType.equals("all") ? "" : " and term_type='" + subType + "'" );
        Connection conn = getConnection();
        Map<String, Group> result = new HashMap<>();
        SqlUtil.iterate( conn, query, rs -> {
            String acc = rs.getString(2);
            String title = rs.getString( 3 );
            result.computeIfAbsent( acc, k -> new Group(acc, title) ).getElements().add( rs.getString( 1 ) );
        });
        List<String> startKeys = new ArrayList<>(result.keySet());
        String parentsQuery = "select t1.acc,t1.name from term t1,term t2,is_a_flat i where t1.id=i.term1_id and t2.id=i.term2_id and t2.acc=?";
        try(PreparedStatement ps = conn.prepareStatement(parentsQuery))
        {
            for(String childAcc: startKeys)
            {
                ps.setString(1, childAcc);
                Group childGroup = result.get(childAcc);
                try(ResultSet rs = ps.executeQuery())
                {
                    while(rs.next())
                    {
                        String parentAcc = rs.getString(1);
                        Group parentGroup = result.get(parentAcc);
                        if(parentGroup == null)
                        {
                            parentGroup = new Group(parentAcc, rs.getString(2));
                            result.put(parentAcc, parentGroup);
                        }
                        parentGroup.getElements().addAll(childGroup.getElements());
                    }
                }
            }
        }
        result.remove("all");
        return result.values();
    }

    protected static final Query ANNOTATION_QUERY = new Query("select level,term_type from term_stats s,term t where s.term_id=t.id and t.acc=$acc$");
    static final DynamicProperty[] dynLevels = new DynamicProperty[30];
    static final DynamicProperty[] dynTypes;

    private enum GOType
    {
        biological_process, cellular_component, molecular_function
    }

    static
    {
        int i=0;
        dynTypes = new DynamicProperty[GOType.values().length];
        PropertyDescriptor pd = StaticDescriptor.create("Ontology");
        for( GOType value : GOType.values() )
        {
            dynTypes[i++] = new DynamicProperty(pd, String.class, value.toString().replace("_", " "));
        }

        pd = StaticDescriptor.create("Level");
        for( i = 0; i < dynLevels.length; i++ )
        {
            dynLevels[i] = new DynamicProperty(pd, Integer.class, i);
        }
    }

    private static class GOInfo
    {
        int level;
        GOType type;

        public GOInfo(int level, GOType type)
        {
            super();
            this.level = level;
            this.type = type;
        }

        public DynamicProperty getLevel()
        {
            return dynLevels[level];
        }

        public DynamicProperty getType()
        {
            return dynTypes[type.ordinal()];
        }
    }

    private static final ConcurrentHashMap<String, GOInfo> elementInfo = new ConcurrentHashMap<>();

    @Override
    protected void annotateElement(Element element)
    {
        GOInfo info = elementInfo.get(element.getAccession());
        if(info == null)
        {
            try
            {
                Object[] row = SqlUtil.queryRow( getConnection(), ANNOTATION_QUERY.str( element.getAccession() ), int.class, String.class );
                if(row != null)
                {
                    info = new GOInfo((int)row[0], GOType.valueOf((String)row[1]));
                    elementInfo.put(element.getAccession(), info);
                }
            }
            catch( BiosoftSQLException e )
            {
            }
        }
        if(info != null)
        {
            element.setValue(info.getLevel());
            if(subType.equals("all"))
                element.setValue(info.getType());
        }
    }

    @Override
    protected ReferenceType getInputReferenceType()
    {
        return ReferenceTypeRegistry.getReferenceType(GeneSymbolTableType.class);
    }

    @Override
    protected String getTableName()
    {
        return "BioUML_groups_"+subType;
    }

    private static final Properties SUPPORTED_MATCHING = new Properties();

    static
    {
        SUPPORTED_MATCHING.setProperty(BioHub.TYPE_PROPERTY, ReferenceTypeRegistry.getReferenceType(GOTableType.class).toString());
    }
    @Override
    public Properties[] getSupportedMatching(Properties input)
    {
        if(input.containsKey(FunctionalHubConstants.FUNCTIONAL_CLASSIFICATION_RECORD))
            return new Properties[] {SUPPORTED_MATCHING};
        return null;
    }
}
