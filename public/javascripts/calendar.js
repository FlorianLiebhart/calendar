/* jsRoutes.controllers.Tags.list().url */

function updateEvent(eventData, revertFunc) {
  $.ajax({
      type: "PUT",
      url: '/appointment/' + eventData.id,
      dataType: 'json',
      accepts: "application/json; charset=utf-8",
      headers: {
        Accept: "application/json; charset=utf-8",
        "Content-Type": "application/json; charset=utf-8"
      },
      data: JSON.stringify({
        'title': eventData.title,
        'start': eventData.start.valueOf(),
        'end': eventData.end.valueOf(),
        'tagIds': eventData.tagIds
      }),
      success: function(data) {
        findConflicts();
      },
      error: function(xhr) {
        revertFunc();
      },
  })
}

/*
 * Create and post an event to the server. If successful, show in calendar.
 * param eventData: Object with 'description', 'start' and 'end'
 */
function createEvent(eventData, callback) {
  return $.ajax({
    type: "POST",
    url: jsRoutes.controllers.Appointments.add().url,
    dataType: "json",
    accepts: "application/json; charset=utf-8",
    headers: {
      Accept: "application/json; charset=utf-8",
      "Content-Type": "application/json; charset=utf-8"
    },
    data: JSON.stringify({
      'title': eventData.title,
      'start': eventData.start.valueOf(), // Long
      'end': eventData.end.valueOf(), // Long
      'tagIds': eventData.tagIds // TODO: Specify Tag from List
    }),
    error: function(err) {
      console.log("createEvent Error: ");
      console.log(err.responseText);
      $('#calendar').fullCalendar('unselect');
    },
    success: function(appointmentWithTags) {
      var highestPriorityTag = getHighestPriorityTag(appointmentWithTags.tags);

      var newEventData = {
        'id': appointmentWithTags.appointment.id,
        'title': appointmentWithTags.appointment.title,
        'start': appointmentWithTags.appointment.start,
        'end': appointmentWithTags.appointment.end,
        'color': highestPriorityTag.color,
        'tagIds': $.map(appointmentWithTags.tags, function(v) {
          return v.id;
        })
      }

      $('#calendar').fullCalendar('unselect');
      $('#calendar').fullCalendar('renderEvent', newEventData, true); // stick? = true
      callback();
    }
  });
}

function createEventPopover(selectedElement, start, end) {
  getTags()
    .done(function(res) {
      $(selectedElement).popover({
        container: 'body',
        placement: 'auto top',
        html: 'true',
        // title       : 'Create a new event',
        delay: {
          show: 400,
          hide: 400
        },
        content     : function(){
          var tags = res.tags
          var tagListElems = [];
          for (var i = 0; i < tags.length; i++) {
          tagListElems += ""
          + "<label class='btn btn-xs' style='background-color: " + tags[i].color + ";'>"
            + "<input type='checkbox' tagId='" + tags[i].id + "'>" + tags[i].name
          + "</label>"
          };

          return "" 
          + "<div id='newEventPopoverContent'>"
          + "<form class='form-horizontal' role='form'>"
            + "<div class='form-group'>"
            + "<label class='col-sm-3 control-label'>Title</label>"
            + "<div class='col-sm-9'>"
              + "<input type='text' class='form-control' id='newEventTitle' placeholder='Title'>"
            + "</div>"
            + "</div>"

            + "<div class='form-group'>"
            + "<label class='col-sm-3 control-label'>Tags</label>"
            + "<div class='col-sm-9' id='newEventTags'> "
              + tagListElems
            + "</div>"
            + "</div>"

            + "<div class='form-group'>"
            + "<label class='col-sm-3 control-label'>From</label>"
            + "<div class='col-sm-9'>"
              + "<p class='form-control-static' id='newEventStart' date='" + start + "'>" + start.format("dd, MMM DD, HH:mm") + "</p>"
            + "</div>"
            + "<label class='col-sm-3 control-label'>To</label>"
            + "<div class='col-sm-9'>"
              + "<p class='form-control-static' id='newEventEnd' date='" + end + "'>" + end.format("dd, MMM DD, HH:mm") + "</p>"
            + "</div>"
            + "</div>"

            + "<div class='form-group'>"
            + "<div class='col-sm-12'>"
              + "<button type='submit' class='btn btn-primary btn-sm col-sm-9'>Create Event</button>"
              + "<button type='button' id='cancelNewEventSubmit' class='btn btn-default btn-sm col-sm-3'>Cancel</button>"
            + "</div>"
            + "</div>"
          + "</form>"
          + "<div>";
        }
      });


      $(selectedElement).popover('show');
      $('#newEventTitle').focus();


      $("#cancelNewEventSubmit").on("click", function(event) {
        $('#calendar').fullCalendar('unselect');
        $(selectedElement).popover('destroy');

      });

      $('#newEventPopoverContent').keyup(function(e) {
        if (e.keyCode == 27) {
          $('#cancelNewEventSubmit').click();
        } // esc
      });

      $("#newEventPopoverContent").on("submit", function(event) {
        event.preventDefault();

        var newEventTitle = $('#newEventTitle');

        if (!newEventTitle.val().trim()) {
          newEventTitle.parent().addClass("has-error");
          return;
        } else {
          newEventTitle.parent().removeClass("has-error");
        }

        var checkedTags = $('#newEventTags input:checkbox:checked').map(function() {
          return parseInt($(this).attr('tagid'));
        }).get();

        var eventData = {
          'title'  : newEventTitle.val(),
          'start'  : start,
          'end'    : end,
          'tagIds' : checkedTags
        }
        createEvent(eventData, function() {
          $(selectedElement).popover('destroy');
          $(lastSelected).popover('destroy');
          findConflicts();
        });
      });

    })
    .fail(function(err) {
      console.log("Error trying to receive list of tags:")
      console.log(err.responseText)
    });
}

function getHighestPriorityTag(tags) {
  var highestPriorityTag = {
    priority: -1,
    color: "#3A87AD"
  };
  $.each(tags, function(index, value) { // todo: better: use max function here
    if (highestPriorityTag.priority < value.priority)
      highestPriorityTag = value;
  });
  return highestPriorityTag;
}

/* Shows only events belonging to any tag given in ids */
function filterEventsByIds(ids, attr) {
  function listContainsAny(list, filters) {
    return $.inArray(true, $.map(filters, function(val) {
      return $.inArray(val, list) > -1;
    })) > -1;
  }

  $("[" + attr + "]").each(function(index) {
    $(this).removeClass("hidden");
  });

  var eventsBelongingToNoTag = $("[" + attr + "]").filter(function(index, el) {
    var isFiltered = !listContainsAny($(el).attr(attr).split(","), ids);
    // un-comment for test purposes: 
    // console.log($(el).attr(attr).split(",") + ": " + isFiltered);
    return isFiltered;
  });

  eventsBelongingToNoTag.each(function(index) {
    $(this).addClass("hidden");
  });
}

/** 
 * Returns a promise of tags
 */
function getTags() {
  return $.ajax({
    url: jsRoutes.controllers.Tags.list().url,
    type: 'GET',
    dataType: 'json',
    headers: {
      Accept: "application/json; charset=utf-8",
      "Content-Type": "application/json; charset=utf-8"
    }
  });
}

function findFreeTimeSlots() {

  var userIds   = $('#inputUsers').selectize()[0].selectize.items
  var duration  = (moment($('.durationpicker').data("DateTimePicker").getDate(), "HH:mm").hours() * 60 + moment($('.durationpicker').data("DateTimePicker").getDate(), "HH:mm").minutes()) * 60 * 1000  // hours and minutes in millis as Long
  var from      = moment($('.datetimepicker1').data("DateTimePicker").getDate()).valueOf()
  var to        = moment($('.datetimepicker2').data("DateTimePicker").getDate()).valueOf()
  var startTime = (moment($('.timepicker1').data("DateTimePicker").getDate(), "h:mm A").hours() * 60 + moment($('.timepicker1').data("DateTimePicker").getDate(), "h:mm A").minutes()) * 60 * 1000  // hours and minutes in millis as Long
  var endTime   = (moment($('.timepicker2').data("DateTimePicker").getDate(), "h:mm A").hours() * 60 + moment($('.timepicker2').data("DateTimePicker").getDate(), "h:mm A").minutes()) * 60 * 1000  // hours and minutes in millis as Long

  return $.ajax({
    url: jsRoutes.controllers.Proposals.findFreeTimeSlots(userIds, duration, from, to, startTime, endTime).url,
    type: "GET",
    dataType: "json",
    headers: {
      Accept: "application/json; charset=utf-8",
      "Content-Type": "application/json; charset=utf-8"
    }
  });
}

function findConflicts() {
  d3.select("#conflicts ul").selectAll("li").data([]).exit().remove();
  d3.json(jsRoutes.controllers.Appointments.conflicts().url, function(error, data) {
    if (error) {
      d3.select("#conflicts li").remove;
      d3.select("#conflicts").append("p").text("No conflicts found!");
      return;
    }
    var conflicts = d3.select("#conflicts ul").selectAll("li").data(data.conflicts);
    conflicts.enter()
      .append("li")
      .classed({
        "list-group-item": true
      })
      .on("click", function(conflict) {
        var start1 = $.fullCalendar.moment(conflict[0].start);
        var start2 = $.fullCalendar.moment(conflict[1].start);
        if (start1.dayOfYear() == start2.dayOfYear()) {
          $("#calendar").fullCalendar('changeView', 'agendaDay');
        } else if (start1.week() == start2.week()) {
          $("#calendar").fullCalendar('changeView', 'agendaWeek');
        } else {
          $("#calendar").fullCalendar('changeView', 'month');
        }
        if (start1.hour() == 0) {
          start1 = start1.add("hour", 1);
        }
        $("#calendar").fullCalendar('gotoDate', $.fullCalendar.moment(start1));
      })
      .text(function(conflict) {
        return conflict[0].title + " - " + conflict[1].title;
      });
    conflicts.exit().remove();
  })
}

function listProposals() {

  d3.json(jsRoutes.controllers.Proposals.list().url, function(error, data) {

    if(!error && data.proposals.length > 0) {

      var proposals = d3.select("#proposals ul").selectAll("li").data(data.proposals);

      var listItem = proposals.enter()
        .append("li")
        .attr("class", "proposal list-group-item")
        .style("background-color", function(proposal) { return proposal.proposal.color; });

      listItem
        .append("h3")
        .text(function(proposal) { return proposal.proposal.title; })

      listItem
        .append("span")
        .attr("class", "participants")
        .text(function(proposal) { return $.map(proposal.participants, function(par) { return par.name; }).join(", "); });
    }
    else {
      d3.selectAll("#proposals li").remove;
      d3.select("#proposals ul").append("li").text("No proposals found!");
    }
  });
}

function proposalSelectTimes(){
  var proposalName = $('#proposalName').val();
  var inputUsers   = $('#inputUsers').val();

  findFreeTimeSlots()
  .done(function(data) {
    console.log("got free time slots: ");
    console.log(data);

    var timeSlots = data.slots;

    for (var i = 0; i < timeSlots.length; i++) {
      eventData = {
        'title'           : proposalName,
        'start'           : moment(timeSlots[i].start),
        'end'             : moment(timeSlots[i].end),
        'eventBackgroundColor' : "#DDDDDD",
        'editable'        : true
      }
      $('#calendar').fullCalendar('renderEvent', eventData, true); // stick? = true
    }
  })
  .fail(function(xhr) {
      console.log("Unable to show free time slots:");
      console.log(xhr.responseText)
  });

  $('#proposalModal').modal('hide');  
}

function setProposalModalDefaultValues(){
  $('#proposalModalForm').trigger("reset");
  $('#inputUsers').selectize()[0].selectize.clear(); 
  $('#inputUsers').selectize()[0].selectize.refreshItems();
  // Duration Picker (duration of event)
  $('.durationpicker') .data("DateTimePicker").setDate(new Date(1979, 0, 1, 2, 0, 0, 0));
  // Datetime picker (time frame of proposal )
  if(test) {
    $('#proposalName').val("TestProposal");
    $('#inputUsers').selectize()[0].selectize.addItem(1);
    $('#inputUsers').selectize()[0].selectize.addItem(2);
    $('.datetimepicker1').data("DateTimePicker").setDate(new Date(2014, 0, 8, 08, 0, 0, 0));
    $('.datetimepicker2').data("DateTimePicker").setDate(new Date(2014, 0, 8, 22, 0, 0, 0));
  }
  else {
    $('.datetimepicker1').data("DateTimePicker").setDate(new Date());
    $('.datetimepicker2').data("DateTimePicker").setDate(moment().add('d', 14));
  }
  // Datetime picker (time frame within a day of proposal)
  $('.timepicker1')    .data("DateTimePicker").setDate(new Date(1979, 0, 1, 08, 0, 0, 0));
  $('.timepicker2')    .data("DateTimePicker").setDate(new Date(1979, 0, 1, 22, 0, 0, 0));
}

function listTags() {
  
  function generateMenu(tag) {
    return $(
      "<div class='menu dropdown'>" +
      "  <a class='open-menu dropdown-toggle caret'" +
      "     style='color:" + tag.color + ";'" +
      "     role='button'" +
      "     data-toggle='dropdown'" +
      "     href='#'/>" +
      "  <ul class='dropdown-menu' role='menu'>" +
      "    <li role='presentation'>" +
      "      <a role='menuitem' tabindex='-1' onclick='startEditTag($(this).closest(\".tag\"))'>Edit tag</a>" +
      "    </li>" +
      "    <li role='presentation'>" +
      "      <a role='menuitem' tabindex='-1' onclick='deleteTag(" + tag.id + ")'>Delete tag</a>" +
      "    </li>" +
      "  </ul>" +
      "</div>"
    );
  }

  function showMenu(container) {
    $(container).append(generateMenu(container.__data__));
    $(".menu", container).on("hidden.bs.dropdown", function() {
      hideMenu(container);
    });
  }

  function hideMenu(container) {
    d3.select(container).select(".menu").remove();
  }

  d3.json(jsRoutes.controllers.Tags.list().url, function(error, data) {

    if (!error && data.tags.length > 0) {

      var tags = d3.select("#tags ul").selectAll("li").data(data.tags);

      // Enter
      tags.enter()
        .append("li")
        .attr("class", "tag")
        .attr("tagid", function(tag) {
          return tag.id
        })
        .on("mouseenter", function() {
          if (!$("#tags").data("editing") && !$(".menu ul", this).is(":visible"))
            showMenu(this);
        })
        .on("mouseleave", function() {
          if (!$(".menu ul", this).is(":visible"))
            hideMenu(this);
        })
        .append("span")
        .attr("class", "name btn btn-xs tag-filter-on")
        .style("background-color", function(tag) {
          return tag.color;
        })
        .text(function(tag) {
          return tag.name;
        })
        .on("click", function() {
          $(this).toggleClass("tag-filter-on");
          $(this).toggleClass("tag-filter-off");
          filterEventsByIds($(".tag-filter-on").parent().map(function() {
            return $(this).attr("tagid");
          }).get(), "tagIds");
        })


      // Update
      tags
        .each(function() {
          d3.select(this).select(".name")
            .style("background-color", function(tag) {
              return tag.color;
            })
            .text(function(tag) {
              return tag.name;
            });
        });

      // Exit
      tags.exit().remove();

    } else {

      d3.selectAll("#tags li").remove;
      d3.select("#tags").append("p").text("No tags found!");
    }
  })
}

function generateForm(name, priority) {
  return $(
    "<form id='edit-tag'>" +
    "  <input name='name'" +
    "         type='text'" +
    "         class='form-control'" +
    "         value='" + name + "'" +
    "         placeholder='Name'>" +
    "  <input name='priority'" +
    "         type='number'" +
    "         class='form-control'" +
    "         value='" + priority + "'" +
    "         min='1'" +
    "         max='5'" +
    "         step='1'>" +
    "  <div class='form-group'>" +
    "    <button name='submit' type='submit' class='btn btn-xs btn-primary'>Save</button>" +
    "    <button name='cancel' type='button' class='btn btn-xs btn-default'>Cancel</button>" +
    "  </div>" +
    "</form>"
  );
}

function startEditTag(tag) {
  $("#tags").data("editing", true);

  var name = tag[0].__data__.name;
  var priority = tag[0].__data__.priority;

  $(".name", tag).remove();

  var form = generateForm(name, priority);

  tag.append(form);
  $("input[name=name]", form).focus();
  $("button[name=cancel]", form).on("click", function() {
    cancelEdit($(this).closest('.tag'));
  });
  form.submit(function() {
    saveEdit($(this).closest('.tag'));
    return false;
  });
}

function saveEdit(tag) {
  var newTagName = $("input[name=name]", tag).val();
  var newPriority = parseInt($("input[name=priority]", tag).val());

  var data = {
    "name": newTagName,
    "priority": newPriority,
    "color": tag[0].__data__.color
  };

  $.putJson(jsRoutes.controllers.Tags.update(tag[0].__data__.id).url, data)
    .done(function(data) {
      $(tag).remove();
      $("#tags").data("editing", false);
      listTags();
    })
    .fail(function() {
      console.log("Unable to update tag with id " + tag[0].__data__.id);
    });
}

function cancelEdit(tag) {
  $("#edit-tag").remove();
  $("<span class='name btn btn-xs tag-filter-on' style='background-color:" + tag[0].__data__.color + "'>" + tag[0].__data__.name + "</span>").appendTo(tag);
  $("#tags").data("editing", false);
}

function deleteTag(id) {
  $.delete(jsRoutes.controllers.Tags.delete(id).url)
    .done(function(data) {
      listTags();
    })
    .fail(function() {
      console.log("Unable to delete tag with id " + id);
    });
}

function startAddTag() {
  $("#tags").data("editing", true);

  var tag = $("<li class='tag'></li>").appendTo($("#tags ul"));
  var form = generateForm("", 1);

  tag.append(form);
  $("input[name=name]", form).focus();
  $("button[name=cancel]", form).on("click", function() {
    cancelAdd($(this).closest('.tag'));
  });
  form.submit(function() {
    saveAdd($(this).closest('.tag'));
    return false;
  });
}

function cancelAdd(tag) {
  tag.remove();
  $("#tags").data("editing", false);
}

function saveAdd(tag) {
  var data = {
    "name": $("#edit-tag input[name=name]").val(),
    "priority": parseInt($("#edit-tag input[name=priority]").val()),
    "color": "#c3a12c"
  };

  $.postJson(jsRoutes.controllers.Tags.add().url, data)
    .done(function(data) {
      tag.remove();
      $("#tags").data("editing", false);
      listTags();
    })
    .fail(function() {
      console.log("Unable to add tag!");
    });
}