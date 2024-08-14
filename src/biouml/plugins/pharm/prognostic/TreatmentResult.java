package biouml.plugins.pharm.prognostic;

import java.util.HashMap;
import java.util.Map;

import one.util.streamex.StreamEx;

public class TreatmentResult
{

    public String drug;
    public double total;
    public double success;
    public double weakEffect;
    public double negativeEffect;
    public double incorrect;

    private Map<String, String[]> drop = new HashMap<>();

    public void addDrop(String name, String mean, String sd)
    {
        drop.put(name, new String[] {mean, sd});
    }

    public String[] getDrops()
    {
        return StreamEx.of(drop.keySet()).toArray(String[]::new);
    }

    public String getAverage(String name)
    {
        return drop.containsKey(name) ? drop.get(name)[0] : "";
    }

    public String getSD(String name)
    {
        return drop.containsKey(name) ? drop.get(name)[1] : "";
    }

    public double getTotal()
    {
        return total;
    }

    public double getSuccess()
    {
        return success;
    }

    public double getWeakEffect()
    {
        return weakEffect;
    }

    public double getNegativeEffect()
    {
        return negativeEffect;
    }

    public double getIncorrect()
    {
        return incorrect;
    }
    public String getDrug()
    {
        return drug;
    }
}