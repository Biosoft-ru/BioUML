package biouml.plugins.pharm.prognostic;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.util.bean.BeanInfoEx2;

public class PopulationGenerationParameters extends AbstractAnalysisParameters
{
    private DataElementPath input;
    private DataElementPath output;
    private PatientPhysiology patientPhysiology = new PatientPhysiology();
    private OptimizationSettings optimizationSettings = new OptimizationSettings();

    private int patientsNumber = 500;

    public DataElementPath getInput()
    {
        return input;
    }
    public void setInput(DataElementPath input)
    {
        DataElementPath oldValue = this.input;
        this.input = input;
        firePropertyChange( "input", oldValue, input );
    }

    public DataElementPath getOutput()
    {
        return output;
    }
    public void setOutput(DataElementPath output)
    {
        DataElementPath oldValue = this.output;
        this.output = output;
        firePropertyChange( "output", oldValue, output );
    }

    public int getPatientsNumber()
    {
        return patientsNumber;
    }
    public void setPatientsNumber(int num)
    {
        this.patientsNumber = num;
    }

    public PatientPhysiology getPatientPhysiology()
    {
        return patientPhysiology;
    }

    public void setPatientPhysiology(PatientPhysiology patientPhysiology)
    {
        this.patientPhysiology = patientPhysiology;
    }

    public OptimizationSettings getOptimizationSettings()
    {
        return optimizationSettings;
    }
    public void setOptimizationSettings(OptimizationSettings optimizationSettings)
    {
        this.optimizationSettings = optimizationSettings;
    }

    public static class OptimizationSettings
    {
        private int slowModelNumOfIterations = 125;
        private int slowModelSurvivalSize = 100;

        private int fastModelNumOfIterations = 150;
        private int fastModelSurvivalSize = 15;

        private boolean checkSodiumLoadExperiment = true;

        private double possibleDeviation = 0.05;
        
        public int getSlowModelNumOfIterations()
        {
            return slowModelNumOfIterations;
        }
        public void setSlowModelNumOfIterations(int slowModelNumOfIterations)
        {
            this.slowModelNumOfIterations = slowModelNumOfIterations;
        }

        public int getFastModelNumOfIterations()
        {
            return fastModelNumOfIterations;
        }
        public void setFastModelNumOfIterations(int fastModelNumOfIterations)
        {
            this.fastModelNumOfIterations = fastModelNumOfIterations;
        }

        public int getSlowModelSurvivalSize()
        {
            return slowModelSurvivalSize;
        }
        public void setSlowModelSurvivalSize(int slowModelSurvivalSize)
        {
            this.slowModelSurvivalSize = slowModelSurvivalSize;
        }

        public int getFastModelSurvivalSize()
        {
            return fastModelSurvivalSize;
        }
        public void setFastModelSurvivalSize(int fastModelSurvivalSize)
        {
            this.fastModelSurvivalSize = fastModelSurvivalSize;
        }

        public boolean isCheckSodiumLoadExperiment()
        {
            return checkSodiumLoadExperiment;
        }
        public void setCheckSodiumLoadExperiment(boolean checkSodiumLoadExperiment)
        {
            this.checkSodiumLoadExperiment = checkSodiumLoadExperiment;
        }

        public double getPossibleDeviation()
        {
            return possibleDeviation;
        }
        public void setPossibleDeviation(double possibleDeviation)
        {
            this.possibleDeviation = possibleDeviation;
        }
    }
    
    public static class OptimizationSettingsBeanInfo extends BeanInfoEx2<OptimizationSettings>
    {
        public OptimizationSettingsBeanInfo()
        {
            super( OptimizationSettings.class );
        }

        @Override
        public void initProperties() throws Exception
        {
            property( "slowModelNumOfIterations" ).title( "SLOW_MODEL_NUM_OF_ITERATIONS" ).add();
            property( "slowModelSurvivalSize" ).title( "SLOW_MODEL_SURVIVAL_SIZE" ).add();
            property( "fastModelNumOfIterations" ).title( "FAST_MODEL_NUM_OF_ITERATIONS" ).add();
            property( "fastModelSurvivalSize" ).title( "FAST_MODEL_SURVIVAL_SIZE" ).add();
            property( "possibleDeviation" ).title( "POSSIBLE_DEVIATION" ).add();
            property( "checkSodiumLoadExperiment" ).title( "CHECK_SODIUM_LOAD_EXPERIMENT" ).add();
        }

        @Override
        public String getResourceString(String key)
        {
            return MessageBundle.getMessage( key );
        }
    }
}
