package biouml.plugins.optimization;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.plugins.optimization.ExperimentalTableSupport.WeightMethod;
import biouml.standard.diagram.CompositeDiagramType;
import biouml.standard.diagram.Util;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

public class ParameterConnection extends Option implements DataElement
{
    private OptimizationExperiment experiment;
    private int connectionNumber;

    private Diagram diagram;

    private String subdiagramPath = "";
    private String nameInFile = "";
    private String nameInDiagram = "";

    private int relativeTo = -1;

    public ParameterConnection()
    {
    }

    public ParameterConnection(OptimizationExperiment experiment, int connectionNumber)
    {
        this.connectionNumber = connectionNumber;
        this.experiment = experiment;
    }

    public void setDiagram(Diagram diagram)
    {
        this.diagram = diagram;
    }
    public Diagram getDiagram()
    {
        return this.diagram;
    }

    public OptimizationExperiment getExperiment()
    {
        return experiment;
    }

    @PropertyName ( "Path" )
    @PropertyDescription ( "Path" )
    public String getSubdiagramPath()
    {
        return this.subdiagramPath;
    }
    public void setSubdiagramPath(String subdiagramPath)
    {
        String oldValue = this.subdiagramPath;
        this.subdiagramPath = subdiagramPath;
        Diagram innerDiagram = Util.getInnerDiagram( diagram, subdiagramPath );
        setNameInDiagram(innerDiagram.getRole(EModel.class).getVariables().getNameList().get(0));
        firePropertyChange("subdiagramPath", oldValue, subdiagramPath);
    }

    @PropertyName ( "Name in the file" )
    @PropertyDescription ( "Name in the file" )
    public String getNameInFile()
    {
        return this.nameInFile;
    }
    public void setNameInFile(String nameInFile)
    {
        String oldValue = this.nameInFile;
        this.nameInFile = nameInFile;
        if( !oldValue.equals("") )
            firePropertyChange("nameInFile", oldValue, nameInFile);
    }

    @PropertyName ( "Name in the model" )
    @PropertyDescription ( "Name in the model" )
    public String getNameInDiagram()
    {
        return this.nameInDiagram;
    }
    public void setNameInDiagram(String nameInDiagram)
    {
        String oldValue = this.nameInDiagram;
        this.nameInDiagram = nameInDiagram;
        firePropertyChange("nameInDiagram", oldValue, nameInDiagram);
    }

    public String getVariableNameInDiagram()
    {
        if(getSubdiagramPath().equals(""))
            return getNameInDiagram();
        else
            return getSubdiagramPath() + "/" + getNameInDiagram();
    }

    @PropertyName ( "Time point" )
    @PropertyDescription ( "The time point relative to which all experimental values will be recalculated. If the point is unspecified, the values are assumed to be exact." )
    public int getRelativeTo()
    {
        return relativeTo;
    }
    public void setRelativeTo(int relativeTo)
    {
        int oldValue = this.relativeTo;
        this.relativeTo = relativeTo;
        firePropertyChange("relativeTo", oldValue, relativeTo);
    }

    @PropertyName ( "Weight" )
    @PropertyDescription ( "Weight" )
    public double getWeight()
    {
        if( experiment.getTableSupport() != null )
            return experiment.getTableSupport().getWeights()[connectionNumber];
        return 0;
    }
    public void setWeight(double weight)
    {
        if( experiment.getTableSupport() != null )
            experiment.getTableSupport().getWeights()[connectionNumber] = weight;
    }

    /**
     * The method using for the specification of {@link ParameterConnectionBeanInfo}
     */
    public boolean isSubdiagramPathHidden()
    {
        return ! ( diagram.getType() instanceof CompositeDiagramType );
    }

    /**
     * The method using for the specification of {@link ParameterConnectionBeanInfo}
     */
    public boolean isWeightReadOnly()
    {
        return !WeightMethod.toString(WeightMethod.EDITED).equals(experiment.getTableSupport().getWeightMethod());
    }

    /**
     * The method using for the specification of {@link ParameterConnectionBeanInfo}
     */
    public boolean isSteadyState()
    {
        return experiment.isSteadyState();
    }

    @Override
    public String getName()
    {
        return this.nameInFile;
    }
    @Override
    public DataCollection getOrigin()
    {
        return null;
    }

    public ParameterConnection clone(OptimizationExperiment experiment, int connectionNumber)
    {
        ParameterConnection clone = new ParameterConnection(experiment, connectionNumber);
        clone.setDiagram(getDiagram());
        clone.setSubdiagramPath(getSubdiagramPath());
        clone.setNameInDiagram(getNameInDiagram());
        clone.setNameInFile(getNameInFile());
        clone.setWeight(getWeight());
        clone.setRelativeTo(getRelativeTo());
        return clone;
    }
}
