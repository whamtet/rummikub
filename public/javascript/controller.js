controller = {};

$(document).keypress(function(event) {
	console.log(event.charCode);
	switch (event.which) {
	case 13:
		core.controller.pass();
		break;
	case 112:
		core.controller.pick_up();
		break;
	case 115:
		core.controller.sort_tiles();
		break;
	}
});

controller.drag = function(event, ui) {
  var helper = ui.helper[0];
  var left = helper.style.left;
  left = Number(left.substr(0, left.length - 2));
  var top = helper.style.top;
  top = Number(top.substr(0, top.length - 2));
  core.controller.record(left, top);
};
controller.privatize = function(event, ui) {
	
	var helper = ui.helper[0];
	var left = helper.style.left;
	left = Number(left.substr(0, left.length - 2));
	var top = helper.style.top;
	top = Number(top.substr(0, top.length - 2));
	core.controller.record(left, top);
    core.controller.privatize(helper.id, left, top);	
//  core.controller.set_location(core.model.game, ui.helper[0].id, player, left, top);
  //core.controller.post_update();
};
controller.publicize = function(event, ui) {
	var helper = ui.helper[0];
	var left = helper.style.left;
	left = Number(left.substr(0, left.length - 2));
	var top = helper.style.top;
	top = Number(top.substr(0, top.length - 2));
	core.controller.record(left, top);
    core.controller.publicize(helper.id, left, top);
//  core.controller.set_location(core.model.game, ui.helper[0].id, 'table', left, top);
  //core.controller.post_update();
};
controller.start_drag = function(event, ui) {
	var helper = ui.helper[0];
	core.controller.start_drag(helper.id);
};
controller.set_position = function(id, x, y) {
	$('#' + id).css({left: x, top: y});
};
controller.after_display = function(game_width, game_height, player) {
  $('.tilefromtable, .tilefrom' + player).draggable(
  {
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