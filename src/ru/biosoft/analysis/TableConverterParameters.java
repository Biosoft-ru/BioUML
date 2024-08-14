package ru.biosoft.analysis;

import java.util.Properties;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.BioHubRegistry;
import ru.biosoft.access.biohub.BioHubSupport;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.repository.IconFactory;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.workbench.editors.ReferenceTypeSelector;
import biouml.standard.type.Species;

public class TableConverterParameters extends AbstractTableConverterParameters
{
    private DataElementPath sourceTable;
    private String sourceType;
    private String idsColumnName = ColumnNameSelector.NONE_COLUMN;
    private DataElementPath unmatchedTable;
    private int maxMatches = 0;

    public TableConverterParameters()
    {
        setSourceType( ReferenceTypeRegistry.getDefaultReferenceType().getDisplayName() );
    }

    @PropertyName("Input table")
    @PropertyDescription("Data set to be converted")
    public DataElementPath getSourceTable()
    {
        return sourceTable;
    }

    public void setSourceTable(DataElementPath sourceTable)
    {
        Object oldValue = this.sourceTable;
        this.sourceTable = sourceTable;
        firePropertyChange("sourceTable", oldValue, sourceTable);
        TableDataCollection table = sourceTable == null ? null : sourceTable.optDataElement(TableDataCollection.class);
        try
        {
            setSpecies(Species.getDefaultSpecies(table));
        }
        catch(Exception e)
        {
        }
        if( oldValue == null || table == null || ( !oldValue.equals( sourceTable ) && !table.getColumnModel().hasColumn( getColumnName() ) ) )
            setColumnName( ColumnNameSelector.NONE_COLUMN );
        if( oldValue == null || table == null
                || ( !oldValue.equals( sourceTable ) && !table.getColumnModel().hasColumn( getIdsColumnName() ) ) )
            setIdsColumnName(ColumnNameSelector.NONE_COLUMN);
    }

    @PropertyName("Column with IDs")
    @PropertyDescription("Column to be used as source ID. Select "+ColumnNameSelector.NONE_COLUMN+" to use row IDs")
    public String getIdsColumnName()
    {
        return idsColumnName;
    }

    public void setIdsColumnName(String idsColumnName)
    {
        Object oldValue = this.idsColumnName;
        this.idsColumnName = idsColumnName;
        firePropertyChange("idsColumnName", oldValue, idsColumnName);
        if(getSourceType() == null || !getSourceType().equals(ReferenceTypeSelector.AUTO_DETECT_MESSAGE))
        {
            try
            {
                TableDataCollection table = getSourceTable().getDataElement(TableDataCollection.class);
                if(idsColumnName.equals(ColumnNameSelector.NONE_COLUMN))
                {
                    if(table.getReferenceType() != null && !table.getReferenceType().equals( ReferenceTypeRegistry.getDefaultReferenceType().toString() ))
                        setSourceType(table.getReferenceType());
                }
                else
                {
                    String referenceType = table.getColumnModel().getColumn(idsColumnName).getValue(ReferenceTypeRegistry.REFERENCE_TYPE_PROPERTY);
                    if(referenceType != null)
                        setSourceType(referenceType);
                }
            }
            catch( Exception e )
            {
            }
        }
    }

    @PropertyName("Input type")
    @PropertyDescription("Type of references in input table")
    public String getSourceType()
    {
        return sourceType;
    }

    public ReferenceType getSourceTypeObject()
    {
        if( sourceType == null )
            return null;
        if( sourceType.equals(ReferenceTypeSelector.AUTO_DETECT_MESSAGE) )
        {
            if(getIdsColumnName().equals(ColumnNameSelector.NONE_COLUMN))
                return ReferenceTypeRegistry.getElementReferenceType(sourceTable.optDataCollection());
            try
            {
                return ReferenceTypeRegistry.optReferenceType(sourceTable.getDataElement(TableDataCollection.class).getColumnModel()
                        .getColumn(getIdsColumnName()).getValue(ReferenceTypeRegistry.REFERENCE_TYPE_PROPERTY));
            }
            catch( Exception e )
            {
            }
        }
        return ReferenceTypeRegistry.optReferenceType(sourceType);
    }

    public void setSourceType(String sourceType)
    {
        Object oldValue = this.sourceType;
        this.sourceType = sourceType;
        firePropertyChange("sourceType", oldValue, sourceType);
        if(!insideRead && !ReferenceTypeSelector.AUTO_DETECT_MESSAGE.equals( sourceType )) // Omit slow code during read
        {
            try
            {
                Properties input = BioHubSupport.createProperties( getSpecies().getLatinName(), sourceType );
                ReferenceType[] reachableTypes = BioHubRegistry.getReachableTypes(input);
                ReferenceType targetType = ReferenceTypeRegistry.optReferenceType(getTargetType());
                for(ReferenceType type: reachableTypes)
                {
                    if(targetType == type) return;
                }
                for(ReferenceType type: reachableTypes)
                {
                    if(type.toString().equals(sourceType))
                    {
                        setTargetType(sourceType);
                        return;
                    }
                }
                setTargetType(reachableTypes[0].getDisplayName());
            }
            catch( Exception e )
            {
                setTargetType("");
            }
        }
    }

    public String getUnmatchedIcon()
    {
        try
        {
            return IconFactory.getClassIconId(ReferenceTypeRegistry.getReferenceType(
                    getSourceTable().getDataElement(TableDataCollection.class).getReferenceType()).getClass());
        }
        catch( Exception e )
        {
            return null;
        }
    }

    @PropertyName("Unmatched rows")
    @PropertyDescription("Path to store unmatched rows of the table")
    public DataElementPath getUnmatchedTable()
    {
        return unmatchedTable;
    }

    /**
     * @param unmatchedTable the unmatchedTable to set
     */
    public void setUnmatchedTable(DataElementPath unmatchedTable)
    {
        Object oldValue = this.unmatchedTable;
        this.unmatchedTable = unmatchedTable;
        firePropertyChange("unmatchedTable", oldValue, unmatchedTable);
    }

    private boolean insideRead = false;

    @Override
    public void read(Properties properties, String prefix)
    {
        insideRead = true;
        try
        {
            super.read(properties, prefix);
        }
        finally
        {
            insideRead = false;
        }
    }

    @PropertyName("Restrict multiple matching to max")
    @PropertyDescription("Input accession will be excluded from result if it matches to more accessions " +
                         "than specified in this parameter (set 0 to turn this feature off)")
    public int getMaxMatches()
    {
        return maxMatches;
    }

    public void setMaxMatches(int maxMatches)
    {
        this.maxMatches = maxMatches;
    }
}