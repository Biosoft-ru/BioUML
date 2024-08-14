package ru.biosoft.bsa.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import one.util.streamex.StreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.SqlDataInfo;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataCollectionInfo;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementDescriptor;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.bsa.VCFSqlTrack;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;

public class ConvertTracksToVCF extends AnalysisMethodSupport<ConvertTracksToVCF.ConvertTracksToVCFParameters>
{
    private static final String ALL_PROJECTS = "(all user projects)";
    List<ru.biosoft.access.core.DataElementPath> vcfTracks = null;

    public ConvertTracksToVCF(DataCollection<?> origin, String name)
    {
        super( origin, name, new ConvertTracksToVCFParameters() );
        vcfTracks = new ArrayList<>();
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        String[] projects = parameters.getProjects();
        if(projects == null || projects.length == 0)
        {
            log.warning("No projects were selected, exiting");

        }
        if( StreamEx.of( projects ).anyMatch( ALL_PROJECTS::equals ) )
            projects = CollectionFactoryUtils.getUserProjectsPath().getDataCollection().names().toArray( String[]::new );

        DataElementPath userPrjPath = CollectionFactoryUtils.getUserProjectsPath();
        DataElementPath dataPath = userPrjPath.getParentPath();
        jobControl.forCollection(Arrays.asList(projects), projectName -> {
            log.info( "Searching for tracks in project " + projectName );
            DataElementPath projectPath = DataElementPath.create( projectName ).getDepth() == 1 ? userPrjPath.getChildPath( projectName )
                    : dataPath.getRelativePath( projectName );
            DataCollection<? extends DataElement> project = projectPath
                    .getChildPath( "Data" ).optDataCollection();
            if( project == null )
            {
                log.warning( "Project " + projectName + ": not found; skipping" );
                return true;
            }
            vcfTracks.clear();
            findInFolder( project );
            if( parameters.isActualConvert() )
            {
                log.info( "Converting tracks for project " + projectName );
                jobControl.forCollection( vcfTracks, trackPath -> {
                    convertTrack( trackPath );
                    return true;
                } );
            }
            return true;
        });
        return null;
    }

    private void convertTrack(DataElementPath trackPath)
    {
        SqlTrack originalTrack = trackPath.getDataElement( SqlTrack.class );
        SqlTrack result;
        try
        {
            Properties properties = new Properties();
            properties.put( DataCollectionConfigConstants.NAME_PROPERTY, trackPath.getName() );
            DataCollectionInfo info = ( (DataCollection<?>)originalTrack ).getInfo();
            Object labelProperty = info.getProperty( "label" );
            if( labelProperty != null )
                properties.put( "label", labelProperty );
            if( info.getProperty( Track.SEQUENCES_COLLECTION_PROPERTY ) != null )
                properties.setProperty( Track.SEQUENCES_COLLECTION_PROPERTY, info.getProperty( Track.SEQUENCES_COLLECTION_PROPERTY ) );
            properties.setProperty( SqlDataInfo.ID_PROPERTY, info.getProperty( SqlDataInfo.ID_PROPERTY ) );

            result = new VCFSqlTrack( trackPath.optParentCollection(), properties );

            DataCollectionUtils.copyPersistentInfo( result, (DataCollection<?>)originalTrack );
            result.getInfo().setNodeImageLocation( SqlTrack.class, "resources/trackvcf.png" );
            DataCollectionUtils.copyAnalysisParametersInfo( originalTrack, result );
            CollectionFactoryUtils.save( result );
        }
        catch( Exception e )
        {
        }

    }

    private void findInFolder(DataCollection<? extends DataElement> folder)
    {
        DataElementPath folderPath = folder.getCompletePath();
        List<String> folderItems = folder.getNameList();
        DataCollection<?> folderDC = folderPath.getDataCollection();
        jobControl.forCollection( folderItems, name -> {
            DataElementPath dePath = folderPath.getChildPath( name );
            DataElementDescriptor desc = folderDC.getDescriptor( name );
            Class<? extends DataElement> clazz = null;
            if( desc != null )
                clazz = desc.getType();
            else
            {
                DataElement de = dePath.optDataElement();
                if( de == null )
                {
                    if( parameters.isVerbose() )
                        log.info( "Can not get data element for path " + dePath.toString() );
                    return true;
                }
                clazz = de.getClass();
            }
            if( SqlTrack.class.isAssignableFrom( clazz ) && !VCFSqlTrack.class.isAssignableFrom( clazz ) )
            {
                DataElement de = dePath.optDataElement();
                if( isVCF( (SqlTrack)de ) )
                {
                    vcfTracks.add( dePath );
                    if( parameters.isVerbose() )
                        log.info( "VCF track found: " + dePath.toString() );
                }
            }
            else if( FolderCollection.class.isAssignableFrom( clazz ) )
            {
                DataElement de = dePath.optDataElement();
                findInFolder( (FolderCollection)de );
            }
            return true;
        } );
      
    }

    private boolean isVCF(SqlTrack track)
    {
        List<String> props = TrackUtils.getTrackSitesProperties( track );
        if( props.contains( "RefAllele" ) && props.contains( "AltAllele" ) )
            return true;
        else
            return false;
    }

    public static class ConvertTracksToVCFParameters extends AbstractAnalysisParameters
    {
        String[] projects = new String[0];
        boolean actualConvert = true;
        boolean verbose = true;

        @PropertyName ( "Projects to convert" )
        @PropertyDescription ( "List of projects in which tracks having appropriate properties will be converted to VCF tracks" )
        public String[] getProjects()
        {
            return projects;
        }
        public void setProjects(String[] projects)
        {
            Object oldValue = this.projects;
            this.projects = projects;
            firePropertyChange( "projects", oldValue, projects );
        }

        @PropertyName ( "Perform converting" )
        @PropertyDescription ( "Whether to actually convert tracks to VCF tracks or print statistics" )
        public boolean isActualConvert()
        {
            return actualConvert;
        }
        public void setActualConvert(boolean actualConvert)
        {
            Object oldValue = this.actualConvert;
            this.actualConvert = actualConvert;
            firePropertyChange( "actualConvert", oldValue, actualConvert );
        }

        @PropertyName ( "Print track names" )
        @PropertyDescription ( "Whether to print track names" )
        public boolean isVerbose()
        {
            return verbose;
        }
        public void setVerbose(boolean verbose)
        {
            Object oldValue = this.verbose;
            this.verbose = verbose;
            firePropertyChange( "verbose", oldValue, verbose );
        }
    }

    public static class ConvertTracksToVCFParametersBeanInfo extends BeanInfoEx
    {
        public ConvertTracksToVCFParametersBeanInfo()
        {
            super( ConvertTracksToVCFParameters.class, true );
        }

        @Override
        protected void initProperties() throws Exception
        {
            add( "projects", AdvancedProjectsSelector.class );
            add( "verbose" );
            add( "actualConvert" );
        }
    }

    public static class AdvancedProjectsSelector extends GenericMultiSelectEditor
    {
        @Override
        protected String[] getAvailableValues()
        {
            List<String> projects = new ArrayList<>();
            projects.add( ALL_PROJECTS );
            DataElementPath userPrjPath = CollectionFactoryUtils.getUserProjectsPath();
            projects.addAll( userPrjPath.getDataCollection().names().collect( Collectors.toList() ) );

            DataElementPath dataPath = userPrjPath.getParentPath();
            DataCollection<DataElement> data = dataPath.getDataCollection();
            data.names().filter( p -> !p.equals( userPrjPath.getName() ) ).forEach( name -> {
                try
                {
                    DataElementPath path = dataPath.getChildPath( name );
                    DataCollection<DataElement> dc = path.optDataCollection( ru.biosoft.access.core.DataElement.class );
                    if( dc != null )
                        dc.names().forEach( innerName -> {
                            projects.add( DataElementPath.create( name, innerName ).toString() );
                        } );
                }
                catch( Exception e )
                {
                }

            } );
            return projects.toArray( new String[0] );
        }
    }
}
