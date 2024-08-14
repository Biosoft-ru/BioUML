package biouml.plugins.agentmodeling._test;

import javax.annotation.Nonnull;

import biouml.model.Diagram;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.table.TableDataCollection;

/**
 * @author lan
 *
 */
public class AgentTestingUtils
{
    protected static void initRepository() throws Exception
    {
//        String repositoryPath = "../data";
                String repositoryPath = "../data_resources";
        CollectionFactory.createRepository(repositoryPath);
    }
    
    public static @Nonnull Diagram loadDiagram(String diagramName, String collectionName) throws Exception
    {
        initRepository();
        return DataElementPath.create(collectionName).getChildPath(diagramName).getDataElement(Diagram.class);
    }

    public static @Nonnull TableDataCollection loadTable(String diagramName, String collectionName) throws Exception
    {
        initRepository();
        return DataElementPath.create(collectionName).getChildPath(diagramName).getDataElement(TableDataCollection.class);
    }
    
    protected static @Nonnull DataCollection loadCollection(String collectionName) throws Exception
    {
        initRepository();
        return DataElementPath.create(collectionName).getDataCollection();
    }
}
