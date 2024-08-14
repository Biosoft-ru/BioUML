package biouml.plugins.gtrd._test;

import java.sql.Connection;
import java.util.List;

import junit.framework.TestCase;
import ru.biosoft.access.sql.Connectors;
import ru.biosoft.access.sql.SqlUtil;

public class T1 extends TestCase
{
    public void test1()
    {
        Connection con = Connectors.getConnection( "gtrd_current" );
        checkConstraints( con );
    }
    private void checkConstraints(Connection connection)
    {
        //Find experiments that have no matching uniprot
        List<String> unmapped = SqlUtil.queryStrings( connection, "select distinct concat(tfClassId,'(',chip_experiments.specie,')') from chip_experiments left join "
        + "(select * from hub where input_type='ProteinGTRDType' and output_type='UniprotProteinTableType' ) h "
        + "on(h.input=tfClassId AND chip_experiments.specie=h.specie) where tfClassId like '%.%.%.%.%' and output is NULL");
        
        if(!unmapped.isEmpty())
        {
            for(String s : unmapped)
                System.err.println("No uniprot for " + s);
            fail("Some experiments has no matching uniprot");
        }
        
        List<String> multipleUniprotPerTFClass = SqlUtil.queryStrings( connection, "select concat(input,'(',specie,')'), count(*) c from hub where input_type='ProteinGTRDType' AND output_type='UniprotProteinTableType' group by input,specie having c > 1");
        if(!multipleUniprotPerTFClass.isEmpty())
        {
            for(String s : multipleUniprotPerTFClass)
                System.err.println( "Multiple uniprot per TFClass " + s );
            fail("Multiple uniprot per TFClass");
        }
        
        
        List<String> multipleTFClassesPerUniprot = SqlUtil.queryStrings( connection, "select output, count(*) c from hub where input_type='ProteinGTRDType' AND output_type='UniprotProteinTableType' group by output having c > 2 order by c");
        if(!multipleTFClassesPerUniprot.isEmpty())
        {
            for(String s : multipleTFClassesPerUniprot)
                System.err.println( "More then 2 TFClasses per uniprot " + s );
            fail("Multiple TFClasses per uniprot");
        }
    }
}
