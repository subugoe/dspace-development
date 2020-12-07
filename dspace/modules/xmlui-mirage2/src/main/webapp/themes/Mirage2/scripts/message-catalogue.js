/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

$(function() {
	// this is ugly but it works; modal needs to be higher up
	// to work probably.
	$("#main-container").append($("#deleteModal"));
	$("#main-container").append($("#addModal"));

	$(window).on('load', function() {
		setTimeout(function(){ $("#backdropDiv").hide(); }, 2000);
	});

	$("#closeErrorAddAlert").click(function() {
		$("#errorAddAlert").hide();
	})

	$("#closeErrorDeleteAlert").click(function() {
		$("#errorDeleteAlert").hide();
	})

	$("#closeSuccessAddAlert").click(function() {
		$("#successAddAlert").hide();
	})

	$("#closeSuccessDeleteAlert").click(function() {
		$("#successDeleteAlert").hide();
	})

	$("#closeWarningInvalidXmlAlert").click(function() {
		$("#warningInvalidXmlAlert").hide();
	})

	// Edit buttons
	$('button[name="Edit"]').click(function(e) {
		makeEditable($(this));
	})

	function makeEditable(elem) {
		var inputs = findTextInput(elem);
		var text_input = inputs[0]
		var hidden_input = inputs[1]
		text_input.prop("disabled", false);
		var span = elem.find("span");
		span.removeClass();
		span.addClass("glyphicon glyphicon-floppy-disk");

		// remove previous success/failure indicators
		var div = text_input.closest("div");
		div.children(".result-indicator").remove();
		div.removeClass();

		elem.off("click");
		elem.click(function() {
			var span = elem.find("span");
			span.removeClass();
			span.addClass("glyphicon glyphicon-repeat right-spinner");
			saveMessage($(this))
		});
	}
	function saveMessage(elem) {
		var urlParams = new URLSearchParams(window.location.search);

		var inputs = findTextInput(elem);
		var text_input = inputs[0]
		var hidden_input = inputs[1]

		var isValid = isXmlValid(text_input.val());
		if(!isValid) {
			var div = text_input.closest("div");

			// create save button
			var span = elem.find("span");
			span.removeClass();
			span.addClass("glyphicon glyphicon-floppy-disk");

			elem.off("click");
			elem.click(function() {
				var span = elem.find("span");
				span.removeClass();
				span.addClass("glyphicon glyphicon-repeat right-spinner");
				saveMessage($(this))
			});

			displayFieldFeedback(div, "glyphicon-warning-sign", "(warning)", "has-warning")
			$(".alert").hide();
			$("#warningInvalidXmlAlert").show();
			return;
		}
		$.ajax({
			// FIXME
			url: window.DSpace.context_path + "/admin/catalogue/message/save",
			method: 'post',
			data: {
				'message-key': hidden_input.val(),
				'message-value': text_input.val(),
				'message-catalogue': urlParams.get('catalogue')
			},
			success: function(response) {
				text_input.prop("disabled", true);
				showEditBtn(elem);

				$(".alert").hide();
				// remove previous success/failure indicators (if any)
				var div = text_input.closest("div");
				displayFieldFeedback(div, "glyphicon-ok", "(success)", "has-success")
			},
			error: function(response) {
				text_input.prop("disabled", true);
				showEditBtn(elem);

				$(".alert").hide();
				// remove previous success/failure indicators (if any)
				var div = text_input.closest("div");
				displayFieldFeedback(div, "glyphicon-remove", "(error)", "has-error")
			}

		})
	}

	function showEditBtn(elem) {
		var span = elem.find("span");
		span.removeClass();
		span.addClass("glyphicon glyphicon-edit");
		elem.off("click");
		elem.click(function(e) {
			makeEditable($(this));
		});
	}

	function displayFieldFeedback(div, icon, indicatorText, className) {
		div.removeClass();
		div.children(".result-indicator").remove();

		// set new success indicator
		div.append('<span class="result-indicator glyphicon ' + icon + ' form-control-feedback" aria-hidden="true"></span>');
		div.append('<span class="result-indicator sr-only">' + indicatorText + '</span>');
		div.addClass(className + " has-feedback");
	}

	// Add buttons
	$("#addMessageBtn").click(function() {
		$("#okAddBtn").click(function() {
			var msgKey = $("#new-message-key").val();
			if (msgKey) {
				var valid = addMessage();
				if (valid) {
					$("#new-message-key").val('');
					$('#new-message-value').val('');
					$("#invalidXmlErrorMsg").hide();
					$("#addModal").modal("hide");
				}
			} else {
				$("#key-error-msg").show();
			}
		})
		$("#addModal").modal("show");
	})

	function addMessage() {
		var urlParams = new URLSearchParams(window.location.search);

		var newMessageKey = $("#new-message-key").val()
		var newMessageValue = $("#new-message-value").val()

		if (!isXmlValid(newMessageValue)) {
			$("#invalidXmlErrorMsg").show();
			return false;
		}

		$.ajax({
			// FIXME
			url: window.DSpace.context_path + "/admin/catalogue/message/add",
			method: 'post',
			data: {
				'message-key': newMessageKey,
				'message-value': newMessageValue,
				'message-catalogue': urlParams.get('catalogue')
			},
			success: function(response) {
				var nrOfRows = $('#message-table tr').length;
				var oddOrEvenClass = nrOfRows % 2 == 0 ? "odd" : "even";
				var tr = $('<tr class="ds-table-row ' + oddOrEvenClass + '"></tr>');
				var td1 = $("<td>" + newMessageKey + "</td>");
				tr.append(td1);

				var td2 = $("<td></td>");
				tr.append(td2);
				var div = $("<div></div>");
				td2.append(div);
				var input = $('<textarea cols="18" rows="3" id="CatalogueEditTransformer_field_text_' + newMessageKey.replaceAll(".", "_") + '" class="ds-text-field form-control" disabled="" name="' + newMessageKey.replaceAll(".", "_") + '" />') ;
				input.val(newMessageValue.replaceAll('"', "&quot;"));
				div.append(input);

				var td3 = $("<td></td>");
				tr.append(td3)
				var input2 = $('<input id="CatalogueEditTransformer_field_' + newMessageKey.replaceAll(".", "_") + '" class="ds-hidden-field form-control" name="' + newMessageKey.replaceAll(".", "_") + '" type="hidden" value="' + newMessageKey + '">')
				td3.append(input2);
				var nobr = $("<nobr></nobr>");
				var editBtn = $('<button name="Edit" class="btn btn-default"><span class="glyphicon glyphicon-edit"></span></button>');
				editBtn.click(function(e) {
					makeEditable(editBtn);
				})
				nobr.append(editBtn);
				var deleteBtn = $('<button name="Delete" class="btn btn-default"><span class="glyphicon glyphicon-trash"></span></button>');
				deleteBtn.click(function(e) {
					deleteHandler(deleteBtn);
				})
				nobr.append(deleteBtn);
				td3.append(nobr);

				$("#message-table").append(tr);
				$(".alert").hide();
				$("#successAddAlert").show();
			},
			error: function(response) {
				$(".alert").hide();
				$("#errorAddAlert").show();
			}
		})

		return true;
	}

	// Delete buttons
	$('button[name="Delete"]').click(function(e) {
		deleteHandler($(this));
	})

	function deleteHandler(elem) {
		var inputs = findTextInput(elem);
		var hidden_input = inputs[1]
		$("#okDeleteBtn").click(function() {
			deleteMessage(hidden_input);
			$("#deleteModal").modal("hide");
		})
		$("#keyToDelete").text(hidden_input.val());
		$("#deleteModal").modal("show");
	}

	function deleteMessage(hidden_input) {
		var urlParams = new URLSearchParams(window.location.search);
		$.ajax({
			// FIXME
			url: window.DSpace.context_path + "/admin/catalogue/message/remove",
			method: 'post',
			data: {
				'message-key': hidden_input.val(),
				'message-catalogue': urlParams.get('catalogue')
			},
			success: function(response) {
				var tr = hidden_input.closest("tr");
				tr.remove();
				$(".alert").hide();
				$("#successDeleteAlert").show();
			},
			error: function(response) {
				$(".alert").hide();
				$("#errorDeleteAlert").show();
			}
		})
	}



	function findTextInput(elem) {
		var td = elem.closest("td");
		var hidden_input = td.find('input[type="hidden"]');
		return [$("#CatalogueEditTransformer_field_text_" + hidden_input.attr("name")), hidden_input];
	}

	function isXmlValid(message) {
		try {
			var msg = $.parseXML("<root>" + message + "</root>")
		} catch(error) {
			console.log(error)
			return false
		}
		return true
	}
})
