package ru.biosoft.workbench.documents;

import ru.biosoft.access.ImageElement;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.ImageView;
import ru.biosoft.graphics.editor.ViewPane;
import ru.biosoft.gui.Document;

/**
 * @author lan
 *
 */
public class ImageDocument extends Document
{
    /**
     * @param model
     */
    public ImageDocument(ImageElement model)
    {
        super(model);
        this.viewPane = new ViewPane();
        CompositeView view = new CompositeView();
        ImageView imageView = new ImageView(model.getImage(null), 0, 0);
        imageView.setModel(model);
        imageView.setSelectable(true);
        imageView.setActive(true);
        view.add(imageView);
        this.viewPane.setView(view);
    }

    @Override
    public String getDisplayName()
    {
        return getModel().getName();
    }

    @Override
    public boolean isChanged()
    {
        return false;
    }

    @Override
    public boolean isMutable()
    {
        return false;
    }

    @Override
    public ImageElement getModel()
    {
        return (ImageElement)super.getModel();
    }
}
