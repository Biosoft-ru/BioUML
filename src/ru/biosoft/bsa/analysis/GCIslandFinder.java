package ru.biosoft.bsa.analysis;

import gnu.trove.list.array.TDoubleArrayList;

import java.util.Iterator;
import java.util.NoSuchElementException;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Basis;
import ru.biosoft.bsa.Precision;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SequenceCollection;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SiteType;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.Track;
import ru.biosoft.util.bean.BeanInfoEx2;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetSupport;

public class GCIslandFinder extends AnalysisMethodSupport<GCIslandFinder.Parameters>
{
    public GCIslandFinder(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        DataCollection<AnnotatedSequence> sequences = parameters.getInputSequences().getDataCollection(AnnotatedSequence.class);

        SqlTrack result = SqlTrack.createTrack( parameters.getOutputTrack(), null );

        for( AnnotatedSequence seq : sequences )
            findIslands( seq.getSequence(), result );

        result.finalizeAddition();

        return result;
    }

    private void findIslands(Sequence seq, SqlTrack result) throws Exception
    {
        Iterator<Integer> it = new GCIterator( seq );

        TDoubleArrayList profile = new TDoubleArrayList();
        double threshold = parameters.getGcThreshold() * parameters.getWindowSize();
        int i = 0;
        while( it.hasNext() )
        {
            int gcCount = it.next();
            boolean pass = parameters.isGreater() ? gcCount >= threshold : gcCount <= threshold;
            if( pass )
                profile.add( gcCount * 100.0 / parameters.getWindowSize() );
            else if( !profile.isEmpty() )
            {
                DynamicPropertySet properties = new DynamicPropertySetSupport();
                properties.add( new DynamicProperty( "profile", double[].class, profile.toArray() ) );
                properties.add( new DynamicProperty( "maxGC", Double.class, profile.max() ) );
                properties.add( new DynamicProperty( "minGC", Double.class, profile.min() ) );

                Site site = new SiteImpl( result, seq.getName(), SiteType.TYPE_MISC_FEATURE, Basis.BASIS_ANNOTATED, i - profile.size() + 1 + parameters.getWindowSize() / 2
                        + seq.getStart(), profile.size(), Precision.PRECISION_CUT_BOTH, StrandType.STRAND_BOTH, seq, properties );
                result.addSite( site );
                profile.reset();
            }
            i++;
        }
    }

    @SuppressWarnings ( "serial" )
    public static class Parameters extends AbstractAnalysisParameters
    {
        private DataElementPath inputSequences;
        private int windowSize = 100;
        private boolean greater = true;
        private double gcThreshold = 0.6;
        private DataElementPath outputTrack;

        public DataElementPath getInputSequences()
        {
            return inputSequences;
        }
        public void setInputSequences(DataElementPath inputSequences)
        {
            Object oldValue = this.inputSequences;
            this.inputSequences = inputSequences;
            firePropertyChange( "inputSequences", oldValue, inputSequences );
        }
        public int getWindowSize()
        {
            return windowSize;
        }
        public void setWindowSize(int windowSize)
        {
            Object oldValue = this.windowSize;
            this.windowSize = windowSize;
            firePropertyChange( "windowSize", oldValue, windowSize );
        }
        public boolean isGreater()
        {
            return greater;
        }
        public void setGreater(boolean greater)
        {
            Object oldValue = this.greater;
            this.greater = greater;
            firePropertyChange( "greater", oldValue, greater );
        }
        public double getGcThreshold()
        {
            return gcThreshold;
        }
        public void setGcThreshold(double gcThreshold)
        {
            Object oldValue = this.gcThreshold;
            this.gcThreshold = gcThreshold;
            firePropertyChange( "gcThreshold", oldValue, gcThreshold );
        }
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
            property( "inputSequences" ).inputElement( SequenceCollection.class ).add();
            add( "windowSize" );
            add( "greater" );
            add( "gcThreshold" );
            property( "outputTrack" ).outputElement( Track.class ).add();
        }
    }

    private class GCIterator implements Iterator<Integer>
    {
        private int curGC;
        private int pos;
        private Sequence seq;

        public GCIterator(Sequence seq)
        {
            this.seq = seq;
            if( seq.getLength() < parameters.getWindowSize() )
                return;
            for( int i = 0; i < parameters.getWindowSize(); i++ )
            {
                char letter = Character.toLowerCase( (char)seq.getLetterAt( i + seq.getStart() ) );
                if( letter == 'c' || letter == 'g' )
                    curGC++;
            }
        }

        @Override
        public boolean hasNext()
        {
            return pos + parameters.getWindowSize() <= seq.getLength();
        }

        @Override
        public Integer next()
        {
            if(!hasNext())
                throw new NoSuchElementException();
            int gc = curGC;
            advance();
            return gc;
        }

        private void advance()
        {
            pos++;
            if( !hasNext() )
                return;

            char letter = Character.toLowerCase( (char)seq.getLetterAt( pos - 1 + seq.getStart() ) );
            if( letter == 'c' || letter == 'g' )
                curGC--;

            letter = Character.toLowerCase( (char)seq.getLetterAt( pos - 1 + parameters.getWindowSize() + seq.getStart() ) );
            if( letter == 'c' || letter == 'g' )
                curGC++;
        }
    }
}
