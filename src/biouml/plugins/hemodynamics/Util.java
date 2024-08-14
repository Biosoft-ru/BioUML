package biouml.plugins.hemodynamics;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import one.util.streamex.DoubleStreamEx;
import one.util.streamex.StreamEx;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Variable;
import Jama.Matrix;

public class Util
{
    public static double[] sum(double[] vector1, double[] vector2, double scalar) throws Exception
    {
//        if( vector1.length != vector2.length )
//            throw new Exception("vector dimensions doesn't match");
        double[] result = new double[vector1.length];
        for( int i = 0; i < vector1.length; i++ )
            result[i] = ( vector1[i] + vector2[i] ) * scalar;
        return result;
    }
    
    public static double[] sum(double[] vector1, double[] vector2) throws Exception
    {
//        if( vector1.length != vector2.length )
//            throw new Exception("vector dimensions doesn't match");
        double[] result = new double[vector1.length];
        for( int i = 0; i < vector1.length; i++ )
            result[i] = ( vector1[i] + vector2[i] );
        return result;
    }

    public static double[][] diff(double[][] matrix1, double[][] matrix2, double scalar) throws Exception
    {
        int n = matrix1.length;
        int m = matrix1[0].length;
//        if( n != matrix2.length || m != matrix2.length )
//            throw new Exception("matrix dimensions doesn't match");
        double[][] result = new double[n][m];
        for( int i = 0; i < n; i++ )
        {
            for( int j = 0; j < m; j++ )
                result[i][j] = ( matrix1[i][j] - matrix2[i][j] ) * scalar;
        }
        return result;
    }

    public static double[][] sum(double[][] matrix1, double[][] matrix2) throws Exception
    {
        int n = matrix1.length;
        int m = matrix1[0].length;
//        if( n != matrix2.length || m != matrix2[0].length )
//            throw new Exception("matrix dimensions doesn't match");
        double[][] result = new double[n][m];
        for( int i = 0; i < n; i++ )
        {
            for( int j = 0; j < m; j++ )
                result[i][j] = matrix1[i][j] + matrix2[i][j];
        }
        return result;
    }

    public static double[] diff(double[] vector1, double[] vector2) throws Exception
    {
//        if( vector1.length != vector2.length )
//            throw new Exception("vector dimensions doesn't match");
        double[] result = new double[vector1.length];
        for( int i = 0; i < vector1.length; i++ )
            result[i] = vector1[i] - vector2[i];
        return result;
    }

    public static double[] diff(double[] vector1, double[] vector2, double scalar) throws Exception
    {
//        if( vector1.length != vector2.length )
//            throw new Exception("vector dimensions doesn't match");
        double[] result = new double[vector1.length];
        for( int i = 0; i < vector1.length; i++ )
            result[i] = ( vector1[i] - vector2[i] ) * scalar;
        return result;
    }

    /**
     * Multiplying scalar on vector
     */
    public static double[] multiply(double scalar, double[] vector)
    {
        return DoubleStreamEx.of( vector ).map( d -> scalar * d ).toArray();
    }

    /**
     * Multiplying scalar on matrix
     */
    public static double[][] multiply(double scalar, double[][] matrix)
    {
        return StreamEx.of( matrix ).map( row -> multiply( scalar, row ) ).toArray( double[][]::new );
    }

    /**
     * Multiplying scalar on matrix
     */
    public static double[][] multiply(double[][] matrix1, double[][] matrix2)
    {
        int n = matrix1.length;
        int m = matrix2[0].length;

        int l = matrix1[0].length;

        double[][] result = new double[n][m];
        for( int i = 0; i < n; i++ )
        {
            for( int j = 0; j < m; j++ )
            {
                double val = 0;
                for( int k = 0; k < l; k++ )
                    val += matrix1[i][k] * matrix2[k][j];
                result[i][j] = val;
            }
        }
        return result;
    }
    
    public static double[][] multiply2m(double[][] matrix1, double[][] matrix2)
    {
        int m = matrix2[0].length;
        double[][] result = new double[2][m];
        for( int i = 0; i < m; i++ )
        {
            result[0][i] = matrix1[0][0] * matrix2[0][i] + matrix1[0][1] * matrix2[1][i];
            result[1][i] = matrix1[1][0] * matrix2[0][i] + matrix1[1][1] * matrix2[1][i];
        }
        return result;
    }
    
    public static double[] multiply(double[][] matrix, double[] vector)
    {
        int m = vector.length;

        int n = matrix.length;

        double[] result = new double[n];
        for( int i = 0; i < n; i++ )
        {
            double val = 0;
            for( int j = 0; j < m; j++ )
                val += matrix[i][j] * vector[j];
            result[i] = val;
        }
        return result;
    }

    public boolean equals(double[][] m1, double[][] m2)
    {
        int n = m1.length;
        int m = m1[0].length;
        if( m2.length != n || m2[0].length != m )
            return false;

        for( int i = 0; i < n; i++ )
        {
            for( int j = 0; j < m; j++ )
            {
                if( Math.abs(m1[i][j] - m2[i][j]) > 1E-15 )
                    return false;
            }
        }
        return true;
    }

    public static double[][] subMatrix(double[][] matrix, int iStart, int jStart, int n, int m)
    {
        double[][] result = new double[n][m];

        for( int i = 0; i < n; i++ )
        {
            for( int j = 0; j < m; j++ )
                result[i][j] = matrix[i + iStart][j + jStart];
        }
        return result;
    }

    public static double[] columnVector(double[][] matrix, int iStart, int jStart, int m)
    {
        double[] result = new double[m];

        for( int j = 0; j < m; j++ )
            result[j] = matrix[iStart + j][jStart];
        return result;
    }

    public static void copy(double[][] src, int iSrc, int jSrc, double[][] dst, int iDst, int jDst, int iLength, int jLength)
    {
        for( int i = 0; i < iLength; i++ )
        {
            for( int j = 0; j < jLength; j++ )
                dst[iDst + i][jDst + j] = src[i + iSrc][j + jSrc];
        }
    }

    public boolean equals(Matrix m1, Matrix m2)
    {
        return equals(m1.getArray(), m2.getArray());
    }


    public static double[][] invertLower(double[][] matrix) throws Exception
    {
        int n = matrix.length;
        double[][] result = new double[n][n];
        double sum;

        for( int j = 0; j < n; j++ )
        {
            result[j][j] = 1.0 / matrix[j][j];

            for( int i = j + 1; i < n; i++ )
            {
                sum = 0.0;
                for( int k = j; k < i; k++ )
                    sum -= matrix[i][k] * matrix[k][j];
                result[i][j] = sum / matrix[i][i];
            }
        }
        return result;
    }


    public static double[][] invertUpper(double matrix[][]) throws Exception
    {
        int n = matrix.length;
        double[][] result = new double[n][n];
        Util.copy(matrix, 0, 0, result, 0, 0, n, n);

        double sum;
        for( int j = n - 1; j > -1; j-- )
        {
            result[j][j] = 1.0 / result[j][j];
            for( int i = j - 1; i > -1; i-- )
            {
                sum = 0.0;
                for( int k = j; k > i; k-- )
                    sum -= result[i][k] * result[k][j];

                result[i][j] = sum / result[i][i];
            }
        }
        return result;
    }

    public static double[][] invertUpper2(double matrix[][]) throws Exception
    {
        int n = matrix.length;
        double[][] result = new double[n][n];

        for( int i = 0; i < n; i++ )
            result[i][i] = 1.0 / matrix[i][i];

        for( int offs = 1; offs < n; offs++ )
        {
            for( int i = 0; i < n - offs; i++ )
            {
                double val = 0;
                for( int k = i + 1; k < Math.min(i + offs + 1, n); k++ )
                    val -= matrix[i][k] * result[k][i + offs];
                result[i][i + offs] = val / matrix[i][i];
            }
        }
        return result;
    }

    public static double[][] transpose(double[][] matrix)
    {
        int n = matrix.length;
        int m = matrix[0].length;
        double[][] result = new double[m][n];
        for( int i = 0; i < n; i++ )
        {
            for( int j = 0; j < m; j++ )
                result[j][i] = matrix[i][j];
        }
        return result;
    }


    public static Matrix solveUpperTriangle(Matrix A, Matrix b)
    {
        int n = A.getColumnDimension();
        Matrix X = new Matrix(n, 1);
        for( int j = n - 1; j >= 0; j-- )
        {
            double s = b.get(j, 0);
            for( int i = j + 1; i < n; i++ )
                s -= A.get(j, i) * X.get(i, 0);
            X.set(j, 0, s / A.get(j, j));
        }
        return X;
    }

    public static Matrix solveBottomTriangle(Matrix A, Matrix b)
    {
        int n = A.getColumnDimension();
        Matrix X = new Matrix(n, 1);
        for( int j = 0; j < n; j++ )
        {
            double s = b.get(j, 0);
            for( int i = 0; i < j; i++ )
                s -= A.get(j, i) * X.get(i, 0);
            X.set(j, 0, s / A.get(j, j));
        }
        return X;
    }


    public static boolean isHeart(Node node)
    {
        return ( node.getKernel() != null && node.getKernel().getType().equals("heart") );
    }
    
    public static boolean isVessel(Edge edge)
    {
        return ( edge.getKernel() != null && "vessel".equals(edge.getKernel().getType()) );
    }
    
    public static Vessel getVessel(Edge edge)
    {
        DynamicProperty dp = edge.getAttributes().getProperty("vessel");
        return ( dp == null || ! ( dp.getValue() instanceof Vessel ) ) ? null : (Vessel)dp.getValue();
    }
    
    public static SimpleVessel getSimpleVessel(Edge edge, ArterialBinaryTreeModel atm, EModel emodel)
    {
        double betaFactor = atm.factorBeta;
        double areaFactor = atm.factorArea;
        String betaFactorVarName = edge.getAttributes().getValueAsString("Beta factor");
        if (betaFactorVarName != null)
        {
            Variable betaFactorVar = emodel.getVariable(betaFactorVarName);
            if (betaFactorVar != null)
                betaFactor = betaFactorVar.getInitialValue();
        }
        String areaFactorVarName = edge.getAttributes().getValueAsString("Area factor");
        if (areaFactorVarName != null)
        {
            Variable areaFactorVar = emodel.getVariable(areaFactorVarName);
            if (areaFactorVar != null)
               areaFactor = areaFactorVar.getInitialValue();
        }
        
        DynamicProperty dp = edge.getAttributes().getProperty( "vessel" );
        if (dp == null || !(dp.getValue() instanceof Vessel))
            return null;
        Vessel v = (Vessel)dp.getValue();
        double pressure = v.referencedPressure == -1? atm.referencedPressure: v.referencedPressure;
        
        double newUnweightedArea = areaFactor* Util.getUnweightArea(v.unweightedArea, pressure, v.getBeta()*betaFactor);
        double newUnweightedArea1 = areaFactor* Util.getUnweightArea(v.unweightedArea1, pressure, v.getBeta()*betaFactor);
        SimpleVessel simpleVessel = new SimpleVessel( v.getName(), v.title, v.length * atm.factorLength, newUnweightedArea, newUnweightedArea1, v.getBeta() * betaFactor );
        simpleVessel.area = new double[(int)atm.vesselSegments + 1];
        for( int i = 0; i < simpleVessel.area.length; i++ )
            simpleVessel.area[i] = v.unweightedArea;
        simpleVessel.index = v.index;
        return simpleVessel;
    }



    public static double generateInputPressure(double time)
    {
        double sh = time % 0.86;//Math.IEEEremainder( time, 0.86 );//time - 0.24 * ( (int) ( time / 0.24 ) );
        if( sh < 0.46 )//Math.abs(sh - 0.22) >= 0.02 )
        {
            return 80;//* ( 1 - Math.exp( - ( time ) * ( time ) / 10) );
        }
        else
        {
            return 80 + 40 * Math.sin(Math.PI * ( sh - 0.46 ) / 0.4);//Math.exp(1 - 0.04 / ( 0.04 - Math.pow( 0.86 - sh, 2) ));
            //* ( 1 - Math.exp( - ( time ) * ( time ) / 10) );
        }

    }
    
    public static boolean isEvenNode(Vessel v)
    {
        return v.depth % 2 == 0;
    }
    
    public static boolean isEvenNode(SimpleVessel v)
    {
        return v.depth % 2 == 0;
    }
    
    public static boolean isTerminal(SimpleVessel v)
    {
        return v.left == null && v.right == null;
    }
    
    public static boolean isTerminal(Vessel v)
    {
        return v.left == null && v.right == null;
    }
    
    
    public static Edge findRootVessel(Diagram diagram) throws Exception
    {
        Node[] nodes = diagram.getNodes();

        Node heartNode = null;
        for( Node node : nodes )
        {
            if( isHeart( node ) )
            {
                heartNode = node;
                break;
            }
        }

        if( heartNode == null )
            throw new Exception( "Can not find heart node in the diagram " + diagram.getName() + " Model can not be created." );

        Edge rootEdge = null;

        for( Edge edge : heartNode.getEdges() )
        {
            if( isVessel( edge ) )
            {
                if( rootEdge == null )
                    rootEdge = edge;
                else
                    throw new Exception( "Heart node in the diagram " + diagram.getName()
                            + " has more than one vessels. Incorrect arterial tree structure." );
            }
        }
        return rootEdge;
    }
    
    public static void numerateVessels(Diagram diagram) throws Exception
    {
        Edge rootEdge =  findRootVessel(diagram);
        int end = initVessel(rootEdge, 0);
        
        diagram.stream(Edge.class).filter(e->Util.isVessel(e)).forEach(e->Util.getVessel(e).setParent(e));
//        System.out.println(end);
    }
    
    private static int initVessel(Edge rootEdge, int index) throws Exception
    {
        List<Edge> branches = getBranches( rootEdge );
        
        if( branches.size() == 0 )
            return index;

        Edge leftEdge = branches.get( 0 );
        Vessel left = getVessel( leftEdge );
        Vessel parent = getVessel( rootEdge );
        parent.left = left;
        left.index = ++index;
        left.depth = parent.depth + 1;

        Edge rightEdge = null;
        if( branches.size() > 1 )
        {
            rightEdge = branches.get( 1 );
            Vessel right = getVessel( rightEdge );
            parent.right = right;
            right.index = ++index;
            right.depth = parent.depth+1;
        }
        
        index = initVessel(leftEdge, index);
        
        if( branches.size() > 1 )
        index = initVessel(rightEdge, index);
        
        return index;
    }
    
    public static List<Edge> getBranches(Edge edge) throws Exception
    {
        List<Edge> result = new ArrayList<>();

        Node output = edge.getOutput();
        for( Edge e : output.getEdges() )
        {
            if( !e.equals( edge ) && Util.isVessel( e ) )
                result.add( e );
        }

        if( result.size() > 2 )
        {
            throw new Exception( "Wrong model: edge" + edge.getName() + " have " + result.size()
                    + " branches. Two or zero branches expected." );
        }
        return result;
    }
    
    
    public static void print(Matrix matrix, File f) throws Exception
    {
        try(BufferedWriter bw = ApplicationUtils.utfWriter( f ))
        {
            for (int i = 0; i<matrix.getColumnDimension();  i++)
            {
                for (int j = 0; j< matrix.getRowDimension(); j++)
                {
                    bw.write( String.valueOf( matrix.get( j, i )) );
                    bw.write( "\t" );
                }
                bw.write( "\n" );
            }
        }
    }
    
    public static void writeParameters(File f, ArterialBinaryTreeModel atm) throws Exception
    {
//        HemodynamicsEModel emodel = (HemodynamicsEModel)diagram.getRole();
        try(BufferedWriter bw = ApplicationUtils.utfWriter( f ))
        {
            for (SimpleVessel vessel: atm.vessels)
            {
                bw.write( "Vessel\t"+vessel.name );
                bw.write( "\nPressure" );
                for( double element : vessel.pressure )
                    bw.write( "\t" + element);
//                    bw.write( "\t" + vessel.pressure[vessel.pressure.length-1] +"\t"+vessel.pressure[0]);
                
                bw.write( "\nFlow" );
                for( double element : vessel.flow )
                    bw.write( "\t" + element);
//                    bw.write( "\t" + vessel.flow[0] );
                
                bw.write( "\nArea" );
                for( double element : vessel.area )
                    bw.write( "\t" + element);
//                    bw.write( "\t" + vessel.area[i] );
//                bw.write( "\t" + vessel.area[vessel.area.length-1] +"\t"+vessel.area[0]);
                bw.write( "\n" );
            }
          
//            for (Variable var: emodel.getVariables())
//            {
//                bw.write( var.getName()+"/t"+var.getInitialValue()+"/n" );
//            }
        }
    }
    
    public static void readParameters(File f, ArterialBinaryTreeModel atm, int size) throws Exception
    {
        try(BufferedReader br = ApplicationUtils.utfReader( f ))
        {
            String line = br.readLine() ;
            SimpleVessel currentVessel = null;
            while (line != null)
            {
                String[] lineStr = line.split( "\t" );
                switch( lineStr[0] )
                {
                    case "Vessel":
                        currentVessel = atm.vesselMap.get( lineStr[1] );
                        break;
                    case "Pressure":
                        currentVessel.pressure = fieldsToDoubles( lineStr, size );
                        break;
                    case "Area":
                        currentVessel.area = fieldsToDoubles( lineStr, size );
                        break;
                    case "Flow":
                        currentVessel.flow = fieldsToDoubles( lineStr, size );
                        break;
                }
                line = br.readLine();
            }
        }
    }

    private static double[] fieldsToDoubles(String[] lineStr, int size)
    {
        return StreamEx.of( lineStr ).skip( 1 ).limit( size ).mapToDouble( Double::parseDouble ).toArray();
    }
    
    private static void interpolate(double[] result, double x1, double x2)
    {
        Arrays.setAll( result, i -> x1 + i*(x2-x1)/6 );
    }
    private static double DP = 1333.223684;
    public static double getUnweightArea(double area, double pressure, double beta)
    {
        double val = pressure / beta * DP;
        double sqrtArea = ( -1 + Math.sqrt( 1 + 4 * val * Math.sqrt( area ) ) ) / ( 2 * val );
        return pressure == 0 ? area : sqrtArea * sqrtArea;
    }
    
    public static double calculateResistance(SimpleVessel vessel, int segment, double viscosity)
    {
        double area = vessel.area[segment];
        return 8*Math.PI*4.5*vessel.length/(area*area)/1000/DP*10;
    }
    
    public static double calculateArteriolarResistance(SimpleVessel vessel, double viscosity, double minArea, double capillaryResistance)
    {
        int length = vessel.area.length;
        double endArea = vessel.area[length - 1];
        double factor = Math.log(endArea/minArea)/Math.log(2);
        double n = Math.max(factor,0);
        double resistance = calculateResistance(vessel, vessel.area.length-1, viscosity);
        return Math.max(0, n*resistance + Math.pow(2, -n)* capillaryResistance);
    }
    
    public static double calculateMinArea(ArterialBinaryTreeModel model)
    {
        double result = Double.POSITIVE_INFINITY;
        for (SimpleVessel vessel: model.vesselMap.values())
        {
            double area = vessel.getArea()[(int)model.vesselSegments];
            
            if (area < result)
                result = area;
        }
        return result;
    }
    
    public static double[] calcDerivative(double[] pressure, double step)
    {
        int n = pressure.length;
        double[] result = new double[n];
        
        for (int i=1; i<n - 1; i++)
            result[i] = (pressure[i+1] - pressure[i-1])/(2*step);
        
        result[0] = (pressure[1] - pressure[0])/step;
        result[n-1] = (pressure[n-1] - pressure[n-2])/step;
        return result;
    }
    
    public static Vessel getVessel(String title, Diagram diagram)
    {
        return diagram.stream(Edge.class).filter(e -> Util.isVessel(e)).map(e -> Util.getVessel(e)).filter(v -> v.title.equals(title))
                .findAny().orElse(null);
    }
    
    public static StreamEx<Vessel> getVessels(Diagram diagram)
    {
        return diagram.stream(Edge.class).filter(e -> Util.isVessel(e)).map(e->Util.getVessel(e));
    }
    
    public static double calcReflection(Vessel vessel, double pressure)
    {
        if( vessel.left != null && vessel.right != null )
            return calcReflection(getArea(vessel, pressure), vessel.getBeta(), getArea(vessel.left, pressure), vessel.left.getBeta(),
                    getArea(vessel.right, pressure), vessel.right.getBeta());
        else if( vessel.left != null )
            return calcReflection(getArea(vessel, pressure), vessel.getBeta(), getArea(vessel.left, pressure), vessel.left.getBeta());
        else if( vessel.right != null )
            return calcReflection(getArea(vessel, pressure), vessel.getBeta(), getArea(vessel.right, pressure), vessel.right.getBeta());

        return 0;
    }
    
    public static double calcReflection(double a, double beta, double a1, double beta1, double a2, double beta2)
    {
        double y = calcAdmittance(a, beta);
        double leftY = calcAdmittance(a1, beta1);
        double rightY = calcAdmittance(a2, beta2);
        return ( y - leftY - rightY ) / ( y + leftY + rightY );
    }
    
    public static double calcReflection(double a, double beta, double a1, double beta1)
    {
        double y = calcAdmittance(a, beta);
        double y1 = calcAdmittance(a1, beta1);
        return ( y - y1 ) / ( y + y1 );
    }

    public static double calcAdmittance(double a, double beta)
    {
        return a / calcPWV(a, beta);
    }
    
    /**
     * calculates vessel admittance under given pressure
     * @return
     */
    public static double calcAdmittance(Vessel vessel, double pressure)
    {
        return calcAdmittance(Util.getArea(vessel, pressure), vessel.getBeta());
    }
    
    public static double calcPWV(double a, double beta)
    {
        return Math.sqrt(beta / (2 * Math.sqrt(a)));
    }
    
    /**
     * Calculates area which should be set as corresponding to referencedPressure in order to gain actualArea under actualPressure
     */
    public static double getArea(double actualArea, double actualPressure, double referencedPressure, double beta)
    {
        double area0 = getUnweightArea(actualArea, actualPressure, beta);
        return Math.pow(Math.sqrt(area0) + referencedPressure * DP / beta * area0, 2);
    }  
    
    /**
     * Calculates area which will be gained in given vessel under given pressure during simulation
     * @return
     */
    public static double getArea(Vessel vessel, double pressure)
    {
       return getArea(vessel.unweightedArea, getReferencedPressure(vessel), pressure, getBetaFactor(vessel)*vessel.getBeta());
    }
    
    public static Node getHeart(Diagram diagram)
    {
        return diagram.stream(Node.class).filter(n->isHeart(n)).findAny().orElse(null);
    }
    
    public static Vessel getRoot(Diagram diagram)
    {
        Node heart = getHeart(diagram); 
        Edge rootEdge = heart == null? null: heart.edges().filter(e->isVessel(e)).findAny().orElse(null);
        return rootEdge == null? null: getVessel(rootEdge);
    }
    
    public static double getAreaFactor(Vessel vessel)
    {
        Edge edge = (Edge)vessel.getParent();
        Diagram d = Diagram.getDiagram(edge);
        EModel emodel = d.getRole(EModel.class);
        String areaFactorVarName = edge.getAttributes().getValueAsString("Area factor");
        if( areaFactorVarName != null )
        {
            Variable areaFactorVar = emodel.getVariable(areaFactorVarName);
            if( areaFactorVar != null )
                return areaFactorVar.getInitialValue();
        }
        return 1;
    }

    public static double getBetaFactor(Vessel vessel)
    {
        Edge edge = (Edge)vessel.getParent();
        Diagram d = Diagram.getDiagram(edge);
        EModel emodel = d.getRole(EModel.class);
        String betaFactorVarName = edge.getAttributes().getValueAsString("Beta factor");
        if( betaFactorVarName != null )
        {
            Variable betaFactorVar = emodel.getVariable(betaFactorVarName);
            if( betaFactorVar != null )
                return betaFactorVar.getInitialValue();
        }
        return 1;
    }
    
    public static double getReferencedPressure(Vessel vessel)
    {
        double pressure = vessel.referencedPressure;
        if (pressure >= 0)
            return pressure;

        Edge edge = (Edge)vessel.getParent();
        Diagram d = Diagram.getDiagram(edge);
        EModel emodel = d.getRole(EModel.class);
        
        return emodel.getVariable("referencedPressure").getInitialValue();
    }
    
    public static double calcNoReflectionResistance(SimpleVessel v, double venousPressure)
    {
        int m = v.getArea().length - 1;
        double a = v.area[m];
        double a0 = v.area0[m];
        double p = v.pressure[m];
        double beta = v.beta; // DP; should be in g/s^2
        double pDiff = (p-venousPressure);
        return pDiff / ( 2 * a * Math.sqrt(2 * beta / a0) * ( Math.pow(a, 0.25) - Math.pow(a0, 0.25) ));
    }
}