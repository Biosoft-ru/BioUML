package biouml.plugins.hemodynamics;

public class InitialValuesCalculator
{
    int vesselSize;
    double factor = 1333.223684;
    public void calculate(ArterialBinaryTreeModel atm, int inputSondition, int outputCondition)
    {
        tailLength = new int[atm.size()];
        vesselSize = (int)atm.vesselSegments;
        int maxLength = calcTailLength( atm.root );
        calcPressures( atm.root, ( atm.inputPressure - atm.outputPressure ) / maxLength, atm.inputPressure, atm.outputPressure );
        interpolatePressures( atm );
        calcAreas( atm );
        calcResistances( atm );
        calcFlows( atm );
        //
        //        for( int i = 0; i < atm.size(); i++ )
        //        {
        //            SimpleVessel v = atm.vessels.get( i );
        //            System.out.println( atm.vessels.get( i ).index + "\t" + atm.vessels.get( i ).name + "\t" + v.pressure[0] + "\t"
        //                    + v.pressure[vesselSize - 1] + "\t" + tailLength[i] );
        //        }

    }

    int[] tailLength;
    public int calcTailLength(SimpleVessel vessel)
    {
        if( Util.isTerminal( vessel ) )
        {
            tailLength[vessel.index] = 1;
            return 1;
        }

        int leftLength = 0;
        int rightLength = 0;

        if( vessel.left != null )
        {
            leftLength = calcTailLength( vessel.left );
        }
        if( vessel.right != null )
        {
            rightLength = calcTailLength( vessel.right );
        }

        int length = Math.max( leftLength, rightLength ) + 1;
        tailLength[vessel.index] = length;
        return length;
    }

    public void calcPressures(SimpleVessel vessel, double gradient, double inputPressure, double terminalPressure)
    {
        double outputPressure = inputPressure - gradient;

        vessel.pressure = new double[vesselSize + 1];
        vessel.pressure[0] = inputPressure;
        vessel.pressure[vesselSize] = outputPressure;

        SimpleVessel leftVessel = vessel.left;
        SimpleVessel rightVessel = vessel.right;

        if( Util.isTerminal( vessel ) )
        {
            return;
        }

        if (leftVessel == null)
        {
            if (rightVessel != null)
            {
                calcPressures( rightVessel, gradient, outputPressure, terminalPressure );
            }
            else 
            {
                return;
            }
        }
        else if (rightVessel == null)
        {
            calcPressures( leftVessel, gradient, outputPressure, terminalPressure );
        }
        else
        {
            int leftTail = tailLength[leftVessel.index];
            int rightTail = tailLength[rightVessel.index];

            if( leftTail == rightTail )
            {
                calcPressures( leftVessel, gradient, outputPressure, terminalPressure );
                calcPressures( rightVessel, gradient, outputPressure, terminalPressure );
                return;
            }
            if( leftTail > rightTail )
            {
                calcPressures( leftVessel, gradient, outputPressure, terminalPressure );
                double newGradient = ( outputPressure - terminalPressure ) / rightTail;
                calcPressures( rightVessel, newGradient, outputPressure, terminalPressure );
            }
            else
            {
                double newGradient = ( outputPressure - terminalPressure ) / leftTail;
                calcPressures( leftVessel, newGradient, outputPressure, terminalPressure );
                calcPressures( rightVessel, gradient, outputPressure, terminalPressure );
            }

        }
    }

    public void interpolatePressures(ArterialBinaryTreeModel atm)
    {
        for( SimpleVessel v : atm.vessels )
        {
            double inPressure = v.pressure[0];
            double outPressure = v.pressure[vesselSize];
            for( int i = 1; i < vesselSize; i++ )
                v.pressure[i] = inPressure + ( outPressure - inPressure ) * i / ( vesselSize - 1 );
        }
    }

    public void calcAreas(ArterialBinaryTreeModel atm)
    {
        for( SimpleVessel v : atm.vessels )
        {
            v.area = new double[vesselSize + 1];
            for( int i = 0; i <= vesselSize; i++ )
            {
                double sqrtArea = factor * v.pressure[i] * v.area0[i] / v.beta + Math.sqrt( v.area0[i] );
                v.area[i] = sqrtArea * sqrtArea;
            }
        }
    }


    public void calcResistances(ArterialBinaryTreeModel atm)
    {
        for( SimpleVessel v : atm.vessels )
        {
            v.resistance = 8 * atm.bloodViscosity * v.length * Math.PI / ( v.area[0] * v.area[0] );
        }
    }

    public void calcFlows(ArterialBinaryTreeModel atm)
    {
        for( SimpleVessel v : atm.vessels )
        {
            v.flow = new double[vesselSize + 1];
            double flow = ( v.pressure[0] - v.pressure[vesselSize] ) / v.resistance;
            for( int i = 0; i <= vesselSize; i++ )
            {
                v.flow[i] = flow;
            }
        }
    }
}
