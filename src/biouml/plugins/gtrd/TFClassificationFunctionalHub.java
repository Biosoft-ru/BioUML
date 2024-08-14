package biouml.plugins.gtrd;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import biouml.plugins.enrichment.FunctionalHubConstants;
import biouml.plugins.enrichment.SqlCachedFunctionalHubSupport;
import biouml.plugins.ensembl.tabletype.UniprotProteinTableType;

/**
 * @author lan
 *
 */
public class TFClassificationFunctionalHub extends SqlCachedFunctionalHubSupport
{
    public TFClassificationFunctionalHub(Properties properties)
    {
        super(properties);
    }

    @Override
    protected Iterable<Group> getGroups() throws Exception
    {
        Map<String, Group> result = new HashMap<>();
        Connection conn = getConnection();
        if(conn == null) return null;
        try( Statement st = conn.createStatement(); ResultSet resultSet = st.executeQuery( "select name, title from classification" ) )
        {
            while(resultSet.next())
                result.put(resultSet.getString(1), new Group(resultSet.getString(1), resultSet.getString(2)));
        }
        try( Statement st = conn.createStatement();
                ResultSet resultSet = st.executeQuery(
                        "select input,output from hub where input_type='ProteinGTRDType' and output_type='UniprotProteinTableType'" ) )
        {
            while(resultSet.next())
            {
                String groupName = resultSet.getString(1);
                String element = resultSet.getString(2);
                while(true)
                {
                    Group group = result.get(groupName);
                    if(group != null) group.addElement(element);
                    int pos = groupName.lastIndexOf('.');
                    if(pos == -1) break;
                    groupName = groupName.substring(0, pos);
                }
            }
        }
        return result.values();
    }

    @Override
    protected ReferenceType getInputReferenceType()
    {
        return ReferenceTypeRegistry.getReferenceType(UniprotProteinTableType.class);
    }

    @Override
    protected String getTableName()
    {
        return "functional_classification";
    }

    private static final Properties SUPPORTED_MATCHING = new Properties()
    {{
        setProperty(TYPE_PROPERTY, ReferenceTypeRegistry.getReferenceType(ClassGTRDType.class).toString());
    }};
    @Override
    public Properties[] getSupportedMatching(Properties input)
    {
        if(input.containsKey(FunctionalHubConstants.FUNCTIONAL_CLASSIFICATION_RECORD))
            return new Properties[] {SUPPORTED_MATCHING};
        return null;
    }
}
