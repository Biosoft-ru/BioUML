package ru.biosoft.bsastats.processors;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import java.util.logging.Logger;
import org.mozilla.javascript.NativeArray;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.access.ImageElement;
import ru.biosoft.exception.InternalException;
import ru.biosoft.plugins.jsreport.JavaScriptReport.Report;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import com.developmentontheedge.beans.annot.PropertyName;

/**
 * @author lan
 *
 */
public abstract class AbstractStatisticsProcessor implements StatisticsProcessor
{
    protected static final LetterCounter gcCounter = LetterCounter.getLetterCounter("GCgc");
    protected Logger log;
    protected Quality qualityValue = Quality.OK;

    @Override
    public void init(Logger log)
    {
        this.log = log;
    }
    
    protected int getGCcount(byte[] sequence)
    {
        return gcCounter.count(sequence);
    }
    
    @Override
    public @Nonnull String getName()
    {
        String value = getClass().getAnnotation(PropertyName.class).value();
        if(value == null)
            throw new InternalException( "@PropertyName is not specified for "+getClass() );
        return value;
    }

    @Override
    public Quality getQuality()
    {
        return qualityValue;
    }
    
    @Override
    public void mergeReports(DataElementPathSet inputReports, Report outputReport) throws Exception
    {
    }
    
    protected void raiseWarning()
    {
        if(qualityValue != Quality.ERROR) qualityValue = Quality.WARN;
    }
    
    protected void raiseError()
    {
        qualityValue = Quality.ERROR;
    }

    /**
     * Helper method for mergeReports which copies all graphs (getName()+" chart") into merged report
     */
    protected void copyGraphs(DataElementPathSet inputReports, Report outputReport, String filePrefix) throws Exception
    {
        int nImages = 0;
        for(DataElementPath path: inputReports)
        {
            DataElement de = path.getChildPath(getName()+" chart").optDataElement();
            if(de instanceof ImageElement)
            {
                BufferedImage image = ((ImageElement)de).getImage(null);
                if(image != null)
                {
                    if(nImages == 0)
                    {
                        outputReport.addSubHeader(getName());
                        outputReport.addHTML("<table>");
                    }
                    if(nImages % 2 == 0)
                    {
                        outputReport.addHTML("<tr>");
                    }
                    outputReport.addHTML("<td>");
                    outputReport.addSubSubHeader(path.getName());
                    outputReport.addImage(image, filePrefix+"_"+nImages, path.getName());
                    nImages++;
                }
            }
        }
        if(nImages > 0)
        {
            outputReport.addHTML("</table>");
        }
    }

    /**
     * Helper method for mergeReports: combine all input tables into one
     */
    protected void mergeTables(DataElementPathSet inputReports, Report outputReport, String[] columns) throws Exception
    {
        List<NativeArray> data = new ArrayList<>();
        for(DataElementPath path: inputReports)
        {
            TableDataCollection table = path.getChildPath(getName()).optDataElement(TableDataCollection.class);
            if(table == null) continue;
            try
            {
                if(table.getColumnModel().getColumnCount() == 0)
                {
                    Object[] values = new Object[columns.length];
                    values[0] = path.getName();
                    values[1] = 0;
                    for(int i=2; i<values.length; i++) values[i] = "n/a";
                    data.add(new NativeArray(values));
                    continue;
                }
                boolean first = true;
                for(RowDataElement rde: table)
                {
                    Object[] values = new Object[columns.length];
                    values[0] = first?path.getName():"";
                    values[1] = first?table.getSize():"";
                    values[2] = rde.getName();
                    for(int i=3; i<values.length; i++) values[i] = rde.getValue(columns[i]);
                    data.add(new NativeArray(values));
                    first = false;
                }
            }
            catch( Exception e )
            {
            }
        }
        if(data.size() > 0)
        {
            outputReport.addSubHeader(getName());
            outputReport.addTable(new NativeArray(columns), new NativeArray(data.toArray(new NativeArray[data.size()])), "data");
        }
    }
}
