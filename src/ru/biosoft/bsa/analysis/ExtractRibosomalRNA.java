package ru.biosoft.bsa.analysis;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.file.FileDataElement;
import ru.biosoft.access.sql.Query;
import ru.biosoft.access.sql.SqlConnectionPool;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.BasicGenomeSelector;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SequenceRegion;
import ru.biosoft.util.bean.BeanInfoEx2;

public class ExtractRibosomalRNA extends AnalysisMethodSupport<ExtractRibosomalRNA.Parameters>
{
    private static final Query TRANSCRIPT_IDS = new Query( "SELECT transcript_id FROM transcript WHERE biotype=$biotype$" );

    public ExtractRibosomalRNA(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        FileDataElement fastaFileElement = new FileDataElement( parameters.getResult().getName(), parameters.getResult()
                .getParentCollection(), DataCollectionUtils.getChildFile( parameters.getResult().getParentCollection(), parameters
                .getResult().getName() ) );

        DataCollection<AnnotatedSequence> sequences = parameters.getGenome().getSequenceCollection();
        Connection con = SqlConnectionPool.getConnection( sequences );
        List<Integer> transcriptIDs = SqlUtil.queryInts( con, TRANSCRIPT_IDS.str( parameters.getBioType() ) );
        try (PreparedStatement st = con
                .prepareStatement( "select seq_region.name, seq_region_start, seq_region_end, seq_region_strand from exon"
                        + " join exon_transcript using(exon_id)" + " join seq_region using(seq_region_id)"
                        + " join coord_system using(coord_system_id)"
                        + " where transcript_id=? and coord_system.rank=1 order by exon_transcript.rank" );
                BufferedWriter writer = new BufferedWriter( new OutputStreamWriter( new FileOutputStream( fastaFileElement.getFile() ),
                        StandardCharsets.UTF_8 ) ))
        {
            for( Integer transcript : transcriptIDs )
            {
                StringBuilder rnaString = new StringBuilder();

                st.setInt( 1, transcript );
                try (ResultSet rs = st.executeQuery())
                {
                    while( rs.next() )
                    {
                        String chr = rs.getString( 1 );
                        int start = rs.getInt( 2 );
                        int end = rs.getInt( 3 );
                        int strand = rs.getInt( 4 );
                        AnnotatedSequence chrSequence = sequences.get( chr );
                        if(chrSequence == null)
                            continue;
                        Sequence seq = chrSequence.getSequence();
                        SequenceRegion region = new SequenceRegion( seq, start, end - start + 1, strand != 1, false );
                        rnaString.append( region.toString() + '\n' );
                    }
                }

                if( rnaString.length() > 0 )
                {
                    writer.write( ">" + transcript + "\n" );
                    writer.write( rnaString.toString() );
                }
            }
        }

        parameters.getResult().save( fastaFileElement );

        return fastaFileElement;
    }

    @SuppressWarnings ( "serial" )
    public static class Parameters extends AbstractAnalysisParameters
    {
        private BasicGenomeSelector genome = new BasicGenomeSelector();
        private DataElementPath result;
        private String bioType = "rRNA";

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

        public String getBioType()
        {
            return bioType;
        }

        public void setBioType(String bioType)
        {
            this.bioType = bioType;
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
            add( "bioType" );
            property( "result" ).outputElement( FileDataElement.class ).add();
        }

    }

}
