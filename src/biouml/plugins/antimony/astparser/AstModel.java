/* Generated By:JJTree: Do not edit this line. AstModel.java Version 7.0 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=false,TRACK_TOKENS=true,NODE_PREFIX=Ast,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package biouml.plugins.antimony.astparser;

import java.util.ArrayList;

public class AstModel extends SimpleNode
{
    public final static String OUTSIDE_MODEL_NAME = "composit model";

    public AstModel(int id)
    {
        super(id);
    }

    public AstModel(AntimonyParser p, int id)
    {
        super(p, id);
    }


    public AstSymbol getNameSymbol()
    {
        if( isOutsideModel() )
            return null;
        for( int i = 0; i < this.jjtGetNumChildren(); i++ )
        {
            if( this.jjtGetChild(i) instanceof AstSymbol )
                return (AstSymbol)this.jjtGetChild(i);
        }
        return null;
    }

    public ArrayList<AstSymbol> getParameters()
    {
        ArrayList<AstSymbol> parameters = new ArrayList<AstSymbol>();

        if( isOutsideModel() )
            return parameters;

        AstSymbol symbol = getNameSymbol();
        if( symbol == null )
            return null;
        for( int j = 0; j < symbol.jjtGetNumChildren(); j++ )
            if( symbol.jjtGetChild(j) instanceof AstSymbol )
                parameters.add((AstSymbol)symbol.jjtGetChild(j));
        return parameters;
    }

    public void setModelName(String modelName)
    {
        if( isOutsideModel() )
            return;
        modelName = modelName.replaceAll(" ", "_");
        AstSymbol name = getNameSymbol();
        if( name != null )
            name.setName(modelName);
    }

    public String toString()
    {
        if( isOutsideModel() )
            return "";
        return "model";
    }

    public void addParameter(AstSymbol symbol)
    {
        if( isOutsideModel() )
            return;
        AstSymbol name = getNameSymbol();
        for( int i = name.jjtGetNumChildren() - 1; i >= 0; i-- )
        {
            if( name.jjtGetChild(i) instanceof AstRegularFormulaElement
                    && ( (AstRegularFormulaElement)name.jjtGetChild(i) ).toString().equals(")") )
            {
                int index = i;
                name.addWithDisplacement(symbol, index);
                if( name.jjtGetChild(index - 1) instanceof AstSymbol )
                {
                    name.addWithDisplacement(new AstComma(AntimonyParser.JJTCOMMA), index);
                }
                return;
            }
        }
        name.children = null;
        AstRegularFormulaElement lb = new AstRegularFormulaElement(AntimonyParser.JJTREGULARFORMULAELEMENT);
        lb.setElement("(");
        AstRegularFormulaElement rb = new AstRegularFormulaElement(AntimonyParser.JJTREGULARFORMULAELEMENT);
        rb.setElement(")");
        name.addAsLast(lb);
        name.addAsLast(symbol);
        name.addAsLast(rb);
    }

    public enum ModelType
    {
        SimpleModel, MainModel, MainOutsideModel;
    }
    private ModelType typeModel = ModelType.SimpleModel;
    public ModelType getModelType()
    {
        return typeModel;
    }
    public void setTypeModel(ModelType type)
    {
        typeModel = type;
    }

    public boolean isOutsideModel()
    {
        return typeModel.equals(ModelType.MainOutsideModel);
    }

    public boolean isMainModel()
    {
        return typeModel.equals(ModelType.MainModel);
    }

    public boolean isSimpleModel()
    {
        return typeModel.equals(ModelType.SimpleModel);
    }

    public ArrayList<AstFunction> getAstFunctions()
    {
        ArrayList<AstFunction> functions = new ArrayList<AstFunction>();
        if( !isOutsideModel() )
            return functions;
        for( int i = 0; i < this.jjtGetNumChildren(); i++ )
        {
            if( this.jjtGetChild(i) instanceof AstFunction )
                functions.add((AstFunction)this.jjtGetChild(i));
        }
        return functions;
    }

    public ArrayList<AstUnit> getAstUnits()
    {
        ArrayList<AstUnit> units = new ArrayList<AstUnit>();
        for( int i = 0; i < this.jjtGetNumChildren(); i++ )
        {
            if( this.jjtGetChild(i) instanceof AstUnit )
                units.add((AstUnit)this.jjtGetChild(i));
        }
        return units;
    }
}
/* JavaCC - OriginalChecksum=2f2be3c85bd9c43550c84d2bc93be46a (do not edit this line) */
