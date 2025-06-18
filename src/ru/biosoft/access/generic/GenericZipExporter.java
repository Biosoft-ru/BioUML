package ru.biosoft.access.generic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListResourceBundle;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.DataElementExporter;
import ru.biosoft.access.DataElementExporterRegistry;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.DataElementExporterRegistry.ExporterInfo;
import ru.biosoft.access.FileExporter;
import ru.biosoft.util.LazyValue;
import ru.biosoft.util.TempFiles;
import ru.biosoft.util.TextUtil2;
import ru.biosoft.workbench.editors.GenericComboBoxEditor;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControl;

public class GenericZipExporter implements DataElementExporter
{
    private ExporterProperties properties = null;
    private static final String MY_FORMAT = "Archive containing exported elements (*.zip)";

    @Override
    public int accept(DataElement de)
    {
        if( ! ( de instanceof DataCollection ) )
            return DataElementExporter.ACCEPT_UNSUPPORTED;
        if( DataCollectionUtils.checkPrimaryElementType(de, FolderCollection.class) )
        {
            return DataElementExporter.ACCEPT_HIGH_PRIORITY;
        }
        return DataElementExporter.ACCEPT_UNSUPPORTED;
    }

    @Override
    public void doExport(@Nonnull ru.biosoft.access.core.DataElement de, @Nonnull File file) throws Exception
    {
        doExport(de, file, null);
    }

    private static class ZipDirectory
    {
        private final DataCollection<? extends DataElement> gdc;
        private final String relativePath;

        public ZipDirectory(DataCollection<? extends DataElement> gdc, String relativePath)
        {
            this.gdc = gdc;
            this.relativePath = relativePath;
        }

        public ZipDirectory(DataCollection<? extends DataElement> gdc)
        {
            this(gdc, "");
        }

        public ZipDirectory getChildDirectory(String name) throws Exception
        {
            return new ZipDirectory((DataCollection<? extends DataElement>)gdc.get(name), relativePath + name + "/");
        }

        public DataCollection<? extends DataElement> getCollection()
        {
            return gdc;
        }

        public String getRelativePath()
        {
            return relativePath;
        }
    }

    @Override
    public void doExport(@Nonnull ru.biosoft.access.core.DataElement de, @Nonnull File file, FunctionJobControl jobControl) throws Exception
    {
        Deque<ZipDirectory> dirList = new ArrayDeque<>();
        dirList.add(new ZipDirectory((DataCollection<? extends DataElement>)de));
        
        Map<String, DataElement> elementsToExport = new HashMap<>();
        
        while( !dirList.isEmpty() )
        {
            ZipDirectory directory = dirList.pollFirst();
            Iterator<? extends DataElement> iterator = directory.getCollection().iterator();
            while( iterator.hasNext() )
            {
                DataElement element;
                try
                {
                    element = iterator.next();
                }
                catch( Exception e )
                {
                    continue;
                }
                if(element == null) continue;
                if( DataCollectionUtils.checkPrimaryElementType(element, FolderCollection.class) )
                {
                    dirList.add(directory.getChildDirectory(element.getName()));
                    continue;
                }
                elementsToExport.put( directory.getRelativePath() + element.getName(), element);
            }
        }

        try (ZipOutputStream zip = new ZipOutputStream( new FileOutputStream( file ) ))
        {
            int i = 0;
            for(Entry<String,ru.biosoft.access.core.DataElement> exportEntry : elementsToExport.entrySet())
            {
                String path = exportEntry.getKey();
                DataElement element = exportEntry.getValue();
                ExporterInfo[] infos = DataElementExporterRegistry.getExporterInfo(properties.getExportFormat(), element);
                if( infos == null || infos.length == 0 )
                {
                    infos = new ExporterInfo[] {new ExporterInfo( new FileExporter(), new Properties() )};
                }
                for( ExporterInfo info : infos )
                {
                    DataElementExporter exporter = info.cloneExporter();
                    if( exporter.accept(element) > 0 )
                    {
                        String suffix = TextUtil2.isEmpty(info.getSuffix()) ? "" : "." + info.getSuffix();
                        File elementFile = null;
                        try
                        {
                            elementFile = TempFiles.file("zipExport"+suffix);
                            exporter.doExport(element, elementFile);
                        }
                        catch( Exception e )
                        {
                            if(elementFile != null)
                                elementFile.delete();
                            continue;
                        }
                        if( elementFile.exists() )
                        {
                            ZipEntry entry = new ZipEntry(path + suffix);
                            zip.putNextEntry(entry);
                            copyStream(new FileInputStream(elementFile), zip);
                            zip.closeEntry();
                            elementFile.delete();
                        }
                        break;
                    }
                }
                if( jobControl != null )
                {
                    if( jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST )
                    {
                        file.delete();
                        return;
                    }
                    jobControl.setPreparedness( i * 100 / elementsToExport.size() );
                }
                i++;
            }

        }
        catch( Exception e )
        {
            file.delete();
            throw e;
        }
    }

    @Override
    public Object getProperties(DataElement de, File file)
    {
        if( properties == null )
            properties = new ExporterProperties();
        return properties;
    }

    @Override
    public boolean init(Properties properties)
    {
        return true;
    }

    public static class ExporterProperties extends Option
    {
        private static final long serialVersionUID = 1L;
        private String exportFormat = "Tab-separated text (*.txt)";

        public String getExportFormat()
        {
            return exportFormat;
        }

        public void setExportFormat(String exportFormat)
        {
            Object oldValue = this.exportFormat;
            this.exportFormat = exportFormat;
            firePropertyChange("exportFormat", oldValue, this.exportFormat);
        }
    }

    public static class ExporterPropertiesMessageBundle extends ListResourceBundle
    {
        @Override
        protected Object[][] getContents()
        {
            return contents;
        }

        private static final Object[][] contents = {
                {"CN_CLASS", "Export properties"},
                {"CD_CLASS", "Export properties"},
                {"PN_EXPORT_FORMAT", "Export format"},
                {"PD_EXPORT_FORMAT",
                        "Export all elements in the specified format (elements not compatible with this format will be skipped)"},};
    }// end of class MessagesBundle

    public static class ExportFormatSelector extends GenericComboBoxEditor
    {
        private static final LazyValue<String[]> formats = new LazyValue<>( "Export formats",
                () -> DataElementExporterRegistry.formats().filter( f -> !MY_FORMAT.equals( f ) ).sorted().toArray( String[]::new ) );

        @Override
        protected Object[] getAvailableValues()
        {
            return formats.get();
        }
    }

    public static class ExporterPropertiesBeanInfo extends BeanInfoEx
    {
        public ExporterPropertiesBeanInfo()
        {
            super(ExporterProperties.class);
            beanDescriptor.setDisplayName(getResourceString("CN_CLASS"));
            beanDescriptor.setShortDescription(getResourceString("CD_CLASS"));
        }

        @Override
        protected void initProperties() throws Exception
        {
            add(new PropertyDescriptorEx("exportFormat", beanClass), ExportFormatSelector.class, getResourceString("PN_EXPORT_FORMAT"),
                    getResourceString("PD_EXPORT_FORMAT"));
        }
    }

    protected static final int BUFF_SIZE = 100000;

    public static void copyStream(InputStream in, OutputStream out) throws IOException
    {
        byte[] buffer = new byte[BUFF_SIZE];
        try
        {
            while( true )
            {
                synchronized( buffer )
                {
                    int amountRead = in.read(buffer);
                    if( amountRead == -1 )
                    {
                        break;
                    }
                    out.write(buffer, 0, amountRead);
                }
            }
        }
        finally
        {
            if( in != null )
            {
                in.close();
            }
        }
    }
    
    @Override
    public List<Class<? extends DataElement>> getSupportedTypes()
    {
        return Arrays.asList( ru.biosoft.access.core.DataCollection.class );
    }
}
