package ru.biosoft.server.servlets.webservices;

import biouml.model.Diagram;
import ru.biosoft.graph.Graph;
import ru.biosoft.graph.LayoutJobControl;
import ru.biosoft.graph.PathwayLayouter;

public class LayoutThread extends Thread
{
    protected PathwayLayouter layouter;
    protected Diagram diagram;
    protected Graph graph;
    protected LayoutJobControl jobControl;

    public LayoutThread(Diagram diagram, PathwayLayouter pathwayLayouter, Graph graph, LayoutJobControl jobControl) throws Exception
    {
        this.diagram = diagram;
        this.layouter = pathwayLayouter;
        this.graph = graph;
        this.jobControl = jobControl;
    }

    @Override
    public void run()
    {
        layouter.doLayout(graph, jobControl);
    }

    public LayoutJobControl getJobControl()
    {
        return jobControl;
    }

    public Graph getGraph()
    {
        return graph;
    }
}