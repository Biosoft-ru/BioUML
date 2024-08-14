package ru.biosoft.analysis;

import java.util.Random;

import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.bean.BeanInfoEx2;

@ClassIcon ( "resources/SelectRandomRows.png" )
public class SelectRandomRows extends AnalysisMethodSupport<SelectRandomRows.Parameters>
{

    public SelectRandomRows(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }
    
    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        super.validateParameters();
        
        checkRange( "rndPercent", 0.0, 100.0 );
        checkRange( "rndCount", 0, parameters.getRndCount() );
        if( parameters.getRndTable() == null )
            throw new IllegalArgumentException( "Select output table" );
    }
   
    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        TableDataCollection in = parameters.getInputTable().getDataElement( TableDataCollection.class );
        TableDataCollection out = TableDataCollectionUtils.createTableLike( in, parameters.getRndTable() );
        int outPercCount = (int)Math.round( parameters.getRndPercent() * in.getSize() / 100 );
        int outSize = Math.min( outPercCount, parameters.getRndCount() );

        Random rnd = new Random();
        int remaining = in.getSize();
        int required = outSize - out.getSize();
        for( RowDataElement rowDE : in )
        {
            if( rnd.nextInt( remaining ) < required )
            {
                TableDataCollectionUtils.addRow( out, rowDE.getName(), rowDE.getValues(), true );
                required--;
            }
            remaining--;
        }
        out.finalizeAddition();
        return out;
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
        
        private double rndPercent = 20;
        @PropertyName ( "Random percent" )
        public double getRndPercent()
        {
            return rndPercent;
        }
        public void setRndPercent(double rndPercent)
        {
            double oldValue = this.rndPercent;
            this.rndPercent = rndPercent;
            firePropertyChange( "rndPercent", oldValue, rndPercent );
        }

        private int rndCount = 300;
        @PropertyName ( "Random count" )
        public int getRndCount()
        {
            return rndCount;
        }
        public void setRndCount(int rndCount)
        {
            int oldValue = this.rndCount;
            this.rndCount = rndCount;
            firePropertyChange( "rndCount", oldValue, rndCount );
        }
        
        private DataElementPath rndTable;
        @PropertyName ( "Randomized output table" )
        public DataElementPath getRndTable()
        {
            return rndTable;
        }
        public void setRndTable(DataElementPath rndTable)
        {
            Object oldValue = this.rndTable;
            this.rndTable = rndTable;
            firePropertyChange( "rndTable", oldValue, rndTable );
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
            add( "rndCount" );
            add( "rndPercent" );
            property( "rndTable" ).outputElement( TableDataCollection.class ).auto( "$rndTable random$" ).add();
        }
    }
}
