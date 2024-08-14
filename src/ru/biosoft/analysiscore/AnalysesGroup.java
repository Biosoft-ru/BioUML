package ru.biosoft.analysiscore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementDescriptor;
import ru.biosoft.access.core.filter.Filter;
import ru.biosoft.access.core.filter.FilteredDataCollection;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.access.repository.IconFactory;
import ru.biosoft.access.security.NetworkRepositoryFilter;

/**
 * Group of analyses inside analyses/Methods branch in the tree
 * @author lan
 */
public class AnalysesGroup extends FilteredDataCollection<AnalysisMethodInfo>
{
    public AnalysesGroup(String name, DataCollection<DataCollection<AnalysisMethodInfo>> parent, Properties properties)
    {
        super( parent, name, new VectorDataCollection<>( name ), createFilter(), null, properties );

    }

    private static Filter<DataElement> createFilter()
    {
        if(AnalysisMethodRegistry.isProtectionEnabled)
            return new NetworkRepositoryFilter();
        return Filter.INCLUDE_ALL_FILTER;
    }

    public AnalysesGroup(DataCollection<DataCollection<AnalysisMethodInfo>> parent, Properties properties) throws Exception
    {
        this(properties.getProperty( DataCollectionConfigConstants.NAME_PROPERTY ), parent, properties);
    }

    /**
     * Overrides getFilteredNames to prevent caching since this collection contains different elements for each user
     */
    @Override
    protected List<String> getFilteredNames()
    {
        List<String> result = new ArrayList<>();
        for(AnalysisMethodInfo primaryDE : primaryCollection)
        {
            if( getFilter().isAcceptable( primaryDE ) )
                result.add(primaryDE.getName());
        }
        Collections.sort( result );
        return result;
    }

    @Override
    public DataElementDescriptor getDescriptor(String name)
    {
        try
        {
            return new DataElementDescriptor(AnalysisMethodInfo.class, IconFactory.getClassIconId(get(name).getAnalysisClass()), true);
        }
        catch( Exception e )
        {
            return null;
        }
    }

    private String description;
    @Override
    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    private String[] related = new String[0];
    public String[] getRelated()
    {
        return related;
    }
    public void setRelated(String[] relatedGroups)
    {
        this.related = relatedGroups;
    }
}
