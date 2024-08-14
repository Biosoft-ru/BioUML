package ru.biosoft.analysis;

import java.awt.Component;

import javax.swing.JLabel;

import com.developmentontheedge.beans.PropertyDescriptorEx;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysis.gui.MessageBundle;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.BeanUtil;
import ru.biosoft.util.OptionEx;
import ru.biosoft.workbench.editors.GenericComboBoxEditor;

public class HypergeometricAnalysisParametersBeanInfo extends UpDownIdentificationParametersBeanInfo
{
    public HypergeometricAnalysisParametersBeanInfo()
    {
        super( HypergeometricAnalysisParameters.class, MessageBundle.class.getName() );
        beanDescriptor.setDisplayName( getResourceString( "CN_CLASS" ) );
        beanDescriptor.setShortDescription( getResourceString( "CD_CLASS" ) );
    }

    @Override
    public void initProperties() throws Exception
    {
        add( new PropertyDescriptorEx( "experimentData", beanClass ), getResourceString( "PN_EXPERIMENT" ),
                getResourceString( "PD_EXPERIMENT" ) );

        PropertyDescriptorEx pde = new PropertyDescriptorEx( "controlData", beanClass );
        pde.setCanBeNull( true );
        add( pde, getResourceString( "PN_CONTROL" ), getResourceString( "PD_CONTROL" ) );

        add( new PropertyDescriptorEx( "inputLogarithmBase", beanClass ), getResourceString( "PN_INPUT_LOGARITHM_BASE" ),
                getResourceString( "PD_INPUT_LOGARITHM_BASE" ) );

        add( new PropertyDescriptorEx( "isControlAveraged", beanClass, "isControlAveraged", "setControlAveraged" ),
                getResourceString( "PN_HYPER_AVERAGE_CONTROL" ), getResourceString( "PD_HYPER_AVERAGE_CONTROL" ) );

        add( new PropertyDescriptorEx( "pvalue", beanClass ), getResourceString( "PN_PVALUE" ), getResourceString( "PD_PVALUE" ) );

        add( new PropertyDescriptorEx( "outputType", beanClass ), getResourceString( "PN_UPDOWN_OUTPUT_TYPE" ),
                getResourceString( "PD_UPDOWN_OUTPUT_TYPE" ) );

        add( BeanUtil.createExpertDescriptor( "threshold", beanClass ), getResourceString( "PN_THRESHOLD" ),
                getResourceString( "PD_THRESHOLD" ) );

        add( BeanUtil.createExpertDescriptor( "bv", beanClass ), getResourceString( "PN_HYPER_BV" ), getResourceString( "PD_HYPER_BV" ) );

        add( new PropertyDescriptorEx( "fdr", beanClass ), getResourceString( "PN_CALCULATING_FDR" ),
                getResourceString( "PD_CALCULATING_FDR" ) );

        add( BeanUtil.createExpertDescriptor( "detailed", beanClass ), getResourceString( "PN_HYPER_DETAILED" ),
                getResourceString( "PD_HYPER_DETAILED" ) );

        add( DataElementPathEditor.registerInputChild( BeanUtil.createExpertDescriptor( "matchingCollectionPath", beanClass ), null, true ),
                getResourceString( "PN_HYPER_MATCHING_COLLECTION" ), getResourceString( "PD_HYPER_MATCHING_COLLECTION" ) );

        add( BeanUtil.createExpertDescriptor( "newKeySource", beanClass ), MatchingFieldSelector.class,
                getResourceString( "PN_HYPER_NEW_KEY_SOURCE" ), getResourceString( "PD_HYPER_NEW_KEY_SOURCE" ) );

        pde = OptionEx.makeAutoProperty( DataElementPathEditor.registerOutput( "outputTablePath", beanClass, TableDataCollection.class ),
                "$experimentData/tablePath$ hyper" );
        pde.setValue( DataElementPathEditor.ICON_ID, MicroarrayAnalysisParameters.class.getMethod( "getIcon" ) );
        add( pde, getResourceString( "PN_OUTPUT_TABLE" ), getResourceString( "PD_OUTPUT_TABLE" ) );

        //        add(OptionEx.makeAutoProperty(DataElementPathEditor.registerOutput("outputTablePath", beanClass, TableDataCollection.class),
        //        "$experimentData/tablePath/$ Hyper $pValue$"), getResourceString("PN_OUTPUT_TABLE"),
        //        getResourceString("PD_OUTPUT_TABLE"));

    }

    public static class MatchingFieldSelector extends GenericComboBoxEditor
    {

        @Override
        public Object[] getAvailableValues()
        {
            try
            {
                DataCollection<?> dataCollection = (DataCollection)getBean().getClass()
                        .getMethod( "getMatchingCollection", (Class<?>[])null ).invoke( getBean(), (Object[])null );
                DataElement dataElement = dataCollection.iterator().next();
                return BeanUtil.getPropertiesList( dataElement );
            }
            catch( Exception ex )
            {
                return new Object[] {};
            }
        }

        @Override
        public Component getCustomRenderer(Component parent, boolean isSelected, boolean hasFocus)
        {
            try
            {
                return new JLabel( getValue().toString() );
            }
            catch( Exception ex )
            {
                return new JLabel( "Not selected" );
            }
        }
    }
}


