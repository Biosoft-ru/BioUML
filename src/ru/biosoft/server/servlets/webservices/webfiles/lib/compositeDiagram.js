/**
 * Composite Diagram document
 *
 * @author anna
 */

function CompositeDiagram(completeName)
{
    //this.DiagramSupportConstructor(completeName, "json");
    this.completeName = completeName;
    this.name = getElementName(this.completeName);
    this.tabId = allocateDocumentId("composite_" + completeName);

    var _this = this;
    
    this.diagram = new Diagram(_this.completeName);
    this.diagram.hidePanel = true;
    this.diagram.parentDocument = this;
    this.activeDiagram = this.diagram;
    
    this.subdiagram = null;
    this.subdiagramName = null;
    
    this.childDiagrams = new Array();
    this.subdiagramSelectListener = new SubdiagramSelectListener(this);
    
    this.open = function(parent)
    {
        opennedDocuments[this.tabId] = this;
        var compositeDiagramDocument = $('<div id="' + this.tabId + '"></div>').css('padding', 0).css('position', 'relative').attr('doc', this);
        parent.append(compositeDiagramDocument);
        
        var diagramContainerDiv = $('<div id="' + this.tabId + '_container" class="documentTab"></div>').css('overflow', 'hidden');
        var splitterContainerDiv = $('<div id="' + this.tabId + '_splitter_container"></div>')
            .css('position', 'relative').css('overflow', 'hidden').css('height', 450).css('width', 700);
        
        diagramContainerDiv.resize( function()
        {
            var h = $(this).height();
            var w = $(this).width();
            splitterContainerDiv.css('width', w).css('height', h);
            splitterContainerDiv.triggerHandler("resize");
        });
        
        splitterContainerDiv.resize(function()
        {
            _this.resizePanes();
            return false;
        });
        
        this.diagramPane = $('<div id="' + this.tabId + '_diagram">').css('height', 350).css('overflow', 'hidden');
        splitterContainerDiv.append(this.diagramPane);
        this.subdiagramPane = $('<div id="' + this.tabId + '_subdiagram">').html('Select child diagram').css('overflow', 'hidden');
        splitterContainerDiv.append(this.subdiagramPane);
        splitterContainerDiv.split(
            {
                orientation: "horizontal",
                limit: 0,
                position: "80%",
                onDragEnd: function(event) {
                    _this.resizePanes();
                }
            });
        
        diagramContainerDiv.append(splitterContainerDiv);
        compositeDiagramDocument.append(diagramContainerDiv);
        
        this.openMainDiagram();
        
    };
    
    this.resizePanes = function()
    {
        var lWidth = _this.diagramPane.width();
        var lHeight = _this.diagramPane.height() - _this.diagramPane.find('.fg-toolbar').outerHeight(true);
        if (lWidth && lHeight) 
        {
            _this.diagramPane.find('.diagramPaneContainer').css('overflow', 'auto').css('width', lWidth).css('height', lHeight).css('position', 'relative');
            _this.diagramPane.find('.documentTab').css('width', lWidth).css('height', lHeight);
            _this.diagramPane.find('.diagramDocumentTab').css('width', lWidth).css('height', lHeight).triggerHandler("resize");
        }
        var rWidth = _this.subdiagramPane.width();
        var rHeight = _this.subdiagramPane.height() - _this.subdiagramPane.find('.fg-toolbar').outerHeight(true);
        if (rWidth && rHeight) 
        {
            _this.subdiagramPane.find('.subdiagramPaneContainer').css('overflow', 'auto').css('width', rWidth).css('height', rHeight).css('position', 'relative');
            _this.subdiagramPane.find('.documentTab').css('width', rWidth).css('height', rHeight);
            _this.subdiagramPane.find('.diagramDocumentTab').css('width', rWidth).css('height', rHeight).triggerHandler("resize");
        }  
    };
    
    this.openMainDiagram = function()
    {
        this.diagram.diagramContainerClass = "diagramPaneContainer";
        this.diagram.showToolbar = true;
        
        var diagramDocument = $('<div id="' + this.diagram.tabId + '"></div>').css('padding', 0).css('position', 'relative').attr('doc', this.diagram);
        this.diagramPane.append(diagramDocument);
        
        this.diagram.toolbar = $('<div id="' + this.diagram.tabId + '_toolbar" class="fg-toolbar ui-widget-header ui-corner-all ui-helper-clearfix"></div>').height(25);
        diagramDocument.append(this.diagram.toolbar);
        
        this.diagram.createDiagram();
        this.diagram.diagramContainerDiv.removeClass("documentTabWithToolbar");
        this.diagram.diagramContainerDiv.css('border', '1px solid #CCC');
        
        this.diagram.loadDiagramToolbar(this.diagram.toolbar);
        
        this.diagram.addSelectListener(_this);
        this.diagram.setLoadedListener(function(){_this.resizePanes();});
        
    };
    
    this.selectControl = function(clickAction, type)
    {
        if (type && type == "composite" && this.diagram != null) 
        {
            this.diagram.selectControl(clickAction);
        }
        else if(this.subdiagram != null)
        {
            this.subdiagram.selectControl(clickAction);
        }
    };
    
    this.createNewEdge = function(event, dcName, type, compositeType)
    {
    	if (compositeType && compositeType == "composite" && this.diagram != null) 
        {
            this.diagram.createNewEdge(event, dcName, type);
        }
        else if (this.subdiagram != null) 
        {
            this.subdiagram.createNewEdge(event, dcName, type);
        }
    };
    
    this.loadSubdiagramPane = function(diagramName)
    {
        if(diagramName == null)
        {
            this.subdiagramPane.html('Select child diagram');
            this.subdiagramName = null;    
            return;
        }
        if (this.subdiagramName != diagramName) 
        {
            CreateDiagramDocument(diagramName, function(diagram)
            {
            	diagram.hidePanel = true;
            	diagram.isDroppable = false;
            	diagram.parentDocument = _this;
                _this.subdiagramPane.html('');
                diagram.diagramContainerClass = "subdiagramPaneContainer";
                diagram.tabId = "child_" + diagram.tabId;
                diagram.showToolbar = false;
                var diagramDocument = $('<div id="' + diagram.tabId + '"></div>').css('padding', 0).css('position', 'relative').attr('doc', diagram);
                _this.subdiagramPane.append(diagramDocument);
                
                //diagram.toolbar = $('<div id="' + diagram.tabId + '_toolbar" class="fg-toolbar ui-widget-header ui-corner-all ui-helper-clearfix"></div>').height(25);
                //diagramDocument.append(diagram.toolbar);
                
                diagram.createDiagram();
                diagram.diagramContainerDiv.css('border', '1px solid #CCC');
                
                //diagram.loadDiagramToolbar(diagram.toolbar);
                
                _this.subdiagram = diagram;
                _this.subdiagramName = diagram.completeName;
                _this.childDiagrams[diagram.completeName] = diagram.name;
                _this.subdiagram.addSelectListener(_this.subdiagramSelectListener);
                _this.subdiagram.setLoadedListener(function(){_this.resizePanes();});
            });
        }
    };
    
    this.nodeSelected = function(node)
    {
	
        var diagramName = this.completeName + "/" + node; 
        queryBioUML("web/diagram/check_diagram_element",
        {
             de: this.completeName,
             node: node
        }, function(data)
        {
            _this.loadSubdiagramPane(data.values);
        }, function(data)
        {
            _this.loadSubdiagramPane(null);
        });
        if(this.activeDiagram != this.diagram)
        {
            this.activeDiagram = this.diagram;
            updateViewParts();
        }
    };
    
    this.close = function(callback)
    {
        if(callback) callback();
    };

    this.isChanged = function()
    {
        if(this.diagram != null)
    	   return this.diagram.isChanged();
    	return false;
    };
    
    this.save = function(callback)
    {
        if (this.diagram != null) 
        {
            queryBioUML("web/doc/save",
                    {
                        de: this.diagram.completeName,
                        action: "save"
                    }, function() 
                    {
                        if(callback) callback(data);
                    }, function(data)
                    {
                        if(callback) callback(data);
                    }); 
        }
    };
    
    this.getDiagram = function()
    {
        return this.activeDiagram.getDiagram();
    };
    
    this.saveAs = function(newPath, callback)
    {
        if(this.diagram != null)
        {
            this.diagram.saveAs(newPath, callback);
        }
    };
    
    this.exportElement = function(value)
    {
        var _this = this;
        $.chainclude(
            {
                'lib/export.js':function(){
                    exportElement(_this.diagram.completeName, "Diagram", {scale: _this.zoom});
                }
            }
        );
    };
}


function SubdiagramSelectListener(diagramDocument)
{
    this.nodeSelected = function(node)
    {
        if (diagramDocument.activeDiagram != diagramDocument.subdiagram) 
        {
            diagramDocument.activeDiagram = diagramDocument.subdiagram;
            updateViewParts();
        }
    }
}

