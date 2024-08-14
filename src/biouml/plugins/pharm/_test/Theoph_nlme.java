package biouml.plugins.pharm._test;

import biouml.plugins.pharm.nlme.MixedEffectModel;
public class Theoph_nlme extends MixedEffectModel
{   
    public Theoph_nlme()
    {
        super(new PKmodel(), new Theoph_info());
        resultIndex = 4;
    }
}