package biouml.plugins.hemodynamics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import biouml.plugins.simulation.Model;

public class ArterialBinaryTreeModel implements Model
{
    protected final static String RRENAL_KEY = "R. Renal";
    protected final static String LRENAL_KEY = "L. Renal";

    public SimpleVessel root = null;

    private int size = 0;

    public ArterialBinaryTreeModel()
    {
        this( null );
    }

    public ArterialBinaryTreeModel(SimpleVessel root)
    {
        if( root != null )
            setRoot( root );
    }

    public void setRoot(SimpleVessel vessel)
    {
        this.root = vessel;
        size = 1;
        vessels = new ArrayList<>();
        vessels.add( root );
        vesselMap = new HashMap<>();
        vesselMap.put( root.getTitle(), root );
    }

    public int size()
    {
        return size;
    }
    
    public int maxDepth = 0;
    public double systole;

    public double time = 0;
    public double inputFlow = 285;
    public double outputFlow = 100;
    public double inputPressure = 100;
    public double outputPressure = 70;
    public double inputArea;
    public double outputArea;
    public double arteriaElasticity;
    public double arterialResistance;
    public double totalVolume = 360;
    public double averagePressure;
    public double venousPressure = 2;
    public double capillaryResistance = 1.0 / 0.9;

    public double bloodViscosity = 0.035; // mPa*c

    public double renalConductivity;
    public double kidneyResistance = 83.33;
    public double kidneyInputFlow = 1.2;
    public double averageTotalVolume;
    public double vascularity = 1;
    public double nueroReceptorsControl = 1;
    public double humoralFactor;
    public double externalPressure = 0;

    public double referencedPressure = 100;
    public double bloodLoss;
    public double ventriclePressure = 120;
    public double vesselSegments = 5;
    public double integrationSegments = 5;
    
    public double factorWeight = 1;
    public double factorLength = 1;
    public double factorArea = 1;
    public double factorBeta = 1;
    public double factorArea2 = 1;
    public double factorBeta2 = 1;
    
    public double capillaryConductivityFactor = 1;
    
    public HashMap<String, SimpleVessel> vesselMap = new HashMap<>();

    public void addChildJunction(SimpleVessel parent, SimpleVessel left, SimpleVessel right)
    {
        if( left != null )
            addVessel( parent, left, true );
        if( right != null )
            addVessel( parent, right, false );
    }

    public void addVessel(SimpleVessel parent, SimpleVessel child, boolean left)
    {
        //        child.index = size;
        child.depth = parent.depth + 1;
        child.parent = parent;
        this.maxDepth = Math.max( child.depth, maxDepth );
        size++;
        if( left )
            parent.left = child;
        else
            parent.right = child;

        vesselMap.put( child.getTitle(), child );
        vessels.add( child );
    }

    @Override
    public int hashCode()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object obj)
    {
        if( this == obj )
            return true;
        if( obj == null )
            return false;
        if( getClass() != obj.getClass() )
            return false;

        final ArterialBinaryTreeModel other = (ArterialBinaryTreeModel)obj;
        if( root == null && other.root == null )
            return true;
        if( !Objects.equals( root, other.root ) || size != other.size )
            return false;

        return root.left.equals( other.root.left ) && root.right.equals( other.root.right );
    }

    @Override
    public double[] getInitialValues()
    {
        //TODO:
        return new double[] {0};
    }

    @Override
    public void init()
    {
       isInit = true;
    }

    @Override
    public void init(double[] initialValues, Map<String, Double> parameters)
    {
    }

    @Override
    public ArterialBinaryTreeModel clone()
    {
        return null;
    }

    protected boolean isInit = false;
    @Override
    public boolean isInit()
    {
        return isInit;
    }

    @Override
    public double[] getCurrentValues()
    {
        double[] result = new double[19];

        result[0] = time;
        result[1] = inputPressure;
        result[2] = outputPressure;
        result[3] = inputFlow;
        result[4] = outputFlow;
        result[5] = inputArea;
        result[6] = outputArea;
        result[7] = venousPressure;
        result[8] = averagePressure;
        result[9] = capillaryResistance;
        result[10] = totalVolume;
        result[11] = nueroReceptorsControl;
        result[12] = vascularity;
        result[13] = renalConductivity;
        result[14] = kidneyResistance;
        result[15] = kidneyInputFlow;
        result[16] = arterialResistance;
        result[17] = externalPressure;
        result[18] = bloodLoss;
        
        return result;
    }

    @Override
    public void setCurrentValues(double[] values) throws Exception
    {
        time = values[0];
        inputPressure = values[1];
        outputPressure = values[2];
        inputFlow = values[3];
        outputFlow = values[4];
        inputArea = values[5];
        outputArea = values[6];
        venousPressure = values[7];
        averagePressure = values[8];
        capillaryResistance = values[9];
        totalVolume = values[10];
        nueroReceptorsControl = values[11];
        vascularity = values[12];
        renalConductivity = values[13];
        kidneyResistance = values[14];
        kidneyInputFlow = values[15];
        arterialResistance = values[16];
        externalPressure = values[17];
        bloodLoss = values[18];
    }

    public List<SimpleVessel> vessels;


    public void calculateParameters(double time)
    {
        //nothing by default;
    }

    static final Map<String, Integer> basicVariables = Collections.<String, Integer>unmodifiableMap( new HashMap<String, Integer>()
    {
        {
            int i = 0;
            put( "time", i++ );
            put( "inputPressure", i++ );
            put( "outputPressure", i++ );
            put( "inputFlow", i++ );
            put( "outputFlow", i++ );
            put( "inputArea", i++ );
            put( "outputArea", i++ );
            put( "venousPressure", i++ );
            put( "averagePressure", i++ );
            put( "capillaryResistance", i++ );
            put( "totalVolume", i++ );
            put( "nueroReceptorsControl", i++ );
            put( "vascularity", i++ );
            put( "renalConductivity", i++ );
            put( "kidneyResistance", i++ );
            put( "kidneyInputFlow", i++);
            put( "arterialResistance", i++ );
            put( "externalPressure", i++ );
            put( "bloodLoss", i++ );
            put( "vesselSegments", i++ );
            put( "integrationSegments", i++ );
            put( "factorBeta", i++ );
            put( "ventriclePressure", i++ );
            put( "humoralFactor", i++ );
            put( "bloodViscosity", i++ );
            put( "systole", i++ );
            put( "referencedPressure", i++ );
            put( "factorArea", i++ );
            put( "factorLength", i++ );
            put( "factorArea2", i++ );
            put( "factorBeta2", i++ );
            put( "factorWeight", i++ );
            put( "capillaryConductivityFactor", i++ );
        }
    });
    
    @Override
	public double[] getCurrentState() throws Exception 
	{
		return getCurrentValues();
	}
}
