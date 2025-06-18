package ru.biosoft.graphics.access;

import java.io.File;

import org.json.JSONObject;

import ru.biosoft.access.file.AbstractFileTransformer;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.file.FileDataElement;
import ru.biosoft.graphics.CompositeView;

import com.developmentontheedge.application.ApplicationUtils;

/**
 * {@link ViewDataElement} to {@link FileDataElement} transformer
 */
public class FileViewElementTransformer extends AbstractFileTransformer<ViewDataElement>
{
    @Override
    public Class<ViewDataElement> getOutputType()
    {
        return ViewDataElement.class;
    }

    @Override
    public ViewDataElement load(File input, String name, DataCollection<ViewDataElement> origin) throws Exception
    {
        CompositeView view = new CompositeView(new JSONObject(ApplicationUtils.readAsString(input)));
        return new ViewDataElement(name, origin, view);
    }

    @Override
    public void save(File output, ViewDataElement element) throws Exception
    {
        ApplicationUtils.writeString(output, element.getView().toJSON().toString());
    }
}
