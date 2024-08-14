package ru.biosoft.bsa.analysis;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.FileDataElement;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Interval;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.bean.BeanInfoEx2;

import com.developmentontheedge.application.ApplicationUtils;

public class BlastAlignmentCoverage extends AnalysisMethodSupport<BlastAlignmentCoverage.Parameters>
{
    public BlastAlignmentCoverage(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        TObjectIntMap<String> subjectLengths = readSequenceLengths( parameters.getSubjectFasta().getDataElement( FileDataElement.class )
                .getFile() );
        TObjectIntMap<String> queryLengths = readSequenceLengths( parameters.getQueryFasta().getDataElement( FileDataElement.class )
                .getFile() );

        Map<String, List<Interval>> alignsBySubject = loadAlignsBySubject();

        TableDataCollection result = TableDataCollectionUtils.createTableDataCollection( parameters.getResult() );
        ColumnModel cm = result.getColumnModel();
        cm.addColumn( "Coverage", Double.class );
        for( Map.Entry<String, List<Interval>> e : alignsBySubject.entrySet() )
        {
            String subjectName = e.getKey();
            if(!subjectLengths.containsKey( subjectName ))
            {
                log.warning( "Unknown length for " + subjectName );
                continue;
            }
            int subjectLen = subjectLengths.get( subjectName );
            List<Interval> aligns = e.getValue();
            Collections.sort( aligns );
            double coverage = (double)new Interval(1, subjectLen).coverage( aligns ) / subjectLen;
            TableDataCollectionUtils.addRow( result, subjectName, new Object[] {coverage}, true );
        }
        result.finalizeAddition();
        parameters.getResult().save( result );
        return result;
    }

    private Map<String, List<Interval>> loadAlignsBySubject()
    {
        Map<String, List<Interval>> alignsBySubject = new HashMap<>();

        for( RowDataElement row : parameters.getBlastOutputTable().getDataElement( TableDataCollection.class ) )
        {
            BlastAlignment align = new BlastAlignment( row );
            if(!align.isValid())
                continue;

            String subject = align.getSubject();
            List<Interval> alignsOnSubject = alignsBySubject.get( subject );
            if( alignsOnSubject == null )
                alignsBySubject.put( subject, alignsOnSubject = new ArrayList<>() );

            alignsOnSubject.add( new Interval( align.getSubjectStart(), align.getSubjectEnd() ) );
        }

        return alignsBySubject;
    }

    private TObjectIntMap<String> readSequenceLengths(File fastaFile) throws IOException
    {
        TObjectIntMap<String> result = new TObjectIntHashMap<>();
        try(BufferedReader reader = ApplicationUtils.utfReader( fastaFile ))
        {
            String line = reader.readLine();
            while( line != null )
            {
                if( line.charAt( 0 ) != '>' )
                    throw new IOException( "Unexpected line '" + line + "', expecting fasta sequence name" );
                String name = line.substring( 1 );
                String[] ids = name.split( "\\x01" );
                int length = 0;
                while( ( line = reader.readLine() ) != null && line.charAt( 0 ) != '>' )
                    length += line.length();
                for(String id : ids)
                    result.put( id, length );
            }
        }
        return result;
    }

    @SuppressWarnings ( "serial" )
    public static class Parameters extends AbstractAnalysisParameters
    {
        private DataElementPath blastOutputTable;
        private DataElementPath subjectFasta, queryFasta;
        private DataElementPath result;
        public DataElementPath getBlastOutputTable()
        {
            return blastOutputTable;
        }
        public void setBlastOutputTable(DataElementPath blastOutputTable)
        {
            this.blastOutputTable = blastOutputTable;
        }
        public DataElementPath getSubjectFasta()
        {
            return subjectFasta;
        }
        public void setSubjectFasta(DataElementPath subjectFasta)
        {
            this.subjectFasta = subjectFasta;
        }
        public DataElementPath getQueryFasta()
        {
            return queryFasta;
        }
        public void setQueryFasta(DataElementPath queryFasta)
        {
            this.queryFasta = queryFasta;
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
            property( "blastOutputTable" ).inputElement( TableDataCollection.class ).add();
            property( "subjectFasta" ).inputElement( FileDataElement.class ).add();
            property( "queryFasta" ).inputElement( FileDataElement.class ).add();

            property( "result" ).outputElement( TableDataCollection.class ).auto( "$blastOutputTable$ coverage" ).add();
        }
    }

    private static class BlastAlignment
    {
        private static final int QUERY = 0;
        private static final int SUBJECT = 1;
        private static final int PERCENT_IDENTITY = 2;
        private static final int ALIGNMENT_LENGTH = 3;
        private static final int MISMATCHES = 4;
        private static final int GAP_OPENS = 5;
        private static final int QUERY_START = 6;
        private static final int QUERY_END = 7;
        private static final int SUBJECT_START = 8;
        private static final int SUBJECT_END = 9;
        private static final int E_VALUE = 10;
        private static final int BIT_SCORE = 11;

        private final Object[] values;

        public BlastAlignment(RowDataElement row)
        {
            this.values = row.getValues();
        }

        public String getQuery()
        {
            return values[QUERY].toString();
        }

        public String getSubject()
        {
            return values[SUBJECT].toString();
        }

        public double getPercentIdentity()
        {
            return ( (Number)values[PERCENT_IDENTITY] ).doubleValue();
        }

        public int getAlignmentLength()
        {
            return (Integer)values[ALIGNMENT_LENGTH];
        }

        public int getMismatches()
        {
            return (Integer)values[MISMATCHES];
        }

        public int getGapOpens()
        {
            return (Integer)values[GAP_OPENS];
        }

        public int getQueryStart()
        {
            return (Integer)values[QUERY_START];
        }

        public int getQueryEnd()
        {
            return (Integer)values[QUERY_END];
        }

        public int getSubjectStart()
        {
            return (Integer)values[SUBJECT_START];
        }

        public int getSubjectEnd()
        {
            return (Integer)values[SUBJECT_END];
        }

        public double getEValue()
        {
            return ( (Number)values[E_VALUE] ).doubleValue();
        }

        public double getBitScore()
        {
            return ( (Number)values[BIT_SCORE] ).doubleValue();
        }

        public boolean isValid()
        {
            return true;
        }


    }
}
