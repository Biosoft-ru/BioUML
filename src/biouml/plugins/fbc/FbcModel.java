package biouml.plugins.fbc;


public interface FbcModel
{
    public void optimize();

    public double getOptimValue(String reactionName);

    public double getValueObjFunc();

    public String[] getReactionNames();
}
