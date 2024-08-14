package ru.biosoft.analysiscore;

import java.util.Properties;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataElementPath;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

/**
 * Base interface for {@link AnalysisMethod} parameters
 */
@PropertyName("Analysis parameters")
@PropertyDescription("Specify analysis parameters")
public interface AnalysisParameters extends Cloneable
{
    /**
     * Return input parameters names
     */
    public @Nonnull String[] getInputNames();
    /**
     * Return output parameters names
     */
    public @Nonnull String[] getOutputNames();
    
    /**
     * May be used to change available parameters
     */
    public void setExpertMode(boolean flag);
    
    /**
     * Read parameters from {@link Properties} object
     */
    public void read(Properties properties, String prefix);
    /**
     * Save parameters to {@link Properties} object
     */
    public void write(Properties properties, String prefix);
    /**
     * Clone parameters object
     */
    public Object clone();
    
    /**
     * Return list of output objects which already exists
     * Handy to use in overwrite prompt window
     */
    public DataElementPath[] getExistingOutputNames();
}
