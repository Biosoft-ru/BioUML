package biouml.plugins.optimization.analysis;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Variable;
import one.util.streamex.StreamEx;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.bean.BeanInfoEx2;

public class ObservedParameterGroup extends Option
{
    private ObservedParameter[] parameters = new ObservedParameter[] {};
    private ObservedParameter[] availableParameters = new ObservedParameter[] {};
    private TableDataCollection collection;
    
    void setDiagram(Diagram diagram)
    {
        this.availableParameters = diagram.getRole(EModel.class).getVariables().stream().map(v -> new ObservedParameter(v))
                .toArray(ObservedParameter[]::new);
    }

    public  ObservedParameter[] getAvailableParameters()
    {
        return availableParameters;
    }
    
    @PropertyName ( "Parameters" )
    public ObservedParameter[] getParameters()
    {
        return parameters;
    }
    public void setParameters(ObservedParameter[] parameters)
    {
        this.parameters = parameters.clone();
    }

    public String calcParameterName(Integer index, Object parameter)
    {
        return ( (ObservedParameter)parameter ).toString();
    }
    
    public void setCollection(TableDataCollection collection)
    {
        this.collection = collection;
    }
    
    public class ObservedParameter
    {
        private String name;
        private String nameInTable;

        public ObservedParameter(Variable variable)
        {
            this.name = variable.getName();
        }

        public ObservedParameter(String name)
        {
            this.name = name;
        }

        @Override
        public String toString()
        {
            return name;
        }
        
        @PropertyName ( "Name in table" )
        public String getNameInTable()
        {
            return nameInTable;
        }
        public void setNameInTable(String nameInTable)
        {
            this.nameInTable = nameInTable;
        }

        public String[] getAvailableNamesInTable()
        {
            return collection == null? new String[0]: TableDataCollectionUtils.getColumnNames(collection);
        }
    }
    
    
    public static class ObservedParameterBeanInfo extends BeanInfoEx2<ObservedParameter>
    {
        public ObservedParameterBeanInfo()
        {
            super(ObservedParameter.class);
            this.setHideChildren(true);
            setCompositeEditor("nameInTable", new java.awt.GridLayout(1, 1));
        }

        @Override
        public void initProperties() throws Exception
        {
            addWithTags("nameInTable", bean -> StreamEx.of(bean.getAvailableNamesInTable()));
        }
    }

}