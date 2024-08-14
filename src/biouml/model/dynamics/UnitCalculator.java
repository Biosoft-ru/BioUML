package biouml.model.dynamics;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import biouml.standard.type.BaseUnit;
import ru.biosoft.math.unitparser.AstConstant;
import ru.biosoft.math.unitparser.AstFunNode;
import ru.biosoft.math.unitparser.AstStart;
import ru.biosoft.math.unitparser.AstType;
import ru.biosoft.math.unitparser.AstUnitNode;
import ru.biosoft.math.unitparser.Node;
import ru.biosoft.math.unitparser.UnitParser;

public class UnitCalculator {
	
    public static AstStart readMath(String math) throws Exception
    {
        UnitParser parser = new UnitParser();
        return parser.parse(math);
    }
    
    public static BaseUnit[] getBaseUnits(AstStart astStart) throws Exception 
    {
        List<BaseUnit> buList = new ArrayList<>();
        getBaseUnits(astStart.jjtGetChild(0), 1, buList);
        return buList.toArray(new BaseUnit[0]);
    }
    
    private static void getBaseUnits(Node node, int power, List<BaseUnit> buList) throws Exception
    {
        if (node instanceof AstUnitNode) {
        	double[] mulNScale = getMulScale(node.jjtGetChild(0), 1, 0, 1);// calculate constant
            if (!(node.jjtGetChild(0) instanceof AstConstant))
                postprocessMulScale(mulNScale);
            Node type = node.jjtGetChild(1);
            if (type instanceof AstType)
                buList.add(new BaseUnit(((AstType) type).getValue(), mulNScale[0], (int) mulNScale[1], power));
            else if (type instanceof AstFunNode)
            {
            	LinkedHashMap<String, Integer> typeMap = getBasesNPowers(type, power);
                boolean separateFirstKey = mulNScale[0] != 1 || mulNScale[1] != 0;
                for (Map.Entry<String, Integer> entry: typeMap.entrySet()) {
                	if (separateFirstKey && entry.getValue() != 1) {
                        if (entry.getValue() > 0) {
                            buList.add(new BaseUnit(entry.getKey(), mulNScale[0], (int) mulNScale[1], 1));
                            buList.add(new BaseUnit(entry.getKey(), 1, 0, entry.getValue() - 1));
                        }
                        else {
                            buList.add(new BaseUnit(entry.getKey(), mulNScale[0], (int) mulNScale[1], -1));
                            buList.add(new BaseUnit(entry.getKey(), 1, 0, entry.getValue() + 1));
                        }
                        separateFirstKey = false;
                    }
                    else
                        buList.add(new BaseUnit(entry.getKey(), 1, 0, entry.getValue()));
                }
            }

        }

        else if (node instanceof AstFunNode) {
            String operation = ((AstFunNode)node).getOperator();
            switch (operation) {
                case "*":
                    getBaseUnits(node.jjtGetChild(0), power, buList);
                    getBaseUnits(node.jjtGetChild(1), power, buList);
                    break;
                case "/":
                    getBaseUnits(node.jjtGetChild(0), power, buList);
                    getBaseUnits(node.jjtGetChild(1), -power, buList);
                    break;
                case "^":
                    Node powerNode = node.jjtGetChild(1);
                    power *= ((AstConstant) powerNode).getValue().intValue();
                    getBaseUnits(node.jjtGetChild(0), power, buList);
                    break;
            }
        }
    }

    private static LinkedHashMap<String, Integer> getBasesNPowers(Node node, int power) {
        LinkedHashMap<String, Integer> typeMap = new LinkedHashMap<>();
        if (node instanceof AstType) {
            String key_name = ((AstType)node).getValue();
            typeMap.put(key_name, power);
        }
        else if (node instanceof AstFunNode) {
            String operation = ((AstFunNode)node).getOperator();
            switch (operation) {
                case "*":
                    typeMap.putAll(getBasesNPowers(node.jjtGetChild(0), power));
                    typeMap.putAll(getBasesNPowers(node.jjtGetChild(1), power));
                    break;
                case "/":
                    typeMap.putAll(getBasesNPowers(node.jjtGetChild(0), power));
                    typeMap.putAll(getBasesNPowers(node.jjtGetChild(1), -power));
                    break;
                case "^":
                    typeMap.putAll(getBasesNPowers(node.jjtGetChild(0),
                            power * ((AstConstant) node.jjtGetChild(1)).getValue().intValue()));
                    break;
            }
        }
        return typeMap;
    }

    private static void postprocessMulScale(double[] mulNScale) {
        while (mulNScale[0] > 10) {
            mulNScale[0] /= 10;
            mulNScale[1] += 1;
        }
        while (mulNScale[0] < 1) {
            mulNScale[0] *= 10;
            mulNScale[1] -= 1;
        }
        mulNScale[0] = Math.round(mulNScale[0]*1E10) / 1E10;
    }

    private static double[] getMulScale(Node node, double multiplier, int scale, int power) {
        double[] mulNScale = new double[]{multiplier, scale};
        if (node instanceof AstFunNode) {
            String operation = ((AstFunNode)node).getOperator();
            switch (operation) {
                case "*":
                    mulNScale = getMulScale(node.jjtGetChild(0), mulNScale[0], (int) mulNScale[1], power);
                    mulNScale = getMulScale(node.jjtGetChild(1), mulNScale[0], (int) mulNScale[1], power);
                    break;
                case "/":
                    mulNScale = getMulScale(node.jjtGetChild(0), mulNScale[0], (int) mulNScale[1], power);
                    mulNScale = getMulScale(node.jjtGetChild(1), mulNScale[0], (int) mulNScale[1], -power);
                    break;
                case "^":
                    power *= ((AstConstant) node.jjtGetChild(1)).getValue().intValue();
                    mulNScale = getMulScale(node.jjtGetChild(0), mulNScale[0], (int) mulNScale[1], power);
                    break;
            }
        }
        else if (node instanceof AstConstant)
        {
            double value = ((AstConstant)node).getValue().doubleValue();
            if (value == 10)
                mulNScale[1] += 1;
            else
                mulNScale[0] *= Math.pow(value, power);

        }
        return mulNScale;
    }

}
