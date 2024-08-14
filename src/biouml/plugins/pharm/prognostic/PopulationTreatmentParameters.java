package biouml.plugins.pharm.prognostic;

import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

public class PopulationTreatmentParameters extends AbstractAnalysisParameters
{
    private DataElementPath population;
    private String[] drugs;

    private DataElementPath input;
    private DataElementPath output;

    private int treatmentTime = 2419200; //in seconds

    private DataElementPath generationInfo;

    public DataElementPath getPopulation()
    {
        return population;
    }
    public void setPopulation(DataElementPath population)
    {
        Object oldValue = this.population;
        this.population = population;
        firePropertyChange( "population", oldValue, population );
    }

    public String[] getDrugs()
    {
        return drugs;
    }
    public void setDrugs(String[] drugs)
    {
        Object oldValue = this.drugs;
        this.drugs = drugs;
        firePropertyChange( "drugs", oldValue, drugs );
    }

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

    public DataElementPath getGenerationInfo()
    {
        return generationInfo;
    }
    public void setGenerationInfo(DataElementPath generationInfo)
    {
        DataElementPath oldValue = this.generationInfo;
        this.generationInfo = generationInfo;
        firePropertyChange( "generationInfo", oldValue, generationInfo );
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

    public int getTreatmentTime()
    {
        return this.treatmentTime;
    }
    public void setTreatmentTime(int treatmentTime)
    {
        int oldValue = this.treatmentTime;
        this.treatmentTime = treatmentTime;
        firePropertyChange( "treatmentTime", oldValue, treatmentTime );
    }

    public static class Drug
    {
        private String title;
        private String[] variables;

        public Drug(String id, String[] variables)
        {
            this.title = MessageBundle.getMessage( id );
            this.variables = variables;
        }

        public String getTitle()
        {
            return this.title;
        }

        public String[] getVariables()
        {
            return this.variables;
        }
    }

    public static String[] getDrugVariables(String drugTitle)
    {
        return StreamEx.of( ALL_DRUGS ).findFirst( drug -> drug.getTitle().equals( drugTitle ) ).get().getVariables();
    }

    public static Drug[] ALL_DRUGS = new Drug[] {
            new Drug( "ALISKIREN_150", new String[] {"Aliskiren_150"} ),
            new Drug( "ALISKIREN_300", new String[] {"Aliskiren_300"} ),
            new Drug( "AZILSARTAN_40", new String[] {"Azilsartan_40"} ),
            new Drug( "LOSARTAN_50", new String[] {"Losartan_50"} ),
            new Drug( "LOSARTAN_100", new String[] {"Losartan_100"} ),
            new Drug( "ENALAPRIL_20", new String[] {"Enalapril_20"} ),
            new Drug( "AMLODIPINE_5", new String[] {"Amlodipine_5"} ),
            new Drug( "BISOPROLOL_5", new String[] {"Bisoprolol_5"} ),
            new Drug( "HCTZ_12_5", new String[] {"HCTZ_12_5"} ),

            new Drug( "ALISKIREN_150_AMLODIPINE_5", new String[] {"Aliskiren_150", "Amlodipine_5"} ),
            new Drug( "ALISKIREN_150_BISOPROLOL_5", new String[] {"Aliskiren_150", "Bisoprolol_5"} ),
            new Drug( "ALISKIREN_150_HCTZ_12_5", new String[] {"Aliskiren_150", "HCTZ_12_5"} ),
            new Drug( "ALISKIREN_300_AMLODIPINE_5", new String[] {"Aliskiren_300", "Amlodipine_5"} ),
            new Drug( "ALISKIREN_300_BISOPROLOL_5", new String[] {"Aliskiren_300", "Bisoprolol_5"} ),
            new Drug( "ALISKIREN_300_HCTZ_12_5", new String[] {"Aliskiren_300", "HCTZ_12_5"} ),
            new Drug( "AZILSARTAN_40_AMLODIPINE_5", new String[] {"Azilsartan_40", "Amlodipine_5"} ),
            new Drug( "AZILSARTAN_40_BISOPROLOL_5", new String[] {"Azilsartan_40", "Bisoprolol_5"} ),
            new Drug( "AZILSARTAN_40_HCTZ_12_5", new String[] {"Azilsartan_40", "HCTZ_12_5"} ),
            new Drug( "LOSARTAN_50_AMLODIPINE_5", new String[] {"Losartan_50", "Amlodipine_5"} ),
            new Drug( "LOSARTAN_50_BISOPROLOL_5", new String[] {"Losartan_50", "Bisoprolol_5"} ),
            new Drug( "LOSARTAN_50_HCTZ_12_5", new String[] {"Losartan_50", "HCTZ_12_5"} ),
            new Drug( "LOSARTAN_100_AMLODIPINE_5", new String[] {"Losartan_100", "Amlodipine_5"} ),
            new Drug( "LOSARTAN_100_BISOPROLOL_5", new String[] {"Losartan_100", "Bisoprolol_5"} ),
            new Drug( "LOSARTAN_100_HCTZ_12_5", new String[] {"Losartan_100", "HCTZ_12_5"} ),
            new Drug( "ENALAPRIL_20_AMLODIPINE_5", new String[] {"Enalapril_20", "Amlodipine_5"} ),
            new Drug( "ENALAPRIL_20_BISOPROLOL_5", new String[] {"Enalapril_20", "Bisoprolol_5"} ),
            new Drug( "ENALAPRIL_20_HCTZ_12_5", new String[] {"Enalapril_20", "HCTZ_12_5"} ),

            new Drug( "ALISKIREN_150_AMLODIPINE_5_BISOPROLOL_5", new String[] {"Aliskiren_150", "Amlodipine_5", "Bisoprolol_5"} ),
            new Drug( "ALISKIREN_150_AMLODIPINE_5_HCTZ_12_5", new String[] {"Aliskiren_150", "Amlodipine_5", "HCTZ_12_5"} ),
            new Drug( "ALISKIREN_150_BISOPROLOL_5_HCTZ_12_5", new String[] {"Aliskiren_150", "Bisoprolol_5", "HCTZ_12_5"} ),
            new Drug( "ALISKIREN_300_AMLODIPINE_5_BISOPROLOL_5", new String[] {"Aliskiren_300", "Amlodipine_5", "Bisoprolol_5"} ),
            new Drug( "ALISKIREN_300_AMLODIPINE_5_HCTZ_12_5", new String[] {"Aliskiren_300", "Amlodipine_5", "HCTZ_12_5"} ),
            new Drug( "ALISKIREN_300_BISOPROLOL_5_HCTZ_12_5", new String[] {"Aliskiren_300", "Bisoprolol_5", "HCTZ_12_5"} ),
            new Drug( "AZILSARTAN_40_AMLODIPINE_5_BISOPROLOL_5", new String[] {"Azilsartan_40", "Amlodipine_5", "Bisoprolol_5"} ),
            new Drug( "AZILSARTAN_40_AMLODIPINE_5_HCTZ_12_5", new String[] {"Azilsartan_40", "Amlodipine_5", "HCTZ_12_5"} ),
            new Drug( "AZILSARTAN_40_BISOPROLOL_5_HCTZ_12_5", new String[] {"Azilsartan_40", "Bisoprolol_5", "HCTZ_12_5"} ),
            new Drug( "LOSARTAN_50_AMLODIPINE_5_BISOPROLOL_5", new String[] {"Losartan_50", "Amlodipine_5", "Bisoprolol_5"} ),
            new Drug( "LOSARTAN_50_AMLODIPINE_5_HCTZ_12_5", new String[] {"Losartan_50", "Amlodipine_5", "HCTZ_12_5"} ),
            new Drug( "LOSARTAN_50_BISOPROLOL_5_HCTZ_12_5", new String[] {"Losartan_50", "Bisoprolol_5", "HCTZ_12_5"} ),
            new Drug( "LOSARTAN_100_AMLODIPINE_5_BISOPROLOL_5", new String[] {"Losartan_100", "Amlodipine_5", "Bisoprolol_5"} ),
            new Drug( "LOSARTAN_100_AMLODIPINE_5_HCTZ_12_5", new String[] {"Losartan_100", "Amlodipine_5", "HCTZ_12_5"} ),
            new Drug( "LOSARTAN_100_BISOPROLOL_5_HCTZ_12_5", new String[] {"Losartan_100", "Bisoprolol_5", "HCTZ_12_5"} ),
            new Drug( "ENALAPRIL_20_AMLODIPINE_5_BISOPROLOL_5", new String[] {"Enalapril_20", "Amlodipine_5", "Bisoprolol_5"} ),
            new Drug( "ENALAPRIL_20_AMLODIPINE_5_HCTZ_12_5", new String[] {"Enalapril_20", "Amlodipine_5", "HCTZ_12_5"} ),
            new Drug( "ENALAPRIL_20_BISOPROLOL_5_HCTZ_12_5", new String[] {"Enalapril_20", "Bisoprolol_5", "HCTZ_12_5"} )};
}
