package ru.biosoft.bsa.analysis;

import java.beans.PropertyDescriptor;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

import one.util.streamex.StreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.analysis.trackutil.SiteInfo;
import ru.biosoft.bsa.analysis.trackutil.SiteProperty;
import ru.biosoft.bsa.analysis.trackutil.VariableSiteLocator;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.util.bean.StaticDescriptor;

/**
 * @author lan
 *
 */
@ClassIcon("resources/filter-one-track-by-another.gif")
public class FilterTrackAnalysis extends AnalysisMethodSupport<FilterTrackAnalysisParameters>
{
    private static final PropertyDescriptor SOURCE_TRACK_DESCRIPTOR = StaticDescriptor.create( "Source" );

    public FilterTrackAnalysis(DataCollection<?> origin, String name)
    {
        super(origin, name, JavaScriptBSA.class, new FilterTrackAnalysisParameters());
    }

    @Override
    public Track justAnalyzeAndPut() throws Exception
    {
        Track input = parameters.getInputTrack().getDataElement(Track.class);
        Track filter = parameters.getFilterTrack().getDataElement(Track.class);
        final VariableSiteLocator locator = new VariableSiteLocator( parameters.getFieldNames(), parameters.getMaxDistance() );
        log.info("Reading filter track...");
        jobControl.pushProgress(0, 40);
        final DataCollection<Site> allSites = filter.getAllSites();
        jobControl.forCollection( DataCollectionUtils.asCollection( allSites, Site.class ), element -> {
            locator.put( element );
            return true;
        } );
        if(jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST) return null;
        jobControl.popProgress();

        log.info("Initializing resulting track...");
        final SqlTrack result = SqlTrack.createTrack( parameters.getOutputTrack(), input, input.getClass() );
        jobControl.pushProgress(40, 99);
        final Set<String> selectedFilterTrackProperties = StreamEx.of( parameters.getSelectedFilterFieldNames() ).toSet();
        log.info("Filtering...");
        jobControl.forCollection(DataCollectionUtils.asCollection(input.getAllSites(), Site.class),
                element -> {
                    try
                    {
                        if( parameters.getMode().equals(FilterTrackAnalysisParameters.MODE_INTERSECT) )
                        {
                            String siteID = locator.find( element );
                            if( siteID != null )
                            {
                                Site newSite = new SiteImpl( result, null, element.getType(), element.getBasis(), element.getStart(),
                                        element.getLength(), element.getPrecision(), element.getStrand(), element.getOriginalSequence(),
                                        element.getComment(), (DynamicPropertySet)element.getProperties().clone() );
                                DynamicPropertySet newSiteDPS = newSite.getProperties();
                                newSiteDPS.add( new DynamicProperty( SOURCE_TRACK_DESCRIPTOR, String.class,
                                        parameters.getInputTrack().getName() ) );

                                Map<String, SiteProperty> newProperties = new HashMap<>();
                                newProperties = SiteProperty.copyDynamicProperties( selectedFilterTrackProperties, allSites.get( siteID ).getProperties(), newProperties );
                                newProperties.values().stream().map( cp -> cp.createDP( parameters.getAggregator() ) )
                                        .forEach( newSiteDPS::add );

                                result.addSite( newSite );
                            }
                        }
                        else if( parameters.getMode().equals(FilterTrackAnalysisParameters.MODE_INTERSECT_BOTH_SITES) )
                        {
                            Collection<SiteInfo> list = locator.getAndMark( element );
                            if( list != null )
                            {
                                Site newSiteFromInput = new SiteImpl(result, null, element.getType(), element.getBasis(), element.getStart(),
                                        element.getLength(), element.getPrecision(), element.getStrand(), element.getOriginalSequence(), element
                                                .getComment(), (DynamicPropertySet)element.getProperties().clone());
                                //new site from input DynamicPropertySet
                                DynamicPropertySet nsfiDPS = newSiteFromInput.getProperties();
                                nsfiDPS.add( new DynamicProperty( SOURCE_TRACK_DESCRIPTOR, String.class,
                                        parameters.getInputTrack().getName() ) );

                                Map<String, SiteProperty> newProperties = new HashMap<>();
                                for( SiteInfo siteInfo : list )
                                {
                                    String siteId = siteInfo.name;
                                    Site element2 = allSites.get( siteId );
                                    //new site from filter DynamicPropertySet
                                    DynamicPropertySet nsffDPS = (DynamicPropertySet)element2.getProperties().clone();
                                    newProperties = SiteProperty.copyDynamicProperties( selectedFilterTrackProperties, nsffDPS, newProperties );
                                    if( !siteInfo.isAdded() )
                                    {
                                        Site newSiteFromFilter = new SiteImpl( result, null, element2.getType(), element2.getBasis(),
                                                element2.getStart(), element2.getLength(), element2.getPrecision(), element2.getStrand(),
                                                element2.getOriginalSequence(), element2.getComment(), nsffDPS );
                                        nsffDPS = newSiteFromFilter.getProperties();
                                        nsffDPS.add( new DynamicProperty( SOURCE_TRACK_DESCRIPTOR, String.class,
                                                parameters.getFilterTrack().getName() ) );
                                        result.addSite( newSiteFromFilter );
                                        siteInfo.setAdded( true );
                                    }
                                }
                                newProperties.values().stream().map( cp -> cp.createDP( parameters.getAggregator() ) )
                                        .forEach( nsfiDPS::add );
                                result.addSite( newSiteFromInput );
                            }
                        }
                        else if( parameters.getMode().equals(FilterTrackAnalysisParameters.MODE_SUBTRACT) )
                        {
                            if( !locator.contains( element ) )
                            {
                                result.addSite(element);
                            }
                        }
                    }
                    catch( Exception e )
                    {
                        log.log(Level.SEVERE, "Unable to add site: " + e.getMessage());
                    }
                    return true;
                });
        if(jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST)
        {
            result.getOrigin().remove(result.getName());
            return null;
        }
        result.finalizeAddition();
        CollectionFactoryUtils.save(result);
        log.info("Track created ("+result.getAllSites().getSize()+" sites)");
        jobControl.popProgress();
        return result;
    }
}
