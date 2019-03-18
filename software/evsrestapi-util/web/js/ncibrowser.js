function submitSearchOnEnter(form, event)
{
	if (event.which){
		if(event.which == 13){
			if (validateSearchForm(form)) {
				window.submitForm('search_form',1,{source:'search'});
			}
			return false;
		}
	} else
	{
		if(window.event.keyCode==13)
		{
			if (validateSearchForm(form)) {
				window.submitForm('search_form',1,{source:'search'});
			}
			return false;
		}
	}
}


function validateSearchForm(form) {
	errors = false;
	errorMsg = "Form validate failures:";
	if (form.search_string.value=="") {
		errorMsg += "\nSearch Text - A value must be entered.";
		errors = true;
	}
	else {
		if (form.search_string.value.length < 3) {
			errorMsg += "\nSearch Text - The search text must be at least 3 characters in length.";
			errors = true;
		}
	}
	if (typeof form.search_in != "undefined") {
		var searchInCheckboxes = 0;
		for (var i=0; i<form.search_in.length;i++) {
			if (form.search_in[i].checked) {
				searchInCheckboxes++;
			}
		}
		if (searchInCheckboxes == 0) {
			errorMsg += "\nSearch In - At least one value must be selected.";
			errors = true;
		}
	}
	if (errors == true) {
		alert(errorMsg);
		return false;
	}
	return true;
}







