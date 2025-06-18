/**
 * $Id: JSFileUploader.js,v 1.8 2012/11/28 11:53:53 lan Exp $
 * 
 * DataElementSelector control (used in BioUML)
 */

function JSFileUploader(propInfo, format)
{
    JSInput.call(this, propInfo, format); // call parent constructor
    this.objectClassName = "JSFileUploader";
    this.originalName = propInfo.getAttribute("originalName");
    this.value = "";
};

JSFileUploader.prototype = new JSInput();
JSFileUploader.superClass = JSInput.prototype;

JSFileUploader.prototype.JSInput_updateView = JSInput.prototype.updateView;

JSFileUploader.prototype.createHTMLNode = function()
{
    var propName = this.propInfo.getName();
    var _this = this;
    this.node = $("<div/>");
    this.uploadID = rnd();
    this.progressBlock = $("<div/>").hide();
    this.progressBar = $("<div/>");
    this.progressBlock.append(this.progressBar);
    this.cancelUpload = $(
            "<input type='button' class='button' value='Cancel'/>").css(
            "float", "right").css("position", "relative").css("z-index", "100")
            .addClass("ui-corner-all").addClass("ui-state-default");
    this.cancelUpload.click(function()
    {
        cancelJob(_this.uploadID);
    });
    this.progressBlock.prepend(this.cancelUpload);
    this.node.append(this.progressBlock);
    this.uploadForm = getFileUploadForm(this.uploadID);
    this.node.append(this.uploadForm);

    this.message = $("<span/>");
    this.node.append(this.message);
    this.uploadInput = this.node.find(".fileUpload");
    if (this.originalName)
    {
        this.message.text("[ " + this.originalName + " ]");
        this.uploadForm.hide();
        var changeButton = $(
                "<input type='button' class='button' value='Change'/>")
                .addClass("ui-corner-all").addClass("ui-state-default");
        changeButton.click(function()
        {
            _this.uploadForm.show();
            changeButton.hide();
        });
        this.message.append(changeButton);
    }
    var fileName = undefined;
    var uploadHandler = function()
    {
        _this.uploadForm.hide();
        _this.progressBlock.show();
        disableDPI(_this.node.closest(".form-js").parent());
        createProgressBar(_this.progressBar, _this.uploadID, function(status,
                message)
        {
            if (status == JobControl.COMPLETED)
            {
                _this.setValue(_this.uploadID);
                _this.message.text(resources.dlgImportFileUploaded.replace(
                        "{file}", fileName.escapeHTML()));
            } else
            {
                _this.message.text("Upload failed");
            }
            _this.progressBlock.hide();
            enableDPI(_this.node.closest(".form-js").parent());
        });
    };

    this.HTMLNode = $("<input type='hidden'/>").get(0);
    this.node.append(this.HTMLNode);
    $(this.uploadInput).change(function(event)
    {
        _this.uploadForm.submit();
        fileName = _this.uploadInput.val().replace(/^.+[\\\/]/, "");
        uploadHandler();
        event.stopPropagation();
        event.preventDefault();
    });
    this.uploadForm.find('#fileRepo').click(
            function()
            {
                createOpenElementDialog(resources.dlgImportSelectFromRepo,
                        "ru.biosoft.access.file.FileDataElement", null, function(
                                filePath)
                        {
                            fileName = getElementName(filePath);
                            sendRepositoryUpload(_this.uploadID, filePath);
                            uploadHandler();
                        });
            });
    this.uploadForm.find('#fileUrl').click(function()
    {
        getUploadURLDialog(_this.uploadID, function(uploadName)
        {
            fileName = uploadName.replace(/^.+[\\\/]/, "");
            uploadHandler();
        });
    });
    this.uploadForm.find('#fileContent').click(function()
    {
        getUploadContentDialog(_this.uploadID, function(uploadName)
        {
            fileName = uploadName.replace(/^.+[\\\/]/, "");
            uploadHandler();
        });
    });

    return this.node.get(0);
};

JSFileUploader.prototype.getValue = function()
{
    return this.value;
};

JSFileUploader.prototype.setValue = function(val)
{
    var oldValue = this.getValue();
    this.value = val;
    this.fireChangeListeners(oldValue, val);
};