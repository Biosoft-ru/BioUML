package ru.biosoft.bsa.analysis;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.bsa.SequenceCollection;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.VCFSqlTrack;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import ru.biosoft.table.datatype.DataType;
import ru.biosoft.util.bean.BeanInfoEx2;

public class TableToVCFTrack extends TableToTrack
{

    public TableToVCFTrack(DataCollection<?> origin, String name)
    {
        super( origin, name, new TableToVCFTrackParameters() );
    }

    @Override
    protected SiteImpl tableRowToSite(RowDataElement rde)
    {
        SiteImpl site = super.tableRowToSite( rde );
        String altAllele = rde.getValue( ( (TableToVCFTrackParameters)parameters ).getAltAlleleColumn() ).toString();
        String refAllele = rde.getValue( ( (TableToVCFTrackParameters)parameters ).getRefAlleleColumn() ).toString();
        //TODO: check if sequences are correct
        DynamicProperty property = new DynamicProperty( "AltAllele", String.class, altAllele );
        site.getProperties().add( property );
        property = new DynamicProperty( "RefAllele", String.class, refAllele );
        site.getProperties().add( property );
        return site;
    }

    @Override
    protected boolean isPropertyUsed(String name)
    {
        return super.isPropertyUsed( name ) || name.equals( ( (TableToVCFTrackParameters)parameters ).getAltAlleleColumn() )
                || name.equals( ( (TableToVCFTrackParameters)parameters ).getRefAlleleColumn() );
    }

    @Override
    protected Class<? extends SqlTrack> getTrackClass()
    {
        return VCFSqlTrack.class;
    }

    protected int getLengthByRow(RowDataElement rde)
    {
        if( ColumnNameSelector.NONE_COLUMN.equals( parameters.getToColumn() ) )
            return rde.getValue( ( (TableToVCFTrackParameters)parameters ).getRefAlleleColumn() ).toString().length();
        else
            return super.getLengthByRow( rde );
    }

    protected int getToByRow(RowDataElement rde)
    {
        if( ColumnNameSelector.NONE_COLUMN.equals( parameters.getToColumn() ) )
            return getLengthByRow( rde ) + (Integer)DataType.Integer.convertValue( rde.getValue( parameters.getFromColumn() ) ) - 1;
        else
            return super.getToByRow( rde );
    }


    public static class TableToVCFTrackParameters extends TableToTrackParameters
    {
        String altAlleleColumn;
        String refAlleleColumn;

        @PropertyName ( "AltAllele column" )
        @PropertyDescription ( "Alterative allele column" )
        public String getAltAlleleColumn()
        {
            return altAlleleColumn;
        }
        public void setAltAlleleColumn(String altAlleleColumn)
        {
            Object oldValue = this.altAlleleColumn;
            this.altAlleleColumn = altAlleleColumn;
            firePropertyChange( "this.altAlleleColumn", oldValue, altAlleleColumn );
        }


        @PropertyName ( "RefAllele column" )
        @PropertyDescription ( "Reference allele column" )
        public String getRefAlleleColumn()
        {
            return refAlleleColumn;
        }
        public void setRefAlleleColumn(String refAlleleColumn)
        {
            Object oldValue = this.refAlleleColumn;
            this.refAlleleColumn = refAlleleColumn;
            firePropertyChange( "this.refAlleleColumn", oldValue, refAlleleColumn );
        }
    }

    public static class TableToVCFTrackParametersBeanInfo extends BeanInfoEx2<TableToVCFTrackParameters>
    {
        public TableToVCFTrackParametersBeanInfo()
        {
            super( TableToVCFTrackParameters.class );
        }

        @Override
        protected void initProperties() throws Exception
        {
            property( "inputTable" ).inputElement( TableDataCollection.class ).add();
            add( ColumnNameSelector.registerSelector( "chromosomeColumn", beanClass, "inputTable", false ) );
            add( ColumnNameSelector.registerNumericSelector( "fromColumn", beanClass, "inputTable", false ) );
            add( ColumnNameSelector.registerNumericSelector( "toColumn", beanClass, "inputTable", true ) );
            add( ColumnNameSelector.registerSelector( "altAlleleColumn", beanClass, "inputTable", false ) );
            add( ColumnNameSelector.registerSelector( "refAlleleColumn", beanClass, "inputTable", false ) );
            add( ColumnNameSelector.registerSelector( "strandColumn", beanClass, "inputTable", true ) );
            property( "sequenceCollectionPath" ).inputElement( SequenceCollection.class ).canBeNull().add();
            property( "genomeId" ).auto( "$sequenceCollectionPath/element/properties/genomeBuild$" ).add();
            property( "outputTrack" ).outputElement( VCFSqlTrack.class ).auto( "$inputTable$ VCF track" ).add();
        }
    }


}
