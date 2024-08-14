
package ru.biosoft.analysiscore;

import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlEvent;

/**
 * @author anna
 *
 */
public class AnalysisParametersExceptionEvent extends JobControlEvent
{
    public AnalysisParametersExceptionEvent(JobControl jobControl)
    {
        super(jobControl);
    }
    public AnalysisParametersExceptionEvent(JobControl jobControl, String message)
    {
        super(jobControl, message);
    }
}
