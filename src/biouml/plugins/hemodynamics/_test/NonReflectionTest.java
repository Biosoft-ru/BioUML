package biouml.plugins.hemodynamics._test;

import java.util.HashMap;

import biouml.model.Diagram;
import biouml.plugins.hemodynamics.Util;
import biouml.plugins.hemodynamics.Vessel;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElementPath;

/**
 * This class recalculates parameters of arterial tree model in order to adjust reflection wave on vessel branching points to desired value (usually - to remove reflctions)
 */
public class NonReflectionTest
{
    private static final String REPOSITORY_PATH = "../data";

    private static final DataElementPath DIAGRAMS_PATH = DataElementPath.create("databases/Virtual Human/Diagrams/");

    private static final String DIAGRAM_NAME = "Arterial Tree new WP";
    private static final String RESULT_DIAGRAM_NAME = "Arterial Tree new WP Matched";

    private static double pressure = 70;
    private static double minArea = 0.3; //in percents from actual
    private static double maxArea = 2; //percents
    private static double stepArea = 0.05; //percents
    private static double minBeta = 0.3; //percents
    private static double maxBeta = 2; //percents
    private static double stepBeta = 0.05; //percents

    private static String startVessel = null;//"Abdominal V";

    private static HashMap<String, Double> reflections = new HashMap<String, Double>()
    {
        {
//            put("Abdominal V", 100.0);
        }
    };

    private static HashMap<String, double[]> boundaries = new HashMap<String, double[]>()
    {
        {
//            put("Abdominal V", new double[] {1, 1, 1, 1});
//            put("R. common Iliac", new double[] {0.2, 3, 0.2, 3});
//            put("L. common Iliac", new double[] {0.2, 3, 0.2, 3});
//            put("L. external Iliac", new double[] {0.2, 3, 0.2, 3});
//            put("R. external Iliac", new double[] {0.2, 3, 0.2, 3});
//            put("L. internal Iliac", new double[] {0.2, 3, 0.2, 3});
//            put("R. internal Iliac", new double[] {0.2, 3, 0.2, 3});
//            put("L. common Iliac", new double[] {0.2, 3, 0.2, 3});
        }
    };

    public static Diagram getDiagram(String name) throws Exception
    {
        CollectionFactory.createRepository(REPOSITORY_PATH);
        Diagram diagram = (Diagram)DIAGRAMS_PATH.getDataCollection().get(name);
        Util.numerateVessels(diagram);
        return diagram;
    }

    public static void main(String ... args) throws Exception
    {
        Diagram diagram = getDiagram(DIAGRAM_NAME);
        Diagram result = diagram.clone(diagram.getOrigin(), RESULT_DIAGRAM_NAME);
        Util.numerateVessels(result);
        match(result);
//        result.save();

//        CollectionFactory.unregisterAllRoot();
//
//        calcReflections(getDiagram(RESULT_DIAGRAM_NAME), getDiagram(DIAGRAM_NAME));
    }

    public static void match(Diagram diagram)
    {
        Vessel rootVessel = startVessel == null ? Util.getRoot(diagram) : Util.getVessel(startVessel, diagram);
        match(rootVessel);
    }

    public static void match(Vessel vessel)
    {
        if( vessel.left != null && vessel.right != null )
        {
            adjustParameters(vessel, vessel.left, vessel.right);
            match(vessel.left);
            match(vessel.right);
        }
        else if( vessel.left != null )
        {
            adjustParameters(vessel, vessel.left);
            match(vessel.left);
        }
        else if( vessel.right != null )
        {
            adjustParameters(vessel, vessel.right);
            match(vessel.right);
        }
    }

    public static void adjustParameters(Vessel vessel, Vessel childVessel1, Vessel childVessel2)
    {
        double a = Util.getArea(vessel, pressure);
        double a1 = Util.getArea(childVessel1, pressure);
        double a2 = Util.getArea(childVessel2, pressure);
        double b1 = childVessel1.getBeta();
        double b2 = childVessel2.getBeta();
        double reflection = Util.calcReflection(a, vessel.getBeta(), a1, b1, a2, b2);

        double desiredReflection = reflections.containsKey(vessel.getTitle()) ? reflections.get(vessel.getTitle()) : 0;

        double resultReflection = reflection;
        double resultA1 = a1;
        double resultA2 = a2;
        double resultB1 = b1;
        double resultB2 = b2;

        double minA1 = a1 * minArea;
        double minA2 = a2 * minArea;
        double minB1 = b1 * minBeta;
        double minB2 = b2 * minBeta;

        double maxA1 = a1 * maxArea;
        double maxA2 = a2 * maxArea;
        double maxB1 = b1 * maxBeta;
        double maxB2 = b2 * maxBeta;

        if( boundaries.containsKey(childVessel1.getTitle()) )
        {
            double[] bounds = boundaries.get(childVessel1.getTitle());
            minA1 = a1*bounds[0]; maxA1 = a1*bounds[1];
            minB1 = b1*bounds[2]; maxB1 = b1*bounds[3];
        }

        if( boundaries.containsKey(childVessel2.getTitle()) )
        {
            double[] bounds = boundaries.get(childVessel2.getTitle());
            minA2 = a2*bounds[0]; maxA2 = a2*bounds[1];
            minB2 = b2*bounds[2]; maxB2 = b2*bounds[3];
        }

        for( double newA1 = minA1; newA1 <= maxA1; newA1 += a1 * stepArea )
        {
            for( double newA2 = minA2; newA2 <= maxA2; newA2 += a2 * stepArea )
            {
                for( double newB1 = minB1; newB1 <= maxB1; newB1 += b1 * stepBeta )
                {
                    for( double newB2 = minB2; newB2 <= maxB2; newB2 += b2 * stepBeta )
                    {

                        double newReflection = Util.calcReflection(a, vessel.getBeta(), newA1, newB1, newA2, newB2);
                        if( Math.abs(newReflection - desiredReflection) < Math.abs(resultReflection - desiredReflection) )
                        {
                            resultReflection = newReflection;
                            resultA1 = newA1;
                            resultA2 = newA2;
                            resultB1 = newB1;
                            resultB2 = newB2;
                        }
                    }
                }
            }
        }

        double referencedArea1 = Util.getArea(resultA1, pressure, Util.getReferencedPressure(childVessel1), resultB1);
        childVessel1.setInitialArea(referencedArea1);
        childVessel1.setInitialArea1(referencedArea1);

        double referencedArea2 = Util.getArea(resultA2, pressure, Util.getReferencedPressure(childVessel2), resultB2);
        childVessel2.setInitialArea(referencedArea2);
        childVessel2.setInitialArea1(referencedArea2);

        childVessel1.setBeta(resultB1);
        childVessel2.setBeta(resultB2);

        System.out.println(childVessel1.getTitle() + "\t" + resultA1 + " ( " + a1 + " ) " + "\t" + resultB1 + " ( " + b1 + " ) " + "\t"
                + resultReflection + " ( " + reflection + " ) ");
        System.out.println(childVessel2.getTitle() + "\t" + resultA2 + " ( " + a2 + " ) " + "\t" + resultB2 + " ( " + b2 + " ) " + "\t"
                + resultReflection + " ( " + reflection + " ) ");
    }

    public static void adjustParameters(Vessel vessel, Vessel childVessel)
    {
        double a = Util.getArea(vessel, pressure);
        double a1 = Util.getArea(childVessel, pressure);
        double b1 = childVessel.getBeta();
        double reflection = Util.calcReflection(a, vessel.getBeta(), a1, childVessel.getBeta());
        double resultA1 = a1;
        double resultB1 = b1;
        double resultReflection = reflection;
        double desiredReflection = reflections.containsKey(vessel.getTitle()) ? reflections.get(vessel.getTitle()) : 0;

        double minA1 = a1 * minArea;
        double minB1 = b1 * minBeta;

        double maxA1 = a1 * maxArea;
        double maxB1 = b1 * maxBeta;

        if( boundaries.containsKey(childVessel.getTitle()) )
        {
            double[] bounds = boundaries.get(childVessel.getTitle());
            minA1 = bounds[0]; maxA1 = bounds[1];
            minB1 = bounds[2]; maxB1 = bounds[3];
        }

        for( double newA1 = minA1; newA1 <= maxA1; newA1 += a1 * stepArea )
        {
            for( double newB1 = minB1; newB1 <= maxB1; newB1 += b1 * stepBeta )
            {
                double newReflection = Util.calcReflection(a, vessel.getBeta(), newA1, newB1);

                if( Math.abs(newReflection - desiredReflection) < Math.abs(resultReflection - desiredReflection) )
                {
                    resultA1 = newA1;
                    resultB1 = newB1;
                    resultReflection = newReflection;
                }
            }
        }

        double referencedArea = Util.getArea(resultA1, pressure, Util.getReferencedPressure(childVessel), resultB1);
        childVessel.setInitialArea(referencedArea);
        childVessel.setInitialArea1(referencedArea);

        childVessel.setBeta(resultB1);

        System.out.println(childVessel.getTitle() + "\t" + resultA1 + " ( " + a1 + " ) " + "\t" + resultB1 + " ( " + b1 + " ) " + "\t"
                + resultReflection + " ( " + reflection + " ) ");
    }


    public static void calcReflections(Diagram diagram, Diagram diagram2) throws Exception
    {
        System.out.println("REFLECTIONS AND ADMITTANCES");

        for( Vessel vessel : Util.getVessels(diagram).filter(v -> !Util.isTerminal(v)) )
        {
            double reflection = Util.calcReflection(vessel, pressure);

            if( vessel.left != null && vessel.right != null )
                System.out.println(vessel.getTitle() + "\t" + reflection + "\t" + Util.calcAdmittance(vessel, pressure) + "\t"
                        + Util.calcAdmittance(vessel.left, pressure) + "\t" + Util.calcAdmittance(vessel.right, pressure));
            else if( vessel.left != null )
                System.out.println(vessel.getTitle() + "\t" + reflection + "\t" + Util.calcAdmittance(vessel, pressure) + "\t"
                        + Util.calcAdmittance(vessel.left, pressure));
        }

        System.out.println("");
        System.out.println("REFLECTIONS AND ADMITTANCES OLD");
        for( Vessel vessel : Util.getVessels(diagram2).filter(v -> !Util.isTerminal(v)) )
        {
            double reflection = Util.calcReflection(vessel, pressure);

            if( vessel.left != null && vessel.right != null )
                System.out.println(vessel.getTitle() + "\t" + reflection + "\t" + Util.calcAdmittance(vessel, pressure) + "\t"
                        + Util.calcAdmittance(vessel.left, pressure) + "\t" + Util.calcAdmittance(vessel.right, pressure));
            else if( vessel.left != null )
                System.out.println(vessel.getTitle() + "\t" + reflection + "\t" + Util.calcAdmittance(vessel, pressure) + "\t"
                        + Util.calcAdmittance(vessel.left, pressure));
        }

        System.out.println("");
        System.out.println("COMPARISON");
        double sum = 0;
        double sum2 = 0;
        for( Vessel vessel : Util.getVessels(diagram).filter(v -> !Util.isTerminal(v)) )
        {
            Vessel vessel2 = Util.getVessel(vessel.getTitle(), diagram2);
            double reflection = Util.calcReflection(vessel, pressure);
            double reflection2 = Util.calcReflection(vessel2, pressure);
            sum += reflection;
            sum2 += reflection2;
            if( reflection != reflection2 )
                System.out.println(vessel.getTitle() + "\t" + reflection + "\t" + reflection2);
        }
        System.out.println("SUMMARY: \t" + sum + "\t" + sum2);

        System.out.println("");
        for( Vessel vessel : Util.getVessels(diagram) )
        {
            Vessel vessel2 = Util.getVessel(vessel.getTitle(), diagram2);
            System.out.println(vessel.getTitle() + "\t" + vessel.unweightedArea / vessel2.unweightedArea1 + "\t"
                    + vessel.getBeta() / vessel2.getBeta());
        }
    }
}