package ru.biosoft.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import ru.biosoft.table.exception.TableNoColumnException;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;

@ClassIcon("resources/SelectTopRows.png")
public class SelectTopRows extends AnalysisMethodSupport<SelectTopRows.Parameters>
{

    public SelectTopRows(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }
    
    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        super.validateParameters();
        
        checkRange( "topPercent", 0.0, 100.0 );
        checkGreater( "topCount", 0 );
        
        checkRange("middlePercent", 0.0, 100.0);
        checkGreater( "middleCount", 0 );
        
        checkRange( "bottomPercent", 0.0, 100.0 );
        checkGreater( "bottomCount", 0 );
        
        if(parameters.getTopTable() == null && parameters.getBottomTable() == null && parameters.getMiddleTable() == null)
            throw new IllegalArgumentException("Select at least one of top, middle, bottom table");
    }
   
    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        TableDataCollection in = parameters.getInputTable().getDataElement( TableDataCollection.class );
        int columnIdx = -1;
        ColumnModel cm = in.getColumnModel();
        if(parameters.getColumn() == null)
        {
            for(int i = 0; i < cm.getColumnCount(); i++)
                if(cm.getColumn( i ).getType().isNumeric())
                {
                    columnIdx = i;
                    break;
                }
            if(columnIdx == -1)
                throw new TableNoColumnException( in, "numeric" );
        }
        else
            columnIdx = cm.getColumnIndex( parameters.getColumn() );
        List<Row> rows = new ArrayList<>();
        for(RowDataElement rowDE : in)
        {
            double value = ((Number)rowDE.getValues()[columnIdx]).doubleValue();
            if(Double.isFinite( value ))
            {
                Row row = new Row( rowDE.getName(), value );
                rows.add( row );
            }
        }
        Collections.sort( rows );
        
        Set<String> topIds = new HashSet<>();
        TableDataCollection topOut = null;
        if(!parameters.isTopHidden())
        {
            

            int count = (int) Math.round(parameters.getTopPercent() * rows.size() / 100);
            if(count > parameters.getTopCount())
                count = parameters.getTopCount();
            
            for(int i = 0; i < count; i++)
            {
                Row row = rows.get( rows.size() - i - 1 );
                topIds.add( row.id );
            }

            if( topIds.size() >= parameters.getTopMinCount() )
                topOut = TableDataCollectionUtils.createTableLike( in, parameters.getTopTable() );
        }
        
        Set<String> middleIds = new HashSet<>();
        TableDataCollection middleOut = null;
        if(!parameters.isMiddleHidden())
        {
            int count = (int) Math.round(parameters.getMiddlePercent() * rows.size() / 100);
            if(count > parameters.getMiddleCount())
                count = parameters.getMiddleCount();
            
            for(int i = 0; i < count; i++)
            {
                Row row = rows.get( (rows.size() - count)/2 + i );
                middleIds.add( row.id );
            }

            if( middleIds.size() >= parameters.getMiddleMinCount() )
                middleOut = TableDataCollectionUtils.createTableLike( in, parameters.getMiddleTable() );
        }
        
        Set<String> bottomIds = new HashSet<>();
        TableDataCollection bottomOut = null;
        if(!parameters.isBottomHidden())
        {
            int count = (int) Math.round(parameters.getBottomPercent() * rows.size() / 100);
            if(count > parameters.getBottomCount())
                count = parameters.getBottomCount();
            
            for(int i = 0; i < count; i++)
            {
                Row row = rows.get( i );
                bottomIds.add( row.id );
            }

            if( bottomIds.size() >= parameters.getBottomMinCount() )
                bottomOut = TableDataCollectionUtils.createTableLike( in, parameters.getBottomTable() );
        }
        
        rows = null;
        
        for(RowDataElement row : in)
        {
            String id = row.getName();
            if(topOut != null && topIds.contains( id ))
                topOut.addRow( row );
            if(middleOut != null && middleIds.contains( id ))
                middleOut.addRow( row );
            if(bottomOut != null && bottomIds.contains( id ))
                bottomOut.addRow( row );
        }
        
        
        List<TableDataCollection> results = new ArrayList<>();
        if(topOut != null)
        {
            topOut.finalizeAddition();
            parameters.getTopTable().save( topOut );
            results.add( topOut );
        }
        if(middleOut != null)
        {
            middleOut.finalizeAddition();
            parameters.getMiddleTable().save( middleOut );
            results.add( middleOut );
        }
        if(bottomOut != null)
        {
            bottomOut.finalizeAddition();
            parameters.getBottomTable().save( bottomOut );
            results.add( bottomOut );
        }
        
        return results.toArray();
    }
    

    private static class Row implements Comparable<Row>
    {
        public final String id;
        public final double value;
        public Row(String id, double value)
        {
            this.id = id;
            this.value = value;
        }
        @Override
        public int compareTo(Row o)
        {
            return Double.compare( value, o.value );
        }
    }

    public static class Parameters extends AbstractAnalysisParameters
    {
        private DataElementPath inputTable;
        @PropertyName("Input table")
        public DataElementPath getInputTable()
        {
            return inputTable;
        }
        public void setInputTable(DataElementPath inputTable)
        {
            Object oldValue = this.inputTable;
            this.inputTable = inputTable;
            firePropertyChange( "inputTable", oldValue, inputTable );
        }
        
        private String column;
        @PropertyName("Column")
        public String getColumn()
        {
            return column;
        }
        public void setColumn(String column)
        {
            Object oldValue = this.column;
            this.column = column;
            firePropertyChange( "column", oldValue, column );
        }
        
        private String[] types = TypeSelector.TYPES;
        @PropertyName("Type")
        public String[] getTypes()
        {
            return types;
        }
        public void setTypes(String[] types)
        {
            String[] oldValue = this.types;
            this.types = types;
            firePropertyChange( "*", oldValue, types );
        }
        private boolean hasType(String type)
        {
            for(String t : types)
                if(t.equals( type ))
                    return true;
            return false;
        }
        public boolean isTopHidden()
        {
            return !hasType( "Top" );
        }
        public boolean isMiddleHidden()
        {
            return !hasType( "Middle" );
        }
        public boolean isBottomHidden()
        {
            return !hasType( "Bottom" );
        }

        private double topPercent = 20;
        @PropertyName("Top percent")
        public double getTopPercent()
        {
            return topPercent;
        }
        public void setTopPercent(double topPercent)
        {
            double oldValue = this.topPercent;
            this.topPercent = topPercent;
            firePropertyChange( "topPercent", oldValue, topPercent );
        }

        private int topCount = 10;
        @PropertyName("Top max count")
        public int getTopCount()
        {
            return topCount;
        }
        public void setTopCount(int topCount)
        {
            int oldValue = this.topCount;
            this.topCount = topCount;
            firePropertyChange( "topCount", oldValue, topCount );
        }
        
        private DataElementPath topTable;
        @PropertyName("Top table output")
        public DataElementPath getTopTable()
        {
            return topTable;
        }
        public void setTopTable(DataElementPath topTable)
        {
            Object oldValue = this.topTable;
            this.topTable = topTable;
            firePropertyChange( "topTable", oldValue, topTable );
        }
        
        private int topMinCount = 0;
        @PropertyName ( "Top min count" )
        public int getTopMinCount()
        {
            return topMinCount;
        }
        public void setTopMinCount(int topMinCount)
        {
            Object oldValue = this.topMinCount;
            this.topMinCount = topMinCount;
            firePropertyChange( "this.topMinCount", oldValue, topMinCount );
        }

        private double middlePercent = 20;
        @PropertyName("Middle percent")
        public double getMiddlePercent()
        {
            return middlePercent;
        }
        public void setMiddlePercent(double middlePercent)
        {
            double oldValue = this.middlePercent;
            this.middlePercent = middlePercent;
            firePropertyChange( "middlePercent", oldValue, middlePercent );
        }

        private int middleCount = 10;
        @PropertyName("Middle max count")
        public int getMiddleCount()
        {
            return middleCount;
        }
        public void setMiddleCount(int middleCount)
        {
            int oldValue = this.middleCount;
            this.middleCount = middleCount;
            firePropertyChange( "middleCount", oldValue, middleCount );
        }
        
        private DataElementPath middleTable;
        @PropertyName("Middle table output")
        public DataElementPath getMiddleTable()
        {
            return middleTable;
        }
        public void setMiddleTable(DataElementPath middleTable)
        {
            Object oldValue = this.middleTable;
            this.middleTable = middleTable;
            firePropertyChange( "middleTable", oldValue, middleTable );
        }
        
        private int middleMinCount = 0;
        @PropertyName ( "Middle min count" )
        public int getMiddleMinCount()
        {
            return middleMinCount;
        }
        public void setMiddleMinCount(int middleMinCount)
        {
            Object oldValue = this.middleMinCount;
            this.middleMinCount = middleMinCount;
            firePropertyChange( "this.middleMinCount", oldValue, middleMinCount );
        }
        
        private double bottomPercent = 20;
        @PropertyName("Bottom percent")
        public double getBottomPercent()
        {
            return bottomPercent;
        }
        public void setBottomPercent(double bottomPercent)
        {
            double oldValue = this.bottomPercent;
            this.bottomPercent = bottomPercent;
            firePropertyChange( "bottomPercent", oldValue, bottomPercent );
        }

        private int bottomCount = 10;
        @PropertyName("Bottom max count")
        public int getBottomCount()
        {
            return bottomCount;
        }
        public void setBottomCount(int bottomCount)
        {
            int oldValue = this.bottomCount;
            this.bottomCount = bottomCount;
            firePropertyChange( "bottomCount", oldValue, bottomCount );
        }

        private DataElementPath bottomTable;
        @PropertyName("Bottom table output")
        public DataElementPath getBottomTable()
        {
            return bottomTable;
        }
        public void setBottomTable(DataElementPath bottomTable)
        {
            Object oldValue = this.bottomTable;
            this.bottomTable = bottomTable;
            firePropertyChange( "bottomTable", oldValue, bottomTable );
        }
        private int bottomMinCount = 0;
        @PropertyName ( "Bottom min count" )
        public int getBottomMinCount()
        {
            return bottomMinCount;
        }
        public void setBottomMinCount(int bottomMinCount)
        {
            Object oldValue = this.bottomMinCount;
            this.bottomMinCount = bottomMinCount;
            firePropertyChange( "this.bottomMinCount", oldValue, bottomMinCount );
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
            property("inputTable").inputElement( TableDataCollection.class ).add();
            add( ColumnNameSelector.registerNumericSelector( "column", beanClass, "inputTable", false ) );
            
            PropertyDescriptorEx pde = new PropertyDescriptorEx("types", beanClass);
            pde.setSimple(true);
            pde.setHideChildren(true);
            pde.setPropertyEditorClass(TypeSelector.class);
            add(pde);
            
            addHidden("topPercent", "isTopHidden");
            addHidden("topCount", "isTopHidden");
            property( "topMinCount" ).hidden( "isTopHidden" ).expert().add();
            property("topTable").hidden( "isTopHidden" ).outputElement( TableDataCollection.class ).auto( "$inputTable$ top" ).add();

            
            addHidden("middlePercent", "isMiddleHidden");
            addHidden("middleCount", "isMiddleHidden");
            property( "middleMinCount" ).hidden( "isMiddleHidden" ).expert().add();
            property("middleTable").hidden("isMiddleHidden").outputElement( TableDataCollection.class ).auto("$inputTable$ middle").add();;
            
            addHidden("bottomPercent", "isBottomHidden");
            addHidden("bottomCount", "isBottomHidden");
            property( "bottomMinCount" ).hidden( "isBottomHidden" ).expert().add();
            property("bottomTable").hidden("isBottomHidden").outputElement( TableDataCollection.class ).auto("$inputTable$ bottom").add();
        }
    }
    
    public static class TypeSelector extends GenericMultiSelectEditor
    {
        private static final String[] TYPES = {"Top", "Middle", "Bottom"};

        @Override
        protected Object[] getAvailableValues()
        {
            return TYPES;
        }
    }
    
}
