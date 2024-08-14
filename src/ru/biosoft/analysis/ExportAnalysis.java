package ru.biosoft.analysis;

import java.io.File;
import java.util.List;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.DataElementExporter;
import ru.biosoft.access.DataElementExporterRegistry;
import ru.biosoft.access.DataElementExporterRegistry.ExporterInfo;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.util.BeanUtil;
import ru.biosoft.util.bean.BeanInfoEx2;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import ru.biosoft.workbench.editors.GenericComboBoxEditor;

import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentModel;
import ru.biosoft.jobcontrol.SubFunctionJobControl;

/**
 * @author lan
 *
 */
@ClassIcon("resources/export-element.gif")
public class ExportAnalysis extends AnalysisMethodSupport<ExportAnalysis.ExportAnalysisParameters>
{
    public ExportAnalysis(DataCollection<?> origin, String name)
    {
        super(origin, name, new ExportAnalysisParameters());
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        super.validateParameters();
        checkNotEmpty("filePath");
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        if(!SecurityManager.isAdmin()) throw new SecurityException("Not enough privileges to run this analysis");
        DataElement de = parameters.getPath().getDataElement();
        File file = new File(parameters.getFilePath());
        ExporterInfo[] infos = DataElementExporterRegistry.getExporterInfo(parameters.getExporter(), de);
        DataElementExporter exporter = infos[0].cloneExporter();
        Object properties = exporter.getProperties(de, file);
        if(properties != null && parameters.getExporterProperties() != null)
        {
            BeanUtil.copyBean(parameters.getExporterProperties(), properties);
        }
        exporter.doExport(de, file, new SubFunctionJobControl(jobControl));
        return null;
    }

    public static class ExportAnalysisParameters extends AbstractAnalysisParameters
    {
        private DataElementPath path;
        private String filePath;
        private String exporter = "";
        private Object exporterProperties;
        
        @PropertyName("Element to export")
        @PropertyDescription("Path to element you want to export")
        public DataElementPath getPath()
        {
            return path;
        }
        
        public void setPath(DataElementPath path)
        {
            Object oldValue = this.path;
            this.path = path;
            firePropertyChange("path", oldValue, path);
            List<String> exporters = DataElementExporterRegistry.getExporterFormats(path.optDataElement());
            if(!exporters.isEmpty() && !exporters.contains(getExporter()))
                setExporter(exporters.get(0));
        }

        @PropertyName("File path")
        @PropertyDescription("Path on the server file system")
        public String getFilePath()
        {
            return filePath;
        }
        
        public void setFilePath(String filePath)
        {
            Object oldValue = this.filePath;
            this.filePath = filePath;
            firePropertyChange("filePath", oldValue, filePath);
        }
        
        @PropertyName("Export format")
        @PropertyDescription("Select format you want to export into")
        public String getExporter()
        {
            return exporter;
        }
        
        public void setExporter(String exporter)
        {
            if(this.exporter.equals(exporter)) return;
            Object oldValue = this.exporter;
            this.exporter = exporter;
            firePropertyChange("exporter", oldValue, exporter);
            DataElement de = getPath().optDataElement();
            ExporterInfo[] exporterInfos = DataElementExporterRegistry.getExporterInfo(exporter, de);
            if(exporterInfos != null && exporterInfos.length > 0)
            {
                setExporterProperties(exporterInfos[0].cloneExporter().getProperties(de, filePath == null ? null : new File(filePath)));
            }
        }
        
        @PropertyName("Format properties")
        @PropertyDescription("Specify format-specific properties")
        public Object getExporterProperties()
        {
            return exporterProperties;
        }
        
        public void setExporterProperties(Object exporterProperties)
        {
            Object oldValue = this.exporterProperties;
            this.exporterProperties = exporterProperties;
            if(exporterProperties != null)
            {
                ComponentModel model = ComponentFactory.getModel(this);
                ComponentFactory.recreateChildProperties(model);
            }
            firePropertyChange("exporterProperties", oldValue, exporterProperties);
        }
        
        public boolean isPropertiesHidden()
        {
            return exporterProperties == null;
        }
    }
    
    public static class ExportAnalysisParametersBeanInfo extends BeanInfoEx2<ExportAnalysisParameters>
    {
        public ExportAnalysisParametersBeanInfo()
        {
            super(ExportAnalysisParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            property( "path" ).inputElement( ru.biosoft.access.core.DataElement.class ).add();
            add("filePath");
            add("exporter", ExporterSelector.class);
            addHidden("exporterProperties", "isPropertiesHidden");
        }
    }
    
    public static class ExporterSelector extends GenericComboBoxEditor
    {
        @Override
        protected Object[] getAvailableValues()
        {
            DataElementPath path = ((ExportAnalysisParameters)getBean()).getPath();
            DataElement de = path == null ? null : path.optDataElement();
            return de == null ? new String[0]:DataElementExporterRegistry.getExporterFormats(de).toArray();
        }
    }
}
