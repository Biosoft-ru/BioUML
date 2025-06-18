package ru.biosoft.access.html;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.annotation.Nonnull;
import javax.imageio.ImageIO;

import ru.biosoft.access.core.CloneableDataElement;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementSupport;
import ru.biosoft.access.HtmlDataElement;
import ru.biosoft.access.ImageDataElement;
import ru.biosoft.access.LocalRepository;
import ru.biosoft.access.core.TextDataElement;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.access.core.RepositoryAccessDeniedException;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.util.HashMapWeakValues;
import ru.biosoft.util.TextUtil2;
import com.developmentontheedge.beans.annot.PropertyName;

import com.developmentontheedge.application.ApplicationUtils;

/**
 * This collection
 * This collection represents Zip-archive without unpacking it
 * @author lan
 */
@ClassIcon("resources/html.gif")
@PropertyName("html")
public class ZipHtmlDataCollection extends VectorDataCollection<DataElement> implements CloneableDataElement
{
    protected ZipFile file;
    private boolean init;

    private ZipHtmlDataCollection(String name, ZipHtmlDataCollection parent)
    {
        super(name, parent, null);
        v_cache = new HashMapWeakValues();
    }

    private void internalPut(DataElement de) throws Exception
    {
        super.doPut(de, true);
    }

    @Override
    public void close() throws Exception
    {
        super.close();
        if(file != null) file.close();
    }

    /**
     * @param parent
     * @param properties
     */
    public ZipHtmlDataCollection(DataCollection<?> parent, Properties properties)
    {
        super(parent, properties);
        v_cache = new HashMapWeakValues();
    }

    @Override
    public int getSize()
    {
        init();
        return super.getSize();
    }

    @Override
    public @Nonnull List<String> getNameList()
    {
        init();
        return super.getNameList();
    }

    public File getFile()
    {
        String fileName = getInfo().getProperty(DataCollectionConfigConstants.FILE_PROPERTY);
        File file = new File(fileName);
        if(file.exists()) return file;
        if(getOrigin() instanceof LocalRepository)
        {
            file = new File(((LocalRepository)getOrigin()).getRootDirectory(), file.getName());
        }
        return file;
    }

    protected synchronized void init()
    {
        if(init) return;
        init = true;
        try
        {
            file = new ZipFile(getFile());
            Enumeration<? extends ZipEntry> entries = file.entries();
            while(entries.hasMoreElements())
            {
                ZipEntry zipEntry = entries.nextElement();
                if(!zipEntry.isDirectory())
                {
                    ZipHtmlDataCollection current = this;
                    String[] nameComponents = TextUtil2.split( zipEntry.getName(), '/' );
                    for(int i=0; i<nameComponents.length-1; i++)
                    {
                        String nameComponent = nameComponents[i];
                        DataElement de = current.get(nameComponent);
                        if(de == null)
                        {
                            de = new ZipHtmlDataCollection(nameComponent, current);
                            ((ZipHtmlDataCollection)de).init = true;
                            current.internalPut(de);
                        }
                        if(!(de instanceof ZipHtmlDataCollection))
                        {
                            throw new IOException("Zip structure invalid: "+nameComponent+": inside file");
                        }
                        current = (ZipHtmlDataCollection)de;
                    }
                    current.internalPut(new ZipEntryElement(nameComponents[nameComponents.length-1], current, zipEntry));
                }
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Problem opening ZIP file for " + getCompletePath() + " (" + getInfo().getProperty(DataCollectionConfigConstants.FILE_PROPERTY) + ")", e);
        }
    }

    @Override
    protected DataElement doGet(String name)
    {
        init();
        DataElement de = super.doGet(name);
        if(!(de instanceof ZipEntryElement)) return de;
        try
        {
            return ((ZipEntryElement)de).transform();
        }
        catch( IOException e )
        {
            return new TextDataElement(de.getName(), de.getOrigin(), "Error reading file: "+e.getMessage());
        }
    }

    @Override
    protected void doPut(DataElement dataElement, boolean isNew)
    {
        throw new RepositoryAccessDeniedException( getCompletePath(), SecurityManager.getSessionUser(), "write" );
    }

    @Override
    protected void doRemove(String name)
    {
        throw new RepositoryAccessDeniedException( getCompletePath(), SecurityManager.getSessionUser(), "write" );
    }

    private class ZipEntryElement extends DataElementSupport
    {
        private final ZipEntry entry;

        public ZipEntryElement(String name, DataCollection<?> origin, ZipEntry entry)
        {
            super(name, origin);
            this.entry = entry;
        }

        private String readFileContent() throws IOException
        {
            return ApplicationUtils.readAsString(file.getInputStream(entry));
        }

        public DataElement transform() throws IOException
        {
            String name = getName().toLowerCase(Locale.ENGLISH);
            if(name.endsWith(".html"))
                return new HtmlDataElement(getName(), getOrigin(), readFileContent());
            else if( name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".gif") || name.endsWith(".jpef")
                    || name.endsWith(".bmp") )
            {
                try (InputStream inputStream = file.getInputStream( entry ))
                {
                    BufferedImage image = ImageIO.read( inputStream );
                    return new ImageDataElement( getName(), getOrigin(), image );
                }
            }
            else return new TextDataElement(getName(), getOrigin(), readFileContent());
        }
    }

    @Override
    public DataCollection<?> clone(DataCollection origin, String name) throws CloneNotSupportedException
    {
        try
        {
            File resultFile = DataCollectionUtils.getChildFile(origin, name);
            ApplicationUtils.linkOrCopyFile(resultFile, new File(getInfo().getProperty(DataCollectionConfigConstants.FILE_PROPERTY)), null);
            Properties properties = new Properties();
            properties.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, name);
            properties.setProperty(DataCollectionConfigConstants.FILE_PROPERTY, resultFile.getAbsolutePath());
            ZipHtmlDataCollection result = new ZipHtmlDataCollection(origin, properties);
            DataCollectionUtils.copyAnalysisParametersInfo( this, result );
            origin.put(result);
            return result;
        }
        catch( Exception e )
        {
            try
            {
                origin.remove(name);
            }
            catch( Exception e1 )
            {
            }
            throw ExceptionRegistry.translateException(e);
        }
    }
}
