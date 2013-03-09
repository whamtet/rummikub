controller = {};

//add keyboard listener
$(document).keypress(function(event) {
	console.log(event.charCode);
	switch (event.which) {
	case 13:
               //player presses enter
		core.controller.pass();
		break;
               //player presses p
	case 112:
		core.controller.pick_up();
		break;
               //player presses s
	case 115:
		core.controller.sort_tiles();
		break;
	}
});

//record the drag path of a tile so it can be sent to opponent
controller.drag = function(event, ui) {
  var helper = ui.helper[0];
  var left = helper.style.left;
  left = Number(left.substr(0, left.length - 2));
  var top = helper.style.top;
  top = Number(top.substr(0, top.length - 2));
  core.controller.record(left, top);
};
//tile has been dragged from table onto player's rack
controller.privatize = function(event, ui) {
	var helper = ui.helper[0];
	var left = helper.style.left;
	left = Number(left.substr(0, left.length - 2));
	var top = helper.style.top;
	top = Number(top.substr(0, top.length - 2));
	core.controller.record(left, top);
    core.controller.privatize(helper.id, left, top);
};
//tile has been dragged from player's rack onto table
controller.publicize = function(event, ui) {
	var helper = ui.helper[0];
	var left = helper.style.left;
	left = Number(left.substr(0, left.length - 2));
	var top = helper.style.top;
	top = Number(top.substr(0, top.length - 2));
	core.controller.record(left, top);
    core.controller.publicize(helper.id, left, top);

};
//at the start of tile dragging, reset drag path
controller.start_drag = function(event, ui) {
	var helper = ui.helper[0];
	core.controller.start_drag(helper.id);
};
//set position of a tile
controller.set_position = function(id, x, y) {
	$('#' + id).css({left: x, top: y});
};

//if it's the current player's turn, enable movement of tiles
controller.after_display = function(game_width, game_height, player) {
  $('.tilefromtable, .tilefrom' + player).draggable({
    drag: controller.drag,
    start: controller.start_drag,
    containment: [0, 0, game_width, game_height]
  });
  $('#rack').droppable({
    drop: controller.privatize
  });
  $('#table').droppable({
    drop: controller.publicize
  });
};
