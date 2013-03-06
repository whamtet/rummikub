view = {};

view.animate = function(id, points, m) {
	console.log($('#' + id));
	$('#' + id).show().animate({
		crSpline: $.crSpline.buildSequence(points),
		duration: 200,
	//	complete: function() {core.controller.finish_update(m);}
	});
//	$('#' + id).show().animate(
//			{
//		queue: false,
//		duration: 200,
//		complete: function() {core.controller.finish_update(m);}
//		
//			});
};