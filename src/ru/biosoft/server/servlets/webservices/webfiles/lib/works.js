function createNewWork(path)
{
	createBeanEditorDialog(resources.dlgCreateWorkTitle, "properties/work", function(data)
	{
		queryBioUML("web/work/newWork", {
			json: data,
			de: path
		}, function(data)
		{
			refreshTreeBranch(path);
		});
	}, true);
}

function editWork(path)
{
	createBeanEditorDialog(resources.dlgEditWorkTitle, "properties/work/"+path, function(data)
	{
		queryBioUML("web/work/editWork", {
			json: data,
			de: path
		}, function(data)
		{
			refreshTreeBranch(getElementPath(path));
		});
	}, true);
}