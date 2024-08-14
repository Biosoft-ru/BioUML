package ru.biosoft.bsa;

import java.util.Properties;

import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.ClassIcon;

/**
 * Class represents vcf type of track containing information about mutations/deletion/insertions etc.
 * @author anna
 *
 */
@ClassIcon ( "resources/trackvcf.png" )
@PropertyName ( "track" )
public class VCFSqlTrack extends SqlTrack
{
    public VCFSqlTrack(DataCollection<?> origin, Properties properties)
    {
        super( origin, properties );
    }

    protected String getNodeImageIcon()
    {
        return "resources/trackvcf.png";
    }
}
