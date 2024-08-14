package ru.biosoft.bsa.analysis.maos.coord_mapping;

import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.analysis.maos.Variation;

public class MappingByVCF extends CoordinateMappingArray
{
    public MappingByVCF(Interval bounds, Variation[] variations)
    {
        super(bounds);

        int prev = bounds.getFrom();
        int altPos = bounds.getFrom();
        for( Variation var : variations )
        {
            for(int j = prev; j < var.getFrom(); j++)
                set(j, altPos++);
            if(var.alt.length == var.ref.length)
                for(int j = var.getFrom(); j <= var.getTo(); j++)
                    set(j, altPos++);
            else
                altPos += var.alt.length;
            prev = var.getTo() + 1;
        }
        for(int i = prev; i <= bounds.getTo(); i++)
            set(i, altPos++);
    }
}