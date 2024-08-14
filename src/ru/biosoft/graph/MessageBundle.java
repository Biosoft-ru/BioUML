package ru.biosoft.graph;

import java.util.ListResourceBundle;

public class MessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents()
    {
        return contents;
    }

    private final static Object[][] contents = {
            {"CN_LAYOUTER", "Layouter"},
            {"CD_LAYOUTER", "Layouter"},

            {"PN_GRID_X", "GridX"},
            {"PD_GRID_X", "X-distance between grid points"},
            {"PN_GRID_Y", "GridY"},
            {"PD_GRID_Y", "Y-distance between grid points"},

            //OrthogonalPathLayouter
            {"PN_ORTHOGONAL_GRID_X", "GridX"},
            {"PD_ORTHOGONAL_GRID_X", "X-distance between grid points"},
            {"PN_ORTHOGONAL_GRID_Y", "GridY"},
            {"PD_ORTHOGONAL_GRID_Y", "Y-distance between grid points"},
            {"PN_ORTHOGONAL_ITER_MAX", "Max iterations"},
            {"PD_ORTHOGONAL_ITER_MAX", "Max iterations"},
            {"PN_ORTHOGONAL_ITER_K", "Iteration K"},
            {"PD_ORTHOGONAL_ITER_K", "Use K more steps if solution found too fast"},
            {"PN_ORTHOGONAL_SMOOTH", "Smooth edges"},
            {"PD_ORTHOGONAL_SMOOTH", "Smooth edges"},
            {"PN_ORTHOGONAL_ORIENTATION", "Edge orientation"},
            {"PD_ORTHOGONAL_ORIENTATION", "Edge orientation"},


            //ForceDirectedLayouter
            {"PN_FORCEDIRECTED_IP", "Initial placement"},
            {"PD_FORCEDIRECTED_IP", "Initial node placement"},
            {"PN_FORCEDIRECTED_EL", "Edge length"},
            {"PD_FORCEDIRECTED_EL", "Optimal edge length"},
            {"PN_FORCEDIRECTED_RP", "Repulsion"},
            {"PD_FORCEDIRECTED_RP", "Repulsion coefficient"},
            {"PN_FORCEDIRECTED_RD", "Repulsion distance"},
            {"PD_FORCEDIRECTED_RD", "Repulse nodes if they are 'Repulsion distance' times closer than 'optimal edge length'"},
            {"PN_FORCEDIRECTED_GR", "Gravity"},
            {"PD_FORCEDIRECTED_GR", "Gravity"},
            {"PN_FORCEDIRECTED_OR", "Orientation"},
            {"PD_FORCEDIRECTED_OR", "Orientation"},
            {"PN_FORCEDIRECTED_DM", "Distance method"},
            {"PD_FORCEDIRECTED_DM", "Distance method"},
            {"PN_FORCEDIRECTED_AT", "Attraction"},
            {"PD_FORCEDIRECTED_AT", "Attraction"},
            {"PN_FORCEDIRECTED_MI", "Magnetic intencity"},
            {"PD_FORCEDIRECTED_MI", "Magnetic intencity"},
            {"PN_FORCEDIRECTED_IN", "Iteration number"},
            {"PD_FORCEDIRECTED_IN", "Iteration number"},
            {"PN_FORCEDIRECTED_MIT", "Minimal temperature"},
            {"PD_FORCEDIRECTED_MIT", "Minimal temperature"},
            {"PN_FORCEDIRECTED_MAT", "Maximum temperature"},
            {"PD_FORCEDIRECTED_MAT", "Maximum temperature"},
            {"PN_FORCEDIRECTED_HM", "Horisontal movement allowed"},
            {"PD_FORCEDIRECTED_HM", "Horisontal movement allowed"},
            {"PN_FORCEDIRECTED_VM", "Vertical movement allowed"},
            {"PD_FORCEDIRECTED_VM", "Vertical movement allowed"},

            //HierarchicLayouter
            {"PN_HIERARCHIC_VO", "Vertical orientation"},
            {"PD_HIERARCHIC_VO", "Vertical orientation"},
            {"PN_HIERARCHIC_HN", "Hoist nodes"},
            {"PD_HIERARCHIC_HN",
                    "Put leaf nodes on the same level"},
            {"PN_HIERARCHIC_LO", "Layer order iteration"},
            {"PD_HIERARCHIC_LO", "Layer order iteration count"},
            {"PN_HIERARCHIC_VND", "Virtual nodes distance"},
            {"PD_HIERARCHIC_VND", "Minimal distance to virtual node"},
            {"PN_HIERARCHIC_PN", "Process neighbours"},
            {"PD_HIERARCHIC_PN", "Process neighbours"},
            {"PN_HIERARCHIC_DX", "Layer delta X"},
            {"PD_HIERARCHIC_DX", "Delta X between node layers"},
            {"PN_HIERARCHIC_DY", "Layer delta Y"},
            {"PD_HIERARCHIC_DY", "Delta Y between node layers"},

            //ModHierarchicLayouter
            {"PN_MODHIERARCHIC_SN", "Same name nodes weight"},
            {"PD_MODHIERARCHIC_SN", "Same name nodes weight"},
            {"PN_MODHIERARCHIC_DE", "Dummy edges coefficient"},
            {"PD_MODHIERARCHIC_DE", "Dummy edges coefficient"},
            {"PN_MODHIERARCHIC_SW", "Score weight"},
            {"PD_MODHIERARCHIC_SW", "Score weight"},
            {"PN_MODHIERARCHIC_EC", "Edges cross coefficient"},
            {"PD_MODHIERARCHIC_EC", "Edges cross coefficient"},

            //CrossCostGridLayouter
            {"PN_CROSSCOST_NE", "Number of iteration"},
            {"PD_CROSSCOST_NE", "Number of iterations on each annealing step"},
            {"PN_CROSSCOST_RC", "Cooling coefficient"},
            {"PD_CROSSCOST_RC", "Cooling coefficient of annealing"},
            {"PN_CROSSCOST_PT", "Pertrubation threshold"},
            {"PD_CROSSCOST_PT", "Pertrubation threshold used in neighbour function"},
            {"PN_CROSSCOST_ST", "Max distance"},
            {"PD_CROSSCOST_ST", "Max distance on grid"},

            //FastGridLayouter
            {"PN_FASTGRID_IT", "Number of iterations"}, {"PD_FASTGRID_IT", "Number of iterations in annealing simulation"},
            {"PN_FASTGRID_CO", "Cooling coefficient"}, {"PD_FASTGRID_CO", "Cooling coefficient of annealing"},
            {"PN_FASTGRID_PR", "Perturbation rate"}, {"PD_FASTGRID_PR", "Perturbation rate in annealing"},
            {"PN_FASTGRID_TC", "Threads count"}, {"PD_FASTGRID_TC", "Parallel threads count for annealing"},
            {"PN_FASTGRID_SFTL", "Keep current layout"}, {"PD_FASTGRID_SFTL", "If true optimization will be started from current layout"},
            {"PN_FASTGRID_COMPSIZE", "Keep compartments size"}, {"PD_FASTGRID_COMPSIZE", "If true, compartments sizes stay unchanged"},

            {"PN_FASTGRID_SREP", "Strong repulsion"}, {"PD_FASTGRID_SREP", "Repulsion between not connected nodes"},
            {"PN_FASTGRID_EECOST", "Edge crossing cost"}, {"PD_FASTGRID_EECOST", "Cost for edge-edge crossing"},
            {"PN_FASTGRID_ENCOST", "Edge node crossing cost"}, {"PD_FASTGRID_ENCOST", "Cost for edge crossing node"},
            {"PN_FASTGRID_NNCOST", "Node node crossing cost"}, {"PD_FASTGRID_NNCOST", "Cost for nodes crossing"},
            {"PN_FASTGRID_SATTR", "Strong attraction"}, {"PD_FASTGRID_SATTR", "Attraction between directly connected nodes"},
            {"PN_FASTGRID_AATTR", "Average attraction"}, {"PD_FASTGRID_AATTR", "Attraction between nodes connected through another"},
            {"PN_FASTGRID_WATTR", "Weak attraction"}, {"PD_FASTGRID_WATTR", "Attraction between nodes connected through 2 other"},
            {"PN_FASTGRID_WREP", "Weak repulsion"}, {"PD_FASTGRID_WREP", "Repulsion between nodes connected through 3 others"},
            {"PN_FASTGRID_AREP", "Average repulsion"}, {"PD_FASTGRID_AREP", "Repulsion between nodes connected in any way"},
            {"PN_FASTGRID_SREP", "Strong repulsion"}, {"PD_FASTGRID_SREP", "Repulsion between not connected nodes"},
            {"PN_ADJUST_REACTIONS", "Adjust reactions"}, {"PD_ADJUST_REACTIONS", "Automatically find compartments for reactions"},
                        
            //PathwayLayouter
            {"PN_PATHWAY_ILAYOUTER", "Layout method"}, {"PD_PATHWAY_ILAYOUTER", "Inner layout method"},

            //Orthogonal Layouter (Greedy Layouter)
            {"PN_GREEDY_DX", "Delta X"}, {"PD_GREEDY_DX", "Delta X between nodes"}, {"PN_GREEDY_DY", "Delta Y"}, {"PD_GREEDY_DY", "Delta Y between nodes"},
            {"PN_SMOOTH_EDGES", "Smooth edges"}, {"PD_SMOOTH_EDGES", "Smooth edges"},

            {"PN_PATH_LAYOUTER", "Edge layouter"}, {"PD_PATH_LAYOUTER", "Layouter for edges"},

            //Subgraph Layouter
            {"PN_SUBGRAPH_DX", "Delta X"}, {"PD_SUBGRAPH_DX", "Delta X between not connected subgraphs"},
            {"PN_SUBGRAPH_DY", "Delta Y"}, {"PD_SUBGRAPH_DY", "Delta Y between not connected subgraphs"},

            {"PN_SUBGRAPH_LAYOUTER", "Subgraph layouter"}, {"PD_SUBGRAPH_LAYOUTER", "Layouter for not connected subgraphs"},
            
            //Path Layouter Wrapper
            {"PN_PATH_LAYOUTER_NAME", "Name"}, {"PD_PATH_LAYOUTER_NAME", "Edge layouter name "},
            {"PN_PATH_LAYOUTER_OPTIONS", "Properties"}, {"PD_PATH_LAYOUTER_OPTIONS", "Path layouter options"},
            
            };
}
