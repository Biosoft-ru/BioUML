/**
 * Functions related to file uploading
 */
var helperFrame;

$(function() {
    //create frame
    var frameId = 'helperFrame';

    helperFrame = $('<iframe id="' + frameId + '" name="' + frameId + '" src="javascript:false"/>')
        .css("position", "absolute").css("top", "-1000px").css("left", "-1000px");     

    $('body').append(helperFrame);
});

function getFileUploadForm(uploadID)
{
    var form = $('<form target="helperFrame" style="display:inline" action="'+appInfo.serverPath+'web/upload?fileID='+uploadID+
        '" method="POST" name="upload' + uploadID + '" id="upload' + uploadID + '" enctype="multipart/form-data">'+
        '<div class="fileUploadForm" title="'+resources.uploadFromComputerToolTip+'"><div class="fileUploadText">'+resources.uploadFromComputerTitle+'</div>'+
        '<input type="file" name="file" class="fileUpload"></div>'+
        '<div id="fileUrl" class="fileUploadForm" title="'+resources.uploadFromWebToolTip+'"><div class="fileUploadText">'+resources.uploadFromWebTitle+'</div></div>'+
        '<div id="fileRepo" class="fileUploadForm" title="'+resources.uploadFromRepositoryToolTip+'"><div class="fileUploadText">'+resources.uploadFromRepositoryTitle+'</div></div>'+
        '<div id="fileContent" class="fileUploadForm" title="'+resources.uploadFromContentToolTip+'"><div class="fileUploadText">'+resources.uploadFromContentTitle+'</div></div></form>');
    return form;
}

function getUploadURLForm(uploadID)
{
    var form = $('<form target="helperFrame" style="display: inline" action="'+appInfo.serverPath+'web/upload?fileID='+uploadID+
        '" method="POST" name="upload' + uploadID + '" id="upload' + uploadID + '" enctype="multipart/form-data">'+
        '<div>URL: <input type="text" name="fileUrl" size="50" placeholder="'+resources.dlgUploadURLPrompt+'"/></div>'+
        '</form>'); 
    return form;
}

function getTypeContentForm(uploadID)
{
    var form = $('<form target="helperFrame" style="display: inline" action="'+appInfo.serverPath+'web/upload?fileID='+uploadID+
        '" method="POST" name="upload' + uploadID + '" id="upload' + uploadID + '" enctype="multipart/form-data">'+
        '<div>'+resources.dlgTypeContentName+'<input type="text" name="fileName" value="'+resources.dlgTypeContentDefaultName+'" size="40"/></div>'+
        '<textarea name="fileContent" placeholder="'+resources.dlgTypeContentPrompt+'" style="width:100%" rows="20"></textarea>'+
        '</form>'); 
    return form;
}

function sendRepositoryUpload(uploadID, path)
{
    queryBioUML("web/upload", {fileID: uploadID, filePath: path}, function(){}, function(){});
}


function getUploadURLDialog(uploadID, okCallback)
{
    var uploadForm = getUploadURLForm(uploadID);
    var dialogDiv = $('<div title="'+resources.dlgUploadURLTitle+'"/>');
    dialogDiv.append(uploadForm);
    dialogDiv.dialog(
    {
        autoOpen: true,
        modal: true,
        width: 400,
        buttons: 
        {
            "Cancel": function()
            {
                $(this).dialog("close");
                $(this).remove();
            },
            "Ok": function()
            {
            	$(":button:contains('Ok')").removeAttr("disabled").attr("disabled", "disabled").addClass("ui-state-disabled");
            	$(":button:contains('Cancel')").removeAttr("disabled").attr("disabled", "disabled").addClass("ui-state-disabled");
            	
                var fileUrl = uploadForm.find(":input[name=fileUrl]");
                fileUrl.val(decodeURIComponent(fileUrl.val()));
                var __this = $(this);
                queryBioUML("web/upload", {type: "getName", url: fileUrl.val()},
                    function(data) {
                        uploadForm.submit();
                        okCallback(data.values.fileName);
                        __this.dialog("close");
                        __this.remove();
                        $(":button:contains('Cancel')").removeAttr("disabled").removeClass("ui-state-disabled");
                    },
                    function() {
                        uploadForm.submit();
                        okCallback(fileUrl.val());
                        __this.dialog("close");
                        __this.remove();
                        $(":button:contains('Cancel')").removeAttr("disabled").removeClass("ui-state-disabled");
                    }
                );
            }
        }
    });
    addDialogKeys(dialogDiv);
}

function getUploadContentDialog(uploadID, okCallback)
{
    var dialogDiv = $('<div title="'+resources.dlgTypeContentTitle+'"/>' );
    var contentForm = getTypeContentForm(uploadID);
    dialogDiv.append(contentForm);
    dialogDiv.dialog(
    {
        autoOpen: true,
        modal: true,
        width: 400,
        buttons: 
        {
            "Cancel": function()
            {
                $(this).dialog("close");
                $(this).remove();
            },
            "Ok": function()
            {
                contentForm.submit();
                var name = contentForm.find(":input[name=fileName]").val();
                if(!name)
                {
                    logger.error(resources.dlgTypeContentErrorNoName);
                    return;
                }
                okCallback(name);
                $(this).dialog("close");
                $(this).remove();
            }
        }
    });
    //addDialogKeys(dialogDiv);
} 

