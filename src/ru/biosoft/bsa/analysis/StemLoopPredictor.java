package ru.biosoft.bsa.analysis;

import java.util.LinkedList;
import java.util.Objects;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.BasicGenomeSelector;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SequenceRegion;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.align.Alignment.Element;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.bean.BeanInfoEx2;

public class StemLoopPredictor extends AnalysisMethodSupport<StemLoopPredictor.Parameters>
{
    public StemLoopPredictor(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {

        final TableDataCollection result = TableDataCollectionUtils.createTableDataCollection( parameters.getOutputTable() );
        result.getColumnModel().addColumn( "chr", String.class );
        result.getColumnModel().addColumn( "From", Integer.class );
        result.getColumnModel().addColumn( "To", Integer.class );
        result.getColumnModel().addColumn( "Strand", Integer.class );
        result.getColumnModel().addColumn( "Score", Integer.class );
        result.getColumnModel().addColumn( "Stem length", Integer.class );
        result.getColumnModel().addColumn( "Loop length", Integer.class );
        result.getColumnModel().addColumn( "Stem loop", String.class );

        jobControl.forCollection( parameters.getGenome().getSequenceCollectionPath().getChildren(), chrPath -> {
            try
            {
                Sequence chr = chrPath.getDataElement(AnnotatedSequence.class).getSequence();
                Track track = parameters.getInputTrack().getDataElement(Track.class);
                DataCollection<Site> sites = track.getSites( chrPath.toString(), chr.getStart(), chr.getStart() + chr.getLength() );
                for( Site s : sites )
                {
                    Interval rightInterval = new Interval( s.getFrom() - 10, s.getFrom() + 89 );
                    StemLoop right = alignStemLoop( new SequenceRegion( s.getOriginalSequence(), rightInterval,
                            s.getStrand() == StrandType.STRAND_MINUS, false ) );

                    Interval leftInterval = new Interval( s.getTo() - 89, s.getTo() + 10 );
                    StemLoop left = alignStemLoop( new SequenceRegion( s.getOriginalSequence(), leftInterval,
                            s.getStrand() == StrandType.STRAND_MINUS, false ) );

                    if( right.getAlignmentScore() > left.getAlignmentScore() )
                    {
                        if( right.getAlignmentScore() >= parameters.getScoreThreshold() )
                        {
                            saveStemLoop( right, s.getOriginalSequence().getName(), rightInterval, s.getStrand(), result );
                        }
                    }
                    else if( left.getAlignmentScore() >= parameters.getScoreThreshold() )
                    {
                        saveStemLoop( left, s.getOriginalSequence().getName(), leftInterval, s.getStrand(), result );
                    }
                }
            }
            catch( Exception e )
            {
                throw ExceptionRegistry.translateException(e);
            }
            return true;
        } );

        result.finalizeAddition();
        parameters.getOutputTable().save( result );
        return result;
    }

    private StemLoop alignStemLoop(Sequence sequence)
    {
        byte[] seq = sequence.toString().toUpperCase().getBytes();

        int bestScore = 0, bestI = 0, bestJ = 0;
        int[][] m = new int[seq.length + 1][seq.length + 1];
        for( int i = 1; i <= seq.length; i++ )
        {
            for( int j = 1; j < seq.length - i - 1; j++ )
            {
                boolean matched = isComplementary( seq[i - 1], seq[seq.length - j] );
                int match = m[i - 1][j - 1] + ( matched ? parameters.getMatch() : parameters.getMismatch() );
                int indel = Math.max( m[i - 1][j], m[i][j - 1] ) + parameters.getGap();
                int curScore = Math.max( 0, Math.max( match, indel ) );
                if( curScore > bestScore )
                {
                    bestScore = curScore;
                    bestI = i;
                    bestJ = j;
                }
                m[i][j] = curScore;
            }
        }

        LinkedList<Element> stemAlignment = new LinkedList<>();
        int i = bestI, j = bestJ;
        while( true )
        {
            if( i > 0
                    && j > 0
                    && ( m[i][j] == m[i - 1][j - 1]
                            + ( isComplementary( seq[i - 1], seq[seq.length - j] ) ? parameters.getMatch() : parameters.getMismatch() ) ) )
            {
                --i;
                --j;
                stemAlignment.addFirst( Element.M );
            }
            else if( i > 0 && m[i][j] == m[i - 1][j] + parameters.getGap() )
            {
                --i;
                stemAlignment.addFirst( Element.I );
            }
            else if( j > 0 && m[i][j] == m[i][j - 1] + parameters.getGap() )
            {
                --j;
                stemAlignment.addFirst( Element.G );
            }
            else
            {
                break;
            }
        }

        Interval stemLoopInterval = new Interval( i, seq.length - 1 - j );
        Interval loopInterval = new Interval( bestI, seq.length - 1 - bestJ );
        return new StemLoop( seq, stemLoopInterval, loopInterval, stemAlignment.toArray( new Element[stemAlignment.size()] ), bestScore );
    }

    private int nextId = 1;

    private void saveStemLoop(StemLoop stemLoop, String chrName, Interval interval, int strand, TableDataCollection table)
    {
        TableDataCollectionUtils.addRow( table, String.valueOf( nextId++ ), new Object[] {chrName, interval.getFrom(), interval.getTo(),
                strand, stemLoop.getAlignmentScore(), stemLoop.getStemLenght(), stemLoop.getLoopInterval().getLength(), stemLoop.toHTML()} );
    }
    private static boolean isComplementary(byte x, byte y)
    {
        if( ( x == 'A' && y == 'T' ) || ( x == 'T' && y == 'A' ) || ( x == 'C' && y == 'G' ) || ( x == 'G' && y == 'C' )
        // GU wooble base pairing
                || ( x == 'G' && y == 'T' ) || ( x == 'T' && y == 'G' ) )
            return true;
        return false;
    }

    private static class StemLoop
    {
        //5'-xxx-STEM-LO
        //             |
        //3'-xxx-STEM-PO
        private final byte[] seq;
        private final Interval stemLoopInterval;
        private final Interval loopInterval;
        private final Element[] stemAlignment;
        private final int alignmentScore;

        public StemLoop(byte[] seq, Interval stemLoopInterval, Interval loopInterval, Element[] stemAlignment, int score)
        {
            this.seq = seq;
            this.stemLoopInterval = stemLoopInterval;
            this.loopInterval = loopInterval;
            this.stemAlignment = stemAlignment;
            this.alignmentScore = score;
        }

        public Interval getLeftStem()
        {
            return new Interval( stemLoopInterval.getFrom(), loopInterval.getFrom() - 1 );
        }

        public Interval getRightStem()
        {
            return new Interval( loopInterval.getTo() + 1, stemLoopInterval.getTo() );
        }

        public int getStemLenght()
        {
            return Math.max( getLeftStem().getLength(), getRightStem().getLength() );
        }

        public Interval getLoopInterval()
        {
            return loopInterval;
        }

        public String toHTML()
        {
            return "<pre style='font-family:monospace'>" + toString() + "</pre>";
        }

        @Override
        public String toString()
        {
            StringBuilder line1 = new StringBuilder();
            StringBuilder line2 = new StringBuilder();
            StringBuilder line3 = new StringBuilder();
            StringBuilder line4 = new StringBuilder();
            StringBuilder line5 = new StringBuilder();


            int leftOverhangLen = stemLoopInterval.getFrom();
            int rightOverhangLen = seq.length - 1 - stemLoopInterval.getTo();
            for( int i = 0; i < leftOverhangLen - rightOverhangLen; i++ )
                line5.append( ' ' );
            for( int i = 0; i < rightOverhangLen - leftOverhangLen; i++ )
                line1.append( ' ' );
            for( int i = 0; i < stemLoopInterval.getFrom(); i++ )
                line1.append( (char)seq[i] );
            for( int i = seq.length - 1; i > stemLoopInterval.getTo(); i-- )
                line5.append( (char)seq[i] );
            for( int i = 0; i < Math.max( leftOverhangLen, rightOverhangLen ); i++ )
            {
                line2.append( ' ' );
                line3.append( ' ' );
                line4.append( ' ' );
            }


            int pos1 = stemLoopInterval.getFrom(), pos2 = stemLoopInterval.getTo();

            for( Element e : stemAlignment )
            {
                switch( e )
                {
                    case M:
                        byte l1 = seq[pos1++];
                        byte l2 = seq[pos2--];
                        if( isComplementary( l1, l2 ) )
                        {
                            line1.append( ' ' );
                            line2.append( (char)l1 );
                            line3.append( '|' );
                            line4.append( (char)l2 );
                            line5.append( ' ' );
                        }
                        else
                        {
                            line1.append( (char)l1 );
                            line2.append( ' ' );
                            line3.append( ' ' );
                            line4.append( ' ' );
                            line5.append( (char)l2 );
                        }
                        break;
                    case G:
                        line1.append( '-' );
                        line2.append( ' ' );
                        line3.append( ' ' );
                        line4.append( ' ' );
                        line5.append( (char)seq[pos2--] );
                        break;
                    case I:
                        line1.append( (char)seq[pos1++] );
                        line2.append( ' ' );
                        line3.append( ' ' );
                        line4.append( ' ' );
                        line5.append( '-' );
                        break;
                }
            }

            if( loopInterval.getLength() == 1 )
            {
                line3.append( seq[loopInterval.getFrom()] );
            }
            else if( loopInterval.getLength() > 1 )
            {
                for( int i = 0; i < ( loopInterval.getLength() - 2 ) / 2; i++ )
                {
                    line1.append( (char)seq[loopInterval.getFrom() + i] );
                    line2.append( ' ' );
                    line3.append( ' ' );
                    line4.append( ' ' );
                    line5.append( (char)seq[loopInterval.getTo() - i] );
                }
                if( loopInterval.getLength() % 2 == 0 )
                {
                    line1.append( ' ' );
                    line2.append( (char)seq[loopInterval.getCenter()] );
                    line3.append( ' ' );
                    line4.append( (char)seq[loopInterval.getCenter() + 1] );
                    line5.append( ' ' );
                }
                else
                {
                    line1.append( ' ' );
                    line2.append( (char)seq[loopInterval.getCenter() - 1] );
                    line3.append( (char)seq[loopInterval.getCenter()] );
                    line4.append( (char)seq[loopInterval.getCenter() + 1] );
                    line5.append( ' ' );
                }
            }

            return ( line1 + "\n" + line2 + "\n" + line3 + "\n" + line4 + "\n" + line5 ).toLowerCase();
        }

        public int getAlignmentScore()
        {
            return alignmentScore;
        }

    }

    @SuppressWarnings ( "serial" )
    public static class Parameters extends AbstractAnalysisParameters
    {
        private DataElementPath inputTrack, outputTable;
        private BasicGenomeSelector genome = new BasicGenomeSelector();
        private int scoreThreshold = 6, match = 1, mismatch = -1, gap = -1;


        public BasicGenomeSelector getGenome()
        {
            return genome;
        }

        public void setGenome(BasicGenomeSelector genome)
        {
            Object oldValue = this.genome;
            this.genome = genome;
            firePropertyChange( "genome", oldValue, genome );
        }

        public DataElementPath getInputTrack()
        {
            return inputTrack;
        }

        public void setInputTrack(DataElementPath inputTrack)
        {
            Object oldValue = this.inputTrack;
            this.inputTrack = inputTrack;
            if( !Objects.equals( oldValue, inputTrack ) )
            {
                genome.setFromTrack( (Track)inputTrack.optDataElement() );
            }
            firePropertyChange( "inputTrack", oldValue, inputTrack );
        }

        public int getScoreThreshold()
        {
            return scoreThreshold;
        }

        public void setScoreThreshold(int scoreThreshold)
        {
            Object oldValue = this.scoreThreshold;
            this.scoreThreshold = scoreThreshold;
            firePropertyChange( "scoreThreshold", oldValue, scoreThreshold );
        }


        public int getMatch()
        {
            return match;
        }

        public int getMismatch()
        {
            return mismatch;
        }

        public void setMismatch(int mismatch)
        {
            Object oldValue = this.mismatch;
            this.mismatch = mismatch;
            firePropertyChange( "mismatch", oldValue, mismatch );
        }

        public int getGap()
        {
            return gap;
        }

        public void setGap(int gap)
        {
            Object oldValue = this.gap;
            this.gap = gap;
            firePropertyChange( "gap", oldValue, gap );
        }

        public void setMatch(int match)
        {
            Object oldValue = this.match;
            this.match = match;
            firePropertyChange( "match", oldValue, match );
        }

        public DataElementPath getOutputTable()
        {
            return outputTable;
        }

        public void setOutputTable(DataElementPath outputTable)
        {
            Object oldValue = this.outputTable;
            this.outputTable = outputTable;
            firePropertyChange( "outputTable", oldValue, outputTable );
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
            super.initProperties();
            property( "inputTrack" ).inputElement( Track.class ).add();
            add( "genome" );
            add( "match" );
            add( "mismatch" );
            add( "gap" );
            add( "scoreThreshold" );
            property( "outputTable" ).outputElement( TableDataCollection.class ).auto( "$inputTrack$ stem loops" ).add();
        }
    }

}
