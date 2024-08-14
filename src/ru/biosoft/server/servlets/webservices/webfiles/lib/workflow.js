/* $Id: workflow.js,v 1.34 2013/08/21 08:27:37 ivan Exp $ */
/**
 * JavaScript WorkflowDocument
 *
 * @author lan
 */
function WorkflowDocument(completeName)
{
	var _this = this;
	this.completeName = completeName;
	this.name = getElementName(this.completeName);
    this.tabId = allocateDocumentId("workflow_"+rnd()+"/"+completeName);
    this.viewAreaListeners = new Array();
    this.expertMode = false;
    
	this.open = function(parent)
	{
        opennedDocuments[this.tabId] = this;
        this.workflowDocument = $('<div id="'+this.tabId+'"/>').css('padding', 0);
        this.workflowDocumentContainer = $('<div id="' + this.tabId + '_container" class="documentTab"/>');
        this.workflowDocument.append(this.workflowDocumentContainer);
        parent.append(this.workflowDocument);
        this.titleDiv = $('<h3></h3>').css("margin", "5pt");
        getDataCollection(completeName).getBeanFields('title', function(result){
            if (result) 
            {
                var title = result.getValue('title');
                if (! title) 
                {
                    title = getElementName(completeName);
                }
                _this.titleDiv.html(title);
            }
        });
        this.workflowDocumentContainer.append(this.titleDiv);
		this.propertyInspector = $('<div id="' + this.tabId + '_pi"></div>').css("margin", "5pt").text(resources.wfLoading);
		this.workflowDocumentContainer.append(this.propertyInspector);
		var taskID = paramHash.taskID;
		var fromDE = paramHash.fromDE;
		paramHash = {};
		if(taskID)
		{
			var jobID = rnd();
			queryBioUML("web/jobcontrol/attach", {jobID: jobID, taskID: taskID}, function(data)
			{
				_this.initPropertyInspectorFromJSON(data);
				_this.expertMode = true;
				_this.modeButton.html(resources.anButtonSimpleMode);
				_this.jobID = jobID;
				_this.runButton.val(resources.anButtonStop);
				_this.showJobProgress();
			}, function() {_this.openProperties();});
		} else if(fromDE)
		{
			queryBean("workflow/relaunch/"+fromDE, {}, function(data)
				{
					_this.initPropertyInspectorFromJSON(data);
					if(data.attributes.expertOptions)
						_this.modeButton.show();
				}, function() 
				{
					_this.openProperties();
				});
		} else
		{
			this.openProperties();
		}
		this.runButton = $("<input type='button' value='"+resources.wfButtonRun+"'/>").css("margin", "5pt").click(function()
		{
			_this.checkOverwrite(function()
			{
				_this.runWorkflow();
			});
		});
		this.editButton = $("<input type='button' value='"+resources.wfButtonEdit+"'/>").css("margin", "5pt").click(function()
		{
		    openDiagram(completeName);
			closeDocument(_this.tabId);
		});
		this.modeButton = $("<div/>")
			.css({margin: "5pt", cursor: "pointer", textDecoration: "underline"})
			.html(resources.anButtonExpertMode)
			.click(function() 
					{
						_this.changeMode();
					});
        
		this.progressBar = $("<div/>");
		this.log = $("<div/>").height(250).addClass("logArea");
		this.workflowDocumentContainer.append(this.modeButton).append(this.runButton).append(this.editButton).append(this.progressBar).append($("<br/>")).append(this.log);
		this.modeButton.hide();
        resizeDocumentsTabs();
	};
	
	this.checkOverwrite = function(callback)
	{
        if (this.jobID) 
            callback();
        else 
        {
            this.setProperties(function(data)
            {
                queryBioUML("web/research", {
                    de: _this.completeName,
                    action: "overwritePrompt"
                }, function(data)
                {
                    enableDPI(_this.propertyInspector);
                    if (data.values.length > 0) 
                        createConfirmDialog(resources.commonConfirmElementsOverwrite.replace("{elements}", "<br>" + data.values.join("<br>") + "<br><br>"), callback);
                    else 
                        callback();
                    
                }, function(data)
                {
                    enableDPI(_this.propertyInspector);
                    logger.error(data.message);
                });
                
            });
        }
	};
	
	this.showJobProgress = function()
	{
        this.opened = {};
        createProgressBar(this.progressBar, this.jobID, function(status, message, pathToOpen) {
			_this.runButton.val(resources.wfButtonRun);
			delete(_this.jobID);
            _this.viewChanged();
			updateLog(_this.log, message);
            if (status == JobControl.COMPLETED) 
            {
                _this.openResults(pathToOpen);
            }
		}, function(status, message, pathToOpen) {
			updateLog(_this.log, message);
            _this.openResults(pathToOpen);
            _this.viewChanged();
		});
	};
	
	this.runWorkflow = function()
	{
		if(this.jobID)
		{
            cancelJob(this.jobID);
			this.runButton.val(resources.wfButtonRun);
			delete(this.jobID);
			return;
		}
        this.jobID = rnd();
		this.propertyPane.updateModel();
        var json = convertDPSToJSON(this.propertyPane.getModel());
		this.runButton.val(resources.wfButtonStop);
		this.log.text("");
        queryBioUML("web/research", 
        {
        	de: this.completeName,
            action: "start_workflow",
            json: json,
            jobID: this.jobID/*,
            researchPath: saveResearchPath*/
            // TODO: save research
        }, function(data)
        {
        	_this.showJobProgress();
        });
	};
    
	this.syncronizeData = function(control)
	{
        this.setProperties(function(data)
            {
                _this.initPropertyInspectorFromJSON(data);
            }, 
            control.getModel().getName());
	};
	
	this.initPropertyInspectorFromJSON = function(data)
	{
		if(data.attributes.expertOptions)
			this.modeButton.show();
        this.propertyInspector.empty();
        var beanDPS = convertJSONToDPS(data.values);
        this.propertyPane = new JSPropertyInspector();
        this.propertyPane.setParentNodeId(this.propertyInspector.attr('id'));
        this.propertyPane.setModel(beanDPS);
        this.propertyPane.generate();
		this.propertyPane.addChangeListener(function(control, oldValue, newValue) {
			_this.syncronizeData(control);
		});
        selectViewPart('diagram.overview');
	};
	
    this.openProperties = function()
    {
    	queryBean("workflow/parameters/"+this.completeName, {useCache: "no", showMode: this.expertMode ? SHOW_EXPERT : SHOW_USUAL}, function(data)
        {
			_this.initPropertyInspectorFromJSON(data);
        });
    };
    
    this.changeMode = function()
    {
        this.expertMode = !this.expertMode;
        var buttonTitle = resources.anButtonExpertMode;
        var showMode = SHOW_USUAL; 
        if (this.expertMode) 
        {
            buttonTitle = resources.anButtonSimpleMode;
            showMode = SHOW_EXPERT;
        }
        var json = convertDPSToJSON(this.propertyPane.getModel());    
    	queryBioUML("web/bean/set", 
    	{
    		de: "workflow/parameters/"+this.completeName,
    		useCache: "no",
    		json: json,
    		showMode: this.expertMode ? SHOW_EXPERT : SHOW_USUAL
    	}, function(data)
        {
            _this.modeButton.html(buttonTitle);
            _this.initPropertyInspectorFromJSON(data);
        });
    };
    
    this.close = function(callback)
    {
        this.progressBar.trigger("destroy");
        this.progressBar.removeData();
        this.progressBar.empty();
        if(callback) callback();
    };
    
    this.isChanged = function()
    {
    	return false;
    };
    
    this.setProperties = function(successCallback, control)
    {
    	disableDPI(this.propertyInspector);
        this.propertyPane.updateModel();
        var json = convertDPSToJSON(this.propertyPane.getModel(), control);
        var requestParameters =  {
            action: "set",
            de: "workflow/parameters/"+this.completeName,
            showMode: this.expertMode ? SHOW_EXPERT : SHOW_USUAL,
            json: json
        };
        queryBioUML("web/bean", requestParameters, successCallback);    
    };
    
    this.getDiagram = function()
    {
        return this;
    };
    
    this.addViewAreaListener = function(listener)
    {
        var alreadyAdded = false;
        for (li = 0; li < this.viewAreaListeners.length; li++)
        {
            if (this.viewAreaListeners[li] == listener)
            {
                alreadyAdded = true;
                break;
            }
        }
        if (!alreadyAdded)
        {
            this.viewAreaListeners.push(listener);
        }
    };
    
    this.viewChanged = function()
    {
    	var vp = getActiveViewPart();
		for (li = 0; li < _this.viewAreaListeners.length; li++)
		{
			if (vp instanceof DiagramOverviewViewPart) 
            {
                _this.viewAreaListeners[li].imageChanged();
            }
            else
            {
                _this.viewAreaListeners[li].setToReload();
            }
		}
    };
    
    this.removeViewAreaListener = function(listener)
    {
        for (li = 0; li < this.viewAreaListeners.length; li++)
        {
            if (this.viewAreaListeners[li] == listener)
            {
                this.viewAreaListeners.splice(li,1);
                break;
            }
        }
    };
    
    this.openResults = function(pathsToOpen)
    {
        if(!pathsToOpen)
            return;
        var refreshPaths = {};
        for(var i = 0; i < pathsToOpen.length; i++)
        {
            if (this.opened[pathsToOpen[i]] == undefined) 
            {
                var path = getElementPath(pathsToOpen[i]);
                if(!refreshPaths[path])
				{
					refreshTreeBranch(path, true);
					refreshPaths[path] = true;
				}
                this.opened[pathsToOpen[i]] = true;
                (function(i)
                {
                    setTimeout(function()
                    {
                    	reopenDocument(pathsToOpen[i]);
                    }, 200 * (i));
                })(i);
            }
        }
    };
    
    this.saveAs = function(newPath, callback)
    {
        var _this = this;
        queryBioUML("web/doc/save", { de: _this.completeName, newPath: newPath }, function(data)
        {
            if(callback) callback(data);
        }, function(data)
        {
            if(callback) callback(data);
        });
    };
}
