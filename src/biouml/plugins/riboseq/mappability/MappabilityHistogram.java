package biouml.plugins.riboseq.mappability;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPInputStream;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.file.FileDataElement;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.datatype.DataType;
import ru.biosoft.util.bean.BeanInfoEx2;

public class MappabilityHistogram extends AnalysisMethodSupport<MappabilityHistogram.Parameters>
{
    public MappabilityHistogram(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        File inputWigFile = parameters.getMinimalUniqueLength().getDataElement( FileDataElement.class ).getFile();

        Map<Integer, AtomicLong> hist = new HashMap<>();
        long total = 0;
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader( new GZIPInputStream( new FileInputStream( inputWigFile ) ) ) ))
        {
            String line = reader.readLine();
            while( line != null )
            {
                String[] fields = line.split( " " );
                if( fields.length != 4 || !fields[0].equals( "fixedStep" ) || !fields[1].startsWith( "chrom=" )
                        || !fields[2].equals( "start=1" ) || !fields[3].equals( "step=1" ) )
                    throw new Exception( "Unexpected line '" + line + "' in " + parameters.getMinimalUniqueLength() );
                
                String chrName = fields[1].substring( "chrom=".length() );
                boolean forward = chrName.endsWith( "+" );
                chrName = chrName.substring( 0, chrName.length() - 1 );
                
                while( ( line = reader.readLine() ) != null && !line.startsWith( "fixedStep" ) )
                {
                    if(!forward)
                        continue;//count only forward strand, the resulting total and hist should be exactly the same for reverse strand
                    total++;
                    int mul = Integer.parseInt( line );
                    //mul - minimal unique length, or the minimal length of the read that can be mapped uniquely to this position
                    //mul = -1 if read can not be mapped to this position with any read length (repeat in sequence tail)
                    if( mul != -1 )
                        hist.computeIfAbsent( mul, k -> new AtomicLong( 0 ) ).incrementAndGet();
                }
            }
        }

        
        TableDataCollection outTable = TableDataCollectionUtils.createTableDataCollection( parameters.getOutTable() );
        ColumnModel cm = outTable.getColumnModel();
        cm.addColumn( "ReadLength", DataType.Integer );
        cm.addColumn( "MappableFraction", DataType.Float );
        cm.addColumn( "MappableBases", DataType.Text );//can be greater then 2^32, but TableDataCollection lucks 64bit integers
        TableColumn col = cm.addColumn( "MappableBasesWithThisReadLengthButNotSmaller", DataType.Text );//can be greater then 2^32, but TableDataCollection lucks 64bit integers
        col.setHidden( true );
        TableDataCollectionUtils.setSortOrder( outTable, "ReadLength", true );

        
        Map<Integer, AtomicLong> histSorted = new TreeMap<>(hist);
        
        long cumSum = 0;
        for( Map.Entry<Integer, AtomicLong> entry : histSorted.entrySet() )
        {
            Integer readLength = entry.getKey();
            long bases = entry.getValue().longValue();
            cumSum += bases;
            double frac = (double)cumSum / total;
            
            Object[] values = new Object[] {readLength, frac, String.valueOf( cumSum ), String.valueOf( bases ) };
            TableDataCollectionUtils.addRow( outTable, String.valueOf( readLength ), values,  true );
            System.out.println( readLength + "\t" + frac + "\t" + cumSum + "\t" + bases );
        }
        
        outTable.finalizeAddition();
        parameters.getOutTable().save( outTable );
        return outTable;
    }

   

    public static class Parameters extends AbstractAnalysisParameters
    {
        private DataElementPath minimalUniqueLength;
        @PropertyName ( "Minimal unique length" )
        @PropertyDescription ( "Minimal unique length file, produced by 'Sequence minimal unique length' analysis" )
        public DataElementPath getMinimalUniqueLength()
        {
            return minimalUniqueLength;
        }
        public void setMinimalUniqueLength(DataElementPath minimalUniqueLength)
        {
            Object oldValue = this.minimalUniqueLength;
            this.minimalUniqueLength = minimalUniqueLength;
            firePropertyChange( "minimalUniqueLength", oldValue, minimalUniqueLength );
        }

        private DataElementPath outTable;
        @PropertyName("Result")
        @PropertyDescription("Table listing mappability for each read length")
        public DataElementPath getOutTable()
        {
            return outTable;
        }
        public void setOutTable(DataElementPath outTable)
        {
            Object oldValue = this.outTable;
            this.outTable = outTable;
            firePropertyChange( "outTable", oldValue, outTable );
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
            property( "minimalUniqueLength" ).inputElement( FileDataElement.class ).add();
            property( "outTable" ).outputElement( TableDataCollection.class ).auto( "$minimalUniqueLength$ hist" ).add();
        }
    }
}
