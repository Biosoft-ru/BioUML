package ru.biosoft.analysis;

import ru.biosoft.access.ImageDataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import ru.biosoft.util.bean.BeanInfoEx2;

public class ChartAnalysisParametersBeanInfo extends BeanInfoEx2<ChartAnalysisParameters>
{
    public ChartAnalysisParametersBeanInfo()
    {
        super( ChartAnalysisParameters.class );
    }
    protected ChartAnalysisParametersBeanInfo(Class<? extends ChartAnalysisParameters> beanClass)
    {
        super( beanClass );
    }
    @Override
    protected void initProperties() throws Exception
    {
        property("inputTable").inputElement( TableDataCollection.class ).add();
        add( ColumnNameSelector.registerNumericSelector( "column", beanClass, "inputTable", false ) );
        add( ColumnNameSelector.registerSelector( "labelsColumn", beanClass, "inputTable", true ) );
        add("maxPieces");
        add("addRemaininig");
        property("paletteName").tags( ChartAnalysisParameters.ADVANCED_PALETTE, ChartAnalysisParameters.PASTEL_PALETTE, ChartAnalysisParameters.DEFAULT_PALETTE).add();
        property("outputChart").outputElement( ImageDataElement.class ).add();

    }
}