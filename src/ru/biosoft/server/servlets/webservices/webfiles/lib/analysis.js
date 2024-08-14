/* $Id: analysis.js,v 1.72 2013/08/23 04:08:41 lan Exp $ */
/**
 * JavaScript AnalysisDocument
 *
 * @author lan
 */
function AnalysisDocument(completeName)
{
    var _this = this;
    this.completeName = completeName;
    this.name = getElementName(this.completeName);
    this.tabId = allocateDocumentId("analysis_"+rnd()+"/"+completeName);
    this.expertMode = false;
    this.propertyDescription = {};
    
    this.open = function(parent)
    {
        opennedDocuments[this.tabId] = this;
        this.analysisDocument = $('<div id="'+this.tabId+'"/>').css('padding', 0);
        this.analysisDocumentContainer = $('<div id="' + this.tabId + '_container" class="documentTab"/>');
        this.analysisDocument.append(this.analysisDocumentContainer);
        parent.append(this.analysisDocument);
        this.propertyInspector = $('<div id="' + this.tabId + '_pi"></div>').css("margin", "5pt").text(resources.anLoading);
        this.analysisDocumentContainer.append(this.propertyInspector);
        
        this.analysisDocumentContainer.resize(function()
        {
            var promptWidth = Math.max(_this.analysisDocumentContainer.width()-340, 100);
            _this.propertyInspector.find(".pi-prompt").width(promptWidth).parent().width(promptWidth);
        });
        
        if(instanceOf(getElementClass(this.completeName), "ru.biosoft.galaxy.DataSourceMethodInfo"))
        {
            this.analysisDocumentContainer.append($("<div/>").html(resources.anGalaxyDataSourceHint).css("padding", "5pt"));
        } else
        {
            this.runButton = $("<input type='button' value='"+resources.anButtonRun+"'/>").css("margin", "5pt").click(function() 
                    {
                        if(_this.runButton.val() == resources.anButtonStop)
                            _this.runAnalysis();
                        else
                            _this.checkOverwrite(function() {_this.runAnalysis();});
                    });
            //this.scriptButton = $("<input type='button' value='"+resources.anButtonScript+"'/>").css("margin", "5pt").click(function() {_this.generateScript();});
            this.modeButton = $("<span/>").css({margin: "5pt", cursor: "pointer", textDecoration: "underline"}).html(resources.anButtonExpertMode).click(function() {_this.changeMode();});
            
            this.progressBar = $("<div/>").css("padding", "1pt");
            this.log = $("<div/>").height(150).addClass("logArea");
            this.analysisDocumentContainer.append(this.modeButton).append(this.runButton).append(this.progressBar).append($("<br/>")).append(this.log);
            this.modeButton.hide();
            resizeDocumentsTabs();
        }
        var taskID = paramHash.taskID;
        var fromDE = paramHash.fromDE;
        paramHash = {};
        getDataCollection(this.completeName).getBeanFields("descriptionHTML;baseId", function(beanDPS)
        {
            var baseId = beanDPS.getProperty("baseId")?beanDPS.getProperty("baseId").getValue():null;
            var description = beanDPS.getProperty("descriptionHTML")?beanDPS.getProperty("descriptionHTML").getValue():"";
            if(!description)
                description = "";
            if(baseId) description = description.replace(/img src=\"/g, "img src=\""+appInfo.serverPath+"web/img?id="+baseId+"/");
            var descElement = $("<div/>").html(description);
            descElement.find("a").each(function()
            {
                $(this).replaceWith($(this).html());
            });
            descElement.find("[data-property]").each(function()
            {
                _this.propertyDescription[$(this).attr("data-property")] = $("<div/>").addClass("elementDescription").html($(this).html());
            });
            if(taskID)
            {
                var jobID = rnd();
                queryBioUML("web/jobcontrol/attach", {jobID: jobID, taskID: taskID}, function(data)
                {
                    _this.initPropertyInspectorFromJSON(data);
                    _this.expertMode = true;
                    _this.modeButton.html(resources.anButtonSimpleMode);
                    if(data.attributes.expertOptions)
                        _this.modeButton.show();
                    _this.jobID = jobID;
                    _this.runButton.val(resources.anButtonStop);
                    _this.showJobProgress();
                }, function() {_this.openProperties();});
            } else if(fromDE)
            {
                queryBean("analysis/relaunch/"+fromDE, {showMode: SHOW_EXPERT}, function(data)
                {
                    _this.initPropertyInspectorFromJSON(data);
                    _this.expertMode = true;
                    _this.modeButton.html(resources.anButtonSimpleMode);
                    if(data.attributes.expertOptions)
                        _this.modeButton.show();
                });
            } else
            {
                _this.openProperties();
            }
        });
    };
    
    this.checkOverwrite = function(callback)
    {
        this.setProperties(function(data)
        {
            queryBioUML("web/analysis/overwritePrompt", 
            {
                de: _this.completeName
            }, function(data)
            {
                enableDPI(_this.propertyInspector);
                if(data.values != undefined && data.values.paths != undefined && data.values.paths.length > 0)
                {
                    createConfirmDialog(
                            resources.commonConfirmElementsOverwrite.replace("{elements}", "<br>"+data.values.paths.join("<br>")+"<br><br>"),
                            callback);
                } else
                {
                    callback();
                }
            }, function(data)
            {
                enableDPI(_this.propertyInspector);
                callback();
            });
        });
    };
    
    this.showJobProgress = function()
    {
        createProgressBar(this.progressBar, this.jobID, function(status, message, results) {
            _this.runButton.val(resources.anButtonRun);
            delete(_this.jobID);
            updateLog(_this.log, message);
            if (status == JobControl.COMPLETED) 
            {
                if(results)
                {
                    for(var i=0; i<results.length; i++)
                    {
                        reopenDocument(results[i]);
                    }
                }
            }
        }, function(status, message) {
            updateLog(_this.log, message);
        });
    };
    
    this.runAnalysis = function()
    {
        if(this.jobID)
        {
            cancelJob(this.jobID);
            this.runButton.val(resources.anButtonRun);
            delete(this.jobID);
            return;
        }
        this.jobID = rnd();
        this.propertyPane.updateModel();
        var json = convertDPSToJSON(this.propertyPane.getModel());
        this.runButton.val(resources.anButtonStop);
        this.log.text("").data("message", "");
        var showMode = SHOW_USUAL;
        if(this.expertMode)
            showMode = SHOW_EXPERT;
        queryBioUML("web/analysis", 
        {
            de: this.completeName,
            json: json,
            showMode : showMode,
            jobID: this.jobID
        }, function(data)
        {
            _this.initPropertyInspectorFromJSON(data);
            _this.showJobProgress();
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
        queryBioUML("web/analysis/changeMode", 
        {
            de: this.completeName,
            showMode : showMode,
            json: json
        }, function(data)
        {
            _this.modeButton.html(buttonTitle);
            _this.initPropertyInspectorFromJSON(data);
        });
    };
    
    this.syncronizeData = function(control)
    {
        this.setProperties(function(data)
            {
                _this.initPropertyInspectorFromJSON(data);
            }, 
            control);
    };
    
    this.initPropertyInspectorFromJSON = function(data)
    {
        this.propertyInspector.empty();
        var beanDPS = convertJSONToDPS(data.values);
        for(var propName in this.propertyDescription)
        {
            var property = beanDPS.getProperty(propName);
            if(property)
            {
                property.getDescriptor().setValue("descriptionHTML", this.propertyDescription[propName]);
            }
        }
        this.propertyPane = new JSPropertyInspector();
        this.propertyPane.setParentNodeId(this.propertyInspector.attr('id'));
        this.propertyPane.setModel(beanDPS);
        this.propertyPane.generate();
        this.analysisDocumentContainer.resize();
        this.propertyPane.addChangeListener(function(control, oldValue, newValue) {
            _this.syncronizeData(control);
        });
    };
    
    this.openProperties = function()
    {
        queryBean("properties/method/parameters/"+this.name, {useCache: "no"}, function(data)
        {
            _this.initPropertyInspectorFromJSON(data);
            if(data.attributes.expertOptions)
                _this.modeButton.show();
        });
    };
    
    this.generateScript = function()
    {
        this.setProperties( function(data)
        {
            enableDPI(_this.propertyInspector);
            queryBioUML("web/analysis/script", 
            {
                de: _this.completeName
            }, function(data)
            {
                if(_.isEmpty(data.values))
                    logger.message(resources.anErrorNoScript);
                var scriptDialog = $("<div/>").attr("title", resources.anDialogScriptsTitle);
                var tabs = $("<div>");
                var tabHeaders = $("<ul/>");
                tabs.append(tabHeaders);
                scriptDialog.append(tabs);
                
                var currentPanel;
                
                var buttons = {
                        "Ok": function()
                        {
                            $(this).dialog("close");
                            $(this).remove();
                        }
                };
                
                if(isViewPartAvailable("script.console"))
                {
                    buttons[resources.anDialogScriptsButtonPaste] = function()
                    {
                        var viewpart = lookForViewPart("script.console");
                        viewpart.selectLanguage(currentPanel);
                        viewpart.languages[currentPanel].editor.setValue("");
                        viewpart.languages[currentPanel].editor.replaceSelection(data.values[currentPanel]);
                        selectViewPart("script.console");
                        $(this).dialog("close");
                        $(this).remove();
                    };
                }
                
                scriptDialog.dialog({
                    autoOpen: true,
                    modal: true,
                    width: 600,
                    height: 450,
                    open: function()
                    {
                        _.each(data.values, function(value, key)
                        {
                            if(!scriptTypes[key]) return;
                            if(!currentPanel)
                                currentPanel = key;
                            tabHeaders.append($("<li/>").append($("<a/>").text(scriptTypes[key].title).attr("href", "#"+key)));
                            var div = $("<div style='height: 300px'/>").attr("id", key);
                            tabs.append(div);
                            CodeMirror(div.get(0), {
                                //readOnly: "nocursor",
                                lineWrapping: true,
                                mode: scriptTypes[key].mode,
                                value: value
                            });
                        });
                        tabs.tabs({
                            select: function(event, ui)
                            {
                                currentPanel = ui.panel.id;
                            }
                        });
                    },
                    close: function()
                    {
                        scriptDialog.dialog('destroy').remove();
                    },
                    buttons: buttons
                });
            });
        });
    };
    
    this.close = function(callback)
    {
        this.progressBar.trigger("destroy");
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
            de: "properties/method/parameters/"+this.name,
            json: json
        };
        if(this.expertMode)
            requestParameters['showMode'] = SHOW_EXPERT;
                
        queryBioUML("web/bean/set", requestParameters, successCallback);    
    };


    this.setParameters = function(params, callback) {
      queryBioUML("web/bean/set", 
      {
          de: "properties/method/parameters/"+this.name,
          json: $.toJSON(params)
      }, function(data) {
          _this.initPropertyInspectorFromJSON(data);
          if(callback)
            callback();
      });

    }
    
    this.getParameter = function(paramName){
        if(this.propertyPane)
        {
            var model = this.propertyPane.getModel();
            if(model)
                return findDPSValue(convertDPSToJSON(model), paramName);
        }
        return null;
    };
}
