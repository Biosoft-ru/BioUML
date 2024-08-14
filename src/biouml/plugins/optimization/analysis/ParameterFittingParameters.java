package biouml.plugins.optimization.analysis;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Variable;
import biouml.plugins.simulation.SimulationEngineWrapper;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysis.optimization.OptimizationMethod;
import ru.biosoft.analysis.optimization.methods.ASAOptMethod;
import ru.biosoft.analysis.optimization.methods.MOCellOptMethod;
import ru.biosoft.analysis.optimization.methods.MOPSOOptMethod;
import ru.biosoft.analysis.optimization.methods.SRESOptMethod;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;

@PropertyName ( "Parameters" )
public class ParameterFittingParameters extends AbstractAnalysisParameters
{
    private static final String SRES = "SRES";
    private static final String ASA = "ASA";
    private static final String GLBSOLVE = "GlbSolve";
    private static final String GENETIC = "Genetic";
    private static final String PARTICLE_SWARM = "Particle swarm";

    public static final String FEEDBACK_GENES = "Genes with feedback (second stage only)";
    public static final String ALL_GENES = "All genes (both stages)";
    public static final String NONFEEDBACK_GENES = "Genes with feedback (first stage only)";

    private DataElementPath diagramPath;
    private DataElementPath parametersTablePath;
    private String dataColumn;
    private DataElementPath experimentPath;
    private DataElementPath outputDiagram;
    private SimulationEngineWrapper engineWrapper;
    private OptimizationParameters algorithm = new OptimizationParameters(this, SRES);
    private static final String[] availableAlgorithms = new String[] {SRES, ASA, GLBSOLVE, GENETIC, PARTICLE_SWARM};
    private boolean allParameters = false;
    private ParameterGroup parameters = new ParameterGroup();
    private String regime = ALL_GENES;

    public ParameterFittingParameters()
    {
        setEngineWrapper(new SimulationEngineWrapper());
    }


    @PropertyName ( "Simulation engine" )
    public SimulationEngineWrapper getEngineWrapper()
    {
        return engineWrapper;
    }
    public void setEngineWrapper(SimulationEngineWrapper engineWrapper)
    {
        Object oldValue = this.engineWrapper;
        this.engineWrapper = engineWrapper;
        this.engineWrapper.setParent(this, "engineWrapper");
        firePropertyChange("engineWrapper", oldValue, this.engineWrapper);
    }

    @PropertyName ( "Input diagram" )
    public DataElementPath getDiagramPath()
    {
        return diagramPath;
    }
    public void setDiagramPath(DataElementPath diagramPath)
    {
        DataElement de = diagramPath.optDataElement();
        if( de instanceof Diagram && ( (Diagram)de ).getRole() instanceof EModel )
        {
            Diagram diagram = ( (Diagram)de );
            //            this.availableParameters = StreamEx.of(diagram.getRole(EModel.class).getVariables().getNameList()).toArray(String[]::new);
            Object oldValue = this.diagramPath;
            this.diagramPath = diagramPath;
            engineWrapper.setDiagram(diagram);
            engineWrapper.getEngine().setCompletionTime(5E6);
            engineWrapper.getEngine().setTimeIncrement(100);
            firePropertyChange("input", oldValue, diagramPath);
            parameters.setDiagram(diagram);

            this.outputDiagram = diagramPath.getSiblingPath(diagramPath.getName() + " optimized");
        }
    }

    @PropertyName ( "Experiment table" )
    public DataElementPath getExperimentPath()
    {
        return experimentPath;
    }
    public void setExperimentPath(DataElementPath experimentPath)
    {
        this.experimentPath = experimentPath;
    }

    @PropertyName ( "Result" )
    public DataElementPath getOutputDiagram()
    {
        return outputDiagram;
    }
    public void setOutputDiagram(DataElementPath outputDiagram)
    {
        this.outputDiagram = outputDiagram;
    }

    @PropertyName ( "Algorithm" )
    public OptimizationParameters getAlgorithm()
    {
        return algorithm;
    }
    public void setAlgorithm(OptimizationParameters optimizationType)
    {
        this.algorithm = optimizationType;
    }

    @PropertyName ( "Parameters group" )
    public ParameterGroup getParameters()
    {
        return parameters;
    }
    public void setParameters(ParameterGroup parameters)
    {
        Object oldValue = this.parameters;
        this.parameters = parameters;
        firePropertyChange("parameters", oldValue, parameters);
    }

    @PropertyName ( "All parameters" )
    public boolean isAllParameters()
    {
        return allParameters;
    }

    public void setAllParameters(boolean allParameters)
    {
        this.allParameters = allParameters;
    }

    @PropertyName ( "Parameters table" )
    public DataElementPath getParametersTablePath()
    {
        return parametersTablePath;
    }
    public void setParametersTablePath(DataElementPath parametersTablePath)
    {
        this.parametersTablePath = parametersTablePath;
    }

    @PropertyName ( "Data column" )
    public String getDataColumn()
    {
        return dataColumn;
    }
    public void setDataColumn(String dataColumn)
    {
        this.dataColumn = dataColumn;
    }

    @PropertyName ( "Regime" )
    public String getRegime()
    {
        return regime;
    }
    public void setRegime(String regime)
    {
        this.regime = regime;
    }

    public static String[] getAvailableRegimes()
    {
        return new String[] {ALL_GENES, FEEDBACK_GENES, NONFEEDBACK_GENES};
    }

    public static class ParameterGroup extends Option
    {
        private Parameter[] parameters = new Parameter[] {};
        private Parameter[] availableParameters = new Parameter[] {};

        private void setDiagram(Diagram diagram)
        {
            this.availableParameters = diagram.getRole(EModel.class).getVariables().stream().map(v -> new Parameter(v))
                    .toArray(Parameter[]::new);
        }

        @PropertyName ( "Parameters" )
        public Parameter[] getParameters()
        {
            return parameters;
        }
        public void setParameters(Parameter[] parameters)
        {
            this.parameters = parameters.clone();
        }

        public String calcParameterName(Integer index, Object parameter)
        {
            return ( (Parameter)parameter ).getName();
        }
    }

    public static class ParameterGroupBeanInfo extends BeanInfoEx2<ParameterGroup>
    {
        public ParameterGroupBeanInfo()
        {
            super(ParameterGroup.class);
        }

        @Override
        public void initProperties() throws Exception
        {
            PropertyDescriptorEx pde = new PropertyDescriptorEx("parameters", beanClass);
            pde.setChildDisplayName(beanClass.getMethod("calcParameterName", new Class[] {Integer.class, Object.class}));
            pde.setPropertyEditorClass(ParametersEditor.class);
            add(pde);
        }
    }

    public static class ParametersEditor extends GenericMultiSelectEditor
    {
        @Override
        protected Parameter[] getAvailableValues()
        {
            return ( (ParameterGroup)getBean() ).availableParameters;
        }
    }

    public static class Parameter extends Option
    {
        private String name;
        private double lowerBound;
        private double upperBound;
        private double initialValue;

        public Parameter(Variable variable)
        {
            this.name = variable.getName();
            this.initialValue = variable.getInitialValue();
            this.upperBound = initialValue * 1.5;
            this.lowerBound = initialValue * 0.5;
        }
        public String getName()
        {
            return name;
        }
        public void setName(String name)
        {
            this.name = name;
        }
        public double getLowerBound()
        {
            return lowerBound;
        }
        public void setLowerBound(double lowerBound)
        {
            this.lowerBound = lowerBound;
        }
        public double getUpperBound()
        {
            return upperBound;
        }
        public void setUpperBound(double upperBound)
        {
            this.upperBound = upperBound;
        }
        public double getInitialValue()
        {
            return initialValue;
        }
        public void setInitialValue(double initialValue)
        {
            this.initialValue = initialValue;
        }

        @Override
        public String toString()
        {
            return getName();
        }
    }

    public static class ParameterBeanInfo extends BeanInfoEx2<Parameter>
    {
        public ParameterBeanInfo()
        {
            super(Parameter.class);
            this.setHideChildren(true);
            setCompositeEditor("initialValue;lowerBound;upperBound", new java.awt.GridLayout(1, 4));
        }

        @Override
        public void initProperties() throws Exception
        {
            add("initialValue");
            add("lowerBound");
            add("upperBound");
        }
    }

    public static class OptimizationParameters extends Option
    {
        private Integer numOfIterations = 500; // for SRES and GLB
        private Integer survivalSize = 200;// for SRES
        private Double delta = 1e-11;// for ASA
        private String type;

        public OptimizationMethod createMethod()
        {
            switch( type )
            {
                case SRES:
                {
                    SRESOptMethod result = new SRESOptMethod(null, "optimization");
                    result.getParameters().setNumOfIterations(numOfIterations);
                    result.getParameters().setSurvivalSize(survivalSize);
                    return result;
                }
                case ASA:
                {
                    ASAOptMethod result = new ASAOptMethod(null, "optimization");
                    result.getParameters().setDelta(delta);
                    return result;
                }
                case PARTICLE_SWARM:
                {
                    MOPSOOptMethod result = new MOPSOOptMethod(null, "optimization");
                    result.getParameters().setNumberOfIterations(numOfIterations);
                    return result;
                }
                case GENETIC:
                    return new MOCellOptMethod(null, "optimization");
                default:
                    return new SRESOptMethod(null, "optimization");
            }
        }

        private OptimizationParameters(Option parent, String type)
        {
            super(parent);
            this.type = type;
        }

        @PropertyName ( "type" )
        public String getType()
        {
            return type;
        }
        public void setType(String type)
        {
            this.type = type;
        }

        @PropertyName ( "Iterations number" )
        public Integer getNumOfIterations()
        {
            return numOfIterations;
        }
        public void setNumOfIterations(Integer parameter)
        {
            this.numOfIterations = parameter;
        }

        @PropertyName ( "Survival size" )
        public Integer getSurvivalSize()
        {
            return survivalSize;
        }
        public void setSurvivalSize(Integer parameter)
        {
            this.survivalSize = parameter;
        }

        @PropertyName ( "Delta" )
        public Double getDelta()
        {
            return delta;
        }
        public void setDelta(Double parameter)
        {
            this.delta = parameter;
        }
    }

    public static class OptimizationParametersBeanInfo extends BeanInfoEx2<OptimizationParameters>
    {
        public OptimizationParametersBeanInfo()
        {
            super(OptimizationParameters.class);
        }

        @Override
        public void initProperties() throws Exception
        {
            addWithTags("type", availableAlgorithms);
            add("numOfIterations");
            add("survivalSize");
        }
    }
}
