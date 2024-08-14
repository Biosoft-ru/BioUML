package biouml.plugins.chemoinformatics.web;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.table.access.TableResolver;
import biouml.plugins.chemoinformatics.access.TableStructureWrapper;
import biouml.standard.type.Structure;

/**
 * Table resolver for Structures. Get table with structure attributes.
 * Is used for SDF-based collections.
 */
public class StructuresTableResolver extends TableResolver
{
    public StructuresTableResolver(BiosoftWebRequest arguments)
    {
    }
    
    @Override
    public DataCollection<?> getTable(DataElement de)
    {
        if( ( de instanceof DataCollection ) && Structure.class.isAssignableFrom( ( (DataCollection<?>)de ).getDataElementType()) )
        {
            return new TableStructureWrapper((DataCollection<Structure>)de);
        }
        return null;
    }
}