package biouml.plugins.agentmodeling.covid19;

public class PopulationStatistics
{
    //proportions of different ages in population:
    // ith element of array means age group [ i , i + 9]. E.g. ageProportion[4] - proportion of persons in age group 40 - 49 years
    private double[] ageProportion = new double[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    //probabilities of random person to be younger then given age
    // ith element of array means probability of a random person to be younger then i + 9 years.
    // E.g. ageProbability[4] - probability of being younger then 49 years
    double[] ageProbability = null;
    
    //probabilities of severe symptoms for different age groups
    double[] ageSevere = new double[] {0, 0.0008, 0.0208, 0.0686, 0.085, 0.0163, 0.0236, 0.0332, 0.0368, 0.0368}; 
    
    //probabilities of critical symptoms for different age groups
    double[] ageCritical = new double[] {0.05, 0.05, 0.05, 0.05, 0.063, 0.0122, 0.0274, 0.0432, 0.0709, 0.0709};
    
    //number of average contacts for different age groups. Not used for now.
    private double[] ageContacts;
    
    //probability of preexisting immunity for different age groups. Not used for now.
    private double[] ageImmunity;

    public PopulationStatistics()
    {
        this.ageProbability = calculateAgeProbability( ageProportion );
    }

    public void setAgeProportion(double[] ageProportion)
    {
        this.ageProportion = ageProportion;
        this.ageProbability = calculateAgeProbability( ageProportion );
    }

    public void setSevereChance(double[] severeChance)
    {
        this.ageSevere = severeChance.clone();
    }

    public void setCriticalChance(double[] criticalChance)
    {
        this.ageCritical = criticalChance.clone();
    }

    /**
     * Calculate age probabilities on the base of age proportions  
     */
    private double[] calculateAgeProbability(double[] ageProportion)
    {
        double[] result = new double[ageProportion.length];
        result[0] = ageProportion[0] / 100;
        for( int i = 1; i < ageProportion.length - 1; i++ )
            result[i] = ageProportion[i] / 100 + result[i - 1];
        result[ageProportion.length - 1] = 1;
        return result;
    }
}
