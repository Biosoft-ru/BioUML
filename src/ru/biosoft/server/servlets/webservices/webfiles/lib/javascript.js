/* $Id: javascript.js,v 1.40 2013/08/22 11:09:53 lan Exp $ */

/**
 * Script (JavaScript) document realization
 * 
 * @author tolstyh
 */
function ScriptDocument(completeName)
{
    this.completeName = completeName;
    this.name = getElementName(this.completeName);
    this.savedDocument = ""; 
    this.tabId = allocateDocumentId("script/"+completeName);
    var dc = getDataCollection(this.completeName);
    this.readOnly = !dc.isMutable();
    dc.addRemoveListener(this);
    
    this.scriptType = "js";
    if(instanceOf(dc.getClass(), "ru.biosoft.plugins.jri.RElement"))
    	this.scriptType = "R";
    
    var _this = this;
    
    this.open = function(parent)
    {
        opennedDocuments[this.tabId] = this;
        var scriptDocument = $('<div id="' + this.tabId + '"></div>').css('padding', 0).css('position', 'relative').attr('doc', this);
        parent.append(scriptDocument);
        
        this.scriptContainerDiv = $('<div id="' + this.tabId + '_container" class="documentTab"></div>');
        this.scriptContainerDiv.html("Loading...");
        scriptDocument.append(this.scriptContainerDiv);
        
        this.textArea = $('<textarea id="' + this.tabId + '_area" style="width:100%; height:100%"></textarea>');
        this.scriptContainerDiv.html(this.textArea);
        
        this.loadScript();

        createTreeItemDroppable(this.scriptContainerDiv, null, function(path, event)
        {
        	if(!_this.editor) return;
        	var coords = _this.editor.coordsChar({left: event.pageX, top: event.pageY});
        	_this.editor.replaceRange(path, coords);
        	_this.editor.setCursor(coords);
        });
        
        //update document size
        resizeDocumentsTabs();
    };
    
    this.loadScript = function()
    {
        var params = {
            "de": this.completeName    
        };
        queryBioUML("web/doc/getcontent", params, function(data)
        {
        	_this.textArea.val(data.values);
        	_this.savedDocument = data.values;
            _this.editor = CodeMirror.fromTextArea(_this.textArea.get(0), {
                mode: scriptTypes[_this.scriptType].mode,
                lineNumbers: true,
                styleActiveLine: true,
                styleSelectedText: true,
                extraKeys: {
                	"Ctrl-Space": scriptTypes[_this.scriptType].mode+"_autocomplete",
                	"Tab": scriptTypes[_this.scriptType].mode+"_autocomplete",
                	"Ctrl-H": "replace", 
                	"Ctrl-R": function() {_this.run();}, 
                	"Ctrl-S": function() {_this.save();}
                }
              });
            _this.editor.focus();
        });
    };
    
    this.save = function(callback)
    {
    	this.saveAs(this.completeName, callback);
        this.fireContentChanged();
    };
    
    this.saveAs = function(newPath, callback)
    {
    	var script = this.editor.getValue();
        queryBioUML("web/doc/savecontent", 
        {
            "de": this.completeName,
            "newPath": newPath,
            "content": script
        }, function(data)
        {
        	if(newPath == _this.completeName)
        		_this.savedDocument = script;
        	if(callback) callback(data);
        }, function(data)
        {
        	if(callback) callback(data);
        });
    };
    
    this.run = function()
    {
        this.fireExecutionStarted();
		var scriptRunning = true;
		var viewpartActivated = false;
		var lastOutput = "";
		var jobID = rnd();
		var updateOutput = function()
		{
			if(!scriptRunning) return;
			queryBioUML("web/jobcontrol", {jobID: jobID}, 
				function(data)
				{
					if(data.status != JobControl.RUNNING)
					{
						scriptRunning = false;
						processEnvironment(jobID, function(data) {
					        _this.fireExecutionFinished(data);
						});
					}
					if(data.values != lastOutput)
					{
						lastOutput = data.values[0];
						if(!viewpartActivated)
					    {
						    selectViewPart('script.output');
						    viewpartActivated = true;
					    }
						_this.fireMessageUpdated(data);
					}
					if(!scriptRunning) return;
					setTimeout(updateOutput, 500);
				}, function(data)
				{
					if(!scriptRunning) return;
					setTimeout(updateOutput, 500);
				}
			);
		};

		queryBioUML("web/script/run", 
        {
			"jobID": jobID,
            "de": this.completeName,
            "script": this.editor.getValue()
        }, function(data)
        {
    		updateOutput();
        });
    };
    
    ///////////////////
    // Listener support
    ///////////////////
    this.executeListeners = new Array();
    
    this.addExecuteListener = function(listener)
    {
        var alreadyAdded = false;
        for (li = 0; li < this.executeListeners.length; li++) 
        {
            if (this.executeListeners[li] == listener) 
            {
                alreadyAdded = true;
                break;
            }
        }
        if (!alreadyAdded) 
        {
            this.executeListeners.push(listener);
        }
    };
    
    this.fireExecutionStarted = function()
    {
        for (li = 0; li < this.executeListeners.length; li++) 
        {
            this.executeListeners[li].executionStarted();
        }
    };
    
    this.fireMessageUpdated = function(info)
    {
        for (li = 0; li < this.executeListeners.length; li++) 
        {
			if(this.executeListeners[li].messageUpdated)
            	this.executeListeners[li].messageUpdated(info);
        }
    };
    
    this.fireExecutionFinished = function(info)
    {
        for (li = 0; li < this.executeListeners.length; li++) 
        {
            this.executeListeners[li].executionFinished(info);
        }
    };
    
    this.close = function(callback)
    {
        if(callback) callback();
    };

    this.isChanged = function()
    {
    	return _this.savedDocument != this.editor.getValue();
    };

    this.dataCollectionRemoved = function()
    {
        closeDocument(this.tabId);
    };

    
    //Conent listeneres
    this.contentListeners = new Array();
    this.addContentListener = function(listener)
    {
        var alreadyAdded = false;
        for (li = 0; li < this.contentListeners.length; li++) 
        {
            if (this.contentListeners[li] == listener) 
            {
                alreadyAdded = true;
                break;
            }
        }
        if (!alreadyAdded) 
        {
            this.contentListeners.push(listener);
        }
    };
    this.fireContentChanged = function() {
        for (li = 0; li < this.contentListeners.length; li++) 
        {
            this.contentListeners[li].contentChanged(this);
        }
    }
}
