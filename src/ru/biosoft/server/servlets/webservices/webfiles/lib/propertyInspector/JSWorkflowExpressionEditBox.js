/**
 * $Id: JSWorkflowExpressionEditBox.js,v 1.4 2011/11/01 12:00:23 lan Exp $
 *
 * Input edit box with workflow variable selector.
 */

function JSWorkflowExpressionEditBox( propInfo, format )
{
    JSEditBox.call(this, propInfo, format); // call parent constructor
    this.objectClassName = "JSWorkflowExpressionEditBox";
};

JSWorkflowExpressionEditBox.prototype = new JSEditBox();
JSWorkflowExpressionEditBox.superClass = JSEditBox.prototype;

// override
JSWorkflowExpressionEditBox.prototype.createHTMLNode = function()
{
    var element = JSEditBox.prototype.createHTMLNode.apply(this);
    var result = $('<div/>');
    var detailsButton = $("<input type='button' class='button' value='...'/>").addClass("ui-corner-all").addClass("ui-state-default");
    result.append(element);
    result.append(detailsButton);
    this.diagramPath = opennedDocuments[activeDocumentId].completeName;
    var workflowViewPart = lookForViewPart("diagram.workflow.main");
    if(workflowViewPart && workflowViewPart.selectedNodeName)
    	this.diagramPath = getElementPath(this.diagramPath+"/"+workflowViewPart.selectedNodeName);
    var _this = this;
    this.rnd = rnd();
    $(detailsButton).click(function() {_this.showVariableSelector();});
    return result.get(0);
};

JSWorkflowExpressionEditBox.prototype.onVariableSelected = function(varPath)
{
    var text = this.HTMLNode.value;
    var selectionStart = this.HTMLNode.selectionStart;
    text = text.substring(0, selectionStart)+"$"+varPath+"$"+text.substring(selectionStart);
    this.HTMLNode.focus();
    this.HTMLNode.value = text;
    this.HTMLNode.setSelectionRange(selectionStart, selectionStart+varPath.length+2);
};

JSWorkflowExpressionEditBox.prototype.showVariableSelector = function()
{
    var _this = this;
    var treeId = "vartree";
    this.dialogContent = $('<div><div id="'+treeId+'" style="height: 300px; overflow: auto"><ul><li id="#var_" class="open"><a href="#">/</a></li></ul></div>Selected variable:<br><input type="text" readonly id="varname" style="width: 100%"><br>Value:<br><input type="text" readonly id="varvalue" style="width: 100%"></div>');
    this.treeNode = this.dialogContent.find("#"+treeId);
    this.createTreeObject();

    this.dialogContent.dialog(
    {
        modal: true,
        autoOpen: true,
        width: 400,
        title: "Select variable",
        buttons: 
        {
            "Cancel": function()
            {
                $(this).dialog("close");
                $(this).remove();
            },
            "Ok": function()
            {
                var value = _this.dialogContent.find("#varname").val();
                $(this).dialog("close");
                $(this).remove();
                _this.onVariableSelected(value);
            }
        }
    });
};

JSWorkflowExpressionEditBox.prototype.getPathByTreeNode = function(node)
{
    var id = $(node).get(0).id;
    return id.substr(5);
};

JSWorkflowExpressionEditBox.prototype.getTreeNodes = function(parentNode, callback)
{
    var _this = this;
    var branch = "";
    if(parentNode.id!="#")
        branch = this.getPathByTreeNode(parentNode);

    _this.creatingItem = false;
    queryBioUML('web/research', {
        action : "var_tree",
        de : this.diagramPath,
        branch : branch
    }, function(data)
    {
        _this.creatingItem = true;
        var elements = [];
        for(var i=0; i<data.values.children.length; i++)
        {
            var item = data.values.children[i];
            var title = item.name.substring(item.name.lastIndexOf("/")+1);
            var childNode =
            {
                id : "#var_"+item.name,
                parent : parentNode.id,
                text : title,
                li_attr: {
                "data-value": item.value,
                "data-path": item.name,
                },
                state : {
                    opened    : false, 
                    disabled  : false, 
                    selected  : false
                },
                'children' : !(item.leaf)
            };
            if(item.leaf)
            {
                childNode.icon = 'icons/leaf.gif';
            } 
            elements.push(childNode);
        }
        _this.creatingItem = false;
        callback(elements);
    });
};


JSWorkflowExpressionEditBox.prototype.createTreeObject = function()
{
    var instance = this;
    this.tree = this.treeNode.jstree({
        'core' : 
        {
          'check_callback' : function(o, n, p, i, m) {
              if(o === "move_node" || o === "copy_node") {
                  return false;
              }
              return true;
          },
          'data' : function (obj, callback) {
              let _this = this;
              instance.getTreeNodes(obj, function(newNodes){
                  callback.call(_this, newNodes)
              });
          },
          'multiple' : false,
          'animation' : 0,
          'themes' : {
              'variant' : 'small'
          },
        },
      'types' : {
          'default' : { 'icon' : 'folder' },
      },
      
      'plugins' : ['changed']
    });
    
    this.tree.on('changed.jstree', function (e, data) {
        if(!data.node)
            return;
        if (data.node.li_attr["data-path"]) 
        {
            instance.dialogContent.find("#varname").val(data.node.li_attr["data-path"]);
            instance.dialogContent.find("#varvalue").val(data.node.li_attr["data-value"]);
        }
    }).on('ready.jstree', function(){ 
        instance.treeLoaded = true;
    });
}
