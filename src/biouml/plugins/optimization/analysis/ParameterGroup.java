package biouml.plugins.optimization.analysis;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Variable;
import biouml.standard.diagram.DiagramUtility;
import ru.biosoft.util.bean.BeanInfoEx2;

public class ParameterGroup extends Option
{
    private EstimatedParameter[] parameters = new EstimatedParameter[] {};
    private EstimatedParameter[] availableParameters = new EstimatedParameter[] {};

    void setDiagram(Diagram diagram)
    {
        this.availableParameters = diagram.getRole(EModel.class).getVariables().stream().map(v -> new EstimatedParameter(diagram, v))
                .toArray(EstimatedParameter[]::new);
    }

    public EstimatedParameter[] getAvailableParameters()
    {
        return availableParameters;
    }
    
    @PropertyName ( "Parameters" )
    public EstimatedParameter[] getParameters()
    {
        return parameters;
    }
    public void setParameters(EstimatedParameter[] parameters)
    {
        this.parameters = parameters.clone();
    }

    public String calcParameterName(Integer index, Object parameter)
    {
        return ( (EstimatedParameter)parameter ).getName();
    }
    
    public static class EstimatedParameter extends ru.biosoft.analysis.optimization.Parameter
    {
        public EstimatedParameter(Diagram diagram, Variable variable)
        {
        	super(DiagramUtility.generatPath(DiagramUtility.generatPath(diagram), variable.getName()), variable.getInitialValue());
        }

        public EstimatedParameter(String name, double value, double lowerBound, double upperBound)
        {
            super(name, value, lowerBound, upperBound);
        }

        @Override
        public String toString()
        {
            return getName();
        }
    }

    public static class EstimatedParameterBeanInfo extends BeanInfoEx2<EstimatedParameter>
    {
        public EstimatedParameterBeanInfo()
        {
            super(EstimatedParameter.class);
            this.setHideChildren(true);
            setCompositeEditor("value;lowerBound;upperBound", new java.awt.GridLayout(1, 4));
        }

        @Override
        public void initProperties() throws Exception
        {
            add("value");
            add("lowerBound");
            add("upperBound");
        }
    }
}