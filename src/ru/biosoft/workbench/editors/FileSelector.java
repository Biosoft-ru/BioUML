package ru.biosoft.workbench.editors;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JLabel;

import org.json.JSONException;
import org.json.JSONObject;

import ru.biosoft.access.core.DataElementImporter;
import ru.biosoft.access.repository.JSONSerializable;
import ru.biosoft.util.FileItem;

import com.developmentontheedge.beans.editors.CustomEditorSupport;
import com.developmentontheedge.application.Application;

public class FileSelector extends CustomEditorSupport implements JSONSerializable
{
    public static final String TITLE = "title";
    private String title;
    
    private JLabel createLabel()
    {
        String title="(none)";
        if(getValue() instanceof File)
        {
            title = ((File)getValue()).getAbsolutePath();
        }
        return new JLabel(title);
    }
    
    @Override
    public Component getCustomRenderer(final Component parent, boolean isSelected, boolean hasFocus)
    {
        return getCustomEditor(parent, isSelected);
    }
    
    @Override
    public Component getCustomEditor(Component parent, boolean isSelected)
    {
        init();
        JLabel label = createLabel();
        MouseListener mouseListener = new MouseAdapter()
        {
            /**
             * Invoked when the mouse has been clicked on a component.
             */
            @Override
            public void mousePressed(MouseEvent e)
            {
                String key = DataElementImporter.PREFERENCES_IMPORT_DIRECTORY;
                String importDirectory = Application.getPreferences().getStringValue(key, ".");
                File initialValue = (getValue() instanceof File)?(File)getValue():new File(importDirectory);
                JFileChooser fileChooser = new JFileChooser(initialValue);
                if(title != null)
                    fileChooser.setDialogTitle(title);
                if(fileChooser.showOpenDialog(Application.getApplicationFrame()) == JFileChooser.APPROVE_OPTION)
                {
                    setValue(new FileItem(fileChooser.getSelectedFile()));
                }
            }
        };
        label.addMouseListener(mouseListener);
        return label;
    }

    private void init()
    {
        Object titleObj = getDescriptor().getValue(TITLE);
        title = titleObj == null?null:titleObj.toString();
    }

    @Override
    public void fromJSON(JSONObject input) throws JSONException
    {
        String fileName = input.optString("filePath");
        String originalName = input.optString("originalName");
        FileItem item = new FileItem(fileName);
        item.setOriginalName(originalName);
        item.setId(input.optString("value"));
        setValue(item);
    }

    @Override
    public JSONObject toJSON() throws JSONException
    {
        JSONObject result = new JSONObject();
        if(getValue() instanceof FileItem)
        {
            FileItem item = (FileItem)getValue();
            result.put("value", item.getId());
            result.put("originalName", item.getOriginalName());
        }
        result.put("type", "uploaded-file");
        return result;
    }
}
