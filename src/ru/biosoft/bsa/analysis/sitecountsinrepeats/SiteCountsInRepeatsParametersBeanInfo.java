
package ru.biosoft.bsa.analysis.sitecountsinrepeats;

import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.table.TableDataCollection;

import com.developmentontheedge.beans.BeanInfoEx;

/**
 * @author yura
 *
 */
public class SiteCountsInRepeatsParametersBeanInfo extends BeanInfoEx
{
    public SiteCountsInRepeatsParametersBeanInfo()
    {
        super(SiteCountsInRepeatsParameters.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(MessageBundle.CN_SITE_COUNTS_IN_REPEATS);
        beanDescriptor.setShortDescription(MessageBundle.CD_SITE_COUNTS_IN_REPEATS);
    }
    
    @Override
    protected void initProperties() throws Exception
    {
        add(DataElementPathEditor.registerInput("siteTrackPath", beanClass, SqlTrack.class), getResourceString("PN_SITE_TRACK_PATH"), getResourceString("PD_SITE_TRACK_PATH"));
        add(DataElementPathEditor.registerOutput("siteCountsInRepeatsTable", beanClass, TableDataCollection.class), getResourceString("PN_SITE_COUNTS_IN_REPEATS_TABLE"), getResourceString("PD_SITE_COUNTS_IN_REPEATS_TABLE"));
    }

}
