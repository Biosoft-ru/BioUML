package ru.biosoft.bsa.analysis;

import static ru.biosoft.bsa.analysis.OptimizeSiteSearchAnalysisParameters.OPTIMIZE_BOTH;
import static ru.biosoft.bsa.analysis.OptimizeSiteSearchAnalysisParameters.OPTIMIZE_CUTOFF;
import static ru.biosoft.bsa.analysis.OptimizeSiteSearchAnalysisParameters.OPTIMIZE_WINDOW;

import com.developmentontheedge.beans.PropertyDescriptorEx;

import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.bsa.MatrixTableType;
import ru.biosoft.bsa.SiteModelCollection;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.gui.MessageBundle;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.OptionEx;
import ru.biosoft.util.bean.BeanInfoEx2;

public class OptimizeSiteSearchAnalysisParametersBeanInfo extends BeanInfoEx2<OptimizeSiteSearchAnalysisParameters>
{
    public OptimizeSiteSearchAnalysisParametersBeanInfo()
    {
        super(OptimizeSiteSearchAnalysisParameters.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(getResourceString("CN_CLASS"));
        beanDescriptor.setShortDescription(getResourceString("CD_CLASS"));
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();
        add(DataElementPathEditor.registerInput("inYesTrack", beanClass, Track.class),
                getResourceString("PN_SITESEARCH_INPUT_YES_TRACK"), getResourceString("PD_SITESEARCH_INPUT_YES_TRACK"));
        add(DataElementPathEditor.registerInput("inNoTrack", beanClass, Track.class),
                getResourceString("PN_SITESEARCH_INPUT_NO_TRACK"), getResourceString("PD_SITESEARCH_INPUT_NO_TRACK"));
        add(new PropertyDescriptorEx("pvalueCutoff", beanClass),
                getResourceString("PN_SITESEARCH_PVALUE_CUTOFF"), getResourceString("PD_SITESEARCH_PVALUE_CUTOFF"));
        property("optimizationType").tags( OPTIMIZE_CUTOFF,OPTIMIZE_WINDOW,OPTIMIZE_BOTH ).title( "PN_SITESEARCH_OPTIMIZATION_TYPE" )
            .description( "PD_SITESEARCH_OPTIMIZATION_TYPE" ).add();
        add(OptionEx.makeAutoProperty(DataElementPathEditor.registerOutput("outYesTrack", beanClass, SqlTrack.class, true), "$inYesTrack$ optimized"),
                getResourceString("PN_SITESEARCH_OUTPUT_YES_TRACK"), getResourceString("PD_SITESEARCH_OUTPUT_YES_TRACK"));
        add(OptionEx.makeAutoProperty(DataElementPathEditor.registerOutput("outNoTrack", beanClass, SqlTrack.class, true), "$inNoTrack$ optimized"),
                getResourceString("PN_SITESEARCH_OUTPUT_NO_TRACK"), getResourceString("PD_SITESEARCH_OUTPUT_NO_TRACK"));
        add(new PropertyDescriptorEx("overrepresentedOnly", beanClass),
                getResourceString("PN_SUMMARY_OVERREPRESENTED_ONLY"), getResourceString("PD_SUMMARY_OVERREPRESENTED_ONLY"));
        add(OptionEx.makeAutoProperty(DataElementPathEditor.registerOutput("outSummaryTable", beanClass, TableDataCollection.class, MatrixTableType.class), "$inYesTrack/parent$/optimization"),
                getResourceString("PN_SUMMARY_OUTPUTNAME"), getResourceString("PD_SUMMARY_OUTPUTNAME"));
        add( DataElementPathEditor.registerOutput( "outProfile", beanClass, SiteModelCollection.class, true ),
                getResourceString( "PN_SUMMARY_OUTPROFILE" ), getResourceString( "PD_SUMMARY_OUTPROFILE" ) );
    }
}
