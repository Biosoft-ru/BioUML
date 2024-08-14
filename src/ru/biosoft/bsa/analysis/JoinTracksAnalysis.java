package ru.biosoft.bsa.analysis;

import java.beans.PropertyDescriptor;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.Track;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.util.bean.StaticDescriptor;

/**
 * @author lan
 *
 */
@ClassIcon ( "resources/join-tracks.gif" )
public class JoinTracksAnalysis extends AnalysisMethodSupport<JoinTracksAnalysis.JoinTracksAnalysisParameters>
{
    public JoinTracksAnalysis(DataCollection<?> origin, String name)
    {
        super(origin, name, new JoinTracksAnalysisParameters());
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        final SqlTrack result = SqlTrack.createTrack( parameters.getOutput(), parameters.getTracks().first().getDataElement( Track.class ),
                getResultingTrackClass() );
        final PropertyDescriptor pd = StaticDescriptor.create("Source");
        jobControl.pushProgress(0, 95);
        jobControl.forCollection(parameters.getTracks(), trackPath -> {
            log.info("Writing "+trackPath.getName()+"...");
            jobControl.forCollection(DataCollectionUtils.asCollection((trackPath.getDataElement(Track.class)).getAllSites(), Site.class), element -> {
                Site newSite = new SiteImpl(result, null, element.getType(), element.getBasis(), element.getStart(),
                        element.getLength(), element.getPrecision(), element.getStrand(), element.getOriginalSequence(), element
                                .getComment(), (DynamicPropertySet)element.getProperties().clone());
                newSite.getProperties().add(new DynamicProperty(pd, String.class, trackPath.getName()));
                try
                {
                    result.addSite(newSite);
                }
                catch( Exception e )
                {
                    throw ExceptionRegistry.translateException(e);
                }
                return true;
            });
            return true;
        });
        if(jobControl.isStopped())
            parameters.getOutput().remove();
        log.info("Finalizing...");
        result.finalizeAddition();
        return result;
    }

    private Class<? extends Track> getResultingTrackClass()
    {
        Track first = parameters.getTracks().first().getDataElement( Track.class );
        Class<? extends Track> clazz = first.getClass();
        for( DataElementPath path : parameters.getTracks() )
        {
            Track tr = path.getDataElement( Track.class );
            if( !clazz.isAssignableFrom( tr.getClass() ) )
                clazz = tr.getClass();
        }
        return clazz;
    }

    @SuppressWarnings ( "serial" )
    public static class JoinTracksAnalysisParameters extends AbstractAnalysisParameters
    {
        private DataElementPathSet tracks;
        private DataElementPath output;

        @PropertyName("Tracks")
        @PropertyDescription("List of tracks you want to join")
        public DataElementPathSet getTracks()
        {
            return tracks;
        }

        public void setTracks(DataElementPathSet tracks)
        {
            Object oldValue = this.tracks;
            this.tracks = tracks;
            firePropertyChange("tracks", oldValue, tracks);
        }

        @PropertyName("Output path")
        @PropertyDescription("Where to store an output")
        public DataElementPath getOutput()
        {
            return output;
        }

        public void setOutput(DataElementPath output)
        {
            Object oldValue = this.output;
            this.output = output;
            firePropertyChange("output", oldValue, output);
        }
    }

    public static class JoinTracksAnalysisParametersBeanInfo extends BeanInfoEx2<JoinTracksAnalysisParameters>
    {
        public JoinTracksAnalysisParametersBeanInfo()
        {
            super(JoinTracksAnalysisParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            add( DataElementPathEditor.registerInputMulti( "tracks", beanClass, SqlTrack.class ) );
            property( "output" ).outputElement( SqlTrack.class ).auto( "$tracks/path$/Joined track" ).add();
        }
    }
}
