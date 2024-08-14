package biouml.plugins.mirprom;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.SqlTransformerSupport;
import ru.biosoft.bsa.StrandType;

public class MiRNAPromoterTransformer extends SqlTransformerSupport<MiRNAPromoter>
{

    @Override
    public boolean init(SqlDataCollection<MiRNAPromoter> owner)
    {
        table = "promoter";
        idField = "promoter.id";
        return super.init( owner );
    }
    
    @Override
    public Class<MiRNAPromoter> getTemplateClass()
    {
        return MiRNAPromoter.class;
    }
    
    @Override
    public String getSelectQuery()
    {
        return "SELECT promoter.id,chrom,position,strand,mirna.name FROM promoter JOIN mirna ON(mirna.id=mirna_id)";
    }

    @Override
    public MiRNAPromoter create(ResultSet resultSet, Connection connection) throws Exception
    {
        int promoterId = resultSet.getInt( 1 );
        String chr = resultSet.getString( 2 );
        int position = resultSet.getInt( 3 );
        String strandStr = resultSet.getString( 4 );
        String miRname = resultSet.getString( 5 );
        MiRNAPromoter promoter = new MiRNAPromoter( promoterId, owner, miRname, chr, position, strandStr.equals( "+" ) ? StrandType.STRAND_PLUS : StrandType.STRAND_MINUS );
        try( PreparedStatement ps = connection.prepareStatement(
                "SELECT name FROM promoter JOIN promoter2cell ON(promoter.id=promoter_id) JOIN cell ON(cell.id=cell_id) WHERE promoter_id=?" ) )
        {
            ps.setInt( 1, promoterId );
            try( ResultSet rs = ps.executeQuery() )
            {
                while( rs.next() )
                {
                    String cell = rs.getString( 1 );
                    promoter.addCell( cell );
                }
            }
        }
        return promoter;
    }

    @Override
    public void addInsertCommands(Statement statement, MiRNAPromoter de) throws Exception
    {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void addDeleteCommands(Statement statement, String name) throws Exception
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSortingSupported()
    {
        return true;
    }

    @Override
    public String[] getSortableFields()
    {
        return new String[] {"name", "miRName", "location"};
    }

    @Override
    public String getSortedNameListQuery(String field, boolean direction)
    {
        String directStr = direction ? "ASC" : "DESC";
        if(field.equals( "miRName" ))
            return "SELECT promoter.id FROM promoter JOIN mirna ON(mirna.id=mirna_id) ORDER BY mirna.name " + directStr;
        if(field.equals( "name" ))
            return "SELECT id FROM promoter ORDER BY id  " + directStr;
        if(field.equals( "location" ))
            return "SELECT id FROM promoter ORDER BY chrom " + directStr + ",position " + directStr + ",strand  " + directStr;
        return getNameListQuery();
    }
}
