
package ru.biosoft.bsa.view;

import java.util.Collections;
import java.util.Comparator;

import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.View;

/**
 * Class for visualization of the site sets.
 */
public class TrackView extends CompositeView
{
    public TrackView()
    {
        super();
    }

    public void sort(Comparator<View> c)
    {
        Collections.sort(children, c);
    }
}
