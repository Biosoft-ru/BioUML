
package ru.biosoft.analysis;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.workbench.editors.GenericComboBoxItem;
import ru.biosoft.workbench.editors.GenericEditorData;

/**
 * Abstract normalization analysis parameters common for Affy, Agilent, Illumina
 */
public class NormalizationParameters extends AbstractAnalysisParameters
{
    private DataElementPath outputPath;
    private GenericComboBoxItem outputLogarithmBase;
    
    public NormalizationParameters()
    {
        GenericEditorData.registerValues("outputLogarithmBase", Util.getLogarithmBaseNames());
        setOutputLogarithmBaseName("log2");
    }
    
    public DataElementPath getOutputPath()
    {
        return outputPath;
    }

    public void setOutputPath(DataElementPath outputPath)
    {
        Object oldValue = this.outputPath;
        this.outputPath = outputPath;
        firePropertyChange("outputPath", oldValue, this.outputPath);
    }

    public GenericComboBoxItem getOutputLogarithmBase()
    {
        return outputLogarithmBase;
    }

    public void setOutputLogarithmBase(GenericComboBoxItem type)
    {
        GenericComboBoxItem oldValue = this.outputLogarithmBase;
        this.outputLogarithmBase = type;
        firePropertyChange("outputLogarithmType", oldValue, outputLogarithmBase);
    }

    public void setOutputLogarithmBaseName(String name)
    {
        setOutputLogarithmBase(new GenericComboBoxItem("outputLogarithmBase", name));
    }

    public Integer getOutputLogarithmBaseCode()
    {
        return Util.logBaseNameToCode.get(outputLogarithmBase.getValue().toString());
    }
}
