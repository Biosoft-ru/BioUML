package ru.biosoft.bsa.analysis;

import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.gui.MessageBundle;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import ru.biosoft.util.OptionEx;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.DataElementComboBoxSelector;
import biouml.standard.type.Species;

import static ru.biosoft.bsa.analysis.ExtractPromotersParameters.*;

public abstract class ExtractPromotersParametersBeanInfo<T> extends BeanInfoEx2<T>
{
    public ExtractPromotersParametersBeanInfo(Class<? extends T> beanClass)
    {
        super(beanClass, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(getResourceString("CN_CLASS"));
        beanDescriptor.setShortDescription(getResourceString("CD_CLASS"));
    }

    protected abstract Class<? extends ReferenceType> getSourceReferenceType();
    
    @Override
    protected  void initProperties() throws Exception
    {
        property( DataElementPathEditor.registerInput( "sourcePath", beanClass, TableDataCollection.class, getSourceReferenceType() ) )
                .title( "PN_GENESET_SOURCE" ).description( "PD_GENESET_SOURCE" ).add();
        property( DataElementComboBoxSelector.registerSelector( "species", beanClass, Species.SPECIES_PATH ) ).title( "PN_SPECIES" )
                .description( "PD_SPECIES" ).add();
        property( "from" ).title( "PN_GENESET_FROM" ).description( "PD_GENESET_FROM" ).add();
        property( "to" ).title( "PN_GENESET_TO" ).description( "PD_GENESET_TO" ).add();

        property( "overlapMergingMode" ).expert()
                .tags( MODE_DO_NOT_MERGE_OVERLAPPING, MODE_SELECT_ONE_MAX, MODE_SELECT_ONE_MIN, MODE_SELECT_ONE_EXTREME ).add();
        property( ColumnNameSelector.registerNumericSelector( "leadingColumn", beanClass, "sourcePath", false ) ).expert()
                .hidden( "isIgnoreOverlaping" ).add();
        property( "minDistance" ).hidden( "isIgnoreOverlaping" ).add();

        property(OptionEx.makeAutoProperty( DataElementPathEditor.registerOutput( "destPath", beanClass, SqlTrack.class ),
                        "$sourcePath$ track" ) ).title( "PN_GENESET_OUTPUTNAME" ).description( "PD_GENESET_OUTPUTNAME" ).add();
    }
}
