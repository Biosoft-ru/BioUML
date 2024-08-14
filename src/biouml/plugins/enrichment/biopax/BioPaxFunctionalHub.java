package biouml.plugins.enrichment.biopax;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Module;
import biouml.model.Node;
import biouml.plugins.biopax.biohub.BioPAXSQLHubBuilder;
import biouml.plugins.enrichment.FunctionalHubConstants;
import biouml.plugins.enrichment.SqlCachedFunctionalHubSupport;
import biouml.standard.type.Base;
import biouml.standard.type.Reaction;

/**
 * @author lan
 *
 */
public class BioPaxFunctionalHub extends SqlCachedFunctionalHubSupport
{
    private String tableName;
    private Module dc;
    private ReferenceType type;

    /**
     * @param properties
     */
    public BioPaxFunctionalHub(Properties properties)
    {
        super(properties);
        init();
    }

    private void init()
    {
        try
        {
            dc = getModulePath().getDataElement(Module.class);
        }
        catch( Exception e )
        {
        }
        if( dc != null )
        {
            type = ReferenceTypeRegistry.getReferenceType(dc);
        }

        tableName = properties.getProperty(BioPAXSQLHubBuilder.HUB_TABLE_PROPERTY) + "_func";
    }

    @Override
    protected Iterable<Group> getGroups() throws Exception
    {
        DataCollection<Diagram> diagramDC = dc.getDiagrams();
        List<Group> result = new ArrayList<>();
        for(Diagram d: diagramDC)
        {
            Set<String> elements = new HashSet<>();
            for(DiagramElement dobj: d)
            {
                if(dobj instanceof Node)
                {
                    Node node = (Node)dobj;
                    Base kernel = node.getKernel();
                    if(kernel != null && ! (kernel instanceof Reaction))
                        elements.add(kernel.getName());
                }
            }
            result.add(new Group(d.getName(), d.getTitle(), elements));
            diagramDC.release(d.getName());
        }
        return result;
    }

    @Override
    protected ReferenceType getInputReferenceType()
    {
        return type;
    }

    @Override
    protected String getTableName()
    {
        return tableName;
    }

    @Override
    public Properties[] getSupportedMatching(Properties input)
    {
        if(input.containsKey(FunctionalHubConstants.FUNCTIONAL_CLASSIFICATION_RECORD))
        {
            Properties result = new Properties();
            result.setProperty(DataCollectionConfigConstants.URL_TEMPLATE, "de:"+DataElementPath.create(dc, "Diagrams")+"/$id$");
            return new Properties[] {result};
        }
        return null;
    }
}
