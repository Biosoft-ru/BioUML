/**
 * ComplexDocument class
 * Special document to display diagram and table in one view pane with splitter delimiter
 * Designed for ExPlain integration
 *
 * @author tolstyh
 */

var activeDiagram = null;
var activeTable = null;
var baseTableSize = null;

function ComplexDocument(completeName)
{
    this.completeName = completeName;
    this.name = getElementName(this.completeName);
    this.tabId = allocateDocumentId(completeName);

    var _this = this;

    this.linkFactorGene = null;

    this.open = function(parent)
    {
        opennedDocuments[this.tabId] = this;
        var complexDocument = $('<div id="' + this.tabId + '"></div>').css('padding', 0).css('position', 'relative').attr('doc', this);
        parent.append(complexDocument);

        var complexContainerDiv = $('<div id="' + this.tabId + '_mainPane" class="documentTab"></div>');
        complexDocument.append(complexContainerDiv);

        complexContainerDiv.resize(function()
        {
            _this.resizePanes();
        });

        this.leftPane = $('<div id="' + this.tabId + '_leftPane">').css('width', '600px').css('overflow', 'hidden');
        complexContainerDiv.append(this.leftPane);
        this.rightPane = $('<div id="' + this.tabId + '_rightPane">').css('overflow', 'hidden');
        complexContainerDiv.append(this.rightPane);

        complexContainerDiv.splitter(
        {
            type: "v",
            sizeLeft: true
        });

        this.loadLeftPane(this.completeName+"/diagram");
        this.loadRightPane(this.completeName+"/sites");


        //update document size
        resizeDocumentsTabs();
    }

    this.resizePanes = function()
    {
        var lWidth = _this.leftPane.width();
        var lHeight = _this.leftPane.height();
        _this.leftPane.find('.leftPaneContainer').css('overflow', 'auto').css('width', lWidth).css('height', lHeight).css('position', 'relative');

        var rWidth = _this.rightPane.width();
        var rHeight = _this.rightPane.height();
        _this.rightPane.find('.rightPaneContainer').css('overflow', 'auto').css('width', rWidth).css('height', rHeight).css('position', 'relative');
    }

    this.getDiagram = function()
    {
        return this.diagram;
    }

    this.loadLeftPane = function(path)
    {
        var diagram = new Diagram(path);
        diagram.diagramContainerClass = "leftPaneContainer";
        diagram.showToolbar = false;
        var diagramDocument = $('<div id="' + diagram.tabId + '"></div>').css('padding', 0).css('position', 'relative').attr('doc', diagram);
        this.leftPane.append(diagramDocument);
        diagram.setLoadedListener(function()
        {
            $('#diagram_diagram_container').scroll(function(e) {$('#table_sites_container').scrollTop($('#diagram_diagram_container').scrollTop())});
            $('#table_sites_container').scroll(function(e) {$('#diagram_diagram_container').scrollTop($('#table_sites_container').scrollTop())});
            activeDiagram = diagram;
            getFactorGeneLinks();
        });
        diagram.createTiledDiagram();
        this.diagram = diagram;
    }

    this.loadRightPane = function(path)
    {
        var table = new Table(path);
        var tableDocument = $('<div id="' + table.tabId + '"></div>').css('padding', 0).css('position', 'relative').attr('doc', table);
        this.rightPane.append(tableDocument);

        table.tableContainerDiv = $('<div id="' + table.tabId + '_container" class="rightPaneContainer"></div>');
        tableDocument.append(table.tableContainerDiv);

/*        table.tableControlButtons = $('<div id="' + table.tabId + '_actions" class="table_actions"></div>');
        table.initActions();
        table.tableContainerDiv.append(table.tableControlButtons);*/

        table.tableDataDiv = $('<div id="' + table.tabId + '_data">Loading...</div>');
        table.tableContainerDiv.append(table.tableDataDiv);

        table.loadTable(true,
        {
            bLengthChange: false,
            iDisplayLength: 100000,
            //bSort: false,
            bInfo: false,
            sDom: "frti",
            aaSorting: [[ 3, "desc" ]],
            fnDrawCallback: function()
            {
                scaleTable();
                updateDiagram();
            }
        });
        table.setLoadedListener(function()
        {
            table.table.fnSetColumnVis(0, false);
            activeTable = table;
        });
    }
    
    this.close = function()
    {
        
    };
}

function updateDiagram ()
{
    if(!activeDiagram)
    {
        setTimeout(updateDiagram, 1000);
    }

    $.getJSON("../biouml/explain",
      {
        "de": activeDiagram.completeName,
        "action": "update"
      }, function(data)
        {
            if (data.type == 0)
            {
                if (activeDiagram.selector)
                {
                    activeDiagram.selector.mouseEvent = null;
                    activeDiagram.selector.selectorDiv.css('visibility', 'hidden');
                }
                activeDiagram.changeDiagramSize(data.size);
                activeDiagram.invalidateTiles(data.refreshArea);
                activeDiagram.viewChanged();
            }
            else
            {
                logger.error(data.message);
            }
        });
}

function getFactorGeneLinks ()
{
    $.getJSON("../biouml/explain",
    {
        "de": activeDiagram.completeName,
        "action": "linkfactorgene"
    }, function(data)
        {
            if (data.type == 0)
            {
                var links = data.values;
                opennedDocuments[activeDocumentId].linkFactorGene = new Array();
                $(links).each(function(index,value)
                   {
                       opennedDocuments[activeDocumentId].linkFactorGene[value.molecule] = value.genes;
                });
            }
            else
            {
                logger.error(data.message);
            }
        });
}



function scaleTable ()
{
    if(!activeTable)
        return false;
    var sitestable = activeTable.tableDataDiv.closest('.documentTab').find('#table_sites_container');
    if(sitestable)
    {
        //cells
        var tds = sitestable.find('td p');
        //header
        var ths = sitestable.find('th');
        var zoom = activeDiagram == undefined?1:activeDiagram.zoom;
        var properties = ['height', 'padding-top', 'padding-bottom', 'font-size', 'margin-top', 'margin-bottom'];
        if(!baseTableSize)
        {
            baseTableSize = new Array();
            for(i in properties)
            {
                baseTableSize['cell'+properties[i]] = parseInt(tds.css(properties[i]));
            }
            baseTableSize['cell'+'height'] = tds.height();
            for(i in properties)
            {
                baseTableSize['header'+properties[i]] = parseInt(ths.css(properties[i]));
            }
            baseTableSize['header'+'height'] = ths.height();
        }
        for(i in properties)
        {
            if(properties[i]!='font-size' || baseTableSize['cell'+properties[i]]*zoom < 20)
                tds.css(properties[i], baseTableSize['cell'+properties[i]]*zoom+'px');
        }

        for(i in properties)
        {
            if(properties[i]!='font-size' || baseTableSize['header'+properties[i]]*zoom < 20)
                ths.css(properties[i], baseTableSize['header'+properties[i]]*zoom+'px');
        }
    }
    return false;
}
