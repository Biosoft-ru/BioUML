package ru.biosoft.access;

import java.io.File;
import java.util.List;
import java.util.Properties;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.jobcontrol.FunctionJobControl;

/**
 * Generic interface to export data element in the specified format
 */
public interface DataElementExporter
{
    public static final int ACCEPT_HIGH_PRIORITY = 20;
    public static final int ACCEPT_MEDIUM_PRIORITY = 10;
    public static final int ACCEPT_LOW_PRIORITY = 5;
    public static final int ACCEPT_UNSUPPORTED = 0;
    
    /** Returns accept priority if the specified data element can be exported in this format and false otherwise. */
    public int accept(DataElement de);

    /**
     * Exports the specified data element into the specified file or directory.
     * @param de - data element to be exported
     * @param file - name of file or directory where the export results will be stored.
     */
    public void doExport(@Nonnull DataElement de, @Nonnull File file) throws Exception;
    public void doExport(@Nonnull DataElement de, @Nonnull File file, FunctionJobControl jobControl) throws Exception;

    /**
     * The same exporter can support several formats.
     * In this case several instances of exporter can be initialized with different
     * <code>format</code> and <code>suffix</code> parameters.
     *
     * @param properties - properties from <export> block in plugin.xml
     * properties.getProperty(DataElementExporterRegistry.FORMAT) - format for export
     * properties.getProperty(DataElementExporterRegistry.SUFFIX) - file suffix for the specified format
     * Other properties are optional
     *
     * @return true if the exporter was initialized successfully
     * for the specified format and false otherwise.
     */
    boolean init(Properties properties);
    
    /**
     * Returns properties for current export.
     * null if properties not necessary
     */
    default Object getProperties(DataElement de, File file) { return null; }
    
    List<Class<? extends DataElement>> getSupportedTypes();
}