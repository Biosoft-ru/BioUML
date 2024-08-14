/**
 * Init account info document
 *
 * @author tolstyh
 */

var countriesList = null;
function initAccountManager()
{
    var accountDiv = $('#account_info_div');
    accountDiv.css('margin', '20px');
    var createProjectButton = accountDiv.find('#create_project');
    createProjectButton.click(function()
    {
    	createProjectButton.removeAttr("disabled").attr("disabled", "disabled").addClass("ui-state-disabled");
    	createNewProject(function(){
        	createProjectButton.removeAttr("disabled").removeClass("ui-state-disabled");
        },
        function(){
        	createProjectButton.removeAttr("disabled").removeClass("ui-state-disabled");
        },
        function(){
        	createProjectButton.removeAttr("disabled").removeClass("ui-state-disabled");
        });
    });
    queryBioUML("support/infoDictionary", 
    {"dictionary_name": "countries"},
    function(data){
        countriesList = data.value;
        queryBioUML("support/userInfo", 
        {}, function(data)
        {
            var bean = data.value;
            accountDiv.find('#username').html(bean.username);
            accountDiv.find('#expiration').html(bean.expiration);
            var editableFields = ["firstName", "lastName", "courtesy", "organization", "department", "workPhone", "country"];
            if(!bean.courtesy)
                bean.courtesy = "";
            if(bean.countryID)
            {
                bean.country = getCountryByID(bean.countryID);
            }
                
            changeEditableMode(editableFields, "show", accountDiv, bean);
            
            accountDiv.find('#password').find('input').click(function(event)
            {
                new ChangePassword(bean.username).showForm();
            });
            var editButton = accountDiv.find('#edit_info');
            var saveButton = accountDiv.find('#save_info');
            var cancelButton = accountDiv.find('#cancel_changes');
            editButton.click(function(){
                changeEditableMode(editableFields, "edit", accountDiv);
                saveButton.show();
                cancelButton.show();
                editButton.hide();
            });
            saveButton.click(function(){
                var newBean = getUserInfoBean(editableFields, accountDiv);
                newBean.user = bean.username;
                newBean.countryID = newBean.country;
                //TODO: split fullname
                if(newBean.fullname)
                {
                    var fullNameVals = newBean.fullname.split(" ");
                    newBean.firstName = fullNameVals[0];
                    if(fullNameVals.length > 1)
                        newBean.lastName = newBean.fullname.substring(newBean.firstName.length + 1);
                    delete newBean.fullname;
                }
                confirmAccount(bean.username, 
                function(password)
                {
                    newBean.pass = password;
                    queryBioUML("support/changeInfo", 
                    newBean, function(data)
                    {
                        changeEditableMode(editableFields, "show", accountDiv);
                        editButton.show();
                        cancelButton.hide();
                        saveButton.hide();
                    }, 
                    function(data)
                    {
                        logger.error(data.message);
                    });
                 }
                );
            });
            cancelButton.click(function(){
                changeEditableMode(editableFields, "show", accountDiv, bean);
                editButton.show();
                cancelButton.hide();
                saveButton.hide();
            });
        });
    });
    
    initProjectsTable();
}

function changeEditableMode(fieldNames, mode, parent, values)
{
    for(var i = 0; i < fieldNames.length; i++)
    {
        var id = fieldNames[i];
        var cell = parent.find("#" + id);
        if(mode == "edit")
        {
            var oldValue = cell.html();
            var control = null;
            if(id == "country")
            {
                control = $('<select/>');
                for(var j=0; j < countriesList.length; j++)
                {
                    var option = $('<option value="'+countriesList[j].id+'">' + countriesList[j].name+'</option>');
                    if(countriesList[j].name == oldValue)
                        option.attr('selected', 'selected');
                    control.append(option);    
                }
            } 
            else if(id == "courtesy" || id == "organizationType")
            {
                var vals = id == "courtesy" ? ["Mr.","Dr.","Ms.","Mrs.","Miss","Prof.","Sir"]:["academic","governmental","commercial"];
                control = $('<select/>');
                for(var j=0; j < vals.length; j++)
                {
                    var option = $('<option value="'+vals[j]+'" >' + vals[j]+'</option>');
                    if(vals[j] == oldValue)
                        option.attr('selected', 'selected');
                    control.append(option);    
                }
            }
            else 
            {
                control = $('<input type="text"/>').val(oldValue);
            }
            cell.empty();
            if(control != null)
                cell.append(control);  
            
        }
        else
        {
            var control = cell.find('input');
            if(! control[0])
                control = cell.find('select');  
            var value = values != undefined ? values[id] : control.val();
            if(id == "country" && values == undefined)
            {
                value = getCountryByID(value);
            }
            if(value == undefined)
                value = ""; 
            cell.html(value);
        }
    }
}

function getUserInfoBean (fieldNames, parentControl)
{
    var bean = {};
    for(var i = 0; i < fieldNames.length; i++)
    {
        var id = fieldNames[i];
        var cell = parentControl.find("#" + id);
        var control = cell.find('input');
        if(!control[0])
            control = cell.find('select');
        if (control) 
        {
            var value = control.val();
            if (value) 
                bean[id] = value;
        }
    }
    return bean;
}

function confirmAccount(username, callback)
{
    var dialogDiv = $('<div title="'+resources.dlgConfirmTitle+'"></div>');
    var loginForm = $('<table><tr><td>Username:</td><td>'+username+'</td></tr><tr><td>Password:</td><td><input type="password" size="25" id="password"/></td></tr></table>');
    dialogDiv.append(loginForm);
    dialogDiv.dialog(
    {
        autoOpen: true,
        modal: true,
        resizable: false,
        width: 300,
        buttons: 
        {
            "Confirm": function()
            {
                callback(dialogDiv.find("#password").val());
                $(this).remove();
            },
            "Cancel": function()
            {
                $(this).remove();
            }
        },
        close: function(ev, ui)
        {
            $(this).remove();
        }
    });
    dialogDiv.find('#password').focus();
    addDialogKeys(dialogDiv, null, "Confirm");
    sortButtons(dialogDiv);
}

function getCountryByID(id)
{
    var country = countriesList[0].name;
    for (var j = 0; j < countriesList.length; j++) 
    {
        if (countriesList[j].id == id) 
        {
            country = countriesList[j].name;
            break;
        }
    }
    return country;
}


function ChangePassword(username)
{
    this.showForm = function(message)
    {
        this.dialogDiv = $('<div title="Change password"></div>');
        if(message)
            this.dialogDiv.append($('<p><font color=red>' + message + '</font></p>'));
        var loginForm = $('<table><tr><td>'+resources.dlgLoginUsername+'</td><td>'+username+'</td></tr>'+
        '<tr><td>'+resources.dlgChangePassOldPass+'</td><td><input type="password" size="25" id="old_password"/></td></tr>'+
        '<tr><td>'+resources.dlgChangePassNewPass+'</td><td><input type="password" size="25" id="new_password"/></td></tr>'+
        '<tr><td>'+resources.dlgChangePassNewPass+'<br/>(repeat)</td><td><input type="password" size="25" id="new_password_2"/></td></tr></table>');
        this.dialogDiv.append(loginForm);
        var _this = this;
        this.dialogDiv.dialog(
        {
            autoOpen: false,
            modal: true,
            resizable: false,
            width: 300,
            buttons: 
            {
                "Change": function()
                {
                    _this.submitForm();
                    $(this).remove();
                },
                "Cancel": function()
                {
                    $(this).remove();
                }
            },
            close: function(ev, ui)
            {
                $(this).remove();
            }
        });
        this.dialogDiv.dialog("open");
        addDialogKeys(this.dialogDiv, null, "Change");
        sortButtons(this.dialogDiv);
        $('#old_password').focus();
    };
    
    this.submitForm = function()
    {
        var _this = this;
        var old_pass = $('#old_password').val();
        var new_pass = $('#new_password').val();
        var new_pass_2 = $('#new_password_2').val();
        if(new_pass != new_pass_2)
        {
            _this.showForm("New passwords are not the same");
        }
        else
        {
            queryBioUML("support/changePasswd", 
            {
                user: username,
                pass: old_pass,
                newpass: new_pass
            }, function(data)
            {
                logger.message("Password successfully changed");
            }, function(data)
            {
                _this.showForm(data.message);
            });
        }
    };
}

function initProjectsTable()
{
    var projectsDC = getDataCollection(appInfo.userProjectsPath);
    var projectsTable = $('#projects_table');
    projectsTable.hide();
    queryBioUML("support/getProjectsData", {}, function(data)
    {
        projectsTable.find('tr.project_row').remove();
        var projects = data.value;
        var pos = 1;
        for(var i=0; i<projects.length; i++)
        {
            var newRow = $('<tr/>').addClass('project_row')
                .append($('<td/>').append(projects[i].name))
                .append($('<td/>').append(projects[i].description))
                .append($('<td/>').text(projects[i].quota === undefined ? "" : formatSize(projects[i].quota)));
            if(projects[i].admin)
            {
                (function(i) {
                    newRow.append($('<td/>').append($('<input type="button" class="ui-state-default"/>').val("Change").click(function()
                    {
                        createPromptDialog(resources.dlgChangeQuotaTitle.replace("{project}", projects[i].name), "", function(newQuota)
                        {
                            queryBioUML("support/setQuota", {project: projects[i].name, quota: newQuota}, function() {initProjectsTable();});
                        }, projects[i].quota||0);
                    })));
                })(i);
            }
            projectsTable.find('tr:nth-child('+pos+')').after(newRow);
            pos++;
        }
        projectsTable.show();
    });
}

function removeProject(completePath)
{
    if(!completePath)
    {
        return;
    }
    var name = getElementName(completePath);
    var dialogDiv = $('<div title="'+resources.dlgRemovePrjTitle+'"></div>');
    var loginForm = $('<table><tr><td>'+resources.dlgLoginUsername+'</td><td><b>'+currentUser+'</b></td></tr>'+
    '<tr><td>'+resources.dlgLoginPassword+'</td><td><input type="password" size="25" id="password"/></td></tr>'+
    '<tr><td colspan="2"><input type="checkbox" id="remove_project"/>'+resources.dlgRemovePrjConfirmation.replace("{name}", name)+'</td></tr>'+
    '</table>');
    dialogDiv.append(loginForm);
    dialogDiv.dialog(
    {
        autoOpen: true,
        modal: true,
        resizable: false,
        width: 400,
        buttons: 
        {
            "Remove": function()
            {
                var pass = dialogDiv.find("#password").val();
                if( document.querySelector( '#remove_project' ).checked )
                {
                    queryBioUML("support/deleteProject", 
                    {
                        "user" : currentUser,
                        "pass" : pass,
                        "project" : name
                    },
                    function(data)
                    {
                        logger.message(resources.dlgRemovePrjRemoved.replace("{name}", name));
                        removeElementOnClient(completePath);
                        refreshTreeBranch(appInfo.userProjectsPath);
                        initProjectsTable();
                        initJournals(function() {
                            if(currentProjectPath && getElementName(currentProjectPath)==name)
                            {
                                currentProjectPath = null;
                                initCurrentProject();
                            }
                        });
                    });
                }
                else
                {
                    logger.error(resources.dlgRemovePrjNotConfirmed);
                }

                $(this).remove();
            },
            "Cancel": function()
            {
                $(this).remove();
            }
        },
        close: function(ev, ui)
        {
            $(this).remove();
        }
    });
    dialogDiv.find('#password').focus();
    addDialogKeys(dialogDiv, null, "Remove");
    sortButtons(dialogDiv);
}

function createNewProject(callback, cancelCallback, errorCallback)
{
    var dialogDiv = $('<div title="'+resources.dlgCreatePrjTitle+'"></div>');
    var loginForm = $('<table><tr><td>'+resources.dlgLoginUsername+'</td><td>'+currentUser+'</td></tr>'+
    '<tr><td>'+resources.dlgLoginPassword+'</td><td><input type="password" size="25" id="password"/></td></tr>'+
    '<tr><td>'+resources.dlgCreatePrjName+'</td><td><input type="text" size="25" id="project_name"/></td></tr>'+
    '<tr><td>Project type</td><td><select id="projectType" name="projectType">'
      +'<option value="SQL">SQL</option>'
      +'<option value="FILE">FILE</option>'
      +'</select></td></tr>'+
    '<tr><td>'+resources.dlgCreatePrjDescription+'</td><td><textarea cols="35" rows="5" style = "resize: vertical;" placeholder="'+resources.dlgCreatePrjDescriptionPlaceholder+'" id="project_description"></textarea></td></tr>'+
    '</table>');
    dialogDiv.append(loginForm);
    dialogDiv.dialog(
    {
        autoOpen: true,
        modal: true,
        resizable: false,
        width: 400,
        buttons: 
        {
            "Create": function()
            {
                var pass = dialogDiv.find("#password").val();
                var projectName = dialogDiv.find("#project_name").val().trim();
                var projectType = dialogDiv.find("#projectType").val();
                var description = dialogDiv.find("#project_description").val();
                if(projectName == "")
                {
                    logger.error(resources.commonErrorEmptyNameProhibited);
                }
                else
                {
                    queryBioUML("support/createProjectWithPermission", 
                    {
                        "user" : currentUser,
                        "pass" : pass,
                        "project" : projectName,
                        "projectType": projectType,
                        "description" : description
                    },
                    function(data)
                    {
                        logger.message(resources.dlgCreatePrjCreated.replace("{name}", projectName));
                        refreshTreeBranch(appInfo.userProjectsPath);
                        initProjectsTable();
                        initJournals(function(){
                            setCurrentProject(projectName);
                        });
                        if(callback)
                            callback(projectName);
                    },
                    function(data)
                    {
                        var message = data.message;
                        if(message.includes("$1"))
                            message = message.replace("$1", appInfo.adminMail);
                        logger.error(message);
                        if(errorCallback)
                        	errorCallback();
                    });
                    
                }
                $(this).remove();
            },
            "Cancel": function()
            {
            	if(cancelCallback)
            		cancelCallback();
                $(this).remove();
            }
        },
        close: function(ev, ui)
        {
        	if(cancelCallback)
            		cancelCallback();
            $(this).remove();
        }
    });
    dialogDiv.find('#password').focus();
    addDialogKeys(dialogDiv, null, "Create");
    sortButtons(dialogDiv);
}

function findProject()
{
    var projectsDC = getDataCollection(appInfo.userProjectsPath);
    projectsDC.getNameList(function(nameList){
        var dialogDiv = $('<div title="'+resources.dlgSearchPrjTitle+'"></div>');
        var searchStr = $('<input type="text" placeholder="Start typing project name here..."/>').width(270);
        dialogDiv.append(searchStr);
        
        var projectList = $('<ul class="elementList"></ul>')
            .css({"padding": "2px", 
                    "maxHeight": "500px",
                    "margin": "auto"});
            
        for(var i = 0; i < nameList.length; i++)
        {
            var item = $("<li>").attr("data-name", nameList[i].name).height(18)
                .css("background-image", getNodeIcon(projectsDC, nameList[i].name)).css("position", "relative");
            var title = nameList[i].title == undefined?nameList[i].name:nameList[i].title;
            var textDiv = $("<div/>");
            item.append(textDiv);
            fitElement(textDiv, title, true, 250);
            item.click( function(){
                projectList.find("li").removeClass("selected");
                searchStr.val($(this).attr("data-name"));
                $(this).addClass("selected");
            });
            projectList.append(item);
        }
        var listDiv = $('<div class="ui-widget-content"/>').width(270).append(projectList);
        dialogDiv.append($("<div/>").html("Available Projects:").css("padding-top", "10px").append(listDiv));
        searchStr.keyup(function(){
            var filter = searchStr.val().toUpperCase();
            // Loop through all list items, and hide those who don't match the search query
            projectList.find("li").each(function(){
                if ($(this).attr("data-name").toUpperCase().indexOf(filter) > -1) {
                    $(this).show();
                } else {
                    $(this).hide();
                }
            });
        });
        dialogDiv.dialog(
        {
            autoOpen: true,
            modal: true,
            resizable: false,
            width: 300,
            buttons: 
            {
                "Open": function()
                {
                    var path = projectsDC.completeName + "/" + searchStr.val();
                    openBranch(path, true);
                    $(this).remove();
                },
                "Cancel": function()
                {
                    $(this).remove();
                }
            },
            close: function(ev, ui)
            {
                $(this).remove();
            }
        });
        searchStr.focus();
        sortButtons(dialogDiv);
    });
    
}

/*
 * Get size of all projects where user is admin
 */
function getProjectsSize ()
{
    queryBioUML("support/getAllProjectsSize", 
            {
            },
            function(data)
            {
                var sizeTotal= data.value.sizeTotal;
                var msg = resources.dlgProjectsSize.replace("{sizeTotal}", sizeTotal);
                logger.message(msg);
            });
}
