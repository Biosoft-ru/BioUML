package ru.biosoft.graphics.access;

import java.io.File;

import org.json.JSONArray;

import ru.biosoft.access.AbstractFileTransformer;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.file.FileDataElement;
import ru.biosoft.graphics.chart.Chart;

import com.developmentontheedge.application.ApplicationUtils;

/**
 * {@link ChartDataElement} to {@link FileDataElement} transformer
 */
public class FileChartTransformer extends AbstractFileTransformer<ChartDataElement>
{
    @Override
    public Class<ChartDataElement> getOutputType()
    {
        return ChartDataElement.class;
    }

    @Override
    public ChartDataElement load(File input, String name, DataCollection<ChartDataElement> origin) throws Exception
    {
        Chart chart = new Chart(new JSONArray(ApplicationUtils.readAsString(input)));
        return new ChartDataElement(name, origin, chart);
    }

    @Override
    public void save(File output, ChartDataElement element) throws Exception
    {
        ApplicationUtils.writeString(output, element.getChart().toJSON().toString());
    }
}
