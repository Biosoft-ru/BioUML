package ru.biosoft.plugins.graph;

import java.util.ArrayList;
import java.util.List;

import ru.biosoft.graph.Layouter;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

public class LayouterOptions
{
    private Layouter layouter;
    
    private List<LayouterOptionsListener> listeners = new ArrayList<>();

    public LayouterOptions(Layouter layouter)
    {
        this.layouter = layouter;
    }

    @PropertyName("Layouter")
    @PropertyDescription("Layouter")
    public Layouter getLayouter()
    {
        return layouter;
    }

    public void setLayouter(Layouter layouter)
    {
        this.layouter = layouter;
        for( LayouterOptionsListener listener : listeners )
            listener.layouterSwitched(layouter);
    }

    public void addListener(LayouterOptionsListener listener)
    {
        listeners.add(listener);
    }

    public void removeListener(LayouterOptionsListener listener)
    {
        listeners.remove(listener);
    }

}
