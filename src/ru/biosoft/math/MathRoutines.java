package ru.biosoft.math;

import java.util.Random;

/*
 * WARNING: READ BEFORE EDITING THIS CLASS!
 * 
 * This class and some others constitutes separate jar which is used during model simulation in BioUML,
 * therefore all class on which it depends should also added to this jar
 * jar file used for simulation is specified by SimulationEngine
 * 
 * Before adding any new dependencies here - please think twice.
 * 
 * If you add dependency - add this class (and all classes from which it depends) to build_bdk.xml
 * (see biouml.plugins.simulation building)
 * @see SimualtionEngine
 */


public class MathRoutines
{
    static public double normal(double a, double b)
    {
        return a + new Random().nextGaussian()*b;
    }
    
    public static double logNormal(double mean, double stddev)
    {
        Random randGen = new Random();
        double varx = Math.pow( stddev, 2 );
        double ess = Math.log( 1.0 + ( varx / Math.pow( mean, 2 ) ) );
        double mu = Math.log( mean ) - ( 0.5 * Math.pow( ess, 2 ) );
        return Math.exp( ( mu + ( ess * randGen.nextGaussian() ) ) );
    }

    static public double binomial(double n, double prob)
    {
        int x = 0;
        for( int i = 0; i < n; i++ )
        {
            if( Math.random() < prob )
                x++;
        }
        return x;
    }
    
    static public double uniform(double a, double b)
    {
        return a+Math.random()*(b-a);
    }

    static public double factorial(int n)
    {
        double result = 1;
        for( int i = 1; i <= n; i++ )
            result = result * i;
        return result;
    }

    static public double factorial(double n)
    {
        return factorial((int)n);
    }

    static public double sh(double x)
    {
        return ( Math.exp(x) - Math.exp( -x) ) / 2.0;
    }

    static public double ch(double x)
    {
        return ( Math.exp(x) + Math.exp( -x) ) / 2.0;
    }  
    
    static public double ctgh(double x)
    {
        double sh = sh(x);
        if( 0 == sh )
            return Double.MAX_VALUE;
        return ch(x) / sh;
    }

    static public double ash(double x)
    {
        return Math.log(x + Math.sqrt(x * x + 1));
    }

    static public double ach(double x)
    {
        double y = x * x - 1;
        if( y < 0 )
            return 0;
        y = x + Math.sqrt(y);
        if( y < 0 )
            return 0;
        return Math.log(y);
    }

    static public double atgh(double x)
    {
        if( x >= 1 || x <= -1 )
            return 0;
        return Math.log( ( 1 + x ) / ( 1 - x )) / 2.0;
    }

    static public double actgh(double x)
    {
        if( x <= 1 && x >= -1 )
            return 0;
        return Math.log( ( x + 1 ) / ( x - 1 )) / 2.0;
    }

    static public double sec(double x)
    {
        double c = Math.cos(x);
        if( c == 0 )
            return Double.MAX_VALUE;
        return 1.0 / c;
    }

    static public double csec(double x)
    {
        double s = Math.sin(x);
        if( s == 0 )
            return Double.MAX_VALUE;
        return 1.0 / s;
    }

    static public double csech(double x)
    {
        double s = sh(x);
        if( s == 0 )
            return Double.MAX_VALUE;
        return 1.0 / s;
    }

    static public double sech(double x)
    {
        return 1.0 / ch(x);
    }

    static public double asec(double x)
    {
        double y = 0;
        if( x == 0 )
            y = Double.MAX_VALUE;
        else
            y = 1.0 / x;
        return Math.acos(y);
    }

    static public double acsec(double x)
    {
        double y = 0;
        if( x == 0 )
            y = Double.MAX_VALUE;
        else
            y = 1.0 / x;
        return Math.asin(y);
    }

    static public double acsech(double x)
    {
        double y = 0;
        if( x == 0 )
            y = Double.MAX_VALUE;
        else
            y = 1.0 / x;
        return ash(y);
    }

    static public double asech(double x)
    {
        double y = 0;
        if( x == 0 )
            y = Double.MAX_VALUE;
        else
            y = 1.0 / x;
        return ach(y);
    }

    static public double actg(Double x)
    {
        //fixed for -0.0 correct handling
        int comparedToZero = x.compareTo(0.0);
        if( comparedToZero >= 0 )
            return Math.PI / 2.0 - Math.atan(x);
        else
            return -Math.PI / 2.0 - Math.atan(x);
    }

    // Alternative to Math.pow (works for exp>=1; significantly faster than Math.pow for exp<1000)
    static public double pow(double x, int exp)
    {
        if(exp == 1)
            return x;
        if(exp == 2)
            return x*x;
        if( ( exp & 1 ) == 0 )
        {
            double sub = pow(x, exp >> 1);
            return sub * sub;
        }
        return x * pow(x, exp - 1);
    }
}