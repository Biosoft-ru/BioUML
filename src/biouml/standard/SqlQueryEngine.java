package biouml.standard;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

import biouml.model.Module;
import biouml.standard.type.Base;
import biouml.standard.type.Reaction;
import biouml.standard.type.SemanticRelation;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.biohub.BioHub;
import ru.biosoft.access.biohub.TargetOptions;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.access.core.VectorDataCollection;

/**
 * SQL - base implementation of <code>StandardQueryEngine</code>
 */
public class SqlQueryEngine extends StandardQueryEngine
{
    @Override
    public String getName( TargetOptions dbOptions )
    {
        return "SQL collection search";
    }
    
    @Override
    public int canSearchLinked(TargetOptions dbOptions)
    {
        //Search linked is available only for 1 database with "SQL" type
        DataElementPathSet collections = dbOptions.getUsedCollectionPaths();
        if( collections.size() == 1 )
        {
            Module module = collections.first().optDataElement(Module.class);
            if( module != null && ( module.getType() instanceof SqlModuleType ) )
            {
                return 17;
            }
        }
        return 0;
    }

    /**
     * Get query for all reactions id, which related with kernel,
     * presented in result RecordSet as field <B>"name"</B>
     */
    protected String getReaction(String kernelName, int direction)
    {
        String sql = "SELECT reactionID AS \"name\" FROM reactionComponents WHERE specieID LIKE '%" + kernelName + "'";

        String whereRole = null;
        if( BioHub.DIRECTION_DOWN == direction )
        {
            whereRole = " AND (role='reactant' OR role='modifier')";
        }
        else if( BioHub.DIRECTION_UP == direction )
        {
            whereRole = " AND role='product'";
        }
        else if( BioHub.DIRECTION_BOTH == direction )
        {
            whereRole = " AND (role='product' OR role='reactant' OR role='modifier')";
        }
        else if( BioHub.DIRECTION_UNDEFINED == direction )
        {
        }
        if( null != whereRole )
            sql += whereRole;

        return sql;
    }

    @Override
    protected DataCollection<Reaction> getReactions(Module module, Base kernel, int direction) throws Exception
    {
        DataCollection<Reaction> fullReactions = module.getCategory(Reaction.class);
        Connection conn = DataCollectionUtils.getSqlConnection(fullReactions);
        if( conn != null )
        {
            VectorDataCollection<Reaction> reactions = new VectorDataCollection<>( module.getType().getCategory( Reaction.class ),
                    fullReactions, new Properties() );

            String kernelName = CollectionFactory.getRelativeName(kernel, module);
            if( kernelName != null )
            {
                try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery( getReaction( kernelName, direction ) ))
                {
                    while( rs.next() )
                    {
                        String id = rs.getString("name");
                        Reaction r = fullReactions.get(id);
                        reactions.put(r);
                    }
                }
            }
            return reactions;
        }
        else
            return super.getReactions(module, kernel, direction);
    }

    /**
     * Get query for all relation id, which related with kernel,
     * presented in result RecordSet as field <B>"name"</B>
     */
    protected String getSemanticRelation(String kernelName, int direction)
    {
        String sql = "SELECT ID AS \"name\" FROM relations WHERE ";

        if( BioHub.DIRECTION_DOWN == direction )
        {
            sql += getSemanticRelationInOutCondition(kernelName, true);
        }
        else if( BioHub.DIRECTION_UP == direction )
        {
            sql += getSemanticRelationInOutCondition(kernelName, false);
        }
        else if( BioHub.DIRECTION_BOTH == direction || BioHub.DIRECTION_UNDEFINED == direction )
        {
            sql += getSemanticRelationInOutCondition(kernelName, true) + " OR " + getSemanticRelationInOutCondition(kernelName, false);
        }

        return sql;
    }

    private String getSemanticRelationInOutCondition(String kernelName, boolean isInput)
    {
        if( isInput )
            return "inputElement LIKE '" + kernelName + "' ";
        else
            return "outputElement LIKE '" + kernelName + "' ";
    }

    @Override
    protected DataCollection<SemanticRelation> getSemanticRelations(Module module, Base kernel, int direction) throws Exception
    {
        DataCollection<SemanticRelation> fullRelations = module.getCategory(SemanticRelation.class);
        Connection conn = DataCollectionUtils.getSqlConnection(fullRelations);
        if( conn != null )
        {
            VectorDataCollection<SemanticRelation> relations = new VectorDataCollection<>(
                    module.getType().getCategory( SemanticRelation.class ), fullRelations, new Properties() );

            String category = module.getType().getCategory(kernel.getClass());
            if( category != null )
            {
                String kernelName = DataElementPath.EMPTY_PATH.getChildPath(kernel.getOrigin().getName(), kernel.getName()).toString();
                try (Statement stmt = conn.createStatement();
                        ResultSet rs = stmt.executeQuery( getSemanticRelation( kernelName, direction ) ))
                {
                    while( rs.next() )
                    {
                        String id = rs.getString("name");
                        SemanticRelation r = fullRelations.get(id);
                        relations.put(r);
                    }
                }
            }
            return relations;
        }
        else
            return super.getSemanticRelations(module, kernel, direction);
    }
}
