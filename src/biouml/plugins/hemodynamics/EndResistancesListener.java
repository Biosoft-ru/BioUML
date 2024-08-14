package biouml.plugins.hemodynamics;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.standard.simulation.ResultListener;
import one.util.streamex.StreamEx;
import ru.biosoft.analysis.Stat;

public class EndResistancesListener implements ResultListener
{

    private boolean finalized = false;
    private Map<String, List<Double>> actualK;
    private Map<String, List<Double>> nonReflectionK;
    
    private ArterialBinaryTreeModel model;

    @Override
    public void start(Object model)
    {
        this.model = (ArterialBinaryTreeModel)model;
        actualK = new HashMap<>();
        nonReflectionK = new HashMap<>();
        finalized = false;

        for( Entry<String, SimpleVessel> e : this.model.vesselMap.entrySet() )
        {
            if (Util.isTerminal(e.getValue()))
            {
                actualK.put(e.getKey(), new ArrayList<>());
                nonReflectionK.put(e.getKey(), new ArrayList<>());
            }
        }
    }

    @Override
    public void add(double t, double[] y) throws Exception
    {

        if( t > 2 && t < 4 && Math.abs(t / 0.1 - Math.round(t / 0.1)) < 0.0001  )
        {
            System.out.println("Listen at t = " + t);
            for( Entry<String, SimpleVessel> e : model.vesselMap.entrySet() )
            {
                SimpleVessel vessel = e.getValue();
                
                if (!Util.isTerminal(vessel))
                    continue;
                String name = e.getKey();

                //this is how we distribute peripheral resistance between vessels
                double actual = 1 / ( model.capillaryResistance * model.outputArea / vessel.getArea()[(int)model.vesselSegments] );
                double nonReflection = 1 /Util.calcNoReflectionResistance(vessel, model.venousPressure);

                actualK.get(name).add(actual);
                nonReflectionK.get(name).add(nonReflection);
            }
        }
        else if( t > 4 )
            finalized = true;

        if( finalized )
        {
            writeResults();
        }
    }

    private void writeResults() throws IOException
    {
        try (BufferedWriter bw = ApplicationUtils.asciiWriter("C:/my/actual"))
        {

            for( Entry<String, List<Double>> e : actualK.entrySet() )
            {
                bw.write(e.getKey() + "\t"+model.vesselMap.get(e.getKey()).getArea()[0]+"\t" + StreamEx.of(e.getValue()).joining("\t")+"\n");
            }
        }
        
        try (BufferedWriter bw = ApplicationUtils.asciiWriter("C:/my/nonReflection"))
        {

            for( Entry<String, List<Double>> e : nonReflectionK.entrySet() )
            {
                bw.write(e.getKey() + "\t"+model.vesselMap.get(e.getKey()).getArea()[0]+"\t" + StreamEx.of(e.getValue()).joining("\t")+"\n");
            }
        }
        
        try (BufferedWriter bw = ApplicationUtils.asciiWriter("C:/my/diff"))
        {
            for( Map.Entry<String, List<Double>> entry : nonReflectionK.entrySet() )
            {
                String key = entry.getKey();
                double actual = Stat.mean(actualK.get(key));
                double nonReflection = Stat.mean( entry.getValue() );
                double diff = actual - nonReflection;
                bw.write(key + "\t" + model.vesselMap.get(key).getArea()[0] + "\t" + actual + "\t" + nonReflection + "\t" + diff + "\n");
            }
        }
    }
}
