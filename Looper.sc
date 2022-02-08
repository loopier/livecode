// !!! TODO
// - add pan
// - add prelevel (+ reclevel?)
// - add pitchbend?
// - add mic independent of recording (to be able to listen to the sound before recording it)
// - make gui vertical?
// - rec length = to first buffer recording length?
// - add startPos slider
// - add quant (sync) button
// - add all_controllers (numboxes) for every param

Looper {
	var m_server;
	var num_buffers;
	var buffer_max_length;
	var <>loopers;
	var buffers;
	var rec_buttons;
	var clear_buttons;
	var mic;

	*new { arg server = Server.default, num_bufs = 4, buf_length = 16;
		^super.new.initLooper(server, num_bufs, buf_length);
	}


	initLooper { arg server, num_bufs, buf_length;
		m_server = server;
		num_buffers = num_bufs;
		buffer_max_length = buf_length;
		loopers = ();
		buffers = ();
		rec_buttons = ();
		clear_buttons = ();
		("Server: "++m_server.value).postln;
		("# of Buffers: "++num_buffers.value).postln;
		("Buffer length (sec): "++buffer_max_length.value).postln;

		this.gui; // if this goes after 'this.setupAudio', audio must be forked
		this.setupAudio;
	}

	setupAudio {
		num_buffers.do({|i|
			buffers.put(i, Buffer.alloc(Server.default, Server.default.sampleRate * 2, 2));
			loopers.put(i, Synth(\looper, [buffer:buffers[i]]));
		});

		mic = Synth(\soundin, [amp:0]);

		// buffers.postln;
		loopers.postln;
	}

	// create a window with controls for as many loopmachines as buffers
	gui {
		var window, width, height, mic_button, rec_all_button, clear_all_button, rate_label, length_label, amp_label;
		var guis = ();
		if(num_buffers < 3, {
			width = 90 * 3; // need to fit 3 buttons (REC ALL, CLEAR ALL and AUDIO IN)
			},{
			width = num_buffers * 90;//640;
		});
		height = 480;//num_buffers * 30 + 60;
		window = Window.new("Looper", Rect(200,300,width,height));

		num_buffers.do({|i|
			i.postln;
			guis.put(i, this.looperGui(window, index:i, x:(i*90)));
		});

		mic_button = Button(window, Rect(0, window.bounds.height - 30, 90, 30));
		mic_button.name_("mic_button");
		mic_button.states_([
			["AUDIO IN", Color.black, Color.gray],
			["AUDIO IN", Color.black, Color.red]
		]);
		mic_button.action_({
			mic.set(\amp, mic_button.value);
		});

		rec_all_button = Button(window, Rect(mic_button.bounds.width, window.bounds.height - 30, 90, 30));
		rec_all_button.name_("rec_all_button");
		rec_all_button.states_([
			["REC ALL", Color.black, Color.gray],
			["REC ALL", Color.black, Color.red]
		]);
		rec_all_button.action_({
			guis.do({|gui_object|
				gui_object[\rec_button].valueAction_(rec_all_button.value);
			});
		});

		clear_all_button = Button(window, Rect(rec_all_button.bounds.left+rec_all_button.bounds.width, window.bounds.height - 30, 90, 30));
		clear_all_button.name_("rec_all_button");
		clear_all_button.states_([
            ["CLEAR ALL", Color.black, Color.gray]
		]);
		clear_all_button.action_({
			guis.do({|i|
				i[\clear_button].valueAction_(rec_all_button.value);
			});
		});

		window.front;
		CmdPeriod.doOnce{window.close};
		window.onClose_({m_server.quit});
	}

	// control panel for one loopmachine (buffer)
	looperGui { |parent, index=0, x=0, y=0, w=30, h=30|

		var rec_button, rate_label, rate_slider, rate_numbox, clear_button, length_label, length_slider, length_numbox, amp_label, amp_slider, amp_numbox, font;

		font = Font("Arial", 11);

		// REC CONTROL
		rec_button = Button(parent, Rect(x,y,w*3,h));
		rec_button.name_("rec_button_"++index);
		rec_button.states_([
            ["REC", Color.black, Color.gray],
            ["STOP", Color.black, Color.red]
		]);
		rec_button.action_({
			rec_button.name.postln;
			loopers[index].set(\t_rec, 1);
		});

		// RATE CONTROL
		y = y + rec_button.bounds.height;

		// label
		rate_label = StaticText(parent, Rect(x,y,w,h));
		rate_label.string_("Rate");
		rate_label.align_(\center);
		rate_label.font_(font);

		// slider
		y = y + rate_label.bounds.height;

		rate_slider = Slider(parent, Rect(x, y, w, 255));
		rate_slider.name_("rate_slider_"++index);
		rate_slider.valueAction_(1.linlin(-4,4, 0,1));
		rate_slider.action_({
			var value = rate_slider.value.linlin(0,1,-4,4);
			loopers[index].set(\rate, value);
			// update numbox when the value has changed in the slider
			rate_numbox.value_(value);
		});
		rate_slider.toolTip_("Rate");

		// numbox
		y = y + rate_slider.bounds.height;

		rate_numbox = NumberBox(parent, Rect(x, y, w, h));
		rate_numbox.name_("rate_numbox_"++index);
		rate_numbox.font_(font);
		rate_numbox.step_(0.1);
		rate_numbox.scroll_step_(0.1);
		rate_numbox.valueAction_(1);
		rate_numbox.action_({
			var value = rate_numbox.value.linlin(-4,4, 0,1);
			rate_slider.value_(value);
			loopers[index].set(\rate, rate_numbox.value);
		});

		// LENGTH CONTROL
		x = x + rate_slider.bounds.width;
		y = rate_label.bounds.top;

		// label
		length_label = StaticText(parent, Rect(x,y,w,h));
		length_label.string_("Len");
		length_label.align_(\center);
		length_label.font_(font);

		// slider
		y = rate_slider.bounds.top;

		length_slider = Slider(parent, Rect(x, y, w, 255));
		length_slider.name_("length_slider_"++index);
		length_slider.value_(1);
		length_slider.action_({
			var value = length_slider.value;
			loopers[index].set(\length, value);
			// update numbox when the value has changed in the slider
			length_numbox.value_(value);
		});

		// numbox
		y = rate_numbox.bounds.top;

		length_numbox = NumberBox(parent, Rect(x, y, w, h));
		length_numbox.name_("length_numbox_"++index);
		length_numbox.font_(font);
		length_numbox.step_(0.1);
		length_numbox.scroll_step_(0.1);
		length_numbox.valueAction_(1);
		length_numbox.action_({
			var value = length_numbox.value;
			length_slider.value_(value);
			loopers[index].set(\length, length_numbox.value);
		});

		// AMP CONTROL
		x = x + length_slider.bounds.width;

		//label
		y = length_label.bounds.top;

		amp_label = StaticText(parent, Rect(x, y, w, h));
		amp_label.string_("Amp");
		amp_label.align_(\center);
		amp_label.font_(font);

		// slider
		y = length_slider.bounds.top;

		amp_slider = Slider(parent, Rect(x, y, w, 255));
		amp_slider.name_("amp_slider_"++index);
		amp_slider.value_(0.5);
		amp_slider.action_({
			var value = amp_slider.value;
			loopers[index].set(\amp, value);
			// update numbox when the value has changed in the slider
			amp_numbox.value_(value);
		});

		// numbox
		y = length_numbox.bounds.top;

		amp_numbox = NumberBox(parent, Rect(x, y, w, h));
		amp_numbox.name_("amp_numbox_"++index);
		amp_numbox.font_(font);
		amp_numbox.step_(0.1);
		amp_numbox.scroll_step_(0.1);
		amp_numbox.valueAction_(1);
		amp_numbox.action_({
			var value = amp_numbox.value;
			amp_slider.value_(value);
			loopers[index].set(\amp, amp_numbox.value);
		});

		// CLEAR CONTROL
		x = rec_button.bounds.left;
		y = rate_numbox.bounds.top + rate_numbox.bounds.height;
		clear_button = Button(parent, Rect(x,y,w*3,h));
		clear_button.name_("clear_button_"++index);
		clear_button.states_([
            ["CLEAR", Color.black, Color.gray]
		]);
		clear_button.action_({
			clear_button.name.postln;
			buffers[index].zero;
		});

		^Dictionary.newFrom([rec_button:rec_button, rate_slider:rate_slider, rate_numbox:rate_numbox, clear_button:clear_button, length_slider:length_slider, length_numbox:length_numbox, amp_slider:amp_slider, amp_numbox:amp_numbox]);
	}

	clear {
		loopers.size.do({|i| loopers[i].free; buffers[i].zero});
	}
}