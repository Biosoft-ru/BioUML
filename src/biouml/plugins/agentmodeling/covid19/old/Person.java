package biouml.plugins.agentmodeling.covid19.old;

public class Person
{
    public int age;
    double p_severe;
    double p_critical;
    double immune;

    int index;
    boolean infected = false; //immune?
    
    public Person(int index)
    {
        this.index = index;
    }
}