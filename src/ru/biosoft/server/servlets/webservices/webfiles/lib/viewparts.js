/**
 * Set of document view parts and view parts utility functions
 * 
 */
var viewParts = new Array();
var opennedViewPartId = null;
var viewPartsInitialized = {};

function getViewPartId(name)
{
    return name;//name.replace(/\./g, "_");
}

function getViewPartSelector(name)
{
    return getJQueryIdSelector(getViewPartId(name));
}

function createViewPartContainer(name)
{
    return $("<div/>").attr("id", getViewPartId(name)).attr("data-id", name);
}

/*
 * Create view part objects
 */
var pagingInitialized = false;
function initViewParts()
{
    var descriptionViewPart = new DescriptionViewPart();
    descriptionViewPart.init();
    viewParts.push(descriptionViewPart);
    
    var srViewPart = new SearchResultViewPart();
    srViewPart.init();
    viewParts.push(srViewPart);
    
    var graphSearchViewPart = new GraphSearchViewPart();
    graphSearchViewPart.init();
    viewParts.push(graphSearchViewPart);
    
    var jsViewPart = new JavaScriptViewPart();
    jsViewPart.init();
    viewParts.push(jsViewPart);
    
    var clipboardViewPart = new ClipboardViewPart();
    clipboardViewPart.init();
    viewParts.push(clipboardViewPart);
    
    var scriptOutputViewPart = new ScriptOutputViewPart();
    scriptOutputViewPart.init();
    viewParts.push(scriptOutputViewPart);
    
    /*var sqlEditorViewPart = new SQLEditorViewPart();
    sqlEditorViewPart.init();
    viewParts.push(sqlEditorViewPart);*/
    
    var tasksViewPart = new TasksViewPart();
    tasksViewPart.init();
    viewParts.push(tasksViewPart);
    
    var geViewPart = new GenomeEnhancerViewPart();
    geViewPart.init();
    viewParts.push(geViewPart);
    
    var logViewPart = new LogViewPart();
    logViewPart.init();
    viewParts.push(logViewPart);

    initWDLViewParts();

    //var viewPartTabs = el.viewPartTabs;

    var tabs = el.viewPartTabs.tabs(
    {
        activate: function(event, ui)
        {
            if (opennedViewPartId != null) 
            {
                var vp = lookForViewPart(opennedViewPartId);
                if (vp != null) 
                {
                    vp.save();
                }
            }
            opennedViewPartId = $(ui.newPanel).attr('data-id');
            var vp = lookForViewPart(opennedViewPartId);
            if (vp != null) 
            {
                updateViewPartsToolbar(vp);
                if(vp.show)
                    vp.show(opennedDocuments[activeDocumentId]);
            }
            el.viewPartTabs.triggerHandler("resize");
        }
    });
    this.viewPartToolbar = $('<div class="ui-corner-all ui-helper-clearfix tabs-shifter" style="min-width: 115px; left: 2px; top: 2px; position:absolute; z-index:1;"></div>');
    tabs.find('.ui-tabs-nav').append(this.viewPartToolbar);
    
    initSettings(tabs);
    
    updateViewParts();
    
    el.viewPartTabs.bind("resize", function(event) {
        if(el.viewPartTabs.has(event.target).length) 
            return;
        if(pagingInitialized)
            el.viewPartTabs.tabs('refreshOnly');
        el.viewPartTabs.children('div')
            .width(el.viewPartTabs.width()-30)
            .height(el.viewPartTabs.height()-el.viewPartTabs.children('ul').outerHeight()-23);
    });
}

function initViewpartsPaging()
{
    if(pagingInitialized)
        return;
    if($(el.viewPartTabs).find( ".ui-tabs-nav li" ).length > 0){
        el.viewPartTabs.tabs('paging', { cycle: true, followOnActive: true, activeOnAdd: false });
        pagingInitialized = true;
    }
}

function refreshViewParts(vpt)
{
    //viewPartTabs.tabs('resizeInit');
    if(pagingInitialized)
        vpt.tabs('refreshOnly');
    
    vpt.children('div')
        .width(vpt.width()-30)
        .height(vpt.height()-vpt.children('ul').outerHeight()-23);
}

function initSettings(tabs)
{
    var viewPartSettings = $('<div class="ui-corner-all ui-helper-clearfix tabs-shifter" style="right: 0px; top: 2px; position:absolute; z-index:2;"></div>');
    tabs.find('.ui-tabs-nav').append(viewPartSettings);
    var block = $('<div class="fg-buttonset ui-helper-clearfix" style="margin: 3px 5px 0 0;"></div>');
    viewPartSettings.append(block);
    var settingsAction = createToolbarButton(resources.vpSettings, "settings.png", editViewpartSettings);
    block.append(settingsAction);
}

function editViewpartSettings()
{
    var dialogDiv = $('<div title="'+resources.vpSettingsDialogTitle+'"></div>');
    
    var table = $('<table class="display"><tr><th>'+resources.vpSettingsTitle+'</th><th>'+resources.vpSettingsChecked+'</th></tr></table>').css("width","300px").css("margin", "0 auto");
    var vpHidePref = getPreference("viewpartsHidden");
    var vpHide = [];
    if(vpHidePref != null)
        vpHide = vpHidePref.split(";");
    var i = 1;
    for (var vpi = 0; vpi < viewParts.length; vpi++) 
    {
        var viewPart = viewParts[vpi];
        var tr = $('<tr><td></td><td></td></tr>').addClass(i++%2?"odd":"even");
        tr.children().eq(0).text(viewPart.tabName);
        var ch = $('<input type="checkbox" class="vpHide"/>').attr("vp-id", viewPart.tabId);
        if(!vpHide.includes(viewPart.tabId))
            ch.attr("checked", "checked");
        tr.children().eq(1).append(ch);
        table.append(tr);
    }
    
    dialogDiv.append(table);
    
    dialogDiv.dialog(
    {
        autoOpen: false,
        width: 350,
        buttons:
        {
            "Save" : function()
            {
                var _thisDialog = $(this);
                //Some viewparts are inititalized only when specific elements like Diagram, Table are opened. 
                //Keep old hidden viewparts ids even if they are not in the list now.
                var currentlyAvailable = {};
                var toHide = [];
                _thisDialog.find(".vpHide:checkbox:not(:checked)").each(function() {
                    toHide.push($(this).attr("vp-id"));
                });
                
                _thisDialog.find(".vpHide:checkbox").each(function() {
                    currentlyAvailable[$(this).attr("vp-id")] = 1;
                });
                for(var i=0; i < vpHide.length; i++)
                {
                    if(currentlyAvailable[vpHide[i]] == undefined)
                        toHide.push(vpHide[i]);
                }
                setPreference("viewpartsHidden", toHide.join(";"), updateViewParts)
                $(this).dialog("close");
                $(this).remove();
            },
            "Cancel": function()
            {
                $(this).dialog("close");
                $(this).remove();
            },
        }
    });
    dialogDiv.dialog("open");  
};


/**
 * Called by viewPart constructor to perform common viewPart initialization steps
 * @param viewPart - viewPart object itself
 * @param id - viewPart id
 * @param name - viewPart title displayed to user
 */
function createViewPart(viewPart, id, name)
{
    viewPart.tabId = id;
    viewPart.tabName = name;
    viewPart.tabDiv = createViewPartContainer(id);
    viewPart.containerDiv = $('<div class="viewPartTab"/>').attr("id", id + '_container');
    viewPart.tabDiv.append(viewPart.containerDiv);
}

function getActiveViewPart()
{
    return lookForViewPart(opennedViewPartId);
}

function isRulePassed(viewPartId, rule)
{
    if(!rule.pattern)
    {
        var pattern = "";
        for(var i=0; i<rule.template.length; i++)
        {
            var char = rule.template.charAt(i);
            if( char == '*' )
                pattern+=".*";
            else if( char == '?' )
                pattern+=".";
            else if( "+()^$.{}[]|\\".indexOf(char) != -1 )
                pattern+='\\'+char; // prefix all metacharacters with backslash
            else
                pattern+=char;
        }
        rule.pattern = new RegExp(pattern, "i");
    }
    return viewPartId.match(rule.pattern);
}

function isViewPartAvailable(viewPartId)
{
    var result = true;
    for(var i in perspective.viewparts)
    {
        var rule = perspective.viewparts[i];
        if(isRulePassed(viewPartId, rule)) result = rule.type == "allow";
    }
    return result;
}

function updateViewPartsToolbar(viewPart)
{
    this.viewPartToolbar.empty();
    var block = $('<div class="fg-buttonset ui-helper-clearfix" style="margin: 3px 0 0 2px;"></div>');
    
    if (viewPart && viewPart.initActions) 
    {
        viewPart.initActions(block);
    }
    
    this.viewPartToolbar.append(block);
    var leftToolbarShift = Math.max(block.children().length*22 + 5, 117)  ;
    $('#viewPartTabs > ul').css("padding-left", leftToolbarShift + "px");
}

/*
 * Show view parts for opened object. Use "null" for global view parts.
 */
function updateViewParts(skipSelection)
{
    var doc = getActiveDocument();
    var viewPartToSelect = null;
    if(activeDocumentId == 'sql_result')
        viewPartToSelect = "sql.editor";
    else
        viewPartToSelect = opennedViewPartId;
    if(viewPartToSelect == null && appInfo.defaultViewPart)
        viewPartToSelect = appInfo.defaultViewPart;
    var viewPartTabs = el.viewPartTabs;
    var currentTabs = [];
    viewPartTabs.children('div').each(function()
    {
        currentTabs[$(this).attr('id')] = $(this);
    });
    
    viewPartTabs.children('ul').children('li').remove();
    
    var vpHidePreference = getPreference("viewpartsHidden");
    var vpHidden = [];
    if(vpHidePreference != null)
        vpHidden = vpHidePreference.split(";");
    var vpNavBar = viewPartTabs.children(".ui-tabs-nav" );
    var vpVisible = [];
    for (var vpi = 0; vpi < viewParts.length; vpi++) 
    {
        var viewPart = viewParts[vpi];
        if(!isViewPartAvailable(viewPart.tabId)) continue;
        if(vpHidden.includes(viewPart.tabId)) continue;
        
        (function(viewPart) // using closure to isolate viewPart variable
        {
            viewPart.isVisible(doc, function(visible)
            {
                if (visible) 
                {
                    vpVisible.push(viewPart.tabId);
                    if(currentTabs[viewPart.tabId])
                    {
                        currentTabs[viewPart.tabId] = null;
                    }
                    else
                    {
                        viewPartTabs.append(viewPart.tabDiv);
                    }
                    $( "<li><a href='#"+viewPart.tabId+"'>"+viewPart.tabName+"</a></li>" ).appendTo(vpNavBar);
                    viewPart.explore(doc);
                }
            });
        })(viewPart);
    }
    viewPartTabs.tabs( "refresh" );
    for(var i in currentTabs)
    {
        if(currentTabs[i]!=null)
        {
            viewPartTabs.children(getViewPartSelector(i)).remove();
            
        }
    }
    //No viewparts visible for current document (all hidden or disabled)
    if(vpVisible.length == 0)
    {
        opennedViewPartId = null;
        //console.log("No viewparts");
        viewPartToSelect = null;
        updateViewPartsToolbar(null);                                   
    }
    else
    {   
        if (!skipSelection && viewPartToSelect != null)
        {
            selectViewPart(viewPartToSelect);
            viewPartToSelect = null;
        }
        else
            el.viewPartTabs.tabs("option", "active", 0);
        //viewPartTabs.trigger("resize");
        refreshViewParts(viewPartTabs);
        initViewpartsPaging();
    }
}

/*
 * Save changes in all view parts
 */
function saveAllViewParts()
{
    var doc = getActiveDocument();
    if (doc != null) 
    {
        for (i = 0; i < viewParts.length; i++) 
        {
            var viewPart = viewParts[i];
            viewPart.isVisible(doc, function(visible)
            {
                if (visible) 
                {
                    viewPart.save();
                }
            });
        }
    }
}

/*
 * Search view part by ID
 */
function lookForViewPart(id)
{
    for (i = 0; i < viewParts.length; i++) 
    {
        var viewPart = viewParts[i];
        if (viewPart.tabId == id) 
        {
            return viewPart;
        }
    }
    return null;
}

/*
 * Select tab by ID
 */
function selectViewPart(id)
{
    viewPartToSelect = id;
    var index = $('#viewPartTabs a[href="#'+id+'"]').parent().index();
    if(index < 0)
        return;
    var anchors = $('#viewPartTabs > ul > li > a');
    var offset = 2; //offset for toolbar and settings
//    if(anchors.length > 0 && anchors.get(0).getAttribute("href") == "#")
//        offset += 1;
    el.viewPartTabs.tabs("option", "active", index-offset);
}

/*
 * JavaScript view part class
 */
function JavaScriptViewPart()
{
    this.tabId = "script.console";
    this.tabName = resources.vpScriptTitle;
    this.tabDiv;
    this.visible = true;
    
    this.languages = [];
    this.currentLanguage = "";
    
    var _this = this;
    
    var scriptRunning;
    
    this.selectLanguage = function(language)
    {
        this.selector.val(language).change();
    };

    /*
     * Submit form action
     */
    function executeScript()
    {
        if(scriptRunning) return;
        var language = _this.languages[_this.currentLanguage];
        var scriptJobID = rnd();
        var scriptValue = language.editor.getValue();
        _this.submitButton.attr("disabled", true).addClass("ui-state-disabled");
        queryBioUML("web/script/runInline", {
            script: scriptValue,
            type: _this.currentLanguage,
            jobID: scriptJobID
        }, function(data) {
            openNonTreeDocument('log', 'log_container', 'Script log', 'script.console');
            var result = $("<div class='script_response'>");
            $("#log_container").children("#log").append($("<div class='script_command'>").text(_this.currentLanguage+"> "+scriptValue)).append(result);
            if(scriptValue == "") return;
            var lastOutput = "";
            result.addClass("running").text("...running...");
            $("#log_container").scrollTop($("#log_container")[0].scrollHeight);
            scriptRunning = true;
            var updateOutput = function()
            {
                if(!scriptRunning) return;
                queryBioUML("web/jobcontrol", {jobID: scriptJobID}, 
                    function(data)
                    {
                        if(data.status != JobControl.RUNNING)
                        {
                            scriptRunning = false;
                            _this.submitButton.attr("disabled", false).removeClass("ui-state-disabled");
                            data.values = data.values[0].replace(/INFO - Completed\n$/, "");
                            processEnvironment(scriptJobID, function(data) {
                                _.each(data.html, function(value) {
                                    var htmlDiv = elementFromHTML(value).addClass("script_response");
                                    $("#log_container").children("#log").append(htmlDiv);
                                    $("#log_container").scrollTop($("#log_container")[0].scrollHeight);
                                });
                            });
                        }
                        else
                        {
                            data.values = data.values[0];
                        }
                        if(data.values != lastOutput || data.status != JobControl.RUNNING)
                        {
                            lastOutput = data.values;
                            result.removeClass("running").html(lastOutput.escapeHTML().replace(/\n/g, "<br>"));
                            $("#log_container").scrollTop($("#log_container")[0].scrollHeight);
                        }
                        if(!scriptRunning) return;
                        setTimeout(updateOutput, 500);
                    }, function(data)
                    {
                        if(data.code == "EX_QUERY_PARAM_NO_JOB")
                        {
                            logger.error(resources.commonErrorMissingJob);
                            scriptRunning = false;
                            _this.submitButton.attr("disabled", false).removeClass("ui-state-disabled");
                            return;
                        }
                        if(!scriptRunning) return;
                        setTimeout(updateOutput, 500);
                    }
                );
            };
            updateOutput();
        });
        //add command to stack
        language.commandStack = _.without(language.commandStack, scriptValue);
        language.commandStack.push(scriptValue);
        language.stackPos = language.commandStack.length;
        
        language.editor.setValue("");
        language.editor.focus();
        return false;
    }

    /*
     * Create div for view part
     */
    this.init = function()
    {
        this.tabDiv = createViewPartContainer(this.tabId);
        var tabs = $("<div/>");
        this.selector = $("<select/>").change(function() {
            _this.currentLanguage = $(_this.selector).val();
            if(_this.currentLanguage)
            {
                tabs.children().hide();
                _this.languages[_this.currentLanguage].tab.show();
                _this.languages[_this.currentLanguage].editor.focus();
            }
        });
        this.submitButton = $('<input type="button"/>').css({float: "right"}).val(resources.vpScriptButtonExecute).addClass("ui-state-default").click(executeScript);
        this.tabDiv.append(this.submitButton);
        this.tabDiv.append($("<span/>").text(resources.vpScriptScriptContext));
        this.tabDiv.append(this.selector);
        this.tabDiv.append(tabs);
        for(var type in scriptTypes)
        {
            if(!scriptTypes[type].allowed) continue;
            var typeTitle = scriptTypes[type].title;
            var mode = scriptTypes[type].mode;
            var language = {};
            this.selector.append($("<option>").val(type).text(typeTitle));
            var div = $("<div/>").height(130).css({border: "1px solid black"});
            var textArea = $('<textarea style="width: 100%; height: 100%;"/>');
            div.append(textArea);
            tabs.append(div);
            language.tab = div;
            language.commandStack = [];
            language.stackPos = 0;
            createTreeItemDroppable(div, null, function(path)
            {
                var editor = _this.languages[_this.currentLanguage].editor;
                if(editor.getValue() === "")
                {
                    var functionName = _this.currentLanguage == 'R' ? "biouml.get" : "data.get";
                    editor.replaceSelection(functionName + "(\""+path.replace(/\"\\/g, "\\$1")+"\")");
                }
                else editor.replaceSelection('"'+path.replace(/\"\\/g, "\\$1")+'"');
            });

            language.editor = CodeMirror.fromTextArea(textArea.get(0), {
                mode: mode,
                lineNumbers: false,
                lineWrapping: true,
                styleActiveLine: false,
                styleSelectedText: false,
                extraKeys: {
                    "Ctrl-Space": mode+"_autocomplete",
                    "Tab": mode+"_autocomplete",
                    "Ctrl-H": "replace", 
                    "Enter": executeScript,
                    "Ctrl-Enter": "newlineAndIndent",
                    "Ctrl-Up": "goLineUp",
                    "Ctrl-Down": "goLineDown",
                    "Up": function() {
                        var language = _this.languages[_this.currentLanguage];
                        if (language.stackPos > 0) 
                        {
                            language.stackPos--;
                            language.editor.setValue(language.commandStack[language.stackPos]);
                            language.editor.setCursor(language.editor.lineCount(), 0);
                        }
                    },
                    "Down": function() {
                        var language = _this.languages[_this.currentLanguage];
                        if (language.stackPos < language.commandStack.length - 1) 
                        {
                            language.stackPos++;
                            language.editor.setValue(language.commandStack[language.stackPos]);
                            language.editor.setCursor(language.editor.lineCount(), 0);
                        }
                        else 
                        {
                            language.stackPos = language.commandStack.length;
                            language.editor.setValue("");
                        }
                    }
                   }
            });            
            this.languages[type] = language;
        }
        this.selector.change();
        if(!_this.currentLanguage) this.visible =false;
    };
    
    /*
     * Indicates if view part is visible
     */
    this.isVisible = function(documentObject, callback)
    {
        callback(_this.visible);
    };
    
    this.show = function()
    {
        this.languages[this.currentLanguage].editor.focus();
    };
    
    this.explore = function()
    {
    };
    
    this.save = function()
    {
    };
}

function DescriptionViewPart()
{
    this.tabId = "common.description";
    this.tabName = resources.vpDescriptionTitle;
    this.tabDiv;
    this.editMode = false;
    this.currentBean = null;
    this.editable = false;
    
    var _this = this;
    
    /*
     * Create div for view part
     */
    this.init = function()
    {
        this.tabDiv = createViewPartContainer(this.tabId).addClass("elementDescriptionScrollPane");
        this.viewerDiv = $('<div id="' + this.tabId + '_viewer"></div>').addClass("elementDescription").html(resources.vpDescriptionNothingSelected);
        this.tabDiv.append(this.viewerDiv);
        this.editorDiv = $('<div id="' + this.tabId + '_editor"></div>').hide();
        this.tabDiv.append(this.editorDiv);
        
        _.bindAll(this, _.functions(this));
    };
    
    /*
     * Indicates if view part is visible
     */
    this.isVisible = function(documentObject, callback)
    {
       callback(true);
    };
    
    /*
     * Open document event handler
     */
    this.explore = function(documentObject)
    {
        if ((documentObject != null) && (documentObject instanceof Diagram || documentObject instanceof ComplexDocument)) 
        {
            this.setElementDescription(documentObject.getDiagram().completeName);
        }
        else if(documentObject != null && documentObject instanceof Table)
        {
            this.setElementDescription(documentObject.completeName);
        }
    };
    
    /*
     * Save function
     */
    this.save = function()
    {
        // nothing to do
    };
    
    /*
     * Creates toolbar actions for this tab
     */
    this.initActions = function(toolbarBlock)
    {
        this.editAction = createToolbarButton(resources.vpDescriptionButtonEdit, "edit.gif", this.editActionClick);
        setToolbarButtonEnabled(this.editAction, this.editable);
        toolbarBlock.append(this.editAction);
        
        this.saveAction = createToolbarButton(resources.vpDescriptionButtonSave, "save.gif", this.saveActionClick);
        toolbarBlock.append(this.saveAction);
    };
    
    this.setElementDescription = function(path)
    {
        if(this.path == path)
            return;
            
        this.initView();
        var dc = getDataCollection(path);
        this.path = path;
        dc.getDescription(function (description, fieldNames, bean)
        {
            _this.editable = false;
            if (description == null) 
            {
                _this.openDescription("");
                return;
            }
            else 
            {
                var type = dc.getClass();
                if (instanceOf(type,"biouml.model.Diagram") || instanceOf(type,"ru.biosoft.table.TableDataCollection") || instanceOf(type,"ru.biosoft.bsa.SqlTrack") || instanceOf(type,"ru.biosoft.access.core.FolderCollection")) 
                {
                    _this.editable = true;
                }
                else if( instanceOf(type,"biouml.model.Module"))
                {
                    var parentDC = getDataCollection(getElementPath(path));
                    var moduleName = getElementName(path);
                    var protection = parentDC.getProtectionStatus(moduleName);
                    var permission = parentDC.getPermission(moduleName);
                    if( protection == 0 || (protection == 2 && permission & 0x04) )
                        _this.editable = true;
                }
                _this.currentBean = bean;
                _this.fieldNames = fieldNames;
                _this.openDescription(description);
            }
        });
    };
    
    this.openEditableDescription = function(description)
    {
        _this.editorDiv.css("height", "100%");
        _this.editorDiv.empty();
        var contentElement = $('<input id="description_content" name="description_content" value="' + description.replace(/"/g, '\'') + '" style="display:none" />');
        _this.editorDiv.append(contentElement);
        var editor = $('<iframe id="editor_frame" src="FCKEditor/editor/fckeditor.html?InstanceName=description_content&amp;Toolbar=BE_Basic" width="100%" height="100%" frameborder="0" scrolling="no"></iframe>');
        _this.editorDiv.append(editor);
    };
    
    this.openDescription = function(description)
    {
        this.description = description;
        //this.viewerDiv.html(resources.vpDescriptionCollectionTitle.replace("{collection}", this.path.escapeHTML()) +'<br/>');
        if(description == "")
        {
            if(this.editable)
                this.viewerDiv.html(resources.vpDescriptionPrompt+"<br/>");
            else
                this.viewerDiv.html(resources.vpDescriptionNotAvailable+"<br/>");
        } else this.viewerDiv.html("");
        this.viewerDiv.append(elementFromHTML(description));
        setToolbarButtonEnabled(this.editAction, this.editable);
    };
    
    this.saveActionClick = function()
    {
        if (this.editMode) 
        {
            var oEditor = FCKeditorAPI.GetInstance('description_content');
            this.description = oEditor.GetHTML();
        }
        var prop = this.currentBean;
        for(var i = 0; i < this.fieldNames.length - 1; i++)
        {
            if (prop) 
            {
                var p = prop.getProperty(this.fieldNames[i]);
                if (p && p.getValue()) 
                    prop = p.getValue();
                else 
                    prop = null;
            }    
        }
        if(prop)
        {
            prop.getProperty(this.fieldNames[this.fieldNames.length - 1]).setValue(this.description);
            var dc = getDataCollection(this.path);
            dc.setBean(this.currentBean, function(){
                showElementInfo(_this.path);
                _this.editActionClick();
            });
        }
    };
    
    this.editActionClick = function()
    {
        if(!_this.editable) return;
        this.viewerDiv.toggle();
        this.editorDiv.toggle();
        this.editMode = !this.editMode;
        if (this.editMode) 
        {
            this.openEditableDescription(this.description);
        }
        else 
        {
            var oEditor = FCKeditorAPI.GetInstance('description_content');
            this.openDescription(oEditor.GetHTML());
        }
    };
    
    this.initView = function()
    {
        this.viewerDiv.html("");
        this.editorDiv.html("");
        this.viewerDiv.show();
        this.editorDiv.hide();
        this.editMode = false;
    };
}

/*
 * Search results view part class
 */
function SearchResultViewPart()
{
    this.tabId = "search.results";
    this.tabName = resources.vpSearchTitle;
    this.visible = false;
    this.tabDiv;
    this.fullMode = false;
    this.values = null;
    this.filter = null;
    
    var _this = this;
    
    /*
     * Create div for view part
     */
    this.init = function()
    {
        this.tabDiv = createViewPartContainer(this.tabId).addClass("viewPartTab");
        
        this.resultDiv = $('<div id="' + this.tabId + '_result"></div>');
        this.tabDiv.append(this.resultDiv);
        
        _.bindAll(this, _.functions(this));
    };
    
    /*
     * Indicates if view part is visible
     */
    this.isVisible = function(documentObject, callback)
    {
        callback(this.visible);
    };
    
    /*
     * Open document event handler
     */
    this.explore = function(documentObject)
    {
    };
    
    /*
     * Save function
     */
    this.save = function()
    {
        // nothing to do
    };
    
    /*
     * Set message to result pane
     */
    this.setMessage = function(message)
    {
        _this.resultDiv.empty();
        _this.resultDiv.append(message);
        _this.tableObj = null;
        _this.visible = true;
        _this.explore(null);
    };
    
    /*
     * Update search result data
     */
    this.updateData = function()
    {
        var tablePath = "beans/searchResult";
        queryBioUML("web/table/sceleton",
        {
            "de": tablePath,
            "read" : true
        }, 
        function(data)
        {
            _this.resultDiv.empty();
            var tableSceleton = $(data.values);
            _this.resultDiv.append(tableSceleton);
            _this.tableObj = _this.resultDiv.children("table");
            _this.tableObj.addClass("selectable_table");
            var params = 
            {
                    rnd: rnd(),
                    read: true,
                    de: tablePath
            };
            if(_this.filter)
            {
                params['filter'] = _this.filter;
            }
            var features = 
            {
                    "bProcessing": true,
                    "bServerSide": true,
                    "bFilter": false,
                    "sPaginationType": "full_numbers_no_ellipses",
                    "lengthMenu": [[30, 50, 100, 200, 500, 1000, -2], [30, 50, 100, 200, 500, "1000", "Custom..."]],
                    "pageLength": 50,
                    "sDom": "pfirlt",
                    "fnDrawCallback": function() {
                        _this.tableObj.find('.table_script_node').each(function() {
                            eval($(this).text());
                        }).remove();
                        if (_this.fullMode == false) 
                        {
                            _this.tableObj.find('tr').children('td').each(function()
                            {
                                var _this = $(this);
                                var links = _this.find('.dataElementLink');
                                if(links.length > 0)
                                {
                                    if(links.length > 2)
                                    {
                                        links.slice(2).remove();
                                        links.parent().append("<span>...</span>");
                                    }
                                } else
                                {
                                    var text = _this.text();
                                    if (text.length > 40) 
                                    {
                                        //dirty workaround for cells containing html tags
                                        _this.html(_this.html().replace(/<br>+/g, " "));
                                        var innerText = _this.text();
                                        var firstElem = _this.children(".cellControl");
                                        _this.html(firstElem);
                                        fitElement(firstElem, innerText, true);
                                    }
                                }
                            });
                        }
                    },
                    "fnRowCallback": function( nRow, aData, iDisplayIndex ) 
                    {
                        var cols = $(nRow).children();
                        return nRow;
                    },
                    "sAjaxSource": appInfo.serverPath+"web/table/datatables?"+toURI(params)
            };
            _this.tableObj.dataTable(features);
            _this.tableObj.children("tbody").on("click", "tr", function() {
              var path = $(this).find(".dataElementLink").attr("data-path");
              if(path)
              {
                openBranch(path, false);
                showElementInfo(path);
              }
            });
            _this.tableObj.css('width', '100%');
        }, function(data)
        {
            _this.setMessage(resources.vpSearchErrorCannotLoad);
        });
    };
    
    /*
     * Creates toolbar actions for this tab
     */
    this.initActions = function(toolbarBlock)
    {
        this.applyAction = createToolbarButton(resources.vpSearchButtonAdd, "applyLayout2.gif", this.applyActionClick);
        toolbarBlock.append(this.applyAction);
        
        this.fullModeAction = createToolbarButton(resources.vpSearchButtonFullMode, "fullMode.gif", this.fullModeActionClick);
        toolbarBlock.append(this.fullModeAction);
        
        this.exportAction = createToolbarButton(resources.vpSearchButtonExport, "export.gif", this.openExportDialog);
        toolbarBlock.append(this.exportAction);
        
        this.saveTableAction = createToolbarButton(resources.vpSearchButtonSaveTable, "table.gif", this.saveTable);
        toolbarBlock.append(this.saveTableAction);
        
        this.copyToClipboardAction = createToolbarButton(resources.vpSearchButtonCopyClipboard, "addClipboard.gif", this.copyToClipboardActionClick);
        toolbarBlock.append(this.copyToClipboardAction);
        
        this.hideViewpartAction = createToolbarButton(resources.vpSearchHideViewpart, "closesearch.png", this.hideActionClick);
        toolbarBlock.append(this.hideViewpartAction);
    };
    
    this.saveTable = function()
    {
        if(_this.tableObj == null)
        {
            logger.error(resources.vpSearchNoResults);
            return;
        }
        createBeanEditorDialog(resources.vpSearchSaveTableDialogTitle, "properties/luceneSearch/saveTable", function(res) {
            var path = findDPSValue(res, 'table');
            var params = 
            {
                json: res,
            }
            if(_this.filter)
            {
                params.filter = _this.filter;
            }
            queryBioUML("web/lucene/save", params, function()
            {
                refreshTreeBranch(getElementPath(path));
                openDocument(path);
            });
        }, true);
    };
    
    this.openExportDialog = function()
    {
        if(_this.tableObj == null)
        {
            logger.error(resources.vpSearchNoResults);
            return;
        }
        $.chainclude(
            {
                'lib/export.js':function(){
                    exportElement("beans/searchResult", "Table");
                }
            }
        );
    };
    
    this.applyActionClick = function()
    {
        var elements = "";
        _this.tableObj.find('tr.row_selected').each( function (event) 
        {
            elements+=$(this).find('.dataElementLink').attr("data-path")+"\n";
        });
        if (elements != "") 
        {
            if ((activeDocumentId != null) && (opennedDocuments[activeDocumentId] != null) && 
                (opennedDocuments[activeDocumentId] instanceof Diagram || opennedDocuments[activeDocumentId] instanceof ComplexDocument)) 
            {
                _this.addElements(opennedDocuments[activeDocumentId], elements);
            }
            else 
            {
                _this.addElements(null, elements);
            }
        }
        else 
        {
            logger.message(resources.vpSearchErrorNoSelection);
        }
    };
    
    this.addElements = function(diagram, elements)
    {
        if (diagram == null) 
        {
            createSaveElementDialog(resources.vpGraphSearchNewDiagramTitle, "biouml.model.Diagram", null, function(diagramPath)
            {
                var path = getElementPath(diagramPath);
                var diagramName = getElementName(diagramPath);
                var dc = getDataCollection(getElementPath(elements.split("\n")[0]));
                dc.getDiagramTypes(function(types) {
                    createNewDiagram(path, diagramName, function(name, type) {
                        _this.addElementsToDiagram(name, elements, function(data) {
                            openDiagram(name);
                        });
                    }, types);
                });
            });
        }
        else 
        {
            _this.addElementsToDiagram(diagram.completeName, elements, function(data)
            {
                if (diagram.selector) 
                {
                    diagram.selector.mouseEvent = null;
                    diagram.selector.clear();
                }
                diagram.update(data, true);
            });
        }
    };
    
    this.addElementsToDiagram = function(diagramName, elements, callback)
    {
        queryBioUML("web/diagram/add_elements", 
        {
            de: diagramName,
            elements: elements,
            resptype: "json"
        }, function(data)
        {
            if(callback)
                callback(data);
        });
    };
    
    this.fullModeActionClick = function()
    {
        if (_this.fullMode == true) 
        {
            _this.fullModeAction.children('span').css('background-image', 'url(icons/fullMode.gif)');
            _this.fullMode = false;
        }
        else 
        {
            _this.fullModeAction.children('span').css('background-image', 'url(icons/fullMode2.gif)');
            _this.fullMode = true;
        }
        _this.updateData();
    };
    
    this.copyToClipboardActionClick = function()
    {
          var clipboard = lookForViewPart('common.clipboard');
        if (clipboard != null) 
        {
            _this.tableObj.find('tr.row_selected').each( function (event) 
            {
                clipboard.addElement($(this).find('.dataElementLink').attr("data-path"));
            });
        }
    };
    
    this.hideActionClick = function()
    {
        this.resultDiv.empty();
        this.visible = false;
        updateViewParts();
    };
}

/*
 * Script document output view part class
 */
function ScriptOutputViewPart()
{
    this.tabId = "script.output";
    this.tabName = resources.vpScriptOutputTitle;
    this.tabDiv;
    
    /*
     * Create div for view part
     */
    this.init = function()
    {
        this.tabDiv = createViewPartContainer(this.tabId);
        this.containerDiv = $('<div id="' + this.tabId + '_container" class="viewPartTab"></div>');
        this.tabDiv.append(this.containerDiv);
    };
    
    /*
     * Indicates if view part is visible
     */
    this.isVisible = function(documentObject, callback)
    {
        if ((documentObject != null) && (documentObject instanceof ScriptDocument)) 
        {
            callback(true);
        }
        else 
        {
            callback(false);
        }
    };
    
    /*
     * Open document event handler
     */
    this.explore = function(documentObject)
    {
        if ((documentObject != null) && (documentObject instanceof ScriptDocument)) 
        {
            this.currentScript = documentObject;
            this.currentScript.addExecuteListener(this);
            
            // update tab size
            resizeDocumentsTabs();
        }
    };
    
    /*
     * Save function
     */
    this.save = function()
    {
        // nothing to do
    };
    
    this.executionStarted = function()
    {
        this.containerDiv.html("");
    };
    
    this.executionFinished = this.messageUpdated = function(info)
    {
        var content = info.values;
        if(Array.isArray(content))
            content = info.values.join("");
        var newContent = content.replace(/\n/g, '<br>').replace(/ /g, '&nbsp;')+(info.html != undefined?info.html.join(""):"");
        var oldContent = this.containerDiv.html();
        if(oldContent !== newContent)
        {
            this.containerDiv.html(newContent);
            selectViewPart("js.output");
        }
    };
}

/*
 * Clipboard view part class
 */
function ClipboardViewPart()
{
    this.tabId = "common.clipboard";
    this.tabName = resources.vpClipboardTitle;
    this.tabDiv;
    this.visible = true;
    
    this.activeElementId = null;
    
    var _this = this;
    
    /*
     * Create div for view part
     */
    this.init = function()
    {
        this.tabDiv = createViewPartContainer(this.tabId);
        this.clipTable = $('<table class="clipboard"><tr id="headerRow"><th>'+resources.vpClipboardHeaderPath+
                '</th><th>'+resources.vpClipboardHeaderName+'</th><th>'+resources.vpClipboardHeaderType+'</th></tr></table>');
        this.tabDiv.append(this.clipTable);
        
        _.bindAll(this, _.functions(this));
    };
    
    /*
     * Indicates if view part is visible
     */
    this.isVisible = function(documentObject, callback)
    {
        callback(_this.visible);
    };
    
    /*
     * Open document event handler
     */
    this.explore = function(documentObject)
    {
        var trs = this.clipTable.find('tr').click(function()
        {
            _this.clipTable.find('tr[id!="headerRow"]').css("background", "#fff");
            $(this).css("background", "#ccc");
            _this.activeElementId = $(this).attr("data-path");
        });
        createTreeItemDroppable(this.tabDiv, null, function(path)
            {
                _this.addElement(path);
            }
        );
    };
    
    /*
     * Save function
     */
    this.save = function()
    {
        // nothing to do
    };
    
    /*
     * Creates toolbar actions for this tab
     */
    this.initActions = function(toolbarBlock)
    {
        this.addAction = createToolbarButton(resources.vpClipboardButtonCopy, "addClipboard.gif", this.addActionClick);
        toolbarBlock.append(this.addAction);
        
        this.insertToDiagramAction = createToolbarButton(resources.vpClipboardButtonPaste, "insertClipboard.gif", this.insertToDiagramActionClick);
        toolbarBlock.append(this.insertToDiagramAction);
        
        this.removeAction = createToolbarButton(resources.vpClipboardButtonRemove, "removeClipboard.gif", this.removeActionClick);
        toolbarBlock.append(this.removeAction);
    };
    
    this.addElement = function(elementName)
    {
        var db = getElementPath(elementName);
        var id = getElementName(elementName);
        var dc = getDataCollection(db);
        var type = dc.getChildClass(id);
        
        var added = false;
        this.clipTable.find('tr[id!="headerRow"]').each(function()
        {
            if ($(this).attr('data-path') == elementName) 
            {
                added = true;
            }
        });
        if (added == false) 
        {
            var tr = $('<tr/>').attr('data-path', elementName);
            tr.append($('<td/>').attr("title", db).text(db.length>60?db.substring(0,60)+"...":db));
            var nameTD = $('<td/>').addClass("clipboardItemName").text(id).attr('data-path', elementName).css("background-image", getNodeIcon(dc, id));
            tr.append(nameTD);
            tr.append($('<td/>').text(type));
            createTreeItemDraggable(nameTD);
            addTreeItemContextMenu(nameTD);
            tr.click(function()
            {
                _this.clipTable.find('tr[id!="headerRow"]').css("background", "#fff");
                $(this).css("background", "#ccc");
                _this.activeElementId = $(this).attr("data-path");
                showElementInfo(_this.activeElementId);
            });
            this.clipTable.append(tr);
        }
        else 
        {
            logger.error(resources.vpClipboardErrorAlreadyAdded.replace("{name}", elementName.escapeHTML()));
        }
    };
    
    this.addActionClick = function()
    {
        var document = opennedDocuments[activeDocumentId];
        if(document != null && document.getSelection)
        {
            var selection = document.getSelection();
            if(selection)
            {
                for(var i=0; i<selection.length; i++)
                {
                    this.addElement(document.completeName+"/"+selection[i]);
                }
                return;
            }
        }
        if (BioUML.selection.lastSelected != null) 
        {
            this.addElement(BioUML.selection.lastSelected);
        }
    };
    
    this.insertToDiagramActionClick = function()
    {
        if (this.activeElementId != null) 
        {
            if ((activeDocumentId != null) && (opennedDocuments[activeDocumentId] != null) && (opennedDocuments[activeDocumentId] instanceof Diagram || opennedDocuments[activeDocumentId] instanceof ComplexDocument )) 
            {
                var diagram = opennedDocuments[activeDocumentId];
                diagram.selectControl(function(event)
                {
                    diagram.addDiagramElement(_this.activeElementId, event);
                    diagram.selectControl(function(event){return true;});
                    return false;
                });
            }
            else 
            {
                logger.message(resources.vpClipboardErrorNoDiagramToPaste);
            }
        }
        else 
        {
            logger.message(resources.vpClipboardErrorNoRowSelected);
        }
    };
    
    this.removeActionClick = function()
    {
        if (this.activeElementId != null) 
        {
            this.clipTable.find('tr[id!="headerRow"]').each(function()
            {
                if (_this.activeElementId == $(this).attr("data-path")) 
                {
                    $(this).remove();
                }
            });
            this.activeElementId = null;
        }
    };
}

/*
 * Graph search view part class
 */
function GraphSearchViewPart()
{
    this.tabId = "search.graph";
    this.tabName = resources.vpGraphSearchTitle;
    this.tabDiv;
    
    var _this = this;
    
    /*
     * Create div for view part
     */
    this.init = function()
    {
        this.tabDiv = createViewPartContainer(this.tabId);
        this.containerDiv = $('<div id="' + this.tabId + '_container" class="viewPartTab"></div>');
        this.tabDiv.append(this.containerDiv);
        
        this.paneTable = $('<table border=0 width="100%"><tr><td valign="top" width="550px" id="left_1"></td><td valign="top" id="right_1"></td></tr></table>');
        this.containerDiv.append(this.paneTable);
        
        this.paneTable.find("#left_1").html("<div>"+resources.vpGraphSearchLoadingTable+"</div>");
        this.paneTable.find("#right_1").html("<div>"+resources.vpGraphSearchLoadingProperties+"</div>");
        
        this.elementsTable = $('<table class="clipboard"><tr id="headerRow">'+
                '<th><input id="add_all" type="checkbox" checked/>'+resources.vpGraphSearchHeaderAdd+'</th>'+
                '<th><input id="use_all" type="checkbox" checked/>'+resources.vpGraphSearchHeaderUse+'</th>'+
                '<th>'+resources.vpGraphSearchHeaderDatabase+'</th>'+
                '<th>'+resources.vpGraphSearchHeaderID+'</th>'+
                '<th>'+resources.vpGraphSearchHeaderTitle+'</th>'+
                '<th>'+resources.vpGraphSearchHeaderType+'</th>'+
                '<th>'+resources.vpGraphSearchHeaderLinkedFrom+'</th></tr></table>');
        
        this.paneTable.find("#left_1").empty();
        this.paneTable.find("#left_1").append(this.elementsTable);
        
        this.paneTable.find("#right_1").empty();
        this.progressbar = $('<div></div>');
        this.progressbar.progressbar(
        {
            value: 0
        });
        this.paneTable.find("#right_1").append(this.progressbar);
        this.propertyInspector = $('<div id="' + this.tabId + '_pi"></div>');
        this.paneTable.find("#right_1").append(this.propertyInspector);
        
        createTreeItemDroppable(this.tabDiv, "biouml.standard.type.Base", function(path)
            {
                _this.addKernelElement(path);
                setToolbarButtonEnabled(_this.applyAction, _this.applyToNewAction, true);
            }
        );
        _.bindAll(this, _.functions(this));
    };
    
    /*
     * Indicates if view part is visible
     */
    this.isVisible = function(documentObject, callback)
    {
        callback(true);
    };
    
    /*
     * Open document event handler
     */
    this.explore = function(documentObject)
    {
        if (!this.propertyPane) 
        {
            queryBioUML("web/bean/get", 
            {
                de: "properties/graphSearch/properties"
            }, function(data)
            {
                _this.propertiesData = data.values;
                _this.openProperties();
            });
        }
        else 
        {
            // update properties
            var dps = _this.propertyPane.getModel();
            _this.updateProperties(dps);
            _this.progressbar.progressbar(
            {
                value: 0
            });
        }
    };
    
    /*
     * Save function
     */
    this.save = function()
    {
        // nothing to do
    };
    
    this.openProperties = function()
    {
        _this.propertyInspector.empty();
        var beanDPS = convertJSONToDPS(_this.propertiesData);
        _this.propertyPane = new JSPropertyInspector();
        _this.propertyPane.setParentNodeId(_this.propertyInspector.attr('id'));
        _this.propertyPane.setModel(beanDPS);
        _this.propertyPane.generate();
    };
    
    this.updateProperties = function(dps)
    {
        _this.propertyInspector.empty();
        _this.propertyPane = new JSPropertyInspector();
        _this.propertyPane.setParentNodeId(_this.propertyInspector.attr('id'));
        _this.propertyPane.setModel(dps);
        _this.propertyPane.generate();
    };
    
    /*
     * Creates toolbar actions for this tab
     */
    this.initActions = function(toolbarBlock)
    {
        this.addAction = createToolbarButton(resources.vpGraphSearchButtonAdd, "icon_plus.gif", this.addActionClick);
        toolbarBlock.append(this.addAction);
        
        this.searchAction = createToolbarButton(resources.vpGraphSearchButtonStart, "search.gif", this.searchActionClick);
        toolbarBlock.append(this.searchAction);
        
        this.clearAction = createToolbarButton(resources.vpGraphSearchButtonClear, "removeClipboard.gif", this.clearActionClick);
        toolbarBlock.append(this.clearAction);
        
        this.applyAction = createToolbarButton(resources.vpGraphSearchButtonPaste, "applyLayout2.gif", this.applyActionClick);
        toolbarBlock.append(this.applyAction);
            
        this.applyToNewAction = createToolbarButton(resources.vpGraphSearchButtonPasteToNew, "leaf.gif", this.applyToNewActionClick);
        toolbarBlock.append(this.applyToNewAction);
        
        var elements = this.getSelectedElements();
        if (elements.length == 0) 
        {
            setToolbarButtonEnabled(this.applyAction, this.applyToNewAction, false);
        }
        else 
        {
            if ((activeDocumentId == null) || (opennedDocuments[activeDocumentId] == null) ||
            !((opennedDocuments[activeDocumentId] instanceof Diagram || opennedDocuments[activeDocumentId] instanceof ComplexDocument))) 
            {
                setToolbarButtonEnabled(this.applyAction, false);
            }
        }
        this.elementsTable.find("#add_all").click(function()
        {
            var ch = $(this).is(':checked');
            _this.elementsTable.find('tr[id!="headerRow"]').each(function()
            {
                $(this).find('input[id="add"]').attr('checked', ch);
            });
        });
        this.elementsTable.find("#use_all").click(function()
        {
            var ch = $(this).is(':checked');
            _this.elementsTable.find('tr[id!="headerRow"]').each(function()
            {
                $(this).find('input[id="use"]').attr('checked', ch);
            });
        });
    };
    
    this.addActionClick = function()
    {
        if (activeDocumentId && opennedDocuments[activeDocumentId] && opennedDocuments[activeDocumentId].selector && opennedDocuments[activeDocumentId].selector.nodes) 
        {
            //multiple select
            this.addDiagramElement($.toJSON(opennedDocuments[activeDocumentId].selector.nodes), true);
        }
        else if (BioUML.selection.lastSelected != null && opennedDocuments[activeDocumentId] && opennedDocuments[activeDocumentId].completeName != BioUML.selection.lastSelected) 
        {
            this.addDiagramElement($.toJSON([BioUML.selection.lastSelected]), true);
        }
        else
        {
            logger.error(resources.vpGraphSearchErrorNoElementSelected);
        }
        
    };
    
    this.addKernelElement = function(path)
    {
        getDataCollection(getElementPath(path)).getElementInfo(getElementName(path), function(info)
        {
            var title = info.title;
            if(title == undefined) title = info.name;
            _this.addElement(path, true, -1, "", 0, "", title,"");
        });
    };
    
    this.addDiagramElement = function(elements, safeMode)
    {
        if ((activeDocumentId != null) && (opennedDocuments[activeDocumentId] != null)) 
        {
            queryBioUML("web/diagram/get_base", 
            {
                de: opennedDocuments[activeDocumentId].completeName,
                path: elements
            }, function(data)
            {
                for(var i in data.values)
                {
                    _this.addElement(data.values[i].name, safeMode, -1, "", 0, "", data.values[i].title, "");
                }
                setToolbarButtonEnabled(_this.applyAction, _this.applyToNewAction, true);
            });
        }
    };
    
    this.addElement = function(completeName, safeMode, linkedDirection, linkedFromPath, linkedLength, relationType, title, classPath)
    {
        var dc = getDataCollection(getElementPath(completeName));
        var id = completeName;
        var db = 'Unknown';
        if (dc != null)
        {
           db = getElementName(dc.getDatabaseName());
           id = getElementName(completeName);
        }
        
        var className = classPath;
        if (className == "")
        {
            className = getElementClass(completeName);
        }
        var type = className;
        var ind = type.lastIndexOf('.');
        if(ind != -1)
        {
            type = type.substring(ind+1, type.length);
        }
        
        var added = false;
        this.elementsTable.find('tr[id!="headerRow"]').each(function()
        {
            if ($(this).attr('id') == completeName && $(this).find('#linkedFromPath').val() == linkedFromPath) 
            {
                added = true;
            }
        });
        if (added == false) 
        {
            // check target database
            if (safeMode == true) 
            {
                var properties = _this.propertyPane.getModel();
                var dbList = properties.getProperty('targetOptions').getValue().getProperty('collections').getValue();
                var isDatabaseFound = false;
                for (var i = 0; i < dbList.length; i++) 
                {
                    if (dbList[i].getProperty('name').getValue() == db) 
                    {
                        isDatabaseFound = true;
                        var property = dbList[i].getProperty('queryEngineNames');
                        var dictionaryName = property.getAttribute("dictionaryName");
                        var dictionaryValues = dictionaries.getDictionary(dictionaryName);
                        var propValue = [];
                        if(dictionaryValues.getKeys().length > 0)
                        {
                            propValue.push(dictionaryValues.getKeys()[0]);
                        }
                        property.setValue(propValue);
                        _this.updateProperties(properties);
                    }
                }
                if(!isDatabaseFound)
                {
                    logger.error(resources.vpGraphSearchErrorNoDatabase);
                    return;
                }
            }
            
            var tr = $('<tr id="' + completeName + '"><td><input id="add" type="checkbox" checked/></td>' +
                '<td><input id="use" type="checkbox" checked/></td><td>' + db + '&nbsp;</td><td>' + id + '&nbsp;</td>'+
                '<td>' + title + '&nbsp;</td><td>' + type + '&nbsp;<input id="className" type="hidden" value="'+className+'"/></td><td>' + getElementName(linkedFromPath) + '&nbsp;<input id="linkedFromPath" type="hidden" value="' + linkedFromPath + '"/><input id="linkedDirection" type="hidden" value="' + linkedDirection + '"/><input id="linkedLength" type="hidden" value="' + linkedLength + '"/><input id="relationType" type="hidden" value="' + relationType + '"/></td></tr>');
            tr.click(function()
            {
                _this.elementsTable.find('tr[id!="headerRow"]').css("background", "#fff");
                $(this).css("background", "#ccc");
            });
            this.elementsTable.append(tr);
        }
        else 
        {
            if (safeMode == true) 
            {
                logger.error(resources.vpGraphSearchErrorAlreadyAdded);
            }
            else 
            {
                var tr = this.elementsTable.find('tr[id="' + completeName + '"]');
                tr.find('input[id="linkedDirection"]').attr('value', linkedDirection);
                tr.find('input[id="linkedFromPath"]').attr('value', linkedFromPath);
                tr.find('input[id="linkedLength"]').attr('value', linkedLength);
                tr.find('input[id="relationType"]').attr('value', relationType);
            }
        }
    };
    
    this.searchActionClick = function()
    {
        var elements = "";
        var total = 0;
        this.elementsTable.find('tr[id!="headerRow"]').each(function()
        {
            if ($(this).find('input[id="use"]').attr('checked')) 
            {
                elements = elements + $(this).attr('id') + ";";
            }
            total++;
        });
        if (elements.length > 0) 
        {
            _this.progressbar.progressbar('value', 0);
            var dps = _this.propertyPane.getModel();
            var json = convertDPSToJSON(dps);
            queryService("graphsearch.service", 501, 
            {
                options: json,
                elements: elements
            }, function(data)
            {
                _this.currentProcess = data.values;
                lookForViewPart('search.graph').onProcessTimer();
            });
        }
        else 
        {
            if(total > 0)
                logger.error(resources.vpGraphSearchErrorNoInput.replace("{action}" ,"Use"));
            else
                logger.error(resources.vpGraphSearchErrorNoList);
        }
    };
    
    /*
     * Callback function for process timer
     */
    this.onProcessTimer = function()
    {
        queryService("graphsearch.service", 503, 
        {
            id: this.currentProcess
        }, function(data)
        {
            var params = data.values.split(':');
            if (params[0] < 3) 
            {
                _this.progressbar.progressbar('value', params[1]);
                setTimeout(function() {lookForViewPart('search.graph').onProcessTimer();}, 2000);
            }
            else if (params[0] == 3) 
            {
                _this.loadSearchResults();
                _this.progressbar.progressbar('value', 100);
            }
            else 
            {
                logger.error(resources.vpGraphSearchErrorGeneral);
            }
        });
    };
    
    this.loadSearchResults = function()
    {
        queryService("graphsearch.service", 504,
        {
            id: this.currentProcess
        }, function(data)
        {
            if(data.values === "")
            {
                logger.error(resources.vpGraphSearchErrorNoResult);
                return;
            }
            var resultStr = data.values.split('\n');
            for (var ri = 0; ri < resultStr.length; ri++) 
            {
                if (resultStr[ri].length > 0) 
                {
                    var resultParams = resultStr[ri].split(';');
                    var elementCompleteName = resultParams[0];
                    _this.addElement(elementCompleteName, false, resultParams[1], resultParams[2], resultParams[3], resultParams[4], resultParams[5], resultParams[6]);
                }
            }
        });
    };
    
    this.clearActionClick = function()
    {
        this.elementsTable.find('tr[id!="headerRow"]').each(function()
        {
            $(this).remove();
        });
        this.clearDatabaseSelection();
        setToolbarButtonEnabled(this.applyAction, this.applyToNewAction, false);
    };
    
    this.applyActionClick = function()
    {
        var elements = this.getSelectedElements();
        var elemStr = "";
        if(elements.length > 0)
        {
            elemStr = elements.join("\n");
            if ((activeDocumentId != null) && (opennedDocuments[activeDocumentId] != null) && 
                (opennedDocuments[activeDocumentId] instanceof Diagram || opennedDocuments[activeDocumentId] instanceof ComplexDocument)) 
            {
                _this.addElements(opennedDocuments[activeDocumentId], elemStr);
            }
            else 
            {
                logger.error(resources.vpGraphSearchErrorNoDiagram);
            }
        }
        else
        {
            logger.error(resources.vpGraphSearchErrorNoInput.replace("{action}" ,"Add"));
        }
    };
    
    this.applyToNewActionClick = function()
    {
        if(this.applyToNewAction.hasClass("ui-state-disabled"))
            return false;
        var elements = this.getSelectedElements();
        var elemStr = "";
        if(elements.length > 0)
        {
            elemStr = elements.join("\n");
            _this.addElements(null, elemStr);
        }
        else
        {
            logger.error(resources.vpGraphSearchErrorNoInput.replace("{action}" ,"Add"));
        }
    };
    
    this.addElements = function(diagram, elements)
    {
        if (diagram == null) 
        {
            createSaveElementDialog(resources.vpSearchNewDiagramTitle, "biouml.model.Diagram", null, function(diagramPath)
            {
                var path = getElementPath(diagramPath);
                var diagramName = getElementName(diagramPath);
                
                var dc = getDataCollection(getElementPath(elements.split("\n")[0]));
                dc.getDiagramTypes(function(types) {
                    createNewDiagram(path, diagramName, function(name, type) {
                        _this.addElementsToDiagram(name, elements, function(data) {
                            openDiagram(name);
                        });
                    }, types);
                });
            });
        }
        else 
        {
            //_this.addElementsToDiagram(diagram, elements);
            _this.addElementsToDiagram(diagram.completeName, elements, function(data)
            {
                diagram.selector.clear();
                diagram.update(data, true);
            });
        }
    };
    
    this.addElementsToDiagram = function(diagramName, elements, callback)
    {
        queryBioUML("web/diagram/add_elements", 
        {
            de: diagramName,
            elements: elements,
            resptype: "json"
        }, function(data)
        {
            if(callback)
                callback(data);
        });
    };
    
    this.getSelectedElements = function()
    {
        var elements = new Array();
        this.elementsTable.find('tr[id!="headerRow"]').each(function()
        {
            if ($(this).find('input[id="add"]').attr('checked')) 
            {
                elements.push($(this).attr('id') + ";" + $(this).find('input[id="linkedDirection"]').attr('value') + ";" + $(this).find('input[id="linkedFromPath"]').attr('value') + ";" + $(this).find('input[id="linkedLength"]').attr('value') + ";" + $(this).find('input[id="relationType"]').attr('value') + ";"+ $(this).find('input[id="className"]').attr('value') + ";");
            }
        });
        return elements;
    };
    
    this.clearDatabaseSelection = function()
    {
        var properties = _this.propertyPane.getModel();
        var dbList = properties.getProperty('targetOptions').getValue().getProperty('collections').getValue();
        for (var i = 0; i < dbList.length; i++) 
        {
            var property = dbList[i].getProperty('queryEngineNames');
            property.setValue([]);
        }
        this.updateProperties(properties);
    };
}

function SQLEditorViewPart ()
{
    this.tabId = "sql.editor";
    this.tabName = resources.vpSQLTitle;
    this.tabDiv;
    this.visible = false;
    this.tablecols = null;
    this.tableData = null;
    this.loaded = false;
    this.history = [];
    
    var _this = this;
    
    /*
     * Create div for view part
     */
    this.init = function()
    {
        this.tabDiv = createViewPartContainer(this.tabId);
        this.loadTables();
    };
    
    /*
     * Indicates if view part is visible
     */
    this.isVisible = function(documentObject, callback)
    {
        if(this.loaded)
            callback(_this.visible)
        else
            this.loadTables(callback);
    };
    
    /*
     * Open document event handler
     */
    this.explore = function(documentObject)
    {
        this.initAreaActions();
    };
    
    /*
     * Save function
     */
    this.save = function()
    {
        // nothing to do
    };
    
    /*
     * Creates toolbar actions for this tab
     */
    this.initActions = function(toolbarBlock)
    {
        this.runAction = createToolbarButton(resources.vpSQLButtonRun, "simulate.gif");
        this.runAction.click(function()
        {
            _this.runQuery(_this.queryArea.val());
        });
        toolbarBlock.append(this.runAction);
        this.explainAction = createToolbarButton(resources.vpSQLButtonExplain, "about.gif");
        this.explainAction.click(function()
        {
            _this.runQuery(_this.queryArea.val(), "explain");
        });
        toolbarBlock.append(this.explainAction);
        this.clearAction = createToolbarButton(resources.vpSQLButtonClear, "leaf.gif");
        this.clearAction.click(function()
        {
            _this.queryArea.val("");
        });
        toolbarBlock.append(this.clearAction);
        this.updateAction = createToolbarButton(resources.vpSQLButtonReload, "redo.gif");
        this.updateAction.click(function()
        {
            _this.tableData = null;
            _this.loadTables();
            _this.initAreaActions();
        });
        toolbarBlock.append(this.updateAction);
    };
    
    this.loadTables = function(callback)
    {
        this.tabDiv.html(resources.commonLoading);
        if(this.tableData != null)
            this.showTables();
        else
        {
            abortQueries("sqleditor.service");

            queryBioUMLWatched("sqleditor.service", "web/data",
            {
                service: "sqleditor.service",
                command: 101
            }, function(data)
            {
                _this.tableData = $.evalJSON(data.values);
                _this.showTables();
                _this.initAreaActions();
                _this.visible = true;
                _this.loaded = true;
                if(callback)
                    callback(_this.visible);
            }, function(data)
            {
                _this.visible = false;
                _this.loaded = true;
                if(callback)
                    callback(_this.visible);
            });
        }
    };
    
    this.showTables = function()
    {
        if(this.tableData == null)
            return false;
        this.tabDiv.html("");
        
        // query
        this.queryArea = $('<textarea></textarea>')
            .attr("rows", 3)
            .attr("cols", 90);

        this.tabDiv.append('SQL query:<br/>')
            .append(this.queryArea)
            .append('<br/>');
        
        // tables and columns
        var tables = this.tableData;
        this.tableSelect = $('<select></select>')
            .attr("id", "table_select")
            .attr("size", "6");
        this.tablecols = {};
        $.each(tables, function(index, value) {
            _this.tablecols[index] = value;
        });
        _.each(_.keys(tables).sort(), function(value) {
            _this.tableSelect.append($('<option/>').val(value).text(value));
        });
            
        this.columnSelect = $('<select></select>')
            .attr("id", "column_select")
            .attr("size", "6")
            .append($('<option/>').text(resources.vpSQLSelectTable));
        
        // history
        var historySelect = $('<select></select>')
            .attr("id", "history_select")
            .attr("size", "6");
        this.historyArea = $('<div></div>')
            .append(resources.vpSQLHistory+'<br/>')
            .append(historySelect)
            .hide();
        if(this.history.length > 0)
        {
            $.each(this.history, function(index, value){
                historySelect.append($('<option/>').val(index).text(value));
            });
            this.historyArea.show();
        }
        
        var selectArea = $('<table></table>')
               .append($('<tr></tr>')
                    .append($('<td></td>').append(resources.vpSQLTables+'<br/>').append(this.tableSelect))
                    .append($('<td></td>').append(resources.vpSQLColumns+'<br/>').append(this.columnSelect))
                    .append($('<td></td>').append(this.historyArea)));
            
        this.tabDiv.append(selectArea);
    };
    
    this.initAreaActions = function ()
    {
        if(this.tableData == null)
            return false;
        this.tableSelect
            .unbind('change')
            .unbind('dblclick')
            .change(function()
            {
                var tableId = _this.tableSelect.val();
                _this.columnSelect.children().remove();
                _.each(_this.tablecols[tableId], function(column){
                    var colname = column["name"];
                    var coltype = column["type"];
                    _this.columnSelect.append($('<option/>').val(colname).text(colname+" - "+coltype));
                });
            })
            .dblclick(function()
            {
                var tableId = _this.tableSelect.val();
                _this.queryArea.val("SELECT * FROM "+tableId);
            });
        this.columnSelect
            .unbind('dblclick')
            .dblclick(function()
            {
                if(!_this.queryArea.val().length)
                {
                    return;
                }
                var oldValue = _this.queryArea.val();
                var columnName = " " + _this.columnSelect.val() + " ";
                if (document.selection) 
                {
                    _this.queryArea.focus();
                    var sel = document.selection.createRange();
                    sel.text = columnName;
                    _this.queryArea.focus();
                }
                else if (_this.queryArea.attr("selectionStart"))
                {
                    var startPos = _this.queryArea.attr("selectionStart");
                    var endPos = _this.queryArea.attr("selectionEnd");
                    var scrollTop = _this.queryArea.attr("scrollTop");
                    _this.queryArea.val(oldValue.substring(0, startPos)+columnName+oldValue.substring(endPos,oldValue.length));
                    _this.queryArea.focus();
                    _this.queryArea.attr("selectionStart", startPos + columnName.length);
                    _this.queryArea.attr("selectionEnd", startPos + columnName.length);
                    _this.queryArea.attr("scrollTop", scrollTop);
                    _this.queryArea.focus();
                } 
                else
                {
                    _this.queryArea.val(oldValue + columnName);
                    _this.queryArea.focus();
                }
            });
        this.historyArea.children('select')
            .unbind('dblclick')
            .dblclick(function()
            {
                var ind = this.value;
                _this.queryArea.val(_this.history[ind]);
            });
    };
    
    this.runQuery = function(query, explain)
    {
        if(explain)
            query = "EXPLAIN " + query;
        var params = {
                        query: query,
                        read: true
                     };
        abortQueries("sql.editor.query");
        queryBioUMLWatched("sql.editor.query", "web/table/sceleton", params, function(data)
        {
            openNonTreeDocument('sql_query_table', 'sql_result', 'SQL query result', 'sql.editor');
            $('#sql_query_table').html(data.values);
            _this.table = $('#sql_query_table').children("table");
            
            var features = 
            {
                "bProcessing": true,
                "bServerSide": true,
                "bFilter": false,
                "bSort" : false,
                "sPaginationType": "input",
                "lengthMenu": [[30, 50, 100, 200, 500, 1000, -2], [30, 50, 100, 200, 500, "1000", "Custom..."]],
                "pageLength": 50,
                "sDom": "pfrlti",
                "sAjaxSource": appInfo.serverPath+"web/table/datatables?" + toURI(params)
            };
            _this.table.dataTable(features);
            _this.table.fnSetColumnVis( 0, false);
            if(explain == undefined)
            {
                _this.history.push(query);
                _this.historyArea.children('#history_select')
                    .append($('<option/>').val(_this.history.length-1).text(query));
                _this.historyArea.show();
            }
        }, function(data)
        {
            $('#sql_query_table').html("");
            logger.error(data.message);
        });
    };
}

/*
 * Tasks view parts
 */
function TasksViewPart()
{
    this.tabId = "common.tasks";
    this.tabName = resources.vpTasksTitle;
    this.tabDiv;
    this.autoupdate = false;
    this.serverLoadUpdateCounter = 0;
    
    var _this = this;
    
    /*
     * Create div for view part
     */
    this.init = function()
    {
        this.tabDiv = createViewPartContainer(this.tabId);
        this.containerDiv = $('<div id="' + this.tabId + '_container" class="viewPartTab"></div>');
        this.tabDiv.append(this.containerDiv);
        
        this.sysLoadInfo = $('<div/>');
        this.containerDiv.append(this.sysLoadInfo);
        
        this.table = $('<div>'+resources.commonLoading+'</div>');
        this.containerDiv.append(this.table);
        this.loadTable();
        
        _.bindAll(this, _.functions(this));
    };
    
    /*
     * Indicates if view part is visible
     */
    this.isVisible = function(documentObject, callback)
    {
        if (currentUser != null) 
        {
            callback(true);
        }
        else 
        {
            callback(false);
        }
    };
    
    /*
     * Open document event handler
     */
    this.explore = function(documentObject)
    {
    };
    
    this.loadTable = function()
    {
        this.table.html('<div>'+resources.commonLoading+'</div>');
        queryBioUML("web/table/sceleton",
        {
            "add_row_id": 1,
            "de": "task/userTasks"
        }, 
        function(data)
        {
            _this.table.html(data.values);
            _this.tableObj = _this.table.children("table");
            _this.tableObj.addClass('selectable_table');
            const active = (status) => status && ( status.indexOf("Running") != -1 || status.indexOf("Created") != -1 ); 
            var features = 
            {
                "bProcessing": true,
                "bServerSide": true,
                "bFilter": false,
                "pagingType": "full_numbers_no_ellipses",
                "lengthMenu": [[30, 50, 100, 200, 500, 1000, -2], [30, 50, 100, 200, 500, "1000", "Custom..."]],
                "pageLength": 50,
                "sDom": "pfrlti",
                "aaSorting": [[ 2, "desc" ]],
                "sAjaxSource": appInfo.serverPath+"web/table/datatables?de=task/userTasks&add_row_id=1" 
                + "&rnd=" + rnd(),
                "fnDrawCallback": function(oSettings)
                {
                    if(_this.timeOut) clearTimeout(_this.timeOut);
                    var statuses = getTableColumnValues(_this.dataTableObj, "Status");
                    var timerPeriod = 3000;
                    if(!statuses.some(active))
                        timerPeriod = 10000;
                    _this.timeOut = setTimeout(_this.onProcessTimer, timerPeriod);
                }
            };
            _this.dataTableObj = _this.tableObj.dataTable(features);
            _this.tableObj.css('width', '100%');
        }, function(data)
        {
            _this.table.html(resources.commonErrorViewpartUnavailable);
            _this.tableObj = null;
            logger.error(data.message);
        });
    };
    
    /*
     * Called when task view part opened
     */
    this.show = function(documentObject)
    {
        this.autoupdate = true;
        this.serverLoadUpdateCounter = 0;
        lookForViewPart('common.tasks').onProcessTimer();
    };
    
    /*
     * Save function
     */
    this.save = function()
    {
        this.autoupdate = false;
        if(this.timeOut) clearTimeout(this.timeOut);
    };
    
    this.onProcessTimer = function()
    {
        if(this.autoupdate == true)
        {
            if(this.serverLoadUpdateCounter <= 0)
            {
                queryBean("system/serverLoad", {}, function(data) 
                {
                    var dps = convertJSONToDPS(data.values);
                    var threads = dps.getValue("runningThreads");
                    var loadAvg = dps.getValue("loadAverage");
                    var msgs = [];
                    if(threads != undefined) msgs.push("Running threads: "+threads);
                    if(loadAvg != undefined) msgs.push("Server load: "+parseFloat(loadAvg).toFixed(2)+"%");
                    _this.sysLoadInfo.text("System info: "+msgs.join("; "));
                }, function() {});
                this.serverLoadUpdateCounter = 10;
            } else this.serverLoadUpdateCounter--;
            // update table
            if(this.dataTableObj)
            {
                this.dataTableObj.fnDraw();
            }
        }
    };
    
    /*
     * Creates toolbar actions for this tab
     */
    this.initActions = function(toolbarBlock)
    {
        this.stopAction = createToolbarButton(resources.vpTasksButtonStop, "stopTask.gif");
        this.stopAction.click(function()
        {
            _this.applyToSelectedTasks("stop_rows");
        });
        toolbarBlock.append(this.stopAction);
        this.removeAction = createToolbarButton(resources.vpTasksButtonRemove, "remove.gif");
        this.removeAction.click(function()
        {
            _this.applyToSelectedTasks("remove_rows");
        });
        toolbarBlock.append(this.removeAction);
    };
    
    this.applyToSelectedTasks = function(action)
    {
        var selectedRows = getTableSelectedRowIds(_this.tableObj);
        var selectedStr;
        if(selectedRows.length>0)
        {
            selectedStr = selectedRows[0];
            for(var i=1; i<selectedRows.length; i++)
            {
                selectedStr+=","+selectedRows[i];
            }
        }
        queryBioUML("web/tasks/"+action,
        {
            "rows": selectedStr
        }, function(){});
    };
}

/*
 * Diagram simulation view part
 */
function DiagramSimulationViewPart()
{
    this.tabId = "diagram.simulation";
    this.tabName = resources.vpSimulationTitle + " old";
    this.tabDiv;
    this.currentObject = null;
    this.data = null;

    var n = 1;

    var _this = this;

    /*
     * Create div for view part
     */
    this.init = function()
    {
        this.tabDiv = createViewPartContainer(this.tabId);
        this.containerDiv = $('<div id="' + this.tabId + '_container" class="viewPartTab"></div>');
        this.tabDiv.append(this.containerDiv);
        
        this.propertyInspector = $('<div id="' + this.tabId + '_pi"></div>').css({"width":"55%", "float":"left"});
        this.containerDiv.append(this.propertyInspector);
        this.mainLogDiv = $('<div id="' + this.tabId + '_log"></div>').css({"width":"40%", "float":"left", "min-height":"300px", "border":"1px solid #ccc", "margin-left":"10px", "padding":"5px"});
        this.containerDiv.append(this.mainLogDiv);
        this.mainLogDiv.hide();
    };

    /*
     * Indicates if view part is visible
     */
    this.isVisible = function(documentObject, callback)
    {
        if ((documentObject != null) && (documentObject instanceof Diagram || documentObject instanceof ComplexDocument )) 
        {
            if( documentObject.getDiagram().getModelObjectName() != null)
            {
                callback(true);
            }
            else
            {
                callback(false);    
            }
        }
        else 
        {
            callback(false);
        }
    };

    /*
     * Open document event handler
     */
    this.explore = function(documentObject)
    {
        if ((documentObject != null) && (documentObject instanceof Diagram || documentObject instanceof ComplexDocument) ) 
        {
            if(this.currentObject != documentObject.getDiagram())
            {
                this.currentObject = documentObject.getDiagram();
                this.loadModel();
                this.currentProcess = undefined;
            }
        }
        else 
        {
            this.currentObject = null;
            this.tabDiv.html(resources.commonErrorViewpartUnavailable);
        }
    };

    /*
     * Save function
     */
    this.save = function()
    {
    };

    this.show = function()
    {
        this.loadModel();
    };

    this.loadModel = function()
    {
        this.currentObject.getModelObjectName(function(modelName)
        {
            if (modelName != null) 
            {
                queryBioUML("web/bean/get", 
                {
                    de: modelName
                }, function(data)
                {
                    _this.tabDiv.empty().append(_this.containerDiv);
                    _this.data = data;
                    _this.initFromJson(data);
                }, function(data)
                {
                    _this.tabDiv.html(resources.commonErrorViewpartUnavailable);
                });
            }
            else
            {
                _this.tabDiv.html(resources.commonErrorViewpartUnavailable);
            }   
        });
    };

    this.initFromJson = function(data)
    {
        _this.propertyInspector.empty();
        var beanDPS = convertJSONToDPS(data.values);
        _this.propertyPane = new JSPropertyInspector();
        _this.propertyPane.setParentNodeId(_this.propertyInspector.attr('id'));
        _this.propertyPane.setModel(beanDPS);
        _this.propertyPane.generate();
        _this.propertyPane.addChangeListener(function(ctl,oldval,newval) {
            _this.propertyPane.updateModel();
            var json = convertDPSToJSON(_this.propertyPane.getModel(), ctl.getModel().getName());
            _this.setFromJson(json);
        });
    };

    this.setFromJson = function(json)
    {
        _this.currentObject.getModelObjectName(function(modelName)
        {
            queryBioUML("web/bean/set",
            {
                de: modelName,
                json: json
            }, function(data)
            {
                _this.initFromJson(data);
            });
        });
    };

    /*
     * Creates toolbar actions for this tab
     */
    this.initActions = function(toolbarBlock)
    {
        this.runAction = createToolbarButton(resources.vpSimulationButtonSimulate, "simulate.gif", this.runActionClick);
        toolbarBlock.append(this.runAction);
        
        this.stopAction = createDisabledToolbarButton(resources.vpSimulationButtonSimulate, "stopTask.gif", this.stopSimulation);
        toolbarBlock.append(this.stopAction);
        
        this.saveAction = createToolbarButton(resources.vpSimulationButtonSave, "save.gif", this.saveSimulatorOptions);
        toolbarBlock.append(this.saveAction);
        
        this.saveResultAction = createDisabledToolbarButton(resources.vpSimulationButtonSaveResult, "saveresult.png", this.saveSimulationResult);
        toolbarBlock.append(this.saveResultAction);
        
        this.adjustButtons(_this.running != undefined);
    };
    
    //method is for testing, not used from BioUML interface
    this.getSimulationResult = function()
    {
        queryBioUML("web/simulation/result", 
        {
            de: _this.currentProcess
        },
        function(data)
        {
            logger.message("Got result with fields " + Object.keys(data.values).join(', '));
        });
    }
    
    this.adjustButtons = function (isRuning)
    {
        setToolbarButtonEnabled(_this.runAction, !isRuning);
        setToolbarButtonEnabled(_this.saveResultAction, !isRuning && _this.currentProcess != undefined);
        setToolbarButtonEnabled(_this.stopAction, isRuning);
    };

    /*
     * Simulate button action handler
     */
    this.runActionClick = function()
    {
        _this.mainLogDiv.show();
        if (_this.propertyPane) 
        {
            var dps = _this.propertyPane.getModel();
            var json = convertDPSToJSON(dps);
            queryBioUML("web/simulation/simulate", 
            {
                de: _this.currentObject.completeName,
                engine: json
            }, function(data)
            {
                updateLog(_this.mainLogDiv,"");
                _this.adjustButtons(true);
                _this.currentProcess = data.values[0];
                var procId = getElementName(_this.currentProcess);
                const plotId = "plot_container_" + procId;
                const simStatusMsgId = "simulation_status_message_" + procId;
                _this.n = data.values[1];
                _this.dialogDiv = [];
                _this.dialogDiv[0] = $('<div title="'+_this.currentObject.name +' '+resources.vpSimulationResultTitle+'"></div>');
                
                
                var tabDivExt = $(
                        '<div>'+
                        '<div id="'+plotId+'" class="floating_container ui-tabs-panel ui-corner-bottom ui-widget-content">'+
                        '</div>'+
                        '</div>'
                        );
                
                tabDivExt.addClass( "ui-tabs ui-helper-clearfix" );
                tabDivExt.css("width","auto");
                tabDivExt.find(".floating_container").css({float:"left", "max-width":"600px"});
                
                _this.dialogDiv[0].append(tabDivExt);
                    
                
                for(var i=0; i<_this.n; i++)
                {
                    var plotDiv = $('<div><div id="simulation_status_message'+i+'" style="float:left;">'+
                            resources.vpSimulationResultComplete+':</div><div id="percent'+i+
                            '" "float:left;"></div><div id="graph'+i+'">&nbsp;</div></div>').css({"min-width":"500px"});
                    tabDivExt.find("#"+plotId).append(plotDiv);
                }
                
                var dialogHeight = _this.n > 1 ? 700 : 520;
                _this.dialogDiv[0].dialog(
                {
                    autoOpen: true,
                    width: 620,
                    height: dialogHeight,
                    buttons: 
                    {
                        "Close": function()
                        {
                            $(this).dialog("close");
                            $(this).remove();
                        }
                        
                    },
                    beforeClose: function( event, ui ) {
                        _this.stopSimulation();
                    }
                    ,
                    open: function(event, ui){
                    }
                });
                lookForViewPart('diagram.simulation').onProcessTimer();
            });
        }
    };

    /*
     * Callback function for process timer
     */
    this.onProcessTimer = function()
    {
        queryBioUML("web/simulation/status", 
        {
            de: this.currentProcess
        },
        function(data)
        {
            if (data.status < 3) 
            {
                
                for(var i=0; i<_this.n; i++)
                {
                    _this.dialogDiv[0].find('#percent'+i).html(data.percent+'%');
                    if (data.values) 
                    {
                        if(data.values[i])
                            _this.dialogDiv[0].find('#graph'+i).html('<img src="'+appInfo.serverPath+'web/img?de=' + data.values[i] + '&rnd=' + rnd() + '" style="width:550px;height:350px;">');
                    }
                }
                if (data.values && data.values.length >=n && data.values[_this.n]){
                    updateLog(_this.mainLogDiv, data.values[_this.n]);
                }
                _this.running = setTimeout("lookForViewPart('diagram.simulation').onProcessTimer()", 2000);
            }
            else 
            {
                _this.running = undefined;
                if (data.status == 3 || data.status == 4) 
                {
                    for(var i=0; i<_this.n; i++)
                    {
                        _this.dialogDiv[0].find('#percent'+i).html(data.percent+'%');
                        if (data.values) 
                        {
                            _this.dialogDiv[0].find('#graph'+i).html('<img src="'+appInfo.serverPath+'web/img?de=' + data.values[i] + '&rnd=' + rnd() + '" style="width:550px;height:350px;">');
                        }
                    } 
                    if (data.values && data.values.length >=n && data.values[_this.n]){
                        updateLog(_this.mainLogDiv, data.values[_this.n]);
                    }
                }
                else 
                {
                    logger.error(resources.commonErrorUnknownStatus);
                }
                _this.adjustButtons(false);
            }
        }, 
        function(data)
        {
            _this.dialogDiv[0].dialog("close");
            _this.dialogDiv[0].remove();
            let procId = getElementName(_this.currentProcess);
            let simTabid = "simulationTabs_" + procId;
            let logId = "log_container_" + procId;
            _this.running = undefined;
            _this.currentProcess = undefined;
            _this.adjustButtons(false);
            updateLog(_this.mainLogDiv, data.message);
        });
    };
    
    this.stopSimulation = function()
    {
        if(_this.running)
        {
            clearTimeout(_this.running);
            queryBioUML("web/simulation/stop", 
                {
                    de: _this.currentProcess
                },
                function(){
                    for(var i=0; i<_this.n; i++)
                    {
                        _this.dialogDiv[0].find('#simulation_status_message'+i).html('Terminated by user:');
                    }   
                }, 
                function(){
                });
            _this.adjustButtons(false);
            _this.running = undefined;
        }
    };
    
    this.saveSimulatorOptions = function()
    {
        _this.currentObject.getModelObjectName(function(modelName)
        {
            if (modelName != null) 
            {
                queryBioUML("web/simulation/save_options", 
                {
                    de: _this.currentObject.completeName,
                    engineBean: modelName
                }, function(data){
                   //bean saved to diagram attributes
                    _this.currentObject.setChanged(true);
                }, function(data){
                    //bean not found
                });
            }
        });
    };
    
    this.saveSimulationResult = function()
    {
        createSaveElementDialog(resources.vpSimulationButtonSaveResult, "biouml.standard.simulation.SimulationResult", "", function(resultPath)
        {
            queryBioUML("web/simulation/save_result", 
            {
                de: resultPath,
                jobID: _this.currentProcess
            }, function(data){
                logger.message(resources.vpSimulationResultSaved);
                refreshTreeBranch(getElementPath(resultPath));
            }, function(data){
                logger.error(data.message);
            });
        });
    }
    
}

/*
 * Genome enhancer diagram view part class
 */
function GenomeEnhancerViewPart()
{
    this.tabId = "genome.enhancer";
    this.tabName = "GE table";

    var _this = this;
    var prevTable = "";
    var isTabShown = false;
    var rowFilter = "";

    /*
     * Create div for view part
     */
    this.init = function()
    {
        this.tabDiv = createViewPartContainer(this.tabId);
        this.containerDiv = $('<div id="' + this.tabId + '_container" class="viewPartTab"></div>');
        this.tabDiv.append(this.containerDiv);

        this.table = $('<div>'+resources.commonLoading+'</div>');
        this.containerDiv.append(this.table);

        _.bindAll(this, _.functions(this));
    };

    /*
     * Indicates if view part is visible
     */
    this.isVisible = function(documentObject, callback)
    {
        var result = false;
        if( ( documentObject != null ) && ( documentObject instanceof Diagram ) )
        {
            var path = getElementPath(documentObject.getDiagram().completeName) + "/Model visualization on Yes set";
            result = isPathExists(path);
        }
        isTabShown = result;
        callback(result);
    };

    /*
     * Open document event handler
     */
    this.explore = function(documentObject)
    {
        if( ( documentObject != null ) && ( documentObject instanceof Diagram ) )
        {
            var tablePath = getElementPath(documentObject.getDiagram().completeName) +"/Model visualization on Yes set";
            if( prevTable != tablePath )
            {
                prevTable = tablePath;
            }
        }
    };
    
    this.show = function()
    {
        this.loadTable(prevTable);
    }

    this.save = function()
    {
    };

    this.loadTable = function(tablePath)
    {
        this.table.html('<div>'+resources.commonLoading+'</div>');
        queryBioUML("web/table/sceleton",
        {
            "de": tablePath,
            "read": true
        }, 
        function(data)
        {
            _this.table.html(data.values);
            _this.tableObj = _this.table.children("table");
            _this.tableObj.addClass('selectable_table');
            var params =
            {
                rnd: rnd(),
                read: true,
                de: tablePath,
            }
            if( _this.rowFilter )
            {
                params['filter'] = _this.rowFilter;
            }
            var features = 
            {
                "bProcessing": true,
                "bServerSide": true,
                "bFilter": false,
                //"sPaginationType": "full_numbers",
                "sPaginationType": "input",
                "sDom": "pfrlti",
                "aaSorting": [[ 4, "desc" ]],
                "lengthMenu": [[30, 50, 100, 200, 500, 1000, -2], [30, 50, 100, 200, 500, "1000", "Custom..."]],
                "pageLength": 50,
                //copied from table.js
                "fnDrawCallback": function() {
                    // Clear list of inner genome browsers if any
                    if(_this.innerGenomeBrowser) _this.innerGenomeBrowser = [];
                    // JSON response may include JavaScript, which does not get executed when inserted as innerHTML to table cell
                    // So, call eval() manually
                    _this.tableObj.find('.table_script_node').each(function() {
                            eval($(this).text());
                        }).remove();
                },
                "sAjaxSource": appInfo.serverPath+"web/table/datatables?" + toURI(params)
            };
            _this.dataTableObj = _this.tableObj.dataTable(features);
            _this.tableObj.css('margin', '0 0');
        }, function(data)
        {
            _this.table.html(resources.commonErrorViewpartUnavailable);
            _this.tableObj = null;
            logger.error(data.message);
        });
    };

    BioUML.selection.addListener(function(completeName) {
        if( isTabShown && prevTable )
        {
            var dc = getDataCollection(completeName);
            dc.getBeanFields("kernel/type;title", function(result){
                var kernel = result.getValue("kernel");
                if( kernel && "model instance" == kernel.getValue("type") )
                {
                    var title = result.getValue("title");
                    var filterStr = "Best_module == '" + title + "'";
                    if( _this.rowFilter !== filterStr )
                        _this.updateFilter(filterStr);
                }
                else
                {
                    if( _this.rowFilter !== filterStr )
                        _this.updateFilter("");
                }
            });
        }
    });
    
    this.updateFilter = function(filterStr)
    {
        queryBioUML("web/table/checkFilter",
                {
                    "de": prevTable,
                    "filter": filterStr
                }, 
                function()
                {
                    _this.rowFilter = filterStr;
                    _this.loadTable(prevTable);
                }
            );
    }
}


function LogViewPart()
{
    createViewPart(this, "common.log", "Logs");
    var _this = this;
    this.autoupdate = false;
    this.disabled = false;
    
    this.init = function()
    {
        this.logDiv = $("<textarea/>").css("width", "100%").css("min-height", "400px");
        this.containerDiv.append(this.logDiv);
    };
    
    this.isVisible = function(documentObject, callback)
    {
        if (currentUser != null) 
        {
            callback(canUseExperimental);
        }
        else 
        {
            callback(false);
        }
    };
    
    this.explore = function(documentObject)
    {
    };
    
    this.show = function(documentObject)
    {
        if(this.disabled)
            return;
        this.autoupdate = true;
        lookForViewPart('common.log').onProcessTimer();
    };
    
    this.updateLog = function()
    {
        queryBioUML("web/log/get", {}, 
        function(data){
            _this.logDiv.text(data.values);
            lookForViewPart('web.log').onProcessTimer();
        },
        function(data){
            _this.logDiv.text("Log is unavailable");
            _this.disabled = true;
            _this.autoupdate = false;
            if(_this.timeOut) clearTimeout(_this.timeOut);
        });
    }
    
    this.onProcessTimer = function()
    {
        if(_this.timeOut) clearTimeout(_this.timeOut);
        queryBioUML("web/log/get", {}, 
            function(data){
                _this.logDiv.text(data.values);
                _this.timeOut = setTimeout(function() {lookForViewPart('common.log').onProcessTimer();}, 10000);
            }, function()
            {
                _this.logDiv.text("Log is unavailable");
                _this.disabled = true;
                _this.autoupdate = false;
                if(_this.timeOut) clearTimeout(_this.timeOut);
            });
        
    };
    
    this.save = function()
    {
        this.autoupdate = false;
        if(this.timeOut) clearTimeout(this.timeOut);
    };
    
    this.initActions = function(toolbarBlock)
    {
        this.updateAction = createToolbarButton("Refresh", "apply.gif", this.updateLog);
        toolbarBlock.append(this.updateAction);
    };
}
