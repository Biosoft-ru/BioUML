var currentProjectPath = null;

function setCurrentProject(projectName, callback)
{
    var journalName = projectNameToJournal(projectName);
    queryBioUML('web/journal/set', { journalName: journalName}, function() {
        setCurrentProjectPath(projectName);
        if(journalBox != null)
            journalBox.val(journalName);
        markActiveProject();
        if(callback)
            callback();
    });
}

//Init current project as first writable if project is required. Use 'userProjectRequired' flag in defines.js 
//For some analyses/hub selectors it is required, but this can not be checked on client side. 
function initCurrentProject()
{
    if( currentProjectPath==null && appInfo.userProjectRequired)
    {
        findWritableProjectPath(function(prjPath) {
            if(prjPath)
                setCurrentProject(getElementName(getElementPath(prjPath)));
            else
                logger.error(resources.commonErrorNoSave);
        });
    }   
}

function projectNameToJournal(projectName)
{
    if(projectName && projectName.length > 1)
        return 'Research: ' + projectName;
    else
        return '-';
}

function journalNameToProject(journalName)
{
    var name = journalName.match(/:\s*([^\s].+)$/);
    if( name != null && name.length > 1 )
        return name[1];
    else
        return null;
}

function setCurrentProjectPath(projectName)
{
    currentProjectPath = null;
    if( projectName != null)
    {
        currentProjectPath = createPath(appInfo.userProjectsPath, projectName);
    }
}

function setProjectByCollection(dc)
{
    if(dc.getName().length > appInfo.userProjectsPath.length && dc.getName().substring(0, appInfo.userProjectsPath.length+1) == appInfo.userProjectsPath+"/")
    {
        var project = getPathComponents(dc.getName())[getPathComponents(appInfo.userProjectsPath).length];
        setCurrentProject(project);
    }
}

function showProjectProperties()
{
    //old variant for enabled project selector
    if(journalBox != null && !BioUML.disableProjectSelector && !Boolean(appInfo.disableProjectSelector) && ( perspective && perspective.projectSelector))
    {
        var project = journalBox.val();
        if(project=="-")
        {
            logger.error(resources.commonProjectIsNotSelected);
            return;
        }
        createBeanEditorDialog(resources.dlgEditPrjProperties.replace("{project}", project), "properties/project/"+project);
    }
    else //project selector integrated with properties
    {
        if(!currentProjectPath)
        {
            logger.error(resources.commonProjectIsNotSelectedNoBox);
            return;
        }
        //(title, getBeanPath, callback, autoUpdate, values, selectedValue, onChange)
        queryBioUML("web/journal/init", {}, function(data){
            createBeanEditorDialogWithSelector(resources.dlgEditPrjPropertiesGeneral, 
                function(project){ 
                    return "properties/project/"+project;  
                }, 
                false, false, data.values.names, data.values.current, 
                function(project, callback){
                    if(journalBox)
                        journalBox.val(project);
                    setCurrentProject(journalNameToProject(project), callback);
                }
            )
        });
    }
}

function getDefaultProjectDataPath()
{
    if( currentProjectPath != null )
        return path = currentProjectPath + "/Data"; 
    else
        return null;
}


function createNewProjectChecked() {
	if( !currentUser || currentUser == '' ) {
		logger.message('Please, login to create your project.');
		return;
	}
	createNewProject();
}

//TODO: merge with getDefaultProjectDataPath
function constructSelectedProjectPath() {
	if( currentProjectPath == null )
		return appInfo.userProjectsPath + '/Demo/Data';
	return currentProjectPath + '/Data';
}

function isWritable(path) {
    if(!isPathExists(path))
      return false;
    var parentPath = getElementPath(path);
    var name = getElementName(path);
    var perm = getDataCollection(parentPath).getPermission(name);
    return perm & 0x04;
}

function findWritableProjectPath(success, failure) {
  if(currentProjectPath && isWritable(currentProjectPath)) {
    success(currentProjectPath);
    return;
  }
  let projects = getDataCollection(appInfo.userProjectsPath);
  projects.getNameList(function(nameList) {
        for(var i = 0; i < nameList.length; i++)
        {
            var name = nameList[i].name;
            var path = appInfo.userProjectsPath + "/" + name + "/Data";
            if(isWritable(path)) {
              success(path);
              return;
            }
        }
        failure();
  }, failure);
}

function renameProject(path)
{
    if(!path) return;
    var projectName = getElementName(path);
    var oldTitle = getDataCollection(path).getAttributes()['displayName'];
    if(!oldTitle)
        oldTitle = projectName;
    createPromptDialog(resources.dlgRenamePrjTitle, resources.dlgRenamePrjName, function(newTitle)
    {
		queryBioUML("support/setProjectTitle", 
        {
            project: projectName,
            title: newTitle
        }, function(data)
        {
            refreshTreeBranch(getElementPath(path));
        }, function(data)
        {
            logger.error(data.message);
        });
    }, oldTitle , true);
}




