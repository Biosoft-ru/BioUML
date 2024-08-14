package ru.biosoft.analysis;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import one.util.streamex.MoreCollectors;
import one.util.streamex.StreamEx;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import ru.biosoft.util.bean.BeanInfoEx2;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.jobcontrol.StackProgressJobControl;

@ClassIcon("resources/FilterDuplicateRows.png")
public class FilterDuplicateRows extends AnalysisMethodSupport<FilterDuplicateRows.Parameters>
{

    public FilterDuplicateRows(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        final TableDataCollection input = parameters.getInputTablePath().getDataElement(TableDataCollection.class);

        jobControl.pushProgress( 0, 50 );
        Map<Object, List<RowDataElement>> groups = groupTableRows( input, parameters.getPrimaryColumn(), jobControl );
        jobControl.popProgress();

        final TableDataCollection result = TableDataCollectionUtils.createTableDataCollection( parameters.getOutputPath() );
        ColumnModel inputCM = input.getColumnModel();
        ColumnModel newCm = result.getColumnModel();
        for( TableColumn tc : inputCM )
        {
            newCm.addColumn( newCm.cloneTableColumn( tc ) );
        }
        final int scoreColIdx = inputCM.optColumnIndex( parameters.getScoreColumn() );

        jobControl.pushProgress( 50, 90 );
        jobControl.forCollection( groups.values(), group -> {
            try
            {
                if( group.size() == 1 )
                    result.addRow( group.get( 0 ).clone() );
                else if( scoreColIdx != -1 )
                {
                    List<RowDataElement> best = StreamEx.of( group ).collect(
                            MoreCollectors.maxAll( Comparator.comparingDouble( row -> ( (Number)row.getValues()[scoreColIdx] )
                                    .doubleValue() ) ) );
                    if( !parameters.isRemoveRowsWithSameScore() || best.size() == 1 )
                        for( RowDataElement e1 : best )
                            result.addRow( e1.clone() );
                }
            }
            catch( Exception e2 )
            {
                throw ExceptionRegistry.translateException(e2);
            }
            return true;
        } );
        jobControl.popProgress();

        result.finalizeAddition();
        parameters.getOutputPath().save( result );
        return result;
    }

    public static Map<Object, List<RowDataElement>> groupTableRows(final TableDataCollection table, String by,
            StackProgressJobControl jobControl)
    {
        final Map<Object, List<RowDataElement>> groups = new HashMap<>();
        final int byIdx = table.getColumnModel().getColumnIndex( by );
        jobControl.forCollection( DataCollectionUtils.asCollection( table, RowDataElement.class), row -> {
            try
            {
                Object[] values = row.getValues();
                Object primaryValue = values[byIdx];
                List<RowDataElement> group = groups.computeIfAbsent( primaryValue, pv -> new ArrayList<>() );
                group.add( row );
            }
            catch( Exception e )
            {
                throw ExceptionRegistry.translateException(e);
            }
            return true;
        } );
        return groups;
    }

    public static class Parameters extends AbstractAnalysisParameters
    {
        private DataElementPath inputTablePath;
        private String primaryColumn = ColumnNameSelector.NONE_COLUMN;
        private String scoreColumn = ColumnNameSelector.NONE_COLUMN;
        private boolean removeRowsWithSameScore = true;
        private DataElementPath outputPath;

        @PropertyName ( "Input table" )
        @PropertyDescription ( "Table to filter duplicate rows from" )
        public DataElementPath getInputTablePath()
        {
            return inputTablePath;
        }
        public void setInputTablePath(DataElementPath inputTablePath)
        {
            Object oldValue = this.inputTablePath;
            this.inputTablePath = inputTablePath;
            firePropertyChange( "inputTablePath", oldValue, inputTablePath );
            TableDataCollection table = inputTablePath.optDataElement(TableDataCollection.class);
            if(table != null)
            {
                ColumnModel columnModel = table.getColumnModel();
                if(columnModel.hasColumn(getPrimaryColumn())) return;
                for(TableColumn column: columnModel)
                {
                    if(column.getType().isNumeric())
                    {
                        setPrimaryColumn(column.getName());
                        return;
                    }
                }
            }
            setPrimaryColumn(ColumnNameSelector.NONE_COLUMN);
        }


        @PropertyName ( "Primary column" )
        @PropertyDescription ( "Column where to find duplicates" )
        public String getPrimaryColumn()
        {
            return primaryColumn;
        }
        public void setPrimaryColumn(String primaryColumn)
        {
            Object oldValue = this.primaryColumn;
            this.primaryColumn = primaryColumn;
            firePropertyChange( "primaryColumn", oldValue, primaryColumn );
        }

        @PropertyName ( "Score column" )
        @PropertyDescription ( "Optional column used to select row among duplicates" )
        public String getScoreColumn()
        {
            return scoreColumn;
        }
        public void setScoreColumn(String scoreColumn)
        {
            Object oldValue = this.scoreColumn;
            this.scoreColumn = scoreColumn;
            firePropertyChange( "scoreColumn", oldValue, scoreColumn );
            firePropertyChange( "*", null, null );
        }


        @PropertyName("Remove rows with the same score value")
        @PropertyDescription("Remove rows with the same score value")
        public boolean isRemoveRowsWithSameScore()
        {
            return removeRowsWithSameScore;
        }
        public void setRemoveRowsWithSameScore(boolean removeRowsWithSameScore)
        {
            Object oldValue = this.removeRowsWithSameScore;
            this.removeRowsWithSameScore = removeRowsWithSameScore;
            firePropertyChange( "removeRowsWithSameScore", oldValue, removeRowsWithSameScore );
        }

        public boolean isRemoveRowsWithSameScoreHidden()
        {
            return getScoreColumn() == null || getScoreColumn().equals(ColumnNameSelector.NONE_COLUMN);
        }


        @PropertyName ( "Output table" )
        @PropertyDescription ( "Resulting table" )
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
    }


    public static class ParametersBeanInfo extends BeanInfoEx2<Parameters>
    {
        public ParametersBeanInfo()
        {
            super( Parameters.class );
        }

        @Override
        protected void initProperties() throws Exception
        {
            super.initProperties();

            property( "inputTablePath" ).inputElement( TableDataCollection.class ).add();
            add( ColumnNameSelector.registerSelector( "primaryColumn", beanClass, "inputTablePath", false ) );
            add( ColumnNameSelector.registerNumericSelector( "scoreColumn", beanClass, "inputTablePath", true ) );
            addHidden( "removeRowsWithSameScore", "isRemoveRowsWithSameScoreHidden" );

            property( "outputPath" ).outputElement( TableDataCollection.class ).auto( "$inputTablePath$ no dup" ).add();
        }
    }

}
