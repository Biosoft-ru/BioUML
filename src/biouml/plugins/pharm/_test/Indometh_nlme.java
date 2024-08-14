package biouml.plugins.pharm._test;

import biouml.plugins.pharm.nlme.MixedEffectModel;
public class Indometh_nlme extends MixedEffectModel
{   
    public Indometh_nlme()
    {
        super(new IndomethPK(), new Indometh_info());
        resultIndex = 6;
    }
}