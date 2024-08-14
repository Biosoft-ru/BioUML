package ru.biosoft.bsa.analysis;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.PropertyDescriptorEx;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.filter.Filter;
import ru.biosoft.access.core.filter.FilteredDataCollection;
import ru.biosoft.bsa.MessageBundle;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.view.TrackViewBuilder;

public class FilteredTrack implements Track
{
    private Track source;
    private Filter<Site> filter;
    private String name;
    private DataCollection<?> origin;
    
    public FilteredTrack(DataCollection<?> origin, Track source, Filter<Site> filter)
    {
        this.source = source;
        this.filter = filter;
        this.name = source.getName();
        this.origin = origin;
    }
    
    public FilteredTrack(Track source, Filter<Site> filter)
    {
        this(source.getOrigin(), source, filter);
    }
    
    public FilteredTrack()
    {
    }
    
    public String getSourcePath()
    {
        return source==null?null:DataElementPath.create(source).toString();
    }
    
    public void setSourcePath(String path)
    {
        source = path==null?null:DataElementPath.create(path).getDataElement(Track.class);
    }
    
    public String getOriginPath()
    {
        return origin==null?null:DataElementPath.create(origin).toString();
    }
    
    public void setOriginPath(String path)
    {
        origin = path==null?null:DataElementPath.create(path).optDataCollection();
    }
    
    public Filter<Site> getFilter()
    {
        return filter;
    }
    
    public void setFilter(Filter<Site> filter)
    {
        this.filter = filter;
    }
    
    @Override
    public int countSites(String sequence, int from, int to) throws Exception
    {
        DataCollection<Site> sites = source.getSites(sequence, from, to);
        if(sites == null) return 0;
        Iterator<Site> iterator = sites.iterator();
        int count = 0;
        
        while(iterator.hasNext())
        {
            Site s = iterator.next();
            if(filter.isAcceptable(s)) count++;
        }
        return count;
    }

    @Override
    public @Nonnull DataCollection<Site> getAllSites()
    {
        return new FilteredDataCollection<>( source.getAllSites(), filter );
    }

    @Override
    public Site getSite(String sequence, String siteName, int from, int to) throws Exception
    {
        Site s = source.getSite(sequence, siteName, from, to);
        if(filter.isAcceptable(s)) return s;
        return null;
    }

    @Override
    public DataCollection<Site> getSites(String sequence, int from, int to)
    {
        DataCollection<Site> sites = source.getSites(sequence, from, to);
        if(sites == null) return null;
        return new FilteredDataCollection<>( sites, filter );
    }

    @Override
    public TrackViewBuilder getViewBuilder()
    {
        return source.getViewBuilder();
    }

    @Override
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    @Override
    public DataCollection<?> getOrigin()
    {
        return origin;
    }

    public static abstract class SiteFilter implements Filter<Site>
    {
        @Override
        public boolean isEnabled()
        {
            return true;
        }
    }

    public static class ModelTrackFilter extends SiteFilter
    {
        private Set<String> matrixNames = new HashSet<>();
        
        public ModelTrackFilter(Set<String> matrixNames)
        {
            this.matrixNames = matrixNames;
        }
        
        public ModelTrackFilter()
        {
        }
        
        public void add(String matrix)
        {
            matrixNames.add(matrix);
        }
        
        public String[] getMatrixNames()
        {
            return matrixNames.toArray(new String[matrixNames.size()]);
        }
        
        public void setMatrixNames(String[] names)
        {
            matrixNames.clear();
            matrixNames.addAll(Arrays.asList(names));
        }
    
        @Override
        public boolean isAcceptable(Site s)
        {
            DynamicProperty matrixProperty = s.getProperties().getProperty("siteModel");
            if(matrixProperty == null) return false;
            return matrixNames.contains(matrixProperty.getValue().toString());
        }
    }
    
    public static class ModelTrackFilterBeanInfo extends BeanInfoEx
    {
        public ModelTrackFilterBeanInfo()
        {
            super(ModelTrackFilter.class, MessageBundle.class.getName());
        }

        @Override
        public void initProperties() throws Exception
        {
            add(new PropertyDescriptorEx("matrixNames", beanClass), getResourceString("PN_MATRIX_NAME"),
                    getResourceString("PD_MATRIX_NAME"));
        }
    }
}
