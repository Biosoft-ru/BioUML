package biouml.plugins.fbc;

import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBModel;

import java.util.logging.Level;
import java.util.logging.Logger;

public class GurobiModel implements FbcModel
{
    Logger log = Logger.getLogger(GurobiModel.class.getName());

    private GRBModel model;
    private String[] reactionNames;

    public GurobiModel(GRBModel model, String[] reactionNames)
    {
        this.model = model;
        this.reactionNames = reactionNames;
    }

    @Override
    public void optimize()
    {
        try
        {
            model.optimize();
        }
        catch( GRBException e )
        {
            log.log(Level.SEVERE, "Can't optimize gurobi model");
        }
    }

    @Override
    public double getOptimValue(String reactionName)
    {
        try
        {
            return model.getVarByName(reactionName).get(GRB.DoubleAttr.X);
        }
        catch( GRBException e )
        {
            return Double.NaN;
        }
    }

    @Override
    public double getValueObjFunc()
    {
        try
        {
            return model.get(GRB.DoubleAttr.ObjVal);
        }
        catch( GRBException e )
        {
            return Double.NaN;
        }
    }

    @Override
    public String[] getReactionNames()
    {
        return reactionNames;
    }

}
