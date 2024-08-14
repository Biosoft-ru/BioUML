package ru.biosoft.bsa.view;

import ru.biosoft.bsa.project.Project;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.HtmlEditor;
import ru.biosoft.util.TextUtil;

@SuppressWarnings ( "serial" )
public class ProjectDescriptionEditor extends HtmlEditor
{
    public ProjectDescriptionEditor()
    {
        view.setEnabled(false);
    }

    @Override
    public boolean canExplore(Object model)
    {
        return model instanceof Project;
    }

    @Override
    public void explore(Object model, Document document)
    {
        super.explore(model, document);
        view.setEnabled(true);

        Project project = (Project)model;
        String text = project.getDescription();
        if( text == null )
            text = " ";

        setText(text);
    }

    @Override
    public void save()
    {
        Project project = (Project)getModel();
        if( project != null )
        {
            String text = getText();
            String body = TextUtil.getSection("body", text);
            if( body != null )
                text = body;

            project.setDescription(text);
        }
    }
}
