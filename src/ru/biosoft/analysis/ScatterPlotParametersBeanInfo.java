package ru.biosoft.analysis;

import ru.biosoft.access.ImageDataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import ru.biosoft.util.bean.BeanInfoEx2;

public class ScatterPlotParametersBeanInfo extends BeanInfoEx2<ScatterPlotParameters>
{
    public ScatterPlotParametersBeanInfo() {
        super(ScatterPlotParameters.class);
    }
    @Override
    protected void initProperties() throws Exception
    {
        property("inputTable").inputElement( TableDataCollection.class ).add();
        add(ColumnNameSelector.registerNumericSelector( "xColumn", ScatterPlotParameters.class, "inputTable", true ));
        addExpert( "xColumnLabel" );
        add(ColumnNameSelector.registerNumericSelector( "yColumn", ScatterPlotParameters.class, "inputTable", true ));
        addExpert( "yColumnLabel" );
        addExpert( "showLegend" );
        addExpert( "skipNaN" );
        property("paletteName").tags( ChartAnalysisParameters.ADVANCED_PALETTE, ChartAnalysisParameters.PASTEL_PALETTE, ChartAnalysisParameters.DEFAULT_PALETTE).add();
        property("outputChart").outputElement( ImageDataElement.class ).add();
    }
}
