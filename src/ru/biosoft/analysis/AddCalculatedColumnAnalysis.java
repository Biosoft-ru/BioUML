package ru.biosoft.analysis;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysis.AddCalculatedColumnAnalysis.AddCalculatedColumnParameters;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.datatype.DataType;
import ru.biosoft.util.bean.BeanInfoEx2;

public class AddCalculatedColumnAnalysis extends AnalysisMethodSupport<AddCalculatedColumnParameters>
{
    public AddCalculatedColumnAnalysis(DataCollection<?> origin, String name)
    {
        super( origin, name, new AddCalculatedColumnParameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        TableDataCollection table = parameters.getInputPath().getDataElement( TableDataCollection.class );
        TableDataCollection result;
        if( parameters.isUseSameTable() )
            result = table;
        else
        {
            DataElementPath outputPath = parameters.getOutputPath();
            result = table.clone( outputPath.getParentCollection(), outputPath.getName() );
            outputPath.getParentCollection().put( result );
        }
        String newName = result.getColumnModel().generateUniqueColumnName( parameters.getNewName() );
        TableColumn column = result.getColumnModel().addColumn( newName, DataType.fromString( parameters.getType() ) );
        String expression = parameters.getExpression();
        column.setExpression( expression );
        if( parameters.isConvertToValues() )
        {
            jobControl.forCollection( DataCollectionUtils.asCollection( result, RowDataElement.class ), rde -> {
                rde.setValue( newName, rde.getValue( newName ) );
                result.put( rde );
                return true;
            } );
            column.setExpression( "" );
        }
        CollectionFactoryUtils.save( result );
        return result;
    }

    public static class AddCalculatedColumnParameters extends AbstractAnalysisParameters
    {
        private DataElementPath inputPath, outputPath;
        private String newName = "New column";
        private String expression;
        private boolean useSameTable = false;
        private boolean convertToValues = true;
        private String type = DataType.fromClass( String.class ).name();

        @PropertyName ( "Input table" )
        @PropertyDescription ( "Specify input table" )
        public DataElementPath getInputPath()
        {
            return inputPath;
        }
        public void setInputPath(DataElementPath inputPath)
        {
            Object oldValue = this.inputPath;
            this.inputPath = inputPath;
            firePropertyChange( "inputPath", oldValue, inputPath );
        }

        @PropertyName ( "Output table" )
        @PropertyDescription ( "Path to newly created table with added column" )
        public DataElementPath getOutputPath()
        {
            return outputPath;
        }
        public void setOutputPath(DataElementPath outputPath)
        {
            Object oldValue = this.outputPath;
            this.outputPath = outputPath;
            firePropertyChange( "outputPath", oldValue, outputPath );
        }

        @PropertyName ( "Column name" )
        @PropertyDescription ( "New column name" )
        public String getNewName()
        {
            return newName;
        }
        public void setNewName(String newName)
        {
            Object oldValue = this.newName;
            this.newName = newName;
            firePropertyChange( "newName", oldValue, newName );
        }
        @PropertyName ( "Expression" )
        @PropertyDescription ( "Expression that will be calculated for all table rows" )
        public String getExpression()
        {
            return expression;
        }
        public void setExpression(String expression)
        {
            Object oldValue = this.expression;
            this.expression = expression;
            firePropertyChange( "expression", oldValue, expression );
        }
        @PropertyName ( "Use same table" )
        @PropertyDescription ( "If true, new column will be added to input table." )
        public boolean isUseSameTable()
        {
            return useSameTable;
        }
        public void setUseSameTable(boolean useSameTable)
        {
            boolean oldValue = this.useSameTable;
            this.useSameTable = useSameTable;
            firePropertyChange( "useSameTable", oldValue, useSameTable );
        }

        @PropertyName ( "Convert to values" )
        @PropertyDescription ( "Write calculated values to column permanently" )
        public boolean isConvertToValues()
        {
            return convertToValues;
        }
        public void setConvertToValues(boolean convertToValues)
        {
            boolean oldValue = this.convertToValues;
            this.convertToValues = convertToValues;
            firePropertyChange( "convertToValues", oldValue, convertToValues );
        }
        @PropertyName ( "Column type" )
        @PropertyDescription ( "Type of column values" )
        public String getType()
        {
            return type;
        }
        public void setType(String type)
        {
            Object oldValue = this.type;
            this.type = type;
            firePropertyChange( "type", oldValue, type );
        }
    }

    public static class AddCalculatedColumnParametersBeanInfo extends BeanInfoEx2<AddCalculatedColumnParameters>
    {
        public AddCalculatedColumnParametersBeanInfo()
        {
            super( AddCalculatedColumnParameters.class );
        }

        @Override
        public void initProperties() throws Exception
        {
            property( "inputPath" ).inputElement( TableDataCollection.class ).add();
            add( "newName" );
            addWithTags( "type", DataType.names() );
            add( "expression" );
            add( "useSameTable" );
            add( "convertToValues" );
            property( "outputPath" ).outputElement( TableDataCollection.class ).hidden( "isUseSameTable" )
                    .auto( "$inputPath$ with new column" ).add();
        }
    }

}
