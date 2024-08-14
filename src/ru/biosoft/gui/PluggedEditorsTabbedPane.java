package ru.biosoft.gui;

public class PluggedEditorsTabbedPane extends EditorsTabbedPane
{
    public PluggedEditorsTabbedPane()
    {
        for(ViewPart viewPart : ViewPartRegistry.getViewParts())
        {
            addViewPart( viewPart, !"false".equals( viewPart.getAction().getValue( ViewPart.DEFAULT_ENABLE ) ) );
        }
    }
}
