package ru.biosoft.plugins.graph.testkit;

import ru.biosoft.graph.Layouter;

public class LayoutOptions
{
    private TestKit testKit;

    public LayoutOptions ( TestKit testKit )
    {
        this.testKit = testKit;
    }

    public Layouter getLayouter ( )
    {
        return testKit.getLayouter ( );
    }

    public void setLayouter ( Layouter layouter )
    {
        testKit.setLayouter ( layouter );
    }
}
