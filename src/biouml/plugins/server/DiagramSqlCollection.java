package biouml.plugins.server;

import java.util.Properties;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import biouml.model.Diagram;
import biouml.standard.type.access.DiagramSqlTransformer;

/**
 * Simple wrapper over DataSqlCollection which applicable only
 * for diagrams
 */
public class DiagramSqlCollection extends DataSqlCollection
{
    
    public DiagramSqlCollection(DataCollection parent, Properties properties)
    {
        super ( parent, properties );
    }

    @Override
    protected void preInit ( Properties properties )
    {
        super.preInit ( properties );
        
        String diagramClass = properties.getProperty ( DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY );
        if ( diagramClass == null )
        {
            properties.put ( DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY, Diagram.class.getName ( ) );
        }
        
        String id = properties.getProperty ( DataCollectionConfigConstants.ID_FORMAT );
        if ( id == null )
        {
            properties.put ( DataCollectionConfigConstants.ID_FORMAT, "DGR0000" );
        }
        
        String transformer = properties.getProperty ( SQL_TRANSFORMER_CLASS );
        if ( transformer == null )
        {
            properties.put ( SQL_TRANSFORMER_CLASS, DiagramSqlTransformer.class.getName ( ) );
        }
    }
    
}
