package ru.biosoft.bsastats.processors;

import java.util.LinkedHashMap;
import java.util.Map;
import com.developmentontheedge.beans.annot.PropertyName;

/**
 * @author lan
 *
 */
public class StatisticsProcessorsRegistry
{
    private static Map<String, Class<? extends StatisticsProcessor>> processors = new LinkedHashMap<>();
    
    private static void register(Class<? extends StatisticsProcessor> clazz)
    {
        processors.put(clazz.getAnnotation(PropertyName.class).value(), clazz);
    }
    
    static
    {
        register(BasicStatsProcessor.class);
        register(QualityPerBaseProcessor.class);
        register(QualityPerSequenceProcessor.class);
        register(NucleotideContentPerBaseProcessor.class);
        register(GCContentPerBaseProcessor.class);
        register(GCPerSequenceProcessor.class);
        register(NContentPerBaseProcessor.class);
        register(LengthDistributionProcessor.class);
        register(DuplicateSequencesProcessor.class);
        register(OverrepresentedSequences.class);
        register(OverrepresentedKmers.class);
        register(OverrepresentedPrefixes.class);
    }

    public static String[] getValues()
    {
        return processors.keySet().toArray(new String[processors.size()]);
    }
    
    public static Class<? extends StatisticsProcessor> getProcessorClass(String name)
    {
        return processors.get(name);
    }
}
