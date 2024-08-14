package biouml.plugins.riboseq.ingolia.asite;

import biouml.plugins.riboseq.ingolia.CoreParameters;
import ru.biosoft.access.core.DataElementPath;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

public class BuildASiteOffsetTableParameters extends CoreParameters
{
    private DataElementPath aSiteOffsetTable;

    @PropertyName("A site offset Table")
    @PropertyDescription("table: length - offset")
    public DataElementPath getASiteOffsetTable()
    {
        return aSiteOffsetTable;
    }

    public void setASiteOffsetTable(DataElementPath aSiteOffsetTable)
    {
        this.aSiteOffsetTable = aSiteOffsetTable;
    }
}
