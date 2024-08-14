package ru.biosoft.analysiscore;

import java.beans.PropertyChangeListener;
import java.util.Map;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataElement;

public interface AnalysisMethod extends DataElement
{
    public String getDescription();
    public void setDescription(String description);

    public AnalysisParameters getParameters();
    public void setParameters(AnalysisParameters parameters);
    public void validateParameters() throws IllegalArgumentException;

    public Logger getLogger();
    public void setLogger(Logger log);

    /**
     * @return estimation of task execution time (0 = very quick task, 1 = quite long task)
     */
    public double estimateWeight();

    /**
     * @return estimation of allocated memory in bytes
     */
    public long estimateMemory();

    /**
     * Generates scripts in available languages
     * @param parameters parameters for which to generate equivalent script
     * @return Map "language" -> "script"
     */
    public Map<String, String> generateScripts(AnalysisParameters parameters);

    public AnalysisJobControl getJobControl();

    public void addPropertyChangeListener(PropertyChangeListener l);
    public void removePropertyChangeListener(PropertyChangeListener l);
}
