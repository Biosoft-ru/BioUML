package ru.biosoft.table;

import java.util.logging.Level;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.mozilla.javascript.Undefined;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.filter.Filter;
import ru.biosoft.access.biohub.BioHub;
import biouml.model.Diagram;
import biouml.model.Module;
import biouml.model.Node;
import biouml.model.util.ReferencesHandler;
import biouml.standard.type.DatabaseInfo;

public class RowFilter implements Filter<DataElement>
{
    protected static final Logger log = Logger.getLogger(RowFilter.class.getName());

    private final String filterStr;
    private final RowJSExpression expression;
    private final Diagram diagram;
    private final BioHub bioHub;
    private final Module module;
    protected Map<String, DatabaseInfo> databaseInfoMap = new HashMap<>();
    
    public RowFilter(String filterStr, DataCollection<?> dc) throws IllegalArgumentException
    {
        this(filterStr, dc, null, null, null);
    }

    public RowFilter(String filterStr, DataCollection<?> dc, Diagram diagram, Module module, BioHub bioHub)
            throws IllegalArgumentException
    {
        this.filterStr = filterStr;
        expression = new RowJSExpression(filterStr, dc);
        this.diagram = diagram;
        this.module = module;
        this.bioHub = bioHub;
    }

    @Override
    public boolean isAcceptable(DataElement de)
    {
        if( !isFilterAccept(de) )
        {
            return false;
        }
        if( !isDiagramFilterAccept(de) )
        {
            return false;
        }

        return true;
    }

    public boolean isFilterAccept(DataElement rowDataElement)
    {
        try
        {
            Object result = expression.evaluate(rowDataElement);
            if(result instanceof Boolean) return (Boolean)result;
            if(result instanceof String) return Boolean.valueOf((String)result);
            if(result instanceof Number) return ((Number)result).intValue() != 0;
            if(result instanceof Undefined) return false;
            return result != null;
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean isEnabled()
    {
        return true;
    }

    private boolean isDiagramFilterAccept(DataElement rowDataElement)
    {
        if( module == null || diagram == null || bioHub == null )
        {
            return true;
        }
        try
        {
            ReferencesHandler.initDatabaseInfoMap(Module.getModule(diagram), databaseInfoMap);
        }
        catch( Exception e )
        {
            e.printStackTrace();
            return true;
        }

        // genes synonyms searching
        return diagram.recursiveStream().select( Node.class ).map( Node::getKernel ).nonNull()
            .flatMap( kernel -> ReferencesHandler.idsForDataElement(kernel, databaseInfoMap, bioHub) )
            .has( rowDataElement.getName() );
    }

    public String getFilterExpression()
    {
        return filterStr;
    }
}
