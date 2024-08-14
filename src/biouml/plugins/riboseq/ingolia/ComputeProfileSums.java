package biouml.plugins.riboseq.ingolia;

import java.util.List;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.riboseq.transcripts.Transcript;
import biouml.plugins.riboseq.transcripts.TranscriptLoader;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.BAMTrack;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.util.bean.JSONBean;

public class ComputeProfileSums extends AnalysisMethodSupport<ComputeProfileSums.Parameters>
{
    private ProfileBuilder profileBuilder;
    private AlignmentConverter alignmentConverter;
    private TableDataCollection resultingTable;

    public ComputeProfileSums(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        jobControl.pushProgress( 0, 10 );
        TranscriptLoader loader = parameters.getTranscriptSet().createTranscriptLoader();
        List<Transcript> transcripts = loader.loadTranscripts( log );
        profileBuilder = parameters.createProfileBuilder();
        alignmentConverter = parameters.createAlignmentConverter();
        jobControl.popProgress();

        jobControl.pushProgress( 10, 90 );
        resultingTable = TableDataCollectionUtils.createTableDataCollection( parameters.getResultPath() );
        for( ProfileSumParameters p : parameters.getSums() )
            resultingTable.getColumnModel().addColumn( p.toString(), Integer.class );

        jobControl.forCollection( transcripts, t -> {
            processTranscript( t );
            return true;
        } );
        jobControl.popProgress();

        jobControl.pushProgress( 90, 100 );
        resultingTable.finalizeAddition();
        jobControl.popProgress();

        return resultingTable;
    }

    private void processTranscript(Transcript t)
    {
        BAMTrack[] samples = parameters.getBAMTracks();

        int[] profile = new int[t.getLength()];
        for( BAMTrack sample : samples )
        {
            List<AlignmentOnTranscript> aligns = alignmentConverter.getTranscriptAlignments( t, sample );
            int[] sampleProfile = profileBuilder.computeProfile( aligns, t.getLength() );
            for( int i = 0; i < t.getLength(); i++ )
            {
                int v = sampleProfile[i];
                profile[i] += v;
            }
        }

        Integer[] values = new Integer[parameters.getSums().length];
        for( int i = 0; i < parameters.getSums().length; i++ )
        {
            ProfileSumParameters p = parameters.getSums()[i];
            if( !p.isApplicable( t ) )
                continue;
            int fromPosition = p.getFromPosition( t );
            int toPosition = p.getToPosition( t );
            if( toPosition < fromPosition || fromPosition < 0 || toPosition >= t.getLength() )
                continue;
            int sum = 0;
            if(p.isInFrame())
            {
                for( int j = fromPosition + p.getFrame(); j <= toPosition; j += 3 )
                    sum += profile[j];
            }
            else
            {
                for( int j = fromPosition; j <= toPosition; j++ )
                    sum += profile[j];
            }
            values[i] = sum;
        }
        TableDataCollectionUtils.addRow( resultingTable, t.getName(), values, true );
    }

    public static class ProfileSumParameters implements JSONBean
    {
        public static final String CDS_END = "CDS end";
        public static final String CDS_START = "CDS start";
        public static final String TRANSCRIPT_END = "Transcript end";
        public static final String TRANSCRIPT_START = "Transcript start";
        static final String[] ANCHORS = {TRANSCRIPT_START, TRANSCRIPT_END, CDS_START, CDS_END};

        private String from = CDS_START;
        private int fromOffset;
        private String to = CDS_END;
        private int toOffset;
        private boolean inFrame;
        private int frame;

        @PropertyName ( "From" )
        @PropertyDescription ( "From" )
        public String getFrom()
        {
            return from;
        }
        public void setFrom(String from)
        {
            this.from = from;
        }

        @PropertyName ( "From offset" )
        @PropertyDescription ( "Add this value to 'From' position" )
        public int getFromOffset()
        {
            return fromOffset;
        }
        public void setFromOffset(int fromOffset)
        {
            this.fromOffset = fromOffset;
        }

        @PropertyName ( "To" )
        @PropertyDescription ( "To" )
        public String getTo()
        {
            return to;
        }
        public void setTo(String to)
        {
            this.to = to;
        }

        @PropertyName ( "To offset" )
        @PropertyDescription ( "Add this value to 'To' position" )
        public int getToOffset()
        {
            return toOffset;
        }
        public void setToOffset(int toOffset)
        {
            this.toOffset = toOffset;
        }

        @PropertyName ( "In frame" )
        @PropertyDescription ( "Count only footprints in specific frame" )
        public boolean isInFrame()
        {
            return inFrame;
        }
        public void setInFrame(boolean inFrame)
        {
            this.inFrame = inFrame;
        }

        @PropertyName ( "Frame" )
        @PropertyDescription ( "Frame 0, 1 or 2" )
        public int getFrame()
        {
            return frame;
        }
        public void setFrame(int frame)
        {
            this.frame = frame;
        }
        public boolean isFrameHidden()
        {
            return !isInFrame();
        }

        public boolean isApplicable(Transcript t)
        {
            if( !t.isCoding() )
                if( CDS_START.equals( from ) || CDS_END.equals( from ) || CDS_START.equals( to ) || CDS_END.equals( to ) )
                    return false;
            return true;
        }

        private int getTranscriptPosition(Transcript t, String anchor, int offset)
        {
            int res;
            switch( anchor )
            {
                case TRANSCRIPT_START:
                    res = 0;
                    break;
                case TRANSCRIPT_END:
                    res = t.getLength() - 1;
                    break;
                case CDS_START:
                    res = t.getCDSLocations().get( 0 ).getFrom();
                    break;
                case CDS_END:
                    res = t.getCDSLocations().get( 0 ).getTo();
                    break;
                default:
                    throw new IllegalArgumentException();
            }
            res += offset;
            return res;
        }

        public int getFromPosition(Transcript t)
        {
            return getTranscriptPosition( t, from, fromOffset );
        }

        public int getToPosition(Transcript t)
        {
            return getTranscriptPosition( t, to, toOffset );
        }

        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();
            sb.append( "from " ).append( from );
            if( fromOffset < 0 )
                sb.append( String.valueOf( fromOffset ) );
            else if( fromOffset > 0 )
                sb.append( '+' ).append( String.valueOf( fromOffset ) );
            sb.append( " to " ).append( to );
            if( toOffset < 0 )
                sb.append( String.valueOf( toOffset ) );
            else if( toOffset > 0 )
                sb.append( "+" ).append( String.valueOf( toOffset ) );
            if( inFrame )
                sb.append( " in frame " ).append( String.valueOf( frame ) );
            return sb.toString();
        }
    }

    public static class ProfileSumParametersBeanInfo extends BeanInfoEx2<ProfileSumParameters>
    {
        public ProfileSumParametersBeanInfo()
        {
            super( ProfileSumParameters.class );
        }

        @Override
        protected void initProperties() throws Exception
        {
            property( "from" ).tags( ProfileSumParameters.ANCHORS ).add();
            property( "fromOffset" ).add();
            property( "to" ).tags( ProfileSumParameters.ANCHORS ).add();
            property( "toOffset" ).add();
            property( "inFrame" ).add();
            property( "frame" ).hidden( "isFrameHidden" ).add();
            ;
        }
    }

    public static class Parameters extends CoreParametersWithASiteTable
    {
        private ProfileSumParameters[] sums = new ProfileSumParameters[] {new ProfileSumParameters()};
        private DataElementPath resultPath;

        @PropertyName ( "Profile sums" )
        @PropertyDescription ( "Profile sums" )
        public ProfileSumParameters[] getSums()
        {
            return sums;
        }
        public void setSums(ProfileSumParameters[] sums)
        {
            ProfileSumParameters[] oldValue = this.sums;
            this.sums = sums;
            firePropertyChange( "sums", oldValue, sums );
        }

        @PropertyName ( "Result path" )
        @PropertyDescription ( "Path to resulting table" )
        public DataElementPath getResultPath()
        {
            return resultPath;
        }
        public void setResultPath(DataElementPath resultPath)
        {
            DataElementPath oldValue = this.resultPath;
            this.resultPath = resultPath;
            firePropertyChange( "resultPath", oldValue, resultPath );
        }
    }

    public static class ParametersBeanInfo extends CoreParametersWithASiteTableBeanInfo
    {
        public ParametersBeanInfo()
        {
            super( Parameters.class );
        }

        @Override
        protected void initProperties() throws Exception
        {
            super.initProperties();
            add( "sums" );
            property( "resultPath" ).outputElement( TableDataCollection.class ).auto( "$bamFiles/path$ profile sums" ).add();
        }
    }

}
