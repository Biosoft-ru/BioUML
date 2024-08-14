package ru.biosoft.bsa.server;

import java.util.logging.Logger;

import ru.biosoft.access.CacheableBeanProvider;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.project.Project;
import ru.biosoft.bsa.view.SiteViewOptions;
import ru.biosoft.util.TextUtil;

public class SiteViewOptionsProvider implements CacheableBeanProvider
{
    private static final Logger log = Logger.getLogger( SiteViewOptionsProvider.class.getName() );
    private static final String PREFIX = "bsa/siteviewoptions";
    @Override
    public Object getBean(String path)
    {
        String[] params = TextUtil.split( path, ';' );
        if(params.length == 1)
        {
            Track track = BSAService.getTrack(params[0]);
            SiteViewOptions viewOptions = track.getViewBuilder().createViewOptions();
            viewOptions.initFromTrack( track );
            return viewOptions;
        }
        else
        {
            DataElementPath projectPath = DataElementPath.create( params[0] );
            DataElementPath trackPath = DataElementPath.create( params[1] );
            Project project = projectPath.getDataElement( Project.class );
            return project.getViewOptions().getTrackViewOptions( trackPath );
        }
    }
    
    public static String getBeanPath(DataElementPath projectPath, DataElementPath trackPath)
    {
        return PREFIX + "/" + (projectPath == null ? "" : (projectPath + ";")) + trackPath;
    }
}
