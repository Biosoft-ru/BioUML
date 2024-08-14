/* $Id: onDemandLoader.js,v 1.18 2013/08/20 03:14:34 lan Exp $ */
function Diagram()
{
}

function CompositeDiagram()
{
}

function OptimizationDocument()
{
}

function SequenceDocument()
{
}

function AnalysisDocument()
{
}

function ComplexDocument()
{
}

function Table()
{
}

function WorkflowDocument()
{
}

function PlotDocument()
{
}

function TextDocument()
{
}

function ImageDocument()
{
}

function TreeTableDocument()
{
}

function JupyterDocument()
{}

function SimulationDocument()
{}

function bpmnDocument()
{}

var modulesCallbacks = {};

function loadBioUMLModules(modules, initFunction, callback)
{
    var lastModule = modules[modules.length-1];
    if(modulesCallbacks[lastModule])
    {
        modulesCallbacks[lastModule].push(callback);
    } else
    {
        modulesCallbacks[lastModule] = [callback];
        var chain = {};
        for(var i=0; i<modules.length; i++)
        {
            chain["lib/"+modules[i]+".js"] = function() {};
        }
        $.chainclude(chain, function()
        {
            initFunction();
            _.each(modulesCallbacks[lastModule], function(callback)
            {
                callback();
            });
            delete modulesCallbacks[lastModule];
        });
    }
}

function createJupyterDocument(path, callback)
{
    loadBioUMLModules(["jupyter"], function(){}, function()
    {
        callback(new JupyterDocument(path));
    });
}
function newJupyterDocument(path)
{
    loadBioUMLModules(["jupyter"], function(){}, function()
    {
        createNewJupyterFile(path);
    });
}
function newJupyterNotebookFromAnalyses(path, callback)
{
    loadBioUMLModules(["jupyter"], function(){}, function()
    {
        //createNewJupyterKernelFromAnalyses(path);
        callback(new JupyterDocument(path, true/*isKernel*/));
    });
}

function CreateDiagramDocument (name, callback)
{
    loadBioUMLModules(["diagramSupport", "viewpartsDiagram","diagram"], function()
    {
        initDiagramViewParts();
        initDiagramContextMenu();
    }, function()
    {
        callback(new Diagram(name));
    });
}

function CreateCompositeDiagramDocument (name, callback)
{
    loadBioUMLModules(["diagramSupport", "viewpartsDiagram","diagram", "compositeDiagram"], function()
    {
        initDiagramViewParts();
        initDiagramContextMenu();
    }, function()
    {
        callback(new CompositeDiagram(name));
    });
}


function CreateOptimizationDocument (name, callback)
{
    loadBioUMLModules(["viewpartsOptimization","optimization"], function()
    {
        initOptimizationViewParts();
    }, function()
    {
        callback(new OptimizationDocument(name));
    });
}

function CreateSequenceDocument (name, callback)
{
    var hash = paramHash;
    loadBioUMLModules(["viewpartsBsa","sequence"], function()
    {
        initBSAViewParts();
    }, function()
    {
        callback(new SequenceDocument(name, hash.de === name ? hash : {}));
    });
}

function CreateAnalysisDocument (name, callback)
{
    loadBioUMLModules(["analysis"], function() {}, function()
    {
        callback(new AnalysisDocument(name));
    });
}

function CreateWorkflowDocument (name, callback)
{
    var hash = paramHash;
    loadBioUMLModules(["viewpartsDiagram","workflow"], function()
    {
        initDiagramViewParts();
    }, function()
    {
        paramHash = hash;
        callback(new WorkflowDocument(name));
    });
}

function createTableDocument (name, callback)
{
    loadBioUMLModules(["viewpartsTable", "table"], function()
    {
        initTableViewParts();
        initTableContextMenu();
    }, function()
    {
        callback(new Table(name));
    });
}

function createPlotDocument (name, callback)
{
    loadBioUMLModules(["viewpartsPlot", "plot"], function()
    {
        initPlotViewParts();
    }, function()
    {
        callback(new PlotDocument(name));
    });
}

function createWebDocument(name, callback)
{
    loadBioUMLModules(["webDocument"], function() {}, function()
    {
        callback(new WebDocument(name));
    });
}

function createMarkdownDocument(name, callback)
{
    loadBioUMLModules(["markdownDocument"], function() {}, function()
    {
        callback(new MarkdownDocument(name));
    });
}

function createTextDocument(name, callback)
{
    loadBioUMLModules(["text"], function() {}, function()
    {
        callback(new TextDocument(name));
    });
}

function createImageDocument(name, callback)
{
    loadBioUMLModules(["image"], function() {}, function()
    {
        callback(new ImageDocument(name));
    });
}

function createTreeTableDocument (name, callback)
{
    loadBioUMLModules(["treeTable"], function() {}, function()
    {
        callback(new TreeTableDocument(name));
    });
}

function createSimulationDocument (name, callback)
{
    loadBioUMLModules(["viewpartsSimulation", "simulation"], function()
    {
        initSimulationViewParts();
    }, function()
    {
        callback(new SimulationDocument(name));
    });
}

function loadPathfinderModules (name)
{
    var hash = paramHash;
    loadBioUMLModules(["viewpartsPathfinder","pathfinder"], function() {initPathfinderViewParts();}, function()
    {
        
    });
}

function openJSRootDocument (name, callback)
{
    loadBioUMLModules(["jsRootDocument"], function() {}, function()
    {
        callback(new JSRootDocument(name));
    });
}
function openJSRootDocument (name, callback)
{
	loadBioUMLModules(["jsRootDocument"], function() {}, function()
	{
		callback(new JSRootDocument(name));
	});
}

function openBpmnDocument (name, callback)
{
    loadBioUMLModules(["bpmn/bpmn-modeler.development", "bpmnDocument"], function() {}, function()
    {
        callback(new bpmnDocument(name));
    });
}

function createBpmnDocument (name, callback)
{
    createSaveElementDialog(resources.dlgCreateScriptTitle,
        "ru.biosoft.bpmn.BPMNDataElement", createPath(name, "newDiagram.bpmn"),
        function(completePath)
        {
            queryBioUML("web/bpmn/create",
            {
                de: completePath
            },
            function(data)
            {
                refreshTreeBranch(getElementPath(completePath));
                loadBioUMLModules(["bpmn/bpmn-modeler.development", "bpmnDocument"], function() {}, function()
                {
                    callback(new bpmnDocument(completePath));
                });
            });
        });
}

function createMoleculeDocument(name, callback)
{
    loadBioUMLModules(["moleculeDocument"], function() {}, function()
    {
        callback(new MoleculeDocument(name));
    });
}

function createPhyloTreeDocument(name, callback)
{
    loadBioUMLModules(["phyloTreeDocument"], function() {}, function()
    {
        callback(new PhyloTreeDocument(name));
    });
    
}

function openOasysDocument( name, callback )
{
    loadBioUMLModules(["oasysDocument"], function() {}, function()
    {
        callback( new OasysDocument( name ) );
    });
}

function openAlignViewDocument( name, callback )
{
    loadBioUMLModules(["alignViewDocument"], function() {}, function()
    {
        callback( new AlignmentViewerDocument( name ) );
    });
}

function createVideoDocument(name, callback)
{
    loadBioUMLModules(["videoDocument"], function() {}, function()
    {
        callback( new VideoDocument( name ) );
    });
}
