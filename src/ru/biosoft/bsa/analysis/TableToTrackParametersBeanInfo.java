package ru.biosoft.bsa.analysis;

import ru.biosoft.bsa.SequenceCollection;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import ru.biosoft.util.bean.BeanInfoEx2;

/**
 * @author lan
 *
 */
public class TableToTrackParametersBeanInfo extends BeanInfoEx2<TableToTrackParameters>
{
    public TableToTrackParametersBeanInfo()
    {
        super(TableToTrackParameters.class);
    }

    @Override
    protected void initProperties() throws Exception
    {
        property( "inputTable" ).inputElement( TableDataCollection.class ).add();
        add(ColumnNameSelector.registerSelector("chromosomeColumn", beanClass, "inputTable", false));
        add(ColumnNameSelector.registerNumericSelector("fromColumn", beanClass, "inputTable", false));
        add(ColumnNameSelector.registerNumericSelector("toColumn", beanClass, "inputTable", false));
        add(ColumnNameSelector.registerSelector("strandColumn", beanClass, "inputTable", true));
        property( "sequenceCollectionPath" ).inputElement( SequenceCollection.class ).canBeNull().add();
        property( "genomeId" ).auto( "$sequenceCollectionPath/element/properties/genomeBuild$" ).add();
        property( "outputTrack" ).outputElement( SqlTrack.class ).auto( "$inputTable$ track" ).add();
    }
}
