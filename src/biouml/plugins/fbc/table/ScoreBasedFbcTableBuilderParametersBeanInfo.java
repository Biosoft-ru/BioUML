package biouml.plugins.fbc.table;

import java.beans.IntrospectionException;
import biouml.model.Diagram;
import biouml.plugins.fbc.table.ScoreBasedFbcTableBuilderParameters;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import ru.biosoft.util.bean.BeanInfoEx2;

public class ScoreBasedFbcTableBuilderParametersBeanInfo extends BeanInfoEx2<ScoreBasedFbcTableBuilderParameters>
{
    public ScoreBasedFbcTableBuilderParametersBeanInfo()
    {
        super( ScoreBasedFbcTableBuilderParameters.class );
    }

    @Override
    public void initProperties() throws IntrospectionException
    {
        property( "inputDiagram" ).inputElement( Diagram.class ).add();
        property( "inputEnzymes" ).inputElement( TableDataCollection.class ).canBeNull().add();

        property( ColumnNameSelector.registerNumericSelector( "maxColumnName", beanClass, "inputEnzymes" ) ).add();
        add( "norm" );

        property( ColumnNameSelector.registerNumericSelector( "scoreColumnName", beanClass, "inputEnzymes" ) ).add();
        property( "correlation" ).expert().add();

        property( "objectiveTable" ).inputElement( TableDataCollection.class ).canBeNull().add();
        property( ColumnNameSelector.registerNumericSelector( "objectiveColumnName", beanClass, "objectiveTable" ) ).add();

        property( "outputPath" ).outputElement( TableDataCollection.class ).auto( "$inputDiagram$ fbc table" ).add();
    }
}
