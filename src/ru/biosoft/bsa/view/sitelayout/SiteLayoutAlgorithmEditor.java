
/**
 * Author:  Igor V. Tyazhev  (champ@developmentontheedge.com)
 *
 * Created: 28.03.2001
 *
 * Description:
 *
 * Copyright (C) 2000, 2001 DevelopmentOnTheEdge.com. All rights reserved.
 */
package ru.biosoft.bsa.view.sitelayout;

import java.beans.PropertyEditorSupport;

/**
 * Implements TagEditor for SiteLayoutAlgorithm.
 * @author Fedor A. Kolpakov
 */
public class SiteLayoutAlgorithmEditor extends PropertyEditorSupport
{
    private static final String[] values = {"Simple", "List", "OptimizedList", };

    @Override
    public String getAsText()
    {
        SiteLayoutAlgorithm sla = (SiteLayoutAlgorithm)getValue();
        if(sla instanceof SimpleLayoutAlgorithm)
        {
            return values[0];
        }
        else if(sla instanceof ListLayoutAlgorithm)
        {
            return values[1];
        }
        else
        {
            return values[2];
        }
    }

    @Override
    public void setAsText( String text )
    {
        if(text.equalsIgnoreCase(values[0]))
        {
            setValue(new SimpleLayoutAlgorithm());
        }
        else if(text.equalsIgnoreCase(values[1]))
        {
            setValue(new ListLayoutAlgorithm());
        }
        else
        {
            setValue(new OptimizedListLayoutAlgorithm());
        }
    }

    @Override
    public String[] getTags()
    {
        return values.clone();
    }
}
