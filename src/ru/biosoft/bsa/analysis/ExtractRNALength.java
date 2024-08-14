package ru.biosoft.bsa.analysis;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.sql.SqlConnectionPool;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.BasicGenomeSelector;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SequenceRegion;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.bean.BeanInfoEx2;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

public class ExtractRNALength extends AnalysisMethodSupport<ExtractRNALength.Parameters>
{
    public ExtractRNALength(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }

    public static class FirstIntron
    {
        public static int length(int firstIntronStart, int firstIntronEnd)
        {
            return firstIntronEnd - firstIntronStart + 1;
        }
        public static String sequence(int firstIntronStart, int firstIntronEnd, boolean reverse, Sequence seq)
        {
            StringBuilder intronString = new StringBuilder();
            SequenceRegion intronRegion = new SequenceRegion( seq, reverse ? firstIntronEnd : firstIntronStart, FirstIntron.length(
                    firstIntronStart, firstIntronEnd ), reverse, false );
            intronString.append( intronRegion.toString() );
            return intronString.toString();
        }
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {

        TableDataCollection result = TableDataCollectionUtils.createTableDataCollection( parameters.getResult() );
        result.getColumnModel().addColumn( "Gene ID", String.class );
        result.getColumnModel().addColumn( "RNA length", Integer.class );
        result.getColumnModel().addColumn( "RNA sequence", String.class );
        result.getColumnModel().addColumn( "Lider length", Integer.class );
        result.getColumnModel().addColumn( "Lider sequence", String.class );
        result.getColumnModel().addColumn( "AUG count", Integer.class );
        result.getColumnModel().addColumn( "Optimal AUG context count", Integer.class );
        result.getColumnModel().addColumn( "CDS length", Integer.class );
        result.getColumnModel().addColumn( "CDS sequence", String.class );
        result.getColumnModel().addColumn( "3'-tail length", Integer.class );
        result.getColumnModel().addColumn( "3'-tail sequence", String.class );
        result.getColumnModel().addColumn( "First intron length", Integer.class );
        result.getColumnModel().addColumn( "First intron", String.class );

        Pattern codon = Pattern.compile( "(?i)atg" );
        Pattern context = Pattern.compile( "(?i)[ag][atgc][atgc]atgg" );

        DataCollection<AnnotatedSequence> sequences = parameters.getGenome().getSequenceCollection();
        Connection con = SqlConnectionPool.getConnection( sequences );

        try( PreparedStatement prep_st = con.prepareStatement(
                "select transcript.transcript_id, transcript_stable_id.stable_id, seq_start, gene_stable_id.stable_id, seq_end, end_exon_id, start_exon_id from transcript"
                        + " join transcript_stable_id using(transcript_id)"
                        + " join translation using(transcript_id)"
                        + " join gene using(gene_id)"
                        + " join gene_stable_id using(gene_id)"
                        + " join seq_region on(transcript.seq_region_id=seq_region.seq_region_id)"
                        + " join coord_system using(coord_system_id) where coord_system.rank=1" );
                ResultSet id_rs = prep_st.executeQuery();
                PreparedStatement st = con
                        .prepareStatement( "select seq_region.name, seq_region_start, seq_region_end, seq_region_strand, exon_id from exon"
                                + " join exon_transcript using(exon_id)" + " join seq_region using(seq_region_id)"
                                + " where transcript_id=? order by exon_transcript.rank" ) )
        {
            while( id_rs.next() )
            {
                int augCount = 0;
                int optimalContext = 0;
                int length = 0;
                int uncodingTail = 0;
                int liderLength = 0;

                boolean liderIsFound = false;

                String rnaSequence = "";

                String liderSequence = "";
                String cdsSequence = "";
                String uncodingTailSequence = "";
                String firstIntronSequence = "";

                int exonNumber = 0;
                int firstIntronStart = 0;
                int firstIntronEnd = 0;
                int firstIntronLength = 0;


                int transcriptId = id_rs.getInt( 1 );
                String stableId = id_rs.getString( 2 );
                int seqStart = id_rs.getInt( 3 );
                int startExonId = id_rs.getInt( 7 );
                String geneId = id_rs.getString( 4 );
                int seqEnd = id_rs.getInt( 5 );
                int endExonId = id_rs.getInt( 6 );


                st.setInt( 1, transcriptId );
                StringBuilder rnaString;
                try(ResultSet rs = st.executeQuery())
                {
                    rnaString = new StringBuilder();
                    rs.isLast();
                    while( rs.next() )
                    {
                        exonNumber++;

                        int exonId = rs.getInt( 5 );

                        int start = rs.getInt( 2 );
                        int end = rs.getInt( 3 );
                        int exonLength = end - start + 1;
                        length = length + exonLength;

                        if( exonId == endExonId )
                        {
                            uncodingTail = exonLength - seqEnd;
                        }
                        if( exonId > endExonId )
                        {
                            uncodingTail = uncodingTail + exonLength;
                        }
                        if( ( exonId != startExonId ) && ( liderIsFound == false ) )
                        {
                            liderLength = liderLength + exonLength;
                        }
                        if( exonId == startExonId )
                        {
                            liderLength = liderLength + seqStart - 1;
                            liderIsFound = true;
                        }
                        String chr = rs.getString( 1 );
                        int strand = rs.getInt( 4 );
                        Sequence seq = sequences.get( chr ).getSequence();
                        boolean reverse = strand != 1;
                        SequenceRegion region = new SequenceRegion( seq, reverse ? end : start, end - start + 1, reverse, false );
                        rnaString.append( region.toString() );

                        if( reverse )
                        {
                            if( ( exonNumber == 1 ) && ( rs.isLast() == false ) )
                            {
                                firstIntronEnd = start - 1;
                            }
                            if( exonNumber == 2 )
                            {
                                firstIntronStart = end + 1;
                                firstIntronLength = FirstIntron.length( firstIntronStart, firstIntronEnd );
                                firstIntronSequence = FirstIntron.sequence( firstIntronStart, firstIntronEnd, reverse, seq );
                            }
                        }
                        else
                        {
                            if( ( exonNumber == 1 ) && ( rs.isLast() == false ) )
                            {
                                firstIntronStart = end + 1;
                            }
                            if( exonNumber == 2 )
                            {
                                firstIntronEnd = start - 1;
                                firstIntronLength = FirstIntron.length( firstIntronStart, firstIntronEnd );
                                firstIntronSequence = FirstIntron.sequence( firstIntronStart, firstIntronEnd, reverse, seq );
                            }
                        }
                    }
                }

                int cdsLength = length - liderLength - uncodingTail;
                rnaSequence = rnaString.toString();

                if( rnaSequence.length() >= liderLength + 5 )
                {
                    liderSequence = rnaSequence.substring( 0, liderLength + 5 );
                }
                if( rnaSequence.length() == liderLength + 3 )
                {
                    liderSequence = rnaSequence.substring( 0, liderLength + 3 );
                }
                if( rnaSequence.length() < liderLength + 2 )
                {
                    liderSequence = rnaSequence.substring( 0, liderLength );
                }
                Matcher codonMatcher = codon.matcher( liderSequence );
                Matcher contextMatcher = context.matcher( liderSequence );

                while( codonMatcher.find() )
                {
                    augCount++;
                }
                while( contextMatcher.find() )
                {
                    optimalContext++;
                }
                liderSequence = rnaSequence.substring( 0, liderLength );
                uncodingTailSequence = rnaSequence.substring( liderLength + cdsLength );

                cdsSequence = rnaSequence.substring( liderLength, liderLength + cdsLength );

                TableDataCollectionUtils.addRow( result, stableId, new Object[] {geneId, length, rnaSequence, liderLength, liderSequence,
                        augCount, optimalContext, cdsLength, cdsSequence, uncodingTail, uncodingTailSequence, firstIntronLength,
                        firstIntronSequence}, true );
            }
        }

        result.finalizeAddition();
        parameters.getResult().save( result );

        return result;
    }

    @SuppressWarnings ( "serial" )
    public static class Parameters extends AbstractAnalysisParameters
    {
        private BasicGenomeSelector genome = new BasicGenomeSelector();
        private DataElementPath result;

        @PropertyName ( "Ensembl database" )
        @PropertyDescription ( "Ensembl database" )
        public BasicGenomeSelector getGenome()
        {
            return genome;
        }

        public void setGenome(BasicGenomeSelector genome)
        {
            this.genome = genome;
        }

        public DataElementPath getResult()
        {
            return result;
        }

        public void setResult(DataElementPath result)
        {
            this.result = result;
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
            add( "genome" );
            property( "result" ).outputElement( TableDataCollection.class ).add();
        }
    }
}
