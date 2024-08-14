package ru.biosoft.bsa.macs;

import com.developmentontheedge.beans.Option;

@SuppressWarnings ( "serial" )
public class MACSLambdaSet extends Option
{
    private int lambda1;
    private int lambda2;
    private int lambda3;

    public MACSLambdaSet(Option parent, int[] lambdas)
    {
        super(parent);
        if( lambdas != null && lambdas.length > 2 )
        {
            lambda1 = lambdas[0];
            lambda2 = lambdas[1];
            lambda3 = lambdas[2];
        }
    }

    public int[] getLambdaSet()
    {
        return new int[] {lambda1, lambda2, lambda3};
    }

    public int getLambda1()
    {
        return lambda1;
    }

    public void setLambda1(int lambda)
    {
        Object oldValue = lambda1;
        lambda1 = lambda;
        firePropertyChange("lambda1", oldValue, lambda);
    }

    public int getLambda2()
    {
        return lambda2;
    }

    public void setLambda2(int lambda)
    {
        Object oldValue = lambda2;
        lambda2 = lambda;
        firePropertyChange("lambda2", oldValue, lambda);
    }

    public int getLambda3()
    {
        return lambda3;
    }

    public void setLambda3(int lambda)
    {
        Object oldValue = lambda3;
        lambda3 = lambda;
        firePropertyChange("lambda3", oldValue, lambda);
    }
}
