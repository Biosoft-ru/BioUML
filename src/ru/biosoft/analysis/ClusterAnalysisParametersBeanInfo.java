package ru.biosoft.analysis;

import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysis.gui.MessageBundle;
import ru.biosoft.table.TableDataCollection;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.editors.StringTagEditorSupport;

public class ClusterAnalysisParametersBeanInfo extends BeanInfoEx
{
    public ClusterAnalysisParametersBeanInfo()
    {
        super(ClusterAnalysisParameters.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(getResourceString("CN_CLASS"));
        beanDescriptor.setShortDescription(getResourceString("CD_CLASS"));
    }

    @Override
    public void initProperties() throws Exception
    {
        add(new PropertyDescriptorEx("experimentData", beanClass), getResourceString("PN_EXPERIMENT"), getResourceString("PD_EXPERIMENT"));
        add(new PropertyDescriptorEx("method", beanClass), MethodEditor.class, getResourceString("PN_CLUSTER_METHOD"),
                getResourceString("PD_CLUSTER_METHOD"));
        add(new PropertyDescriptorEx("clusterCount", beanClass), getResourceString("PN_CLUSTER_COUNT"),
                getResourceString("PD_CLUSTER_COUNT"));
        add(DataElementPathEditor.registerOutput("outputTablePath", beanClass, TableDataCollection.class),
                getResourceString("PN_OUTPUT_TABLE"), getResourceString("PD_OUTPUT_TABLE"));
    }

    public static class MethodEditor extends StringTagEditorSupport
    {
        public MethodEditor()
        {
            super(new String[] {ClusterAnalysisParameters.CLUSTER_HARTIGAN_WONG, ClusterAnalysisParameters.CLUSTER_FORGY,
                    ClusterAnalysisParameters.CLUSTER_LLOYD, ClusterAnalysisParameters.CLUSTER_MACQUEEN});
        }
    }
}
