/**
 * Jupyter document implementation
 * 
 * @author manikitos
 */
function JupyterDocument(completeName, isKernel )
{
    this.completeName = completeName;
    this.name = getElementName(this.completeName);
    this.tabId = allocateDocumentId(completeName);
    //console.log( "this.completeName = " + this.completeName );
    var dc = getDataCollection(this.completeName);
    //console.log( dc );
    dc.addRemoveListener(this);
    this.contanerDiv;
    this.jupyterContainer;
    this.uuid;
    
    var _this = this;
    
    this.open = function(parent)
    {
        opennedDocuments[this.tabId] = this;
        var thisDocument = $('<div id="' + this.tabId + '"></div>').css('padding', 0).css('position', 'relative').attr('doc', this);
        parent.append(thisDocument);
        
        this.containerDiv = $('<div id="' + this.tabId + '_container" class="documentTab"></div>');
        thisDocument.append(this.containerDiv);
        
        this.loadContent();
    };
    
    this.loadContent = function()
    {
        var changeLinks = function(parentElementName)
        {
            for( var aElement in $(parentElementName).contents().find("a") ) {
                $(aElement).attr("target", "_self");
            }
        }
        
        var prevFunction;
        var modifyIframe = function()
        {
            var count = 0;
            setTimeout( function run() {
                var iframe = $("#jupyter_iframe")[0];
                prevFunction = addIframeMouseMoveListener(iframe, prevFunction);
                addIframeListener(iframe, 'focus');
                addIframeListener(iframe, 'blur');
                count++;
                if( count < 600 )
                    setTimeout(run, 100);
            }, 100 );
            
            var hideCount = 0;
            var delayedHide = function()
            {
                var allFound = true;
                var jupyterIframe = $("#jupyter_iframe");
                
                var menubar = jupyterIframe.contents().find("#menubar");
                if( menubar )
                {
                    menubar.find("#file_menu").parent().hide();
                    menubar.find("#widget-submenu").parent().hide();
                    menubar.find("#toggle_header").hide();
                }
                else
                    allFound = false;
                
                var header = jupyterIframe.contents().find("#header-container");
                if( header )
                {
                    header.hide();
                    jupyterIframe.contents().find("#header-bar").hide();
                }
                else
                    allFound = false;
                
                hideCount++;
                if( !allFound || hideCount < 100 )
                    setTimeout( delayedHide, 500 );
            }
            setTimeout( delayedHide, 500 );
        }
        
        var params = {
                "de": this.completeName    
        };
 
        var method = isKernel ? "web/biouml_jupyter/openKernel" : "web/biouml_jupyter/open";
        queryBioUML( method, params, function(data)
        {
            var values = data.values;
            if(values.link)
            {
                _this.jupyterContainer = $('<iframe id="jupyter_iframe" src="' + values.link + '" width="99%" height="99%" frameborder="no"/>');
                _this.containerDiv.append(_this.jupyterContainer);
                _this.jupyterContainer.ready( modifyIframe );
                resizeDocumentsTabs();
                createJupyterDroppable(_this.jupyterContainer, dropInJupyter);
            }
            if( values.uuid )
                _this.uuid = values.uuid;
        });
    };
    
    this.save = function(callback)
    {
        this.saveAs(this.completeName, callback);
    };
    
    this.saveAs = function(newPath, callback)
    {
        //no implementation needed since jupyter saves document
        return;
    };
    
    this.close = function(callback)
    {
        if(callback) callback();
        if( this.uuid )
        {
            var params = {
                    "uuid" : this.uuid
            }
            queryBioUML("web/biouml_jupyter/close", params);
        }
    };

    this.isChanged = function()
    {
        return false;
    };

    this.dataCollectionRemoved = function()
    {
        closeDocument(this.tabId);
    };
}

function dropInJupyter( element, path, event )
{
    event.preventDefault();
    var iframeDocument = $(element).contents().find('body').parent().parent()[0];

    var inner;
    if (iframeDocument.caretPositionFromPoint)
    {
        inner = iframeDocument.caretPositionFromPoint(event.pageX, event.pageY - 50);
        inner = inner.offsetNode;
    }
    else if (iframeDocument.caretRangeFromPoint)
    {
        inner = iframeDocument.caretRangeFromPoint(event.pageX, event.pageY - 50);
        inner = inner.startContainer;
    }

    var el = $(inner).closest(".code_cell");
    if( !el )
        return;
    el = el.find("textarea");
    if( !el )
        return;
    el = el[0];
    if( !el )
        return;
    el.focus();
    var target = $(element)[0].contentWindow.Jupyter.notebook.get_selected_cell();
    var cursor = target.code_mirror.getCursor();
    cursor.ch = 0;
    cursor.line = 0;
    target.code_mirror.setCursor(cursor);
    var newLine = String.fromCharCode(10); //new line character code is 10
    //emulate printing
    var text = 'biouml.get("'+path.replace(/\"\\/g, "\\$1")+'\")' + newLine;
    el.value = text;
    var evt = document.createEvent("Events");
    evt.initEvent("change", true, true);
    el.dispatchEvent(evt);
        target.code_mirror.setCursor(cursor);
}

function createJupyterDroppable(element, f)
{
    $(element).droppable({
        scope: "treeItem",
        tolerance: "pointer",
        accept: function(draggable)
        {
            var path = getTargetPath(getTreeNodePath(draggable.parent().get(0)));
            if(!path) path = draggable.attr("data-path");
            if(!path) return false;
            return true;
        },
        drop: function(event, ui)
        {
            var path = getTargetPath(getTreeNodePath(ui.draggable.parent().get(0)));
            if(!path) path = ui.draggable.attr("data-path");
            event.pageX = ui.offset.left;
            event.pageY = ui.offset.top;
            f.call(event.target, element, path, event);
        }
    });
};

function createNewJupyterFile(path)
{
    if(!path) return;

    var defaultPath = createPath(path, "New jupyter.ipynb");
    var property = new DynamicProperty("path", "data-element-path", defaultPath);
    property.getDescriptor().setDisplayName(resources.dlgCreateScriptTitle);
    property.getDescriptor().setReadOnly(false);
    property.setCanBeNull("no");
    property.setAttribute("dataElementType", "biouml.plugins.jupyter.access.IPythonElement");
    var pathEditor = new JSDataElementPathEditor(property, null);
    pathEditor.setModel(property);
    var pathNode = pathEditor.createHTMLNode();
    pathEditor.setValue(defaultPath);

    var dialogDiv = $('<div title="Create Jupyter file" id="new_ipython_dlg'+ rnd() + '">Path to the file:</div>');
    dialogDiv.append(pathNode);
    dialogDiv.append('<br/><br/>Kernel type:<div id="type_container"/><br/><br/>'+
    '<div id="type_description"/><br/><br/>');

    //TODO: get list of available kernels from server
    var typeContainer = dialogDiv.find('#type_container');
    var select = $('<select/>').attr('id', 'kerneltype').css('width', '280px');
    typeContainer.html(select);
    var option = $('<option value="python3">Python 3</option>');
    option.attr('selected', 'selected');
    select.append(option);
    select.append( $('<option value="rkernel">R (BioUML)</option>') );
    select.append( $('<option value="bioumlkernel">JS (BioUML)</option>') );
    select.append( $('<option value="soskernel">SoS</option>') );
    
    var closeDialog = function()
    {
        dialogDiv.dialog("close");
        dialogDiv.remove();
    };
    
    dialogDiv.dialog(
            {
                autoOpen: false,
                width: 320,
                buttons: 
                {
                    "Ok": function()
                    {
                        var completePath = pathEditor.getValue();
                        var kernelType = dialogDiv.find("#kerneltype").children("option:selected").val();
                        var _this = $(this);
                        queryBioUML("web/biouml_jupyter/create",
                                 {
                                     de: completePath,
                                     kernel: kernelType
                                 },
                                 function(data)
                                 {
                                     refreshTreeBranch(getElementPath(completePath));
                                     closeDialog();
                                     openDocument(completePath);
                                 },
                                 function()
                                 {
                                     closeDialog();
                                 });
                    },
                    "Cancel" : function()
                    {
                        closeDialog();
                    }
                }
            });
    addDialogKeys(dialogDiv);
    sortButtons(dialogDiv);
    
    dialogDiv.dialog("open");
}
