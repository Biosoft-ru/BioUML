
package ru.biosoft.bsa.transformer;

import java.io.BufferedReader;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.AbstractTransformer;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.Entry;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.project.Project;
import ru.biosoft.bsa.project.ProjectAsLists;
import ru.biosoft.bsa.project.Region;
import ru.biosoft.bsa.project.TrackInfo;
import ru.biosoft.bsa.view.MapViewOptions;
import ru.biosoft.bsa.view.SiteViewOptions;
import ru.biosoft.util.BeanXMLUtils;

/**
 * Converts {@link Entry} to the {@link Project} and back.
 *
 */
public class ProjectTransformer extends AbstractTransformer<Entry, Project>
{
    protected static final Logger log = Logger.getLogger(ProjectTransformer.class.getName());

    @Override
    public Class<Entry> getInputType()
    {
        return Entry.class;
    }

    @Override
    public Class<Project> getOutputType()
    {
        return Project.class;
    }

    @Override
    public boolean isOutputType(Class<?> type)
    {
        return Project.class.isAssignableFrom(type);
    }

    ////////////////////////////////////////////////////////////////////////////

    /**
     * Converts Entry to the Project
     */
    @Override
    public Project transformInput(Entry entry) throws Exception
    {
        BufferedReader reader = new BufferedReader(entry.getReader());

        String line;
        Project project = null;
        Region region = null;
        TrackInfo trackInfo = null;
        StringBuffer viewOptionsStringBuffer = new StringBuffer();
        StringBuffer trackDataStringBuffer = null;
        int status = 0;//0 - read Project, 1 - read Region, 2 - read TrackInfo
        while( ( line = reader.readLine() ) != null )
        {
            String left = line.substring(0, 2);
            String right = line.substring(2).trim();
            if( status == 0 )
            {
                if( left.equals("ID") )
                {
                    project = new ProjectAsLists(right, getTransformedCollection());
                }
                else if( left.equals("DE") )
                {
                    if( right.length() > 0 )
                    {
                        if( project.getDescription() == null )
                        {
                            project.setDescription(right);
                        }
                        else
                        {
                            project.setDescription(project.getDescription() + "\n" + right);
                        }
                    }
                }
                else if( left.equals("RG") )
                {
                    status = 1;
                }
                else if( left.equals("TR") )
                {
                    status = 2;
                }
            }
            else if( status == 1 )
            {
                if( left.equals("RG") )
                {
                    status = 0;
                    project.addRegion(region);

                    try
                    {
                        Object viewOptions = BeanXMLUtils.fromXML( viewOptionsStringBuffer.toString() );
                        if( viewOptions instanceof MapViewOptions )
                        {
                            project.getViewOptions().addRegionViewOptions(region, (MapViewOptions)viewOptions);
                        }
                    }
                    catch( Exception e )
                    {
                        log.log(Level.SEVERE, e.getMessage(), e);
                    }

                    viewOptionsStringBuffer = new StringBuffer();
                }
                else if( left.equals("DB") )
                {
                    AnnotatedSequence map = DataElementPath.create(right).getDataElement(AnnotatedSequence.class);
                    region = new Region(map);
                }
                else if( left.equals("TI") )
                {
                    if( right.length() > 0 )
                    {
                        region.setTitle(right);
                    }
                }
                else if( left.equals("FR") )
                {
                    region.setFrom(Integer.parseInt(right));
                }
                else if( left.equals("TO") )
                {
                    region.setTo(Integer.parseInt(right));
                }
                else if( left.equals("OR") )
                {
                    region.setOrder(Integer.parseInt(right));
                }
                else if( left.equals("VI") )
                {
                    region.setVisible(Boolean.parseBoolean(right));
                }
                else if( left.equals("DE") )
                {
                    if( right.length() > 0 )
                    {
                        if( region.getDescription() == null )
                        {
                            region.setDescription(right);
                        }
                        else
                        {
                            region.setDescription(region.getDescription() + "\n" + right);
                        }
                    }
                }
                else if( left.equals("VO") )
                {
                    viewOptionsStringBuffer.append(right).append("\n");
                }
            }
            else if( status == 2 )
            {
                if( left.equals("TR") )
                {
                    status = 0;
                    if(trackDataStringBuffer != null)
                    {
                        try
                        {
                            Object track = BeanXMLUtils.fromXML( trackDataStringBuffer.toString() );
                            if(track instanceof Track)
                            {
                                TrackInfo newTrackInfo = new TrackInfo((Track)track);
                                newTrackInfo.setTitle(trackInfo.getTitle());
                                trackInfo = newTrackInfo;
                            }
                        }
                        catch(Exception e)
                        {
                            log.log(Level.SEVERE, e.getMessage(), e);
                        }
                    }
                    project.addTrack(trackInfo);

                    try
                    {
                        Object viewOptions = BeanXMLUtils.fromXML( viewOptionsStringBuffer.toString() );
                        if( viewOptions instanceof SiteViewOptions )
                        {
                            project.getViewOptions().addTrackViewOptions(trackInfo.getTrack().getCompletePath(), (SiteViewOptions)viewOptions);
                        }
                    }
                    catch( Exception e )
                    {
                        log.log(Level.SEVERE, e.getMessage(), e);
                    }

                    viewOptionsStringBuffer = new StringBuffer();
                    trackDataStringBuffer = null;
                }
                else if( left.equals("DB") )
                {
                    DataElementPath path = DataElementPath.create(right);
                    trackInfo = new TrackInfo(path.optDataElement(Track.class), path);
                }
                else if( left.equals("TI") )
                {
                    if( right.length() > 0 )
                    {
                        trackInfo.setTitle(right);
                    }
                }
                else if( left.equals("GR") )
                {
                    if( right.length() > 0 )
                    {
                        trackInfo.setGroup(right);
                    }
                }
                else if( left.equals("OR") )
                {
                    trackInfo.setOrder(Integer.parseInt(right));
                }
                else if( left.equals("VI") )
                {
                    trackInfo.setVisible(Boolean.parseBoolean(right));
                }
                else if( left.equals("DE") )
                {
                    if( right.length() > 0 )
                    {
                        if( trackInfo.getDescription() == null )
                        {
                            trackInfo.setDescription(right);
                        }
                        else
                        {
                            trackInfo.setDescription(trackInfo.getDescription() + "\n" + right);
                        }
                    }
                }
                else if( left.equals("VO") )
                {
                    viewOptionsStringBuffer.append(right).append("\n");
                }
                else if( left.equals("TD") )
                {
                    if(trackDataStringBuffer == null)
                        trackDataStringBuffer = new StringBuffer();
                    trackDataStringBuffer.append(right).append("\n");
                }
            }
        }

        return project;
    }

    /**
     * Converts Project to the Entry
     */
    @Override
    public Entry transformOutput(Project project) throws Exception
    {
        StringBuffer strBuf = new StringBuffer("ID   " + project.getName() + "\n");

        strBuf.append("DE   ");
        if( project.getDescription() != null )
        {
            strBuf.append(project.getDescription().replaceAll("\n", "\nDE   "));
        }
        strBuf.append("\n");

        for( Region region : project.getRegions() )
        {
            strBuf.append("RG\n");

            strBuf.append("DB   ");
            strBuf.append(region.getSequenceName());
            strBuf.append("\n");
            strBuf.append("TI   ");
            if( region.getTitle() != null )
            {
                strBuf.append(region.getTitle());
            }
            strBuf.append("\n");
            strBuf.append("FR   ");
            strBuf.append(region.getFrom());
            strBuf.append("\n");
            strBuf.append("TO   ");
            strBuf.append(region.getTo());
            strBuf.append("\n");
            strBuf.append("OR   ");
            strBuf.append(region.getOrder());
            strBuf.append("\n");
            strBuf.append("VI   ");
            strBuf.append(region.isVisible());
            strBuf.append("\n");
            strBuf.append("DE   ");
            if( region.getDescription() != null )
            {
                strBuf.append(region.getDescription().replaceAll("\n", "\nDE   "));
            }
            strBuf.append("\n");

            MapViewOptions viewOptions = project.getViewOptions().getRegionViewOptions();
            if( viewOptions != null )
            {
                String str =null;
                try
                {
                    str = BeanXMLUtils.toXML( viewOptions );
                }
                catch( Exception e )
                {
                    log.log(Level.SEVERE, e.getMessage(), e);
                }
                strBuf.append("VO  ");
                if( str != null )
                {
                    strBuf.append(str.replaceAll("\n", "\nVO   "));
                }
                strBuf.append("\n");
            }

            strBuf.append("RG\n");
        }

        for( TrackInfo trackInfo : project.getTracks() )
        {
            if(trackInfo.getTrack() == null)
                continue;
            strBuf.append("TR\n");

            strBuf.append("DB   ");
            strBuf.append(trackInfo.getDbName());
            strBuf.append("\n");
            strBuf.append("TI   ");
            if( trackInfo.getTitle() != null )
            {
                strBuf.append(trackInfo.getTitle());
            }
            strBuf.append("\n");
            strBuf.append("GR   ");
            if( trackInfo.getGroup() != null )
            {
                strBuf.append(trackInfo.getGroup());
            }
            strBuf.append("\n");
            strBuf.append("OR   ");
            strBuf.append(trackInfo.getOrder());
            strBuf.append("\n");
            strBuf.append("VI   ");
            strBuf.append(trackInfo.isVisible());
            strBuf.append("\n");
            strBuf.append("DE   ");
            if( trackInfo.getDescription() != null )
            {
                strBuf.append(trackInfo.getDescription().replaceAll("\n", "\nDE   "));
            }
            strBuf.append("\n");

            if(DataElementPath.create(trackInfo.getTrack()).optDataElement() != trackInfo.getTrack())
            {   // Custom track: store its beaninfo
                String str = null;
                try
                {
                    str = BeanXMLUtils.toXML( trackInfo.getTrack() );
                }
                catch( Exception e )
                {
                    log.log(Level.SEVERE, e.getMessage(), e);
                }
                strBuf.append("TD  ");
                if( str != null )
                {
                    strBuf.append(str.replaceAll("\n", "\nTD   "));
                }
                strBuf.append("\n");
            }

            SiteViewOptions viewOptions = project.getViewOptions().getTrackViewOptions(trackInfo.getTrack().getCompletePath());
            if( viewOptions != null )
            {
                strBuf.append( "VO  " );
                try
                {
                    String xml = BeanXMLUtils.toXML( viewOptions );
                    strBuf.append( xml.replaceAll("\n", "\nVO   ") );
                }
                catch( Exception e )
                {
                    log.log(Level.SEVERE, e.getMessage(), e);
                }
                strBuf.append("\n");
            }

            strBuf.append("TR\n");
        }

        strBuf.append("//\n");
        return new Entry(getPrimaryCollection(), project.getName(), "" + strBuf, Entry.TEXT_FORMAT);
    }
}
