package biouml.plugins.agentmodeling.covid19;

/**
 * @author axec
 * Person in population
 */
public class Person
{
    public int age;
    double p_severe;
    double p_critical;
    boolean immune;
    public int vaccinated;

    int recovered = Integer.MAX_VALUE;
    
    int index;
    int variant;
    boolean infected = false; //immune?
    
    /*public Person(int index) //before 5 April 2022
    {
        this.index = index;
    }*/ 
    
    //after 5 April 2022
    
     public Person(int index, int variant) 
    {
        this.index = index;
        this.variant = variant; //update 5 April 2022
    }
}