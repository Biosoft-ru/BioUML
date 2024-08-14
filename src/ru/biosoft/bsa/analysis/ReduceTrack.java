package ru.biosoft.bsa.analysis;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysis.aggregate.NumericAggregator;
import ru.biosoft.analysis.aggregate.NumericAggregatorEditor;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.analysis.trackutil.SiteInfo;
import ru.biosoft.bsa.analysis.trackutil.SiteProperty;
import ru.biosoft.bsa.analysis.trackutil.TrackPropertiesMultiSelector;
import ru.biosoft.bsa.analysis.trackutil.VariableSiteLocator;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.util.OptionEx;
import ru.biosoft.util.bean.BeanInfoEx2;

public class ReduceTrack extends AnalysisMethodSupport<ReduceTrack.Parameters>
{
    public ReduceTrack(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }

    @Override
    public Track justAnalyzeAndPut() throws Exception
    {
        Track input = parameters.getInputTrack().getDataElement( Track.class );
        final VariableSiteLocator locator = new VariableSiteLocator( parameters.getFieldNames(), 0 );
        log.info( "Reading track..." );
        final DataCollection<Site> allSites = input.getAllSites();
        jobControl.pushProgress( 0, 40 );
        Collection<Site> allSitesCollection = DataCollectionUtils.asCollection( allSites, Site.class );
        jobControl.forCollection( allSitesCollection, element -> {
            locator.put( element );
            return true;
        } );
        if( jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST )
            return null;
        jobControl.popProgress();

        log.info( "Initializing resulting track..." );
        final SqlTrack result = SqlTrack.createTrack( parameters.getOutputTrack(), input, input.getClass() );
        jobControl.pushProgress( 40, 99 );

        final Set<String> propertiesToCopy = new HashSet<>( allSites.iterator().next().getProperties().asMap().keySet() );
        for( String field : parameters.getFieldNames() )
        {
            propertiesToCopy.remove( field );
        }

        log.info( "Filtering..." );
        jobControl.forCollection( allSitesCollection, element -> {
            try
            {
                Collection<SiteInfo> list = locator.getAndMark( element );
                if( list != null )
                {
                    boolean shouldAdd = false;
                    Map<String, SiteProperty> newProperties = new HashMap<>();
                    for( SiteInfo siteInfo : list )
                    {
                        if( siteInfo.isAdded() )
                            continue;
                        shouldAdd = true;
                        String siteId = siteInfo.name;
                        Site element2 = allSites.get( siteId );
                        //new site from duplicate elements DynamicPropertySet
                        DynamicPropertySet nsffDPS = (DynamicPropertySet)element2.getProperties().clone();
                        newProperties = SiteProperty.copyDynamicProperties( propertiesToCopy, nsffDPS, newProperties );
                        siteInfo.setAdded( true );
                    }
                    if( shouldAdd )
                    {
                        Site newSiteFromInput = new SiteImpl( result, null, element.getType(), element.getBasis(), element.getStart(),
                                element.getLength(), element.getPrecision(), element.getStrand(), element.getOriginalSequence(),
                                element.getComment(), (DynamicPropertySet)element.getProperties().clone() );
                        //new site from input DynamicPropertySet
                        //actually this is necessary only for field parameters
                        DynamicPropertySet nsfiDPS = newSiteFromInput.getProperties();

                        newProperties.values().stream().map( cp -> cp.createDP( parameters.getAggregator() ) ).forEach( nsfiDPS::add );
                        result.addSite( newSiteFromInput );
                    }
                }
                else
                    result.addSite( element );
            }
            catch( Exception e )
            {
                log.log( Level.SEVERE, "Unable to add site: " + e.getMessage() );
            }
            return true;
        } );
        if( jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST )
        {
            result.getOrigin().remove( result.getName() );
            return null;
        }
        result.finalizeAddition();
        CollectionFactoryUtils.save( result );
        log.info( "Track created (" + result.getAllSites().getSize() + " sites)" );
        jobControl.popProgress();
        return result;
    }

    @SuppressWarnings ( "serial" )
    public static class Parameters extends AbstractAnalysisParameters
    {
        private DataElementPath inputTrack;
        private DataElementPath outputTrack;
        private String[] fieldNames;
        private boolean ignoreNaNInAggregator = true;
        private NumericAggregator aggregator = NumericAggregator.getAggregators()[0];

        @PropertyName ( "Input track" )
        @PropertyDescription ( "Track to reduce" )
        public DataElementPath getInputTrack()
        {
            return inputTrack;
        }
        public void setInputTrack(DataElementPath inputTrack)
        {
            Object oldValue = this.inputTrack;
            this.inputTrack = inputTrack;
            //drop selection if input track was changed
            if( inputTrack == null || !inputTrack.equals( oldValue ) )
            {
                setFieldNames( null );
            }
            firePropertyChange( "inputTrack", oldValue, inputTrack );
        }

        @PropertyName ( "Fields to compare" )
        @PropertyDescription ( "Consider sites equal when these fields values match aside from sites position and strand" )
        public String[] getFieldNames()
        {
            return fieldNames == null ? new String[0] : fieldNames;
        }

        public void setFieldNames(String[] fieldNames)
        {
            Object oldValue = this.fieldNames;
            this.fieldNames = fieldNames;
            firePropertyChange( "fieldNames", oldValue, fieldNames );
        }

        @PropertyName ( "Ignore empty values" )
        @PropertyDescription ( "Ignore empty values during aggregator work" )
        public boolean isIgnoreNaNInAggregator()
        {
            return ignoreNaNInAggregator;
        }
        public void setIgnoreNaNInAggregator(boolean ignoreNaNInAggregator)
        {
            boolean oldValue = this.ignoreNaNInAggregator;
            this.ignoreNaNInAggregator = ignoreNaNInAggregator;
            firePropertyChange( "ignoreNaNInAggregator", oldValue, ignoreNaNInAggregator );
            if( aggregator != null )
                aggregator.setIgnoreNaNs( ignoreNaNInAggregator );
        }

        @PropertyName ( "Aggregator for numbers" )
        @PropertyDescription ( "Function to be used for numerical properties when sites intervals intersects" )
        public NumericAggregator getAggregator()
        {
            return aggregator;
        }
        public void setAggregator(NumericAggregator aggregator)
        {
            if( aggregator != null )
                aggregator.setIgnoreNaNs( ignoreNaNInAggregator );
            Object oldValue = this.aggregator;
            this.aggregator = aggregator;
            firePropertyChange( "aggregator", oldValue, aggregator );
        }

        @PropertyName ( "Output track" )
        @PropertyDescription ( "Specify the location where to store results" )
        public DataElementPath getOutputTrack()
        {
            return outputTrack;
        }
        public void setOutputTrack(DataElementPath outputTrack)
        {
            Object oldValue = this.outputTrack;
            this.outputTrack = outputTrack;
            firePropertyChange( "outputTrack", oldValue, outputTrack );
        }
    }

    public static class ParametersBeanInfo extends BeanInfoEx2<Parameters>
    {
        public ParametersBeanInfo()
        {
            super( Parameters.class );
        }
        @Override
        protected void initProperties() throws Exception
        {
            add( DataElementPathEditor.registerInput( "inputTrack", beanClass, Track.class ) );
            add( "fieldNames", InputTrackMultiSelector.class );
            addExpert( "ignoreNaNInAggregator" );
            property( "aggregator" ).simple().editor( NumericAggregatorEditor.class ).add();
            add( OptionEx.makeAutoProperty( DataElementPathEditor.registerOutput( "outputTrack", beanClass, SqlTrack.class ),
                    "$inputTrack$ reduced" ) );
        }
    }

    public static class InputTrackMultiSelector extends TrackPropertiesMultiSelector
    {
        @Override
        protected DataElementPath getTrackPath()
        {
            return ( (Parameters)getBean() ).getInputTrack();
        }

        @Override
        protected DataElementPath getSkipTrackPath()
        {
            return null;
        }
    }
}
