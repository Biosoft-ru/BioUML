package biouml.plugins.reactome.access;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ru.biosoft.access.core.DataElementPath;
import biouml.model.Module;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;


public abstract class ReactionLikeEventSqlTransformer extends EventSqlTransformer
{
    @Override
    protected abstract Reaction createElement(ResultSet resultSet, Connection connection) throws SQLException;

    @Override
    public Reaction create(ResultSet resultSet, Connection connection) throws Exception
    {
        Reaction reaction = super.create(resultSet, connection);
        reaction.setSpecieReferences(getParticipants(reaction, connection));
        return reaction;
    }

    protected SpecieReference[] getParticipants(Reaction parent, Connection connection) throws SQLException
    {
        String participants = "SELECT DISTINCT identifier, _displayName,_class, _timestamp, input_rank, dbo.DB_ID,1"
                + " FROM ReactionlikeEvent_2_input re2t LEFT JOIN " + databaseObjectTable
                + " dbo ON(dbo.DB_ID=re2t.input) JOIN StableIdentifier si ON(dbo.stableIdentifier = si.DB_ID) WHERE re2t.DB_ID='"
                + getReactomeId(parent) + "'"
                + " UNION SELECT DISTINCT identifier, _displayName,_class, _timestamp, output_rank, dbo.DB_ID,2"
                + " FROM ReactionlikeEvent_2_output re2t LEFT JOIN " + databaseObjectTable
                + " dbo ON(dbo.DB_ID=re2t.output) JOIN StableIdentifier si ON(dbo.stableIdentifier = si.DB_ID) WHERE re2t.DB_ID='"
                + getReactomeId(parent) + "'"
                + " UNION SELECT DISTINCT identifier, _displayName,_class, _timestamp, catalystActivity_rank, dbo.DB_ID,3"
                + " FROM ReactionlikeEvent_2_catalystActivity re2t LEFT JOIN CatalystActivity ca ON (ca.DB_ID=re2t.catalystActivity)"
                + " LEFT JOIN " + databaseObjectTable + " dbo ON(dbo.DB_ID=ca.physicalEntity) "
                + " JOIN StableIdentifier si ON(dbo.stableIdentifier = si.DB_ID) WHERE re2t.DB_ID='"
                + getReactomeId(parent) + "'"
                + " UNION SELECT DISTINCT identifier, _displayName,_class, _timestamp, output_rank, dbo.DB_ID,4"
                + " FROM ReactionlikeEvent_2_output re2t LEFT JOIN Event_2_precedingEvent e2p ON(e2p.precedingEvent=re2t.DB_ID) LEFT JOIN " + databaseObjectTable
                + " dbo ON(dbo.DB_ID=re2t.output) JOIN StableIdentifier si ON(dbo.stableIdentifier = si.DB_ID) WHERE e2p.DB_ID='"
                + getReactomeId(parent) + "'"
                + " ORDER BY 7,5";
        try (Statement statement = connection.createStatement(); ResultSet reactantSet = statement.executeQuery( participants ))
        {
            List<SpecieReference> reactants = new ArrayList<>();
            DataElementPath modulePath = Module.getModulePath(parent);
            DataElementPath dataPath = modulePath.getChildPath(Module.DATA);
            Set<ru.biosoft.access.core.DataElementPath> kernelPaths = new HashSet<>();
            while( reactantSet.next() )
            {
                String reactantId = reactantSet.getString(1);
                String reactantClass = reactantSet.getString(3);
                DataElementPath kernelPath = dataPath.getChildPath(getCollectionNameByClass(reactantClass), reactantId);
                String role;
                switch( reactantSet.getInt(7) )
                {
                    case 1:
                        role = SpecieReference.REACTANT;
                        break;
                    case 2:
                        role = SpecieReference.PRODUCT;
                        break;
                    case 3:
                        role = SpecieReference.MODIFIER;
                        break;
                    default:
                        if(kernelPaths.contains(kernelPath))
                            continue;
                        role = SpecieReference.MODIFIER;
                }
                kernelPaths.add(kernelPath);
                SpecieReference sr = new SpecieReference(parent, reactantId + " as " + role, role);
                sr.setTitle(reactantSet.getString(2));
                sr.setDate(reactantSet.getString(4));
                if( kernelPath.exists() )
                {
                    sr.setSpecie(kernelPath.getPathDifference(modulePath));
                }
                else
                {
                    sr.setSpecie(DataElementPath.create("stub").getChildPath(reactantClass, reactantId).toString());
                }
                if( reactantClass.equals("SimpleEntity") )
                {
                    sr.setComment("small molecule");
                }
                reactants.add(sr);
            }
            return reactants.toArray(new SpecieReference[reactants.size()]);
        }
    }
}
