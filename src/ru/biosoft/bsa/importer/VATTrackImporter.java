package ru.biosoft.bsa.importer;

import java.io.File;
import java.io.FileFilter;
import java.util.Properties;

import javax.annotation.Nonnull;

import java.util.logging.Logger;

import com.developmentontheedge.beans.DynamicPropertySet;
import ru.biosoft.jobcontrol.FunctionJobControl;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.ImageFileImporter;
import ru.biosoft.bsa.Site;
import ru.biosoft.util.TextUtil;
import ru.biosoft.util.j2html.TagCreator;

public class VATTrackImporter extends VCFTrackImporter
{
    private static final String VARIANT_ANNOTATOR_COLUMN = "Info_VA";

    private DataCollection<?> imageCollection;

    @Override
    protected Site parseLine(String line)
    {
        Site result = super.parseLine(line);
        if(result == null)
            return null;
        DynamicPropertySet dps = result.getProperties();
        Object value = dps.getValue(VARIANT_ANNOTATOR_COLUMN);
        if( value != null )
            dps.setValue(VARIANT_ANNOTATOR_COLUMN, addLinkToImages(value.toString()));

        return result;
    }

    private String addLinkToImages(String annotationString)
    {
        String[] fields = TextUtil.split(annotationString, ':');

        for( int i = 0; i < fields.length; i++ )
        {
            String imageName = fields[i] + ".png";
            if( imageCollection.contains(imageName) )
                fields[i] = TagCreator.a().withHref( "#de=" + DataElementPath.create( imageCollection, imageName ) ).withText( fields[i] )
                        .render();
        }
        return String.join(":", fields);
    }

    @Override
    public DataElement doImport(@Nonnull DataCollection parent, @Nonnull File file, String elementName, FunctionJobControl jobControl, Logger log) throws Exception
    {
        String imageCollectionName = elementName + " images";
        if( parent.contains(imageCollectionName) )
            parent.remove(imageCollectionName);
        imageCollection = DataCollectionUtils.createSubCollection(DataElementPath.create(parent, imageCollectionName));
        ImageFileImporter importer = new ImageFileImporter();
        FileFilter filter = file1 -> file1.getName().endsWith(".png") && file1.isFile();
        File parentFile = file.getParentFile();
        if( parentFile != null )
        {
            File[] images = parentFile.listFiles( filter );
            if( images != null )
            {
                for( File imageFile : images )
                    importer.doImport( imageCollection, imageFile, null, null, log );
            }
        }
        return super.doImport(parent, file, elementName, jobControl, log);
    }
    
    @Override
    public boolean init(Properties properties)
    {
        super.init(properties);
        format = "vat";
        return true;
    }
}
