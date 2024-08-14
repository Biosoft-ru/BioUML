package biouml.plugins.riboseq.ingolia;

import java.util.HashMap;
import java.util.Map;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

public class CoreParametersWithASiteTable extends CoreParameters
{
    private DataElementPath aSiteOffsetTable;
    @PropertyName("Ribosome A-site offset table")
    @PropertyDescription("Table with 2 columns: 'Length' and 'Offset'")
    public DataElementPath getASiteOffsetTable()
    {
        return aSiteOffsetTable;
    }
    public void setASiteOffsetTable(DataElementPath aSiteOffsetTable)
    {
        final Object oldValue = this.aSiteOffsetTable;
        this.aSiteOffsetTable = aSiteOffsetTable;
        firePropertyChange( "aSiteOffsetTable", oldValue, this.aSiteOffsetTable );
    }
    
    public AlignmentConverter createAlignmentConverter()
    {
        AlignmentConverter alignmentConverter = new AlignmentConverter();
        alignmentConverter.setTranscriptOverhangs( getTranscriptOverhangs() );
        return alignmentConverter;
    }
    
    public ProfileBuilder createProfileBuilder()
    {
        ProfileBuilder profileBuilder = new ProfileBuilder();
        profileBuilder.setOnlyPositiveStrand( isStrandSpecific() );
        if( getASiteOffsetTable() != null )
        {
            TableDataCollection aSiteOffsetTable = getASiteOffsetTable().getDataElement( TableDataCollection.class );
            Map<Integer, Integer> lengthToOffset = parseASiteOffsetTable( aSiteOffsetTable );
            profileBuilder.setASiteOffsetTable( lengthToOffset );
        }
        return profileBuilder;
    }
    
    private Map<Integer, Integer> parseASiteOffsetTable(TableDataCollection aSiteOffsetTable)
    {
        final ColumnModel cm = aSiteOffsetTable.getColumnModel();
        final int lengthColumn = cm.getColumnIndex( "Length" );
        final int offsetColumn = cm.getColumnIndex( "Offset" );

        final boolean hasTrustColumn = cm.hasColumn( "Trust" );
        final int trustColumn = hasTrustColumn ? cm.getColumnIndex( "Trust" ) : -1;

        final Map<Integer, Integer> lengthToOffset = new HashMap<>();
        for( final RowDataElement row : aSiteOffsetTable )
        {
            final Object[] values = row.getValues();

            if( hasTrustColumn && values[trustColumn].equals( "false" ) )
            {
                continue;
            }

            final Integer length = (Integer) values[lengthColumn];
            final Integer offset = (Integer) values[offsetColumn];
            lengthToOffset.put( length, offset );
        }
        return lengthToOffset;
    }
}
