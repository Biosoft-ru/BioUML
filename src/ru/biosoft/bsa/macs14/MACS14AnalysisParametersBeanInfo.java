package ru.biosoft.bsa.macs14;

import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.gui.MessageBundle;
import ru.biosoft.util.OptionEx;
import ru.biosoft.util.bean.BeanInfoEx2;

public class MACS14AnalysisParametersBeanInfo extends BeanInfoEx2<MACS14AnalysisParameters>
{
    public MACS14AnalysisParametersBeanInfo()
    {
        super( MACS14AnalysisParameters.class, MessageBundle.class.getName() );
        beanDescriptor.setDisplayName( getResourceString( "CN_CLASS" ) );
        beanDescriptor.setShortDescription( getResourceString( "CD_CLASS" ) );
    }

    @Override
    public void initProperties() throws Exception
    {
        property( DataElementPathEditor.registerInput( "trackPath", beanClass, Track.class ) ).title( "PN_SITESEARCH_TRACK" )
                .description( "PD_SITESEARCH_TRACK" ).add();
        property( DataElementPathEditor.registerInput( "controlPath", beanClass, Track.class, true ) ).title( "PN_MACS_CONTROL" )
                .description( "PD_MACS_CONTROL" ).add();
        //MACS algorithm parameters
        property( "gsize" ).title( "PN_MACS_GSIZE" ).title( "PD_MACS_GSIZE" ).add();
        property( "tsize" ).expert().title( "PN_MACS14_TSIZE" ).description( "PD_MACS14_TSIZE" ).add();
        property( "bw" ).title( "PN_MACS_BW" ).description( "PD_MACS_BW" ).add();
        property( "pvalue" ).title( "PN_MACS_PVALUE" ).description( "PD_MACS_PVALUE" ).add();
        property( "mfoldLower" ).title( "PN_MACS_MFOLD_LOWER" ).description( "PD_MACS_MFOLD_LOWER" ).add();
        property( "mfoldUpper" ).title( "PN_MACS_MFOLD_UPPER" ).description( "PD_MACS_MFOLD_UPPER" ).add();
        property( "nolambda" ).expert().title( "PN_MACS_NOLAMBDA" ).description( "PD_MACS_NOLAMBDA" ).add();

        property( "sLocal" ).hidden( "isNoControl" ).title( "PN_MACS_S_LOCAL" ).description( "PD_MACS_S_LOCAL" ).add();
        property( "lLocal" ).hidden( "isNoControl" ).title( "PN_MACS_L_LOCAL" ).description( "PD_MACS_L_LOCAL" ).add();

        property( "autoOff" ).expert().title( "PN_MACS_AUTO_OFF" ).description( "PD_MACS_AUTO_OFF" ).add();
        property( "nomodel" ).expert().title( "PN_MACS_NOMODEL" ).description( "PD_MACS_NOMODEL" ).add();
        property( "shiftsize" ).expert().title( "PN_MACS_SHIFTSIZE" ).description( "PD_MACS_SHIFTSIZE" ).add();
        property( "keepDup" ).expert().tags( MACS14AnalysisParameters.KEEP_DUPLICATES_VALUES ).title( "PN_MACS_KEEP_DUP" )
                .description( "PD_MACS_KEEP_DUP" ).add();
        property( "toSmall" ).title( "PN_MACS_TO_SMALL" ).description( "PD_MACS_TO_SMALL" ).add();
        property( "computePeakProfile" ).expert().title( "PN_MACS_COMPUTE_PEAK_PROFILE" ).description( "PD_MACS_COMPUTE_PEAK_PROFILE" )
                .add();

        property(OptionEx.makeAutoProperty( DataElementPathEditor.registerOutput( "outputPath", beanClass, SqlTrack.class ),
                        "$trackPath$ peaks" ) ).title( "PN_SITESEARCH_OUTPUTNAME" ).description( "PD_SITESEARCH_OUTPUTNAME" ).add();
    }
}
