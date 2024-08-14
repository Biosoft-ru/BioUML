package biouml.plugins.modelreduction;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.util.bean.BeanInfoEx2;

@PropertyName ("Parameters")
public class ReactionsAnalysisParameters extends AbstractAnalysisParameters
{
    public ReactionsAnalysisParameters()
    {
        analysisTarget = new AnalysisTarget(this);
    }

    private DataElementPath input;

    @PropertyName("Input diagram")
    @PropertyDescription("A diagram to be analyzed.")
    public DataElementPath getInput()
    {
        return input;
    }
    public void setInput(DataElementPath input)
    {
        Object oldValue = this.input;
        this.input = input;
        firePropertyChange("input", oldValue, input);
    }

    private DataElementPath output;

    @PropertyName ("Result path")
    @PropertyDescription ("A folder to save results of the analysis.")
    public DataElementPath getOutput()
    {
        return output;
    }
    public void setOutput(DataElementPath output)
    {
        Object oldValue = this.output;
        this.output = output;
        firePropertyChange("output", oldValue, output);
    }

    private AnalysisTarget analysisTarget;

    @PropertyName ("Analysis target")
    @PropertyDescription ("Targets for the reactions analysis.")
    public AnalysisTarget getAnalysisTarget()
    {
        return analysisTarget;
    }
    public void setAnalysisTarget(AnalysisTarget analysisTarget)
    {
        this.analysisTarget = analysisTarget;
    }

    private double threshold = 10.0;

    @PropertyName ("Threshold")
    @PropertyDescription ("Threshold")
    public double getThreshold()
    {
        return threshold;
    }
    public void setThreshold(double threshold)
    {
        this.threshold = threshold;
    }

    private double initialTime = 0.0;

    @PropertyName ("Initial time")
    @PropertyDescription ("Initial time.")
    public double getInitialTime()
    {
        return initialTime;
    }
    public void setInitialTime(double initialTime)
    {
        this.initialTime = initialTime;
    }

    private double completionTime = 100.0;

    @PropertyName ("Completion time")
    @PropertyDescription ("Completion time.")
    public double getCompletionTime()
    {
        return completionTime;
    }
    public void setCompletionTime(double completionTime)
    {
        this.completionTime = completionTime;
    }

    private double timeIncrement = 1.0;

    @PropertyName ("Time increment")
    @PropertyDescription ("Time increment.")
    public double getTimeIncrement()
    {
        return timeIncrement;
    }
    public void setTimeIncrement(double timeIncrement)
    {
        this.timeIncrement = timeIncrement;
    }

    public boolean isSimulationParametersHidden()
    {
        return !analysisTarget.isPseudoMonomolecularity();
    }

    public static class AnalysisTarget extends Option
    {
        public AnalysisTarget(Option parent)
        {
            setParent(parent);
        }

        private boolean linearity = true;

        @PropertyName ("Linear reactions")
        @PropertyDescription ("Should linear reactions be found?")
        public boolean isLinearity()
        {
            return linearity;
        }
        public void setLinearity(boolean linearity)
        {
            this.linearity = linearity;
        }

        private boolean monomolecularity = true;

        @PropertyName ("Monomolecular reactions")
        @PropertyDescription ("Should monomolecular reactions be found?")
        public boolean isMonomolecularity()
        {
            return monomolecularity;
        }
        public void setMonomolecularity(boolean monomolecularity)
        {
            this.monomolecularity = monomolecularity;
        }

        private boolean pseudoMonomolecularity = true;

        @PropertyName ("Pseudo-monomolecular reactions")
        @PropertyDescription ("Should pseudo-monomolecular reactions be found?")
        public boolean isPseudoMonomolecularity()
        {
            return pseudoMonomolecularity;
        }
        public void setPseudoMonomolecularity(boolean pseudoMonomolecularity)
        {
            this.pseudoMonomolecularity = pseudoMonomolecularity;
            firePropertyChange("*", null, null);

        }
    }

    public static class AnalysisTargetBeanInfo extends BeanInfoEx2<AnalysisTarget>
    {
        public AnalysisTargetBeanInfo()
        {
        	super(AnalysisTarget.class);
        }

        @Override
        public void initProperties() throws Exception
        {
        	property("linearity").add();
        	property("monomolecularity").add();
        	property("pseudoMonomolecularity").add();
        }
    }
}
