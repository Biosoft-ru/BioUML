package ru.biosoft.bsa.server;

import java.awt.Color;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.lang.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;

import ru.biosoft.access.BeanRegistry;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.access.security.SessionCache;
import ru.biosoft.access.security.SessionCacheManager;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.ChrNameMapping;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.Slice;
import ru.biosoft.bsa.SlicedSequence;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.TrackRegion;
import ru.biosoft.bsa.analysis.FilteredTrack;
import ru.biosoft.bsa.analysis.FilteredTrack.ModelTrackFilter;
import ru.biosoft.bsa.project.Project;
import ru.biosoft.bsa.project.ProjectAsLists;
import ru.biosoft.bsa.project.Region;
import ru.biosoft.bsa.project.TrackInfo;
import ru.biosoft.bsa.track.big.BigBedTrack;
import ru.biosoft.bsa.track.big.BigTrack;
import ru.biosoft.bsa.track.big.BigWigTrack;
import ru.biosoft.bsa.track.combined.CombinedTrack;
import ru.biosoft.bsa.view.MapViewOptions;
import ru.biosoft.bsa.view.SequenceView;
import ru.biosoft.bsa.view.SiteViewOptions;
import ru.biosoft.bsa.view.TaggedCompositeView;
import ru.biosoft.bsa.view.TrackBackgroundView;
import ru.biosoft.bsa.view.TrackViewBuilder;
import ru.biosoft.bsa.view.ViewFactory;
import ru.biosoft.bsa.view.ViewOptions;
import ru.biosoft.bsa.view.colorscheme.SiteColorScheme;
import ru.biosoft.bsa.view.sitelayout.SiteLayoutAlgorithm;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.exception.InternalException;
import ru.biosoft.graphics.ComplexTextView;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.font.ColorFont;
import ru.biosoft.server.ServiceSupport;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.templates.TemplateInfo;
import ru.biosoft.templates.TemplateRegistry;
import ru.biosoft.util.TextUtil2;

public class BSAService extends ServiceSupport
{
    @Override
    protected boolean processRequest(ServiceRequest request, int command) throws Exception
    {
        try
        {
            switch( command )
            {
                case BSAServiceProtocol.DB_TRACK_SITES:
                    sendTrackSites(request);
                    break;

                case BSAServiceProtocol.DB_SEQUENCE_START:
                    sendSequenceStart(request);
                    break;

                case BSAServiceProtocol.DB_SEQUENCE_LENGTH:
                    sendSequenceLength(request);
                    break;

                case BSAServiceProtocol.DB_SEQUENCE_PROPERTIES:
                    sendSequenceProperties(request);
                    break;

                case BSAServiceProtocol.DB_SEQUENCE_PART:
                    sendSequencePart(request);
                    break;

                case BSAServiceProtocol.DB_SEQUENCE_REGION:
                    sendSequenceRegion(request);
                    break;

                case BSAServiceProtocol.DB_TRACK_SITE_COUNT:
                    sendTrackSiteCount(request);
                    break;

                case BSAServiceProtocol.DB_TRACK_INDEX_LIST:
                    sendTrackIndexList( request );
                    break;

                case BSAServiceProtocol.DB_TRACK_SEARCH:
                    sendTrackSearch(request);
                    break;

                case BSAServiceProtocol.DB_TRACK_SITES_VIEW:
                    sendTrackSitesView(request);
                    break;

                case BSAServiceProtocol.DB_SITE_INFO:
                    sendSiteInfo(request);
                    break;

                case BSAServiceProtocol.DB_TRACK_SITE_NEAREST:
                    sendSiteNearest(request);
                    break;

                case BSAServiceProtocol.GET_SCHEME_LEGEND:
                    sendSchemeLegend(request);
                    break;

                case BSAServiceProtocol.DB_SAVE_PROJECT:
                    sendSaveProject(request);
                    break;
                //TODO: combined
                case BSAServiceProtocol.CREATE_COMBINED_TRACK:
                    sendCreateCombinedTrack( request );
                    break;

                default:
                    return false;
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "BSA.processRequest: ", e);
            request.error("Internal error while processing genome browser request: " + e.toString());
        }
        return true;
    }


    /**
     * Returns URL parameter as string by given name
     * @param request
     * @param paramName - name of the parameter
     * @return String containing parameter value
     */
    protected static String getParam(ServiceRequest request, String paramName)
    {
        String paramObj = request.get(paramName);
        if( paramObj == null )
            throw new InvalidParameterException("Missing parameter " + paramName);
        return paramObj;
    }

    protected DataElement getDataElement(ServiceRequest request) throws Exception
    {
        return CollectionFactory.getDataElement(getParam(request, BSAServiceProtocol.KEY_DE));
    }
    
    public static Track getTrack(ServiceRequest request)
    {
        String path = getParam(request, BSAServiceProtocol.KEY_DE);
        
        Properties props = new Properties();
        
        String chrNameMapping = request.get( BSAServiceProtocol.CHR_NAME_MAPPING );
        if(chrNameMapping != null)
            props.setProperty( ChrNameMapping.PROP_CHR_MAPPING, chrNameMapping );
        
        String sequence =  request.get( BSAServiceProtocol.SEQUENCE_NAME );
        if(sequence != null)
        {
            String sequencesCollection = DataElementPath.create( sequence ).getParentPath().toString();
            props.setProperty( Track.SEQUENCES_COLLECTION_PROPERTY, sequencesCollection );
        }
        
        return getTrack(path, props);
    }
    
    public static Track getTrack(String path)
    {
        return getTrack(path, new Properties());
    }
    
    public static Track getTrack(String path, Properties props)
    {
        if(BigTrack.isRemotePath( path ))
        {
            props.setProperty( BigBedTrack.PROP_BIGBED_PATH, path );
            try
            {
                if(path.toLowerCase().endsWith(".bb") || path.toLowerCase().endsWith(".bigbed"))
                    return new BigBedTrack<Site>( null, props );
                else if(path.toLowerCase().endsWith(".bw") || path.toLowerCase().endsWith(".bigwig"))
                    return new BigWigTrack( null, props );
            }
            catch( IOException e )
            {
                throw ExceptionRegistry.translateException( e );
            }
        }
        return (Track)CollectionFactory.getDataElement(path);
    }

    /**
     * Returns TrackRegion object constructed from arguments
     * @param request
     * @throws Exception
     */
    protected TrackRegion getTrackRegion(ServiceRequest request) throws Exception
    {
        Track track = getTrack(request);
        if( track == null )
            throw new InvalidParameterException("Unable to get track");
        String sequence = getParam(request, BSAServiceProtocol.SEQUENCE_NAME);
        int from = request.getInt(BSAServiceProtocol.FROM, 0);
        int to = request.getInt(BSAServiceProtocol.TO, 0);
        return ( from == 0 && to == 0 ) ? new TrackRegion(track, sequence) : new TrackRegion(track, sequence, from, to);
    }

    protected void sendTrackSites(ServiceRequest request) throws Exception
    {
        TrackRegion tr = getTrackRegion(request);
        DataCollection<Site> sites = tr.getSites();
        JSONArray sitesJSON = new JSONArray();
        JSONObject descriptorsJSON = new JSONObject();
        for( Site site: sites )
        {
            sitesJSON.put(serializeSite(site, descriptorsJSON));
        }
        JSONArray result = new JSONArray();
        result.put(descriptorsJSON);
        result.put(sitesJSON);
        request.send(result.toString());
    }

    protected JSONArray serializeSite(Site site, JSONObject descriptors) throws JSONException
    {
        JSONArray siteJSON = new JSONArray();
        siteJSON.put(site.getName());
        siteJSON.put(site.getBasis());
        siteJSON.put(site.getStart());
        siteJSON.put(site.getLength());
        siteJSON.put(site.getPrecision());
        siteJSON.put(site.getStrand());
        siteJSON.put(site.getType());
        JSONObject properties = new JSONObject();
        for( DynamicProperty property: site.getProperties() )
        {
            JSONObject descriptor = descriptors.optJSONObject(property.getName());
            if(descriptor == null)
            {
                descriptor = new JSONObject();
                descriptor.put("displayName", property.getDisplayName());
                descriptor.put("description", property.getShortDescription());
                descriptor.put("class", property.getType().getName());
                descriptors.put(property.getName(), descriptor);
            }
            properties.put(property.getName(), TextUtil2.toString(property.getValue()));
        }
        siteJSON.put(properties);
        return siteJSON;
    }

    protected void sendSequenceStart(ServiceRequest request) throws Exception
    {
        ru.biosoft.bsa.AnnotatedSequence map = (ru.biosoft.bsa.AnnotatedSequence)getDataElement(request);
        request.send(String.valueOf(map.getSequence().getStart()));
    }

    protected void sendSequenceLength(ServiceRequest request) throws Exception
    {
        ru.biosoft.bsa.AnnotatedSequence map = (ru.biosoft.bsa.AnnotatedSequence)getDataElement(request);
        request.send(String.valueOf(map.getSequence().getLength()));
    }

    protected void sendSequenceProperties(ServiceRequest request) throws Exception
    {
        ru.biosoft.bsa.AnnotatedSequence map = (ru.biosoft.bsa.AnnotatedSequence)getDataElement(request);
        JSONObject result = new JSONObject();
        result.put("start", map.getSequence().getStart());
        result.put("length", map.getSequence().getLength());
        request.send(result.toString());
    }

    protected void sendSequenceRegion(ServiceRequest request) throws Exception
    {
        ru.biosoft.bsa.AnnotatedSequence map = (ru.biosoft.bsa.AnnotatedSequence)getDataElement(request);
        Sequence sequence = map.getSequence();
        int from = request.getInt(BSAServiceProtocol.FROM, sequence.getStart());
        int to = request.getInt(BSAServiceProtocol.TO, sequence.getLength() + sequence.getStart());
        int fromSeq = from;
        int toSeq = to;

        if( fromSeq < sequence.getStart() )
            fromSeq = sequence.getStart();
        if( fromSeq > sequence.getStart() + sequence.getLength() )
            fromSeq = sequence.getStart() + sequence.getLength();
        if( toSeq < sequence.getStart() )
            toSeq = sequence.getStart();
        if( toSeq > sequence.getStart() + sequence.getLength() )
            toSeq = sequence.getStart() + sequence.getLength();
        SequenceView sequenceView = ViewFactory.createSequenceView(sequence, ( new MapViewOptions() ).getSequenceViewOptions(), fromSeq,
                toSeq, ApplicationUtils.getGraphics());
        StringBuffer resultString = new StringBuffer();
        resultString.append(String.valueOf(fromSeq));
        resultString.append(':');
        resultString.append(String.valueOf(toSeq));
        resultString.append(':');
        resultString.append(sequenceView.getSubSequence(fromSeq, toSeq));
        request.send(resultString.toString());
    }

    protected void sendSequencePart(ServiceRequest request) throws Exception
    {
        ru.biosoft.bsa.AnnotatedSequence map = (ru.biosoft.bsa.AnnotatedSequence)getDataElement(request);
        int position = request.getInt(BSAServiceProtocol.POSITION, 0);
        Sequence sequence = map.getSequence();
        int from, to;
        if( sequence instanceof SlicedSequence )
        {
            Slice slice = ( (SlicedSequence)sequence ).getSlice(position);
            from = slice.from;
            to = slice.to;
        }
        else
        {
            from = position;
            to = position + 1;
        }
        byte[] bytes = new byte[to - from];
        for( int i = from; i < to; i++ )
        {
            bytes[i - from] = sequence.getLetterAt(i);
        }
        StringBuffer resultString = new StringBuffer();
        resultString.append(String.valueOf(from));
        resultString.append(':');
        resultString.append(String.valueOf(to));
        resultString.append(':');
        resultString.append(new String(bytes));
        request.send(resultString.toString());
    }

    protected void sendTrackSiteCount(ServiceRequest request) throws Exception
    {
        TrackRegion tr = getTrackRegion(request);
        int size = tr.countSites();
        request.send(String.valueOf(size));
    }

    protected void sendTrackIndexList(ServiceRequest request) throws Exception
    {
        Track track = getTrack(request);
        if( track == null )
            throw new InvalidParameterException("Unable to get track");
        String result = String.join( ":", track.getIndexes());
        request.send( result );
    }
    
    protected void sendTrackSearch(ServiceRequest request) throws Exception
    {
        Track track = getTrack(request);
        if( track == null )
            throw new InvalidParameterException("Unable to get track");
        String query = getParam(request, BSAServiceProtocol.SEARCH_QUERY);
        String index = getParam( request, BSAServiceProtocol.SEARCH_INDEX );
        try
        {
            Site site = null;
            List<Site> queryResult = track.queryIndex( index, query );
            if(!queryResult.isEmpty())
               site = queryResult.get( 0 );
            JSONObject result = new JSONObject();
            result.put( "name", site.getName() );
            result.put( "chr", site.getSequence().getName() );
            result.put( "from", site.getFrom() );
            result.put( "to", site.getTo() );
            request.send(result.toString());
        }
        catch( Exception e )
        {
            request.error("Site not found");
        }
    }

    protected void sendTrackSitesView(ServiceRequest request) throws Exception
    {
        TrackRegion tr = getTrackRegion(request);
        float logScale = request.getFloat(BSAServiceProtocol.LOGSCALE, 0);

        ViewOptions viewOptions = getViewOptions( request );
        MapViewOptions mapViewOptions = viewOptions.getRegionViewOptions();
        SiteViewOptions siteViewOptions = getSiteViewOptions(request);
        String mode = request.get(BSAServiceProtocol.DISPLAY_MODE);
        if( mode != null && mode.equals("compact") )
            siteViewOptions.setTrackDisplayMode(SiteViewOptions.TRACK_MODE_COMPACT);
        else
            siteViewOptions.setTrackDisplayMode(SiteViewOptions.TRACK_MODE_FULL);
        viewOptions.semanticZoomSet(3 * Math.pow( 2, logScale));

        SequenceView sequenceView = ViewFactory.createSequenceView(tr.getSequenceObject(), mapViewOptions.getSequenceViewOptions(),
                tr.getFrom(), tr.getTo(), ApplicationUtils.getGraphics());
        JSONObject result = new JSONObject();
        try
        {
            DataCollection<Site> sites = tr.getSites();

            TrackViewBuilder viewBuilder = tr.getTrack().getViewBuilder();
            CompositeView trackView = viewBuilder.createTrackView(sequenceView, sites, siteViewOptions, tr.getFrom(), tr.getTo(), SiteLayoutAlgorithm.BOTTOM,
                    ApplicationUtils.getGraphics(), null);
            int width = sequenceView.getBounds().width;
            trackView.insert(new TrackBackgroundView(trackView.getBounds(), width), 0);
            TaggedCompositeView view = new TaggedCompositeView();
            view.add(trackView);
            List<String> tags = new ArrayList<>(view.setTags(siteViewOptions.getViewTagger()));
            Collections.sort(tags);

            result.put("view", view.toJSON());
            result.put("tags", tags);
        }
        catch( Exception e )
        {
            ExceptionRegistry.log( new InternalException( e, "Unable to render "+tr ) );
            CompositeView trackView = new CompositeView();
            String labelString = "Error: "+ExceptionRegistry.translateException( e ).getMessage();
            ComplexTextView errorLabel = new ComplexTextView( labelString, new ColorFont( "arial", 0, 12, new Color( 0x60, 0, 0 ) ),
                    new HashMap<>(), ComplexTextView.LEFT, 100, ApplicationUtils.getGraphics() );
            trackView.add(errorLabel);
            result.put("view", trackView.toJSON());
        }

        request.send(result.toString());
    }

    protected void sendSiteInfo(ServiceRequest request) throws Exception
    {
        TrackRegion tr = getTrackRegion(request);
        String siteIdStr = DataElementPath.create(getParam(request, BSAServiceProtocol.SITE_ID)).getName();
        try
        {
            Site site = tr.getSite(siteIdStr);
            String templateName = request.get( BSAServiceProtocol.TEMPLATE_NAME );
            TemplateInfo[] templates = TemplateRegistry.getSuitableTemplates( site );
            TemplateInfo templateInfo = Stream.of( templates ).filter( t -> t.getName().equals( templateName ) ).findFirst()
                    .orElse( templates[0] );
            String html = TemplateRegistry.mergeTemplate(site, templateInfo.getTemplate()).toString();
            html = html.replaceAll("href=\"de:([^\"]+)\"", "onclick=\"openDocument('$1');return false\" href=\"#\"");
            Pattern pattern = Pattern.compile("<img([^>]*) src=\"([^\"]+)\"");
            Matcher matcher = pattern.matcher(html);
            int start = 0;
            while(matcher.find(start))
            {
                html = html.substring( 0, matcher.start() ) + "<img" + matcher.group( 1 ) + " src=\"../biouml/web/img?id="
                        + TextUtil2.encodeURL( StringEscapeUtils.unescapeHtml( matcher.group( 2 ) ) ) + "\""
                        + html.substring( matcher.end() );
                start = matcher.end();
                matcher = pattern.matcher(html);
            }
            request.send(html);
            return;
        }
        catch( Exception e )
        {
            log.log( Level.SEVERE, e.getMessage(), e );
        }
        request.send(siteIdStr);
    }

    protected void sendSiteNearest(ServiceRequest request) throws Exception
    {
        Track track = getTrack(request);
        if( track == null )
            throw new InvalidParameterException("Unable to get track");
        String sequence = getParam(request, BSAServiceProtocol.SEQUENCE_NAME);
        try
        {
            Site site = null;
            int from = request.getInt(BSAServiceProtocol.FROM, 0);
            int to = request.getInt(BSAServiceProtocol.TO, 0);
            int step = 1000;
            String dir = getParam(request, BSAServiceProtocol.SEARCH_DIRECTION);

            if( dir.equals("up") )
            {
                DataCollection<Site> sites = new TrackRegion(track, sequence, from, from + 1).getSites();
                if( sites.getSize() > 0 )
                {
                    List<String> names = sites.getNameList();
                    Site tmpsite = sites.get(names.get(0));
                    sites = new TrackRegion(track, sequence, from, tmpsite.getTo()).getSites();
                    int newfrom = tmpsite.getTo();
                    if( sites.getSize() > 1 )
                    {
                        names = sites.getNameList();
                        int from_min = to;
                        for( String name : names )
                        {
                            Site tmpsite2 = sites.get(name);
                            if( tmpsite2.getFrom() > from && tmpsite2.getFrom() < from_min )
                            {
                                from_min = tmpsite2.getFrom();
                                site = tmpsite2;
                            }
                            else if( tmpsite2.getTo() > newfrom )
                            {
                                newfrom = tmpsite2.getTo();
                            }
                        }
                    }
                    from = newfrom + 1;
                }
                if( site == null )
                {
                    int fromCurrent = from;
                    int toCurrent = fromCurrent + step < to ? fromCurrent + step : to;
                    int nSites = track.getSites(sequence, fromCurrent, toCurrent).getSize();
                    while( toCurrent < to && nSites == 0 )
                    {
                        step *= 2;
                        fromCurrent = toCurrent;
                        toCurrent = toCurrent + step < to ? toCurrent + step : to;
                        nSites = track.getSites(sequence, fromCurrent, toCurrent).getSize();
                    }
                    sites = new TrackRegion(track, sequence, fromCurrent, toCurrent).getSites();
                    int from_min = to;
                    for( Site tmpsite : sites )
                    {
                        if( from_min > tmpsite.getFrom() )
                        {
                            from_min = tmpsite.getFrom();
                            site = tmpsite;
                        }
                    }
                }
            }
            else
            {
                to--;
                int fromCurrent = to - step > from ? to - step : from;
                int toCurrent = to;
                int nSites = track.getSites(sequence, fromCurrent, toCurrent).getSize();
                while( fromCurrent > from && nSites == 0 )
                {
                    step *= 2;
                    toCurrent = fromCurrent;
                    fromCurrent = fromCurrent - step > from ? fromCurrent - step : from;
                    nSites = track.getSites(sequence, fromCurrent, toCurrent).getSize();
                }
                TrackRegion tr = new TrackRegion(track, sequence, fromCurrent, toCurrent);
                DataCollection<Site> sites = tr.getSites();
                int to_max = from;
                for( Site tmpsite : sites )
                {
                    if( to_max < tmpsite.getTo() )
                    {
                        to_max = tmpsite.getTo();
                        site = tmpsite;
                    }
                }

            }
            String result = site.getName() + ":" + site.getFrom() + ":" + site.getTo();
            request.send(result);
        }
        catch( Exception e )
        {
            request.error(e.getMessage() == null?"Site not found":"Site not found: "+e.getMessage());
        }
    }

    protected void sendSchemeLegend(ServiceRequest request) throws Exception
    {
        SiteViewOptions siteViewOptions = getSiteViewOptions(request);
        SiteColorScheme scheme = siteViewOptions.getColorScheme();
        CompositeView legendView = scheme.getLegend(ApplicationUtils.getGraphics());
        legendView.setLocation(1, 1);
        CompositeView view = new CompositeView();
        view.add(legendView);
        request.send(view.toJSON().toString());
    }

    protected void sendSaveProject(ServiceRequest request) throws Exception
    {
        DataElementPath sequence = DataElementPath.create(getParam(request, BSAServiceProtocol.SEQUENCE_NAME));
        DataElementPath targetPath = DataElementPath.create(getParam(request, BSAServiceProtocol.TARGET_PATH));
        DataElementPath projectPath = DataElementPath.create( request.get( BSAServiceProtocol.PROJECT ) );
        AnnotatedSequence sequenceMap = sequence.getDataElement(AnnotatedSequence.class);
        JSONArray tracks = new JSONArray(getParam(request, BSAServiceProtocol.TRACKS));
        int from = request.getInt(BSAServiceProtocol.FROM, -1);
        int to = request.getInt(BSAServiceProtocol.TO, -1);
        Project result = new ProjectAsLists(targetPath.getName(), targetPath.optParentCollection());
        Region region = new Region(sequenceMap);
        region.setFrom(from);
        region.setTo(to);
        result.addRegion(region);

        for(int i=0; i<tracks.length(); i++)
        {
            JSONArray trackJSON = tracks.getJSONArray(i);
            DataElementPath trackPath = DataElementPath.create(trackJSON.getString(0));
            TrackInfo trackInfo = new TrackInfo( trackPath.getDataElement( Track.class ) );
            trackInfo.setTitle(trackJSON.getString(1));
            trackInfo.setOrder( i );
            trackInfo.setVisible( trackJSON.getBoolean( 2 ) );
            result.addTrack(trackInfo);
            SiteViewOptions viewOptions = getSiteViewOptionsForTrack( SessionCacheManager.getSessionCache(), trackPath, projectPath );
            result.getViewOptions().addTrackViewOptions( trackPath, viewOptions );
        }
        targetPath.save(result);
        SessionCacheManager.getSessionCache().removeObject( targetPath.toString() );
        request.send("ok");
    }

    private ViewOptions getViewOptions(ServiceRequest request)
    {
        String projectPath = request.get( BSAServiceProtocol.PROJECT );
        if(projectPath == null)
            return new ViewOptions();

        String sessionID = getParam(request, SecurityManager.SESSION_ID);
        SessionCache sessionCache = SessionCacheManager.getSessionCache(sessionID);

        String sessionCachePath = BSAServiceProtocol.VIEWOPTIONS_BEAN + projectPath;
        ViewOptions viewOptions = (ViewOptions)sessionCache.getObject(sessionCachePath);
        if(viewOptions == null)
        {
            Project project = DataElementPath.create( projectPath ).getDataElement( Project.class );
            viewOptions = project.getViewOptions();
            sessionCache.addObject(sessionCachePath, viewOptions, true);
        }
        return viewOptions;
    }

    protected SiteViewOptions getSiteViewOptions(ServiceRequest request)
    {
        DataElementPath trackPath = DataElementPath.create( getParam(request, BSAServiceProtocol.KEY_DE) );
        DataElementPath projectPath = DataElementPath.create( request.get( BSAServiceProtocol.PROJECT ) );
        String sessionID = getParam(request, SecurityManager.SESSION_ID);
        SessionCache sessionCache = SessionCacheManager.getSessionCache(sessionID);
        return getSiteViewOptionsForTrack(sessionCache, trackPath, projectPath);
    }

    public static DynamicPropertySet getTableColorSchemes(SessionCache sessionCache, TableDataCollection table)
    {
        DynamicPropertySet result = new DynamicPropertySetAsMap();
        if(table.getSize() > 0)
        {
            RowDataElement row = table.getAt(0);
            for(Object value: row.getValues(true))
            {
                if(value instanceof Project)
                {
                    for(TrackInfo ti: ((Project)value).getTracks())
                    {
                        try
                        {
                            SiteColorScheme colorScheme = getSiteViewOptionsForTrack(sessionCache, DataElementPath.create(ti.getTrack()), null).getColorScheme();
                            if(colorScheme != null)
                            {
                                result.add(new DynamicProperty(ti.getTitle(), colorScheme.getClass(), colorScheme));
                            }
                        }
                        catch( Exception e )
                        {
                        }
                    }
                }
            }
        }
        return result;
    }

    public static SiteViewOptions getSiteViewOptionsForTrack(SessionCache sessionCache, DataElementPath trackPath, DataElementPath projectPath)
    {
        return (SiteViewOptions)BeanRegistry.getBean( SiteViewOptionsProvider.getBeanPath( projectPath, trackPath ), sessionCache );
    }

    private void sendCreateCombinedTrack(ServiceRequest request) throws Exception
    {

        DataElementPath targetPath = DataElementPath.create( getParam( request, BSAServiceProtocol.TARGET_PATH ) );
        JSONArray tracks = new JSONArray( getParam( request, BSAServiceProtocol.TRACKS ) );
        List<Track> tracksList = new ArrayList<>();
        for( int i = 0; i < tracks.length(); i++ )
        {
            JSONArray trackJSON = tracks.getJSONArray( i );
            DataElementPath trackPath = DataElementPath.create( trackJSON.getString( 0 ) );
            Track track = trackPath.getDataElement( Track.class );
            if( track instanceof CombinedTrack )
                continue;
            tracksList.add( track );
        }
        DataElementPath sequence = DataElementPath.create( getParam( request, BSAServiceProtocol.SEQUENCE_NAME ) );
        DataElementPath sequencesColPath = sequence != null ? sequence.getParentPath() : null;
        CombinedTrack result = CombinedTrack.createTrack( targetPath, tracksList, sequencesColPath );
        targetPath.save( result );
        request.send( "ok" );
    }

}
