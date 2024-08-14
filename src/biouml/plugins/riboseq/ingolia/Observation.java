package biouml.plugins.riboseq.ingolia;

public class Observation
{
    public enum Type { YES, NO, UNKNOWN }
    private Type type;
    private double[] predictors;
    private String description;
    private int position;

    public Observation(Type type, double[] predictors)
    {
        this.type = type;
        this.predictors = predictors;
    }

    public Type getType()
    {
        return type;
    }

    public double[] getPredictors()
    {
        return predictors;
    }
    
    public int getPosition()
    {
        return position;
    }
    
    public void setPosition(int position)
    {
        this.position = position;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }
}
