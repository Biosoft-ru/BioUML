package ru.biosoft.bsa.exporter;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;
import com.developmentontheedge.beans.editors.GenericComboBoxEditor;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.DataElementExporter;
import ru.biosoft.access.DataElementExporterRegistry;
import ru.biosoft.access.core.SortableDataCollection;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.TrackRegion;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.util.bean.BeanInfoEx2;

/**
 * General interface to export tracks in the specified format,
 */
public abstract class TrackExporter implements DataElementExporter
{
    /** Returns true if the specified diagram can be exported in this format and false otherwise. */
    public abstract boolean accept(TrackRegion trackRegion);
    protected abstract void doExport(TrackRegion trackRegion, File file, FunctionJobControl jobControl) throws Exception;
    public abstract boolean init(String format, String suffix);

    @Override
    public int accept(DataElement de)
    {
        if( de instanceof Track )
            de = new TrackRegion((Track)de);
        if( ! ( de instanceof TrackRegion ) )
            return DataElementExporter.ACCEPT_UNSUPPORTED;
        if( accept((TrackRegion)de) )
        {
            return DataElementExporter.ACCEPT_HIGH_PRIORITY;
        }
        return DataElementExporter.ACCEPT_UNSUPPORTED;
    }
    
    @Override
    public List<Class<? extends DataElement>> getSupportedTypes()
    {
        return Arrays.asList( Track.class, TrackRegion.class );
    }

    @Override
    public void doExport(@Nonnull ru.biosoft.access.core.DataElement de, @Nonnull File file) throws Exception
    {
        if(parameters == null)
            parameters = createParameters( de, file );
        if( de instanceof Track )
            de = new TrackRegion((Track)de);
        doExport((TrackRegion)de, file, null);
    }

    @Override
    public void doExport(@Nonnull ru.biosoft.access.core.DataElement de, @Nonnull File file, FunctionJobControl jobControl) throws Exception
    {
        if(parameters == null)
            parameters = createParameters( de, file );
        if( de instanceof Track )
            de = new TrackRegion((Track)de);
        if( jobControl == null )
        {
            doExport(de, file);
            return;
        }
        jobControl.functionStarted();
        doExport((TrackRegion)de, file, jobControl);
        if( jobControl.getStatus() != JobControl.TERMINATED_BY_REQUEST && jobControl.getStatus() != JobControl.TERMINATED_BY_ERROR )
        {
            jobControl.setPreparedness(100);
            jobControl.functionFinished();
        }
    }
    
    protected BaseParameters parameters;
    @Override
    public synchronized Object getProperties(DataElement de, File file)
    {
        if(parameters == null)
            parameters = createParameters(de, file);
        return parameters;
    }
    
    protected BaseParameters createParameters(DataElement de, File file)
    {
        Track track = getTrack( de );
        return new BaseParameters( track );
    }
    
    protected Track getTrack(DataElement de) throws IllegalArgumentException
    {
        Track track;
        if(de instanceof Track)
            track = (Track)de;
        else if(de instanceof TrackRegion)
            track = ((TrackRegion)de).getTrack();
        else
            throw new IllegalArgumentException();
        return track;
    }
    
    @Override
    public boolean init(Properties properties)
    {
        return init(properties.getProperty(DataElementExporterRegistry.FORMAT), properties.getProperty(DataElementExporterRegistry.SUFFIX));
    }
    

    public static class BaseParameters extends Option
    {
        private Track track;
        public BaseParameters(Track track)
        {
            this.track = track;
        }
        
        public Track getTrack() {
            return track;
        }
        
        private String sortingColumn = SortingColumnSelector.NONE;
        @PropertyName("Sort by")
        public String getSortingColumn()
        {
            return sortingColumn;
        }

        public void setSortingColumn(String sortingColumn)
        {
            Object oldValue = this.sortingColumn;
            this.sortingColumn = sortingColumn;
            firePropertyChange( "sortingColumn", oldValue, sortingColumn );
        }
        public boolean isSortingColumnHidden()
        {
            DataCollection<Site> sites = track.getAllSites();
            if(!(sites instanceof SortableDataCollection))
                return false;
            SortableDataCollection<Site> sortedSites = (SortableDataCollection<Site>)sites;
            return !sortedSites.isSortingSupported();
        }

        private boolean descending = true;
        @PropertyName("Descending")
        public boolean isDescending()
        {
            return descending;
        }

        public void setDescending(boolean descending)
        {
            boolean oldValue = this.descending;
            this.descending = descending;
            firePropertyChange( "descending", oldValue, descending );
        }
        public boolean isDescendingHidden()
        {
            return SortingColumnSelector.NONE.equals( sortingColumn );
        }

        public static final String SELECT_ALL = "All";
        public static final String SELECT_TOP500 = "Top 500";
        public static final String SELECT_TOP1000 = "Top 1000";
        public static final String SELECT_TOP2000 = "Top 2000";
        public static final String SELECT_TOP5000 = "Top 5000";
        public static final String SELECT_CUSTOM = "Custom...";
        private String select = "All";
        @PropertyName("Select")
        public String getSelect()
        {
            return select;
        }
        public void setSelect(String select)
        {
            Object oldValue = this.select;
            this.select = select;
            firePropertyChange( "select", oldValue, select );
            switch(select)
            {
                case SELECT_ALL: setTop( 100 ); break;
                case SELECT_TOP500: setTop( 500 ); break;
                case SELECT_TOP1000: setTop( 1000 ); break;
                case SELECT_TOP2000: setTop( 2000 ); break;
                case SELECT_TOP5000: setTop( 5000 ); break;
                default:
                    throw new AssertionError();
            }

        }
        public boolean isSelectHidden()
        {
            return SortingColumnSelector.NONE.equals( sortingColumn );
        }
        
        private int top = 100;
        public int getTop()
        {
            return top;
        }
        public void setTop(int top)
        {
            int oldValue = this.top;
            this.top = top;
            firePropertyChange( "top", oldValue, top );
        }
        public boolean isTopHidden()
        {
            return !SELECT_CUSTOM.equals( select );
        }
        
        public Iterator<Site> getIterator(TrackRegion trackRegion)
        {
            final DataCollection<Site> siteDataCollection = trackRegion.getTrack().getAllSites();
            if(!isSortingColumnHidden()
                    && !SortingColumnSelector.NONE.equals( getSortingColumn() ))
            {
                int limit = getSelect().equals( SELECT_ALL ) ? siteDataCollection.getSize() : getTop();
                SortableDataCollection<Site> sortedDC = (SortableDataCollection<Site>)siteDataCollection;
                return sortedDC.getSortedIterator( getSortingColumn(), !isDescending(), 0, limit );
            }
            else
                return siteDataCollection.iterator();
        }

    }
    
    public static class BaseParametersBeanInfo extends BeanInfoEx2<BaseParameters>
    {
        public BaseParametersBeanInfo()
        {
            super( BaseParameters.class );
        }
        
        protected BaseParametersBeanInfo(Class<? extends BaseParameters> beanClass, String resourceBundleName)
        {
            super( beanClass, resourceBundleName );
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            property( "sortingColumn" ).hidden( "isSortingColumnHidden" ).editor( SortingColumnSelector.class ).add();
            property("descending").hidden( "isDescendingHidden" ).add();
            property( "select" )
                .hidden("isSelectHidden")
                .tags( BaseParameters.SELECT_ALL, BaseParameters.SELECT_TOP500, BaseParameters.SELECT_TOP1000, BaseParameters.SELECT_TOP2000, BaseParameters.SELECT_TOP5000, BaseParameters.SELECT_CUSTOM )
                .add();
            property( "top" ).hidden("isTopHidden").add();
        }
    }
    
    public static class SortingColumnSelector extends GenericComboBoxEditor
    {
        public static String NONE = "none";
        @Override
        protected Object[] getAvailableValues()
        {
            Track track = ((BaseParameters)getBean()).getTrack();
            DataCollection<Site> collection = track.getAllSites();
            if(collection instanceof SortableDataCollection)
            {
                SortableDataCollection<Site> sCol = (SortableDataCollection<Site>)collection;
                String[] fields = sCol.getSortableFields();
                String[] res = new String[fields.length+1];
                res[0] = NONE;
                System.arraycopy( fields, 0, res, 1, fields.length );
                return res;
            }
            return new String[0];
        }
    }
    

}
