package biouml.plugins.reactome.biohub;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.sql.SqlUtil;
import biouml.plugins.enrichment.FunctionalHubConstants;
import biouml.plugins.enrichment.SqlCachedFunctionalHubSupport;
import biouml.plugins.reactome.ReactomePathwayTableType;
import biouml.plugins.reactome.ReactomeProteinTableType;

/**
 * @author lan
 *
 */
public class ReactomeFunctionalHub extends SqlCachedFunctionalHubSupport
{
    /**
     * @param properties
     */
    public ReactomeFunctionalHub(Properties properties)
    {
        super(properties);
    }

    @Override
    protected List<Group> getGroups() throws Exception
    {
        Map<String, Group> groups = new HashMap<>();
        Connection conn = getConnection();
        try( Statement st = conn.createStatement();
                ResultSet resultSet = st.executeQuery(
                        "SELECT do.DB_ID,_displayName,identifier FROM DatabaseObject do right join StableIdentifier si on stableIdentifier=si.DB_ID WHERE _class='Pathway'" );
                PreparedStatement psHierarchy = conn.prepareStatement(
                        "SELECT e.hasEvent,e.hasEvent_class,do._displayName FROM Pathway_2_hasEvent e LEFT JOIN DatabaseObject do ON e.hasEvent=do.DB_ID WHERE e.DB_ID=?" );
                PreparedStatement psReact = conn.prepareStatement(
                        "SELECT DISTINCT identifier, _displayName,_class FROM ReactionlikeEvent_2_input re2t LEFT JOIN DatabaseObject dbo ON(dbo.DB_ID=re2t.input) "
                                + "JOIN StableIdentifier si ON(dbo.stableIdentifier = si.DB_ID) WHERE re2t.DB_ID=? "
                                + "UNION SELECT DISTINCT identifier, _displayName,_class FROM ReactionlikeEvent_2_output re2t LEFT JOIN DatabaseObject dbo ON(dbo.DB_ID=re2t.output) "
                                + "JOIN StableIdentifier si ON(dbo.stableIdentifier = si.DB_ID) WHERE re2t.DB_ID=?" ))
        {
            while( resultSet.next() )
            {
                String acc = resultSet.getString( 1 );
                String title = resultSet.getString( 3 ) + ": " + resultSet.getString( 2 );
                psHierarchy.setString( 1, acc );
                Set<String> links = new HashSet<>();
                try (ResultSet rsHierarchy = psHierarchy.executeQuery())
                {
                    while( rsHierarchy.next() )
                    {
                        if( rsHierarchy.getString( 2 ).equals( "Pathway" ) )
                        {
                            links.add( rsHierarchy.getString( 1 ) );
                        }
                        else
                        {
                            psReact.setString( 1, rsHierarchy.getString( 1 ) );
                            psReact.setString( 2, rsHierarchy.getString( 1 ) );
                            try (ResultSet rsReact = psReact.executeQuery())
                            {
                                while( rsReact.next() )
                                    links.add( rsReact.getString( 1 ) );
                            }
                        }
                    }
                }
                groups.put( acc, new Group( acc, title, links ) );
            }
        }

        Map<String, Group> readyGroups = new HashMap<>();
        while( !groups.isEmpty() )
        {
            for( Group group : groups.values().toArray(new Group[groups.size()]) )
            {
                boolean ready = true;
                Set<String> elements = group.getElements();
                for( String element : elements.toArray(new String[elements.size()]) )
                {
                    if( !element.startsWith( "REACT" ) && !element.startsWith( "R-HSA" ) && !element.startsWith( "R-MMU" )
                            && !element.startsWith( "R-RNO" ) && !element.startsWith( "R-ALL" ) )
                    {
                        Group child = readyGroups.get(element);
                        if( child != null )
                        {
                            elements.remove(element);
                            elements.addAll(child.getElements());
                        }
                        else if( !groups.containsKey(element) )
                        {
                            elements.remove(element);
                        }
                        else
                        {
                            ready = false;
                        }
                    }
                }
                if( ready )
                {
                    readyGroups.put(group.getAccession(), group);
                    groups.remove(group.getAccession());
                }
            }
        }
        List<Group> pathwayGroups = new ArrayList<>();
        for( Entry<String, String> entry : SqlUtil.queryMap(conn,
                "select p2r.db_id,p2r.representedPathway from PathwayDiagram_2_representedPathway p2r where representedPathway_rank=0").entrySet() )
        {
            Group group = readyGroups.get(entry.getValue());
            if(group != null)
                pathwayGroups.add(new Group(entry.getKey(), group.getTitle(), group.getElements()));
        }
        return pathwayGroups;
    }

    @Override
    protected ReferenceType getInputReferenceType()
    {
        return ReferenceTypeRegistry.getReferenceType(ReactomeProteinTableType.class);
    }

    @Override
    protected String getTableName()
    {
        return "BioUML_pathway2ensembl";
    }

    private static final Properties SUPPORTED_MATCHING = new Properties()
    {
        {
            setProperty(TYPE_PROPERTY, ReferenceTypeRegistry.getReferenceType(ReactomePathwayTableType.class).toString());
        }
    };
    @Override
    public Properties[] getSupportedMatching(Properties input)
    {
        if( input.containsKey(FunctionalHubConstants.FUNCTIONAL_CLASSIFICATION_RECORD) )
            return new Properties[] {SUPPORTED_MATCHING};
        return null;
    }
}
