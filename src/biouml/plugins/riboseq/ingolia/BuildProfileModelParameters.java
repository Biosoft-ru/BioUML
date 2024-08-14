package biouml.plugins.riboseq.ingolia;

import ru.biosoft.access.core.DataElementPath;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

public class BuildProfileModelParameters extends BasicParameters
{
    private double learningFraction = 2.0/3.0;
    @PropertyName("Learning fraction")
    @PropertyDescription("Fraction of transcripts used for training SVM model")
    public double getLearningFraction()
    {
        return learningFraction;
    }
    public void setLearningFraction(double learningFraction)
    {
        this.learningFraction = learningFraction;
    }

    private long randomSeed;
    @PropertyName("Random seed")
    @PropertyDescription("Random seed for random number generator")
    public long getRandomSeed()
    {
        return randomSeed;
    }
    public void setRandomSeed(long randomSeed)
    {
        this.randomSeed = randomSeed;
    }
    
    private DataElementPath modelFile;
    @PropertyName("SVM model")
    @PropertyDescription("Resulting SVM model")
    public DataElementPath getModelFile()
    {
        return modelFile;
    }
    public void setModelFile(DataElementPath modelFile)
    {
        this.modelFile = modelFile;
    }
    
    private DataElementPath confusionMatrix;
    @PropertyName("Confusion matrix")
    @PropertyDescription("Resulting confusion matrix")
    public DataElementPath getConfusionMatrix()
    {
        return confusionMatrix;
    }
    public void setConfusionMatrix(DataElementPath confusionMatrix)
    {
        this.confusionMatrix = confusionMatrix;
    }
}
