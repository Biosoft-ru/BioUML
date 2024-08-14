package biouml.plugins.ensembl.homology;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;

import org.apache.commons.lang.ArrayUtils;

import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.biohub.BioHubSupport;
import ru.biosoft.access.biohub.Element;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.biohub.TargetOptions;
import ru.biosoft.access.exception.BiosoftSQLException;
import ru.biosoft.access.sql.Query;
import ru.biosoft.access.sql.SqlConnectionPool;
import ru.biosoft.access.sql.SqlUtil;
import biouml.plugins.ensembl.tabletype.EnsemblGeneTableType;
import biouml.plugins.ensembl.tabletype.EnsemblProteinTableType;
import biouml.standard.type.Species;

import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControl;

/**
 * Somewhat specific hub which does cross-species matching
 * @author lan
 */
public class HomologyHub extends BioHubSupport
{
    protected ThreadLocal<Connection> connection = new ThreadLocal<>();
    protected static final Query SPECIES_QUERY = new Query( "SELECT species_id FROM homology_species WHERE species_name = $species$" );
    protected static final String TREE_QUERY = "SELECT tree FROM homology_trees WHERE group_id=";

    public HomologyHub(Properties properties)
    {
        super(properties);
    }

    Connection getConnection() throws BiosoftSQLException
    {
        return SqlConnectionPool.getPersistentConnection(properties);
    }

    @Override
    public int getPriority(TargetOptions dbOptions)
    {
        return 0;
    }

    @Override
    public Element[] getReference(Element startElement, TargetOptions dbOptions, String[] relationTypes, int maxLength, int direction)
    {
        return null;
    }

    @Override
    public Map<String, String[]> getReferences(String[] inputList, Properties input, Properties output, FunctionJobControl jobControl)
    {
        try
        {
            String inputSpecies = input.getProperty( SPECIES_PROPERTY ).toLowerCase().replace( ' ', '_' );
            String outputSpecies = output.getProperty( SPECIES_PROPERTY ).toLowerCase().replace( ' ', '_' );
            ReferenceType inputType = ReferenceTypeRegistry.getReferenceType(input.getProperty(TYPE_PROPERTY));
            ReferenceType outputType = ReferenceTypeRegistry.getReferenceType(output.getProperty(TYPE_PROPERTY));
            String inputColumn = inputType.getClass().equals(EnsemblGeneTableType.class)?"gene_acc":
                inputType.getClass().equals(EnsemblProteinTableType.class)?"protein_acc":null;
            if(inputColumn == null) throw new Exception("Invalid input type");
            String outputColumn = outputType.getClass().equals(EnsemblGeneTableType.class)?"gene_acc":
                outputType.getClass().equals(EnsemblProteinTableType.class)?"protein_acc":null;
            if(outputColumn == null) throw new Exception("Invalid output type");
            boolean outputGenes = outputColumn.equals("gene_acc");
            Connection conn = getConnection();
            int inputSpeciesId = SqlUtil.queryInt( conn, SPECIES_QUERY.str( inputSpecies ), -1 );
            if(inputSpeciesId == -1)
                throw new Exception( "Unknown input species " + inputSpecies );
            int outputSpeciesId = SqlUtil.queryInt( conn, SPECIES_QUERY.str( outputSpecies ), -1 );
            if(outputSpeciesId == -1)
                throw new Exception( "Unknown output species " + outputSpecies );
            String query = "select h1.group_id,h1.protein_acc,h2.gene_acc,h2.protein_acc from homology h1 join homology h2 using(group_id) where h1.species_id=" + inputSpeciesId
                    + " and h2.species_id=" + outputSpeciesId + " and h1."+inputColumn+"=?";
            Map<String, String[]> result = new HashMap<>();
            try(PreparedStatement ps = conn.prepareStatement(query))
            {
                for(String acc: inputList)
                {
                    ps.setString(1, acc);
                    Map<String, String> matched = new HashMap<>();
                    TreeNode tree = null;
                    String inputProtein = null;
                    try( ResultSet resultSet = ps.executeQuery() )
                    {
                        while(resultSet.next())
                        {
                            if(inputProtein == null)
                            {
                                tree = getTreeById(conn, resultSet.getInt(1));
                                inputProtein = resultSet.getString(2);
                            }
                            matched.put(resultSet.getString(4), resultSet.getString(3));
                        }
                    }
                    if(tree == null || matched.size() == 0)
                    {
                        result.put(acc, ArrayUtils.EMPTY_STRING_ARRAY);
                    } else
                    {
                        String bestResult = null;
                        if(matched.size() == 1)
                        {
                            bestResult = matched.keySet().iterator().next();
                        } else
                        {
                            String[] matchedProteins = matched.keySet().toArray(new String[matched.size()]);
                            Map<String, Float> distances = tree.getDistances(inputProtein, matchedProteins);
                            bestResult = EntryStream.of(distances).minBy(Entry::getValue).map(Entry::getKey).orElse(null);
                        }
                        if(bestResult == null)
                        {
                            result.put(acc, ArrayUtils.EMPTY_STRING_ARRAY);
                        } else
                        {
                            result.put(acc, new String[] {outputGenes?matched.get(bestResult):bestResult});
                        }
                        if(jobControl != null)
                        {
                            if(jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST) return null;
                            jobControl.setPreparedness(result.size()*100/inputList.length);
                        }
                    }
                }
            }
            return result;
        }
        catch( Exception e )
        {
            if(jobControl != null)
            {
                jobControl.functionTerminatedByError(e);
            }
        }
        return null;
    }
    
    protected TreeNode getTreeById(Connection connection, int id)
    {
        String tree = SqlUtil.queryString(connection, TREE_QUERY+id);
        return tree != null ? new TreeNode(tree) : null;
    }

    @Override
    public Properties[] getSupportedInputs()
    {
        try
        {
            getConnection();
        }
        catch(BiosoftSQLException e)
        {
            return EMPTY_PROPERTIES;
        }
        ReferenceType typeGene = ReferenceTypeRegistry.getReferenceType(EnsemblGeneTableType.class);
        ReferenceType typeProtein = ReferenceTypeRegistry.getReferenceType(EnsemblProteinTableType.class);
        return StreamEx.of( getSupportedSpecies() ).cross( typeGene, typeProtein )
            .mapKeyValue( HomologyHub::createProperties )
            .toArray( Properties[]::new );
    }

    @Override
    public Properties[] getSupportedMatching(Properties input)
    {
        if(!input.getProperty(TYPE_PROPERTY).equals(ReferenceTypeRegistry.getReferenceType(EnsemblGeneTableType.class).toString())
                && !input.getProperty(TYPE_PROPERTY).equals(ReferenceTypeRegistry.getReferenceType(EnsemblProteinTableType.class).toString()))
            return EMPTY_PROPERTIES;
        Species inputSpecies = Species.getSpecies( input.getProperty( SPECIES_PROPERTY ) );
        if(inputSpecies == null)
            return EMPTY_PROPERTIES;
        return StreamEx.of( getSupportedSpecies() ).without( inputSpecies.getLatinName() )
                .map( species -> createProperties( species, input.getProperty( TYPE_PROPERTY ) ) )
                .toArray( Properties[]::new );
    }

    @Override
    public double getMatchingQuality(Properties input, Properties output)
    {
        if( !output.getProperty(TYPE_PROPERTY).equals(input.getProperty(TYPE_PROPERTY)) )
            return 0;
        if( input.getProperty(TYPE_PROPERTY).equals(ReferenceTypeRegistry.getReferenceType(EnsemblGeneTableType.class).toString()) )
            return 0.4;
        if( input.getProperty(TYPE_PROPERTY).equals(ReferenceTypeRegistry.getReferenceType(EnsemblProteinTableType.class).toString()) )
            return 0.2;
        return 0;
    }
}
