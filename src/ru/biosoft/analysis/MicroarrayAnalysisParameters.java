package ru.biosoft.analysis;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Properties;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.PropertyDescriptorEx;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.repository.IconFactory;
import ru.biosoft.analysis.gui.MessageBundle;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.columnbeans.ColumnGroup;
import ru.biosoft.util.TextUtil;

/**
 * Basic class for microarray analysis parameters
 * provides:
 * - experiment data - input table with user defined columns (without time points)
 * - outputTablePath - output table
 * - pValue - threshold for P-value(result significance)
 * - isCalculatingFDR - flag for calculating FDR
 * - threshold - threshold for data values
 * @author axec
 *
 */
public abstract class MicroarrayAnalysisParameters extends AbstractAnalysisParameters implements PropertyChangeListener
{
    private ColumnGroup experimentData = new ColumnGroup(this);
    private Double pvalue = 0.01;
    private Threshold threshold = new Threshold(this, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    private Boolean fdr = false;
    private DataElementPath outputTablePath;

    public static final int FDR_PERMUTATION_NUMBER = 50;

    public ColumnGroup getExperimentData()
    {
        return experimentData;
    }
    public void setExperimentData(ColumnGroup columns)
    {
        ColumnGroup oldValue = experimentData;
        boolean isNumeric = experimentData.isNumerical();
        experimentData = ( columns != null ) ? columns : new ColumnGroup(this);
        experimentData.setParent(this);
        experimentData.setNumerical(isNumeric);
        firePropertyChange("experimentData", oldValue, columns);
    }

    public DataElementPath getOutputTablePath()
    {
        return outputTablePath;
    }

    public void setOutputTablePath(DataElementPath path)
    {
        DataElementPath oldValue = outputTablePath;
        this.outputTablePath = path;
        firePropertyChange("outputTablePath", oldValue, outputTablePath);
    }

    public TableDataCollection getOutputTable()
    {
        TableDataCollection result = TableDataCollectionUtils.createTableDataCollection( outputTablePath.getParentCollection(),
                outputTablePath.getName() );
        if( experimentData.getTable() != null )
        {
            DataCollectionUtils.copyPersistentInfo(result, experimentData.getTable());
        }
        return result;
    }

    public DataCollection getOutputCollection()
    {
        return getOutputTablePath().optParentCollection();
    }

    //Name for output TableDataCollection
    public String getOutputName()
    {
        return getOutputTablePath() == null ? null : getOutputTablePath().getName();
    }

    //Statistical P-value for output data
    public Double getPvalue()
    {
        return pvalue;
    }
    public void setPvalue(Double p)
    {
        Double oldValue = pvalue;
        pvalue = p;
        firePropertyChange("pvalue", oldValue, p);
    }

    public Double getScoreThreshold()
    {
        return Math.abs(Math.log10(getPvalue()));
    }

    //Threshold for lowest value in data (values lower then treshold will be replced with it)
    public Threshold getThreshold()
    {
        return threshold;
    }
    public void setThreshold(Threshold thresh)
    {
        Threshold oldValue = threshold;
        threshold = thresh;
        threshold.setParent(this);
        firePropertyChange("threshold", oldValue, threshold);
    }

    public Double getThresholdUp()
    {
        return threshold.getThresholdUp();
    }

    public Double getThresholdDown()
    {
        return threshold.getThresholdDown();
    }

    //If analysis should calculate False Discovery Rate during process
    //!WARNING: it will take a long time!
    public Boolean isFdr()
    {
        return fdr;
    }
    public void setFdr(Boolean isCalculating) throws Exception
    {
        Boolean oldValue = fdr;
        fdr = isCalculating;
        firePropertyChange("fdr", oldValue, fdr);
    }

    @Override
    public @Nonnull String[] getInputNames()
    {
        return new String[] {"experimentData/tablePath"};
    }

    public TableDataCollection getExperiment()
    {
        return getExperimentData()==null?null:getExperimentData().getTable();
    }
    public void setExperiment(TableDataCollection table)
    {
        getExperimentData().setTable(table);
    }

    @Override
    public void read(Properties properties, String prefix)
    {
        super.read(properties, prefix);
        String leftGroupStr = properties.getProperty(prefix + "experimentData");
        if( leftGroupStr != null )
        {
            experimentData = ColumnGroup.readObject(this, leftGroupStr);
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        this.firePropertyChange(evt);
    }

    public String getIcon()
    {
        return IconFactory.getIconId(getExperimentData().getTablePath());
    }

    public static class Threshold extends Option
    {
        private Double thresholdDown;
        private Double thresholdUp;

        public Threshold(Option parent, Double thresholdDown, Double thresholdUp)
        {
            super(parent);
            this.thresholdDown = thresholdDown;
            this.thresholdUp = thresholdUp;
        }

        public Threshold(String from)
        {
            String[] fields = TextUtil.split( from, ',' );
            this.thresholdDown = Double.NEGATIVE_INFINITY;
            this.thresholdUp = Double.POSITIVE_INFINITY;
            try
            {
                this.thresholdDown = Double.parseDouble(fields[0]);
                this.thresholdUp = Double.parseDouble(fields[1]);
            }
            catch( NumberFormatException e )
            {
            }
        }

        public Double getThresholdDown()
        {
            return thresholdDown;
        }
        public void setThresholdDown(Double threshold)
        {
            Double oldValue = this.thresholdDown;
            thresholdDown = threshold;
            firePropertyChange("thresholdDown", thresholdDown, oldValue);
        }

        public Double getThresholdUp()
        {
            return thresholdUp;
        }
        public void setThresholdUp(Double threshold)
        {
            Double oldValue = this.thresholdUp;
            thresholdUp = threshold;
            firePropertyChange("thresholdUp", thresholdUp, oldValue);
        }

        @Override
        public String toString()
        {
            return thresholdDown + "," + thresholdUp;
        }
    }

    public static class ThresholdBeanInfo extends BeanInfoEx
    {
        public ThresholdBeanInfo()
        {
            super(Threshold.class, MessageBundle.class.getName());
            beanDescriptor.setDisplayName(getResourceString("PN_THRESHOLD"));
            beanDescriptor.setShortDescription(getResourceString("PN_THRESHOLD"));
            //            setHideChildren(true);
            //            setCompositeEditor("thresholdDown;thresholdUp", new java.awt.GridLayout(1, 2));
        }

        @Override
        public void initProperties() throws Exception
        {
            add(new PropertyDescriptorEx("thresholdDown", beanClass), getResourceString("PN_LOWER_BOUNDARY"),
                    getResourceString("PD_LOWER_BOUNDARY"));
            add(new PropertyDescriptorEx("thresholdUp", beanClass), getResourceString("PN_UPPER_BOUNDARY"),
                    getResourceString("PD_UPPER_BOUNDARY"));
        }
    }
}