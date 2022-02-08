// Example use:
// (
// Pdef(\x, Pbind(
// 	\type, \sample,
// 	\channels, 1,
// 	\sound, ~avlkick,
// 	\n, Pwhite(0,9),
// )).play
// )

LoopierEventTypes {
	*new {
		Event.addEventType(\sample, { |server|
			~sound = ~sound ? [];
			~n = ~n ? 0;
			~channels = ~channels ? 2;
			~instrument = [\playbufm, \playbuf][~channels-1];
			~buf = ~sound.at(~n.mod(~sound.size));
			// TODO: !!! ~note modifies rate
			~type = \note;
			currentEnvironment.play;

			// ~sound.size.debug("num samples");
			// ~channels.debug("channels");
			// ~instrument.debug("ins");
			// ~n.debug("n");
			// ~buf.debug("buf");
			// ~octave.debug;
			// "".postln;
		},
			// defaults
			(legato: 1)
		);

		Event.addEventType(\midiOnCtl, { |server|
			var original = currentEnvironment.copy.put(\type, \midi);
			~midicmd.do { |cmd|
				original.copy.put(\midicmd, cmd).play;
			};
		});

		Event.addEventType(\drumkit, { |server|
			// TODO: load from directory with standardized names
			~k = ~k ? ~kick;
			~sn = ~sn ? 1;
			~kicksound = ~k ? ~kicksound;
			~snsnoud = ~snsound ? [];
			~kickchannels = ~kickchannels ? 2;
			~snchannels = ~snchannels ? 2;
			~kickamp = ~kickamp ? 0.5;
			~snamp = ~snamp ? 0.5;
			~instrument = [\playbufm, \playbuf][
				~kickchannels - 1,
				~snchannels - 1
			];

			~buf = [
				~kick.at((~kickamp * ~kick.size).floor),
				~sn.at((~snamp * ~sn.size).floor),
				~kick.at((~kickamp * ~kick.size).floor),
			];
			~type = \note;
			currentEnvironment.play;
		},
			//defaults
			(legato:1)
		);

		// Event.addEventType(\speak, { |server|
		// 	~voice = ~voice ? "kal";
		// 	~say = ~say ? "hello";
		// 	~unixcmd = Pfunc{ "mimic -voice % -t %".format(~voice, ~say).unixCmd };
		// 	~amp = 0;
		// 	currentEnvironment.play;
		//
		// 	~unixcmd.debug("unix");
		// 	"".postln;
		// 	},
		// 	// defaults
		// 	(legato: 1)
		// );

	}
}

Loopier {
	// boot server and display meter and scope with 2 channels
	*boot { arg scopeStyle = 2, server = Server.default, inputChannels = 2, outputChannels = 2, meter = false, plotTree = false, scope = false, freqScope = false;
		var freqscopeWindow;
		this.serverOptions(server, inputChannels, outputChannels);
		server.waitForBoot {

			if (meter) {
				server.meter(inputChannels, outputChannels)
				.window.bounds_(Rect(0,0,138 + ((inputChannels + outputChannels - 4) * 20),253))
				.alwaysOnTop_(true);
			};
			// server.meter(server.numInputBusChannels, server.numOutputBusChannels);
			if (freqScope) {
				freqscopeWindow = FreqScope.new(400, 200, 0, server: server)
				.window.bounds_(Rect(145, 0, 485, 250))
				.alwaysOnTop_(true);
			};
			if (scope) {
				server.scope(2).style_(scopeStyle)
				.window.bounds_(Rect( 145 + 485, 0, 200, 250))
				.alwaysOnTop_(true);
			};
			if (plotTree) {
				server.plotTree;
			};
		};
	}

	*serverOptions { arg server = Server.default, inputChannels = 2, outputChannels = 2;
		server.options.numBuffers = 1024 * 16; // increase this if you need to load more samples
		server.options.memSize = 8192 * 32; // increase this if you get "alloc failed" messages
		server.options.maxNodes = 1024 * 32;
		server.options.numInputBusChannels = inputChannels;
		server.options.numOutputBusChannels = outputChannels;
	}

	*quit { arg server = Server.default;
		server.quit;
	}

	*livecode { arg sampleList;
		// depends on LoopierEventTypes (LoopierPatterns)
		var samples = sampleList ? Loopier.loadSamplePathsList(sampleList);
		LoopierEventTypes.new;
		SynthDescLib.read;
		Loopier.loadSamplesAsSymbols(samples);
		Loopier.listLoadedSamples;
	}

	// generates buffers to be played with Synth(\eliosc)
	//
	// TODO: Maybe the SynthDef could be included here
	//
	// original code by Eli Fieldsteel:
	// https://sccode.org/1-5bF
	//
	// \param	s	Server
	*elibufs { |s|
		var wt_sig, wt_buf;
		//10 wavetables with increasing complexity
		wt_sig = 10.collect({
			arg i;

			//random number of envelope segments
			var numSegs = i.linexp(0,9,4,40).round;

			Env(
				//env always begins and ends with zero
				//inner points are random from -1.0 to 1.0
				[0]++({1.0.rand}.dup(numSegs-1) * [1,-1]).scramble++[0],

				//greater segment duration variety in higher-index wavetables
				{exprand(1,i.linexp(0,9,1,50))}.dup(numSegs),

				//low-index wavetables tend to be sinusoidal
				//high index wavetables tend to have sharp angles and corners
				{[\sine,0,exprand(1,20) * [1,-1].choose].wchoose([9-i,3,i].normalizeSum)}.dup(numSegs)
			).asSignal(1024);
		});

		//load into 10 buffers in wavetable format
		wt_buf = Buffer.allocConsecutive(10, s, 2048, 1, {
			arg buf, index;
			buf.setnMsg(0, wt_sig[index].asWavetable);
		});

		^wt_buf;
	}

	*tidal { arg extraBufs = nil, server = Server.default, inputChannels = 2, outputChannels = 2;
		this.serverOptions(server, inputChannels, outputChannels);
		server.waitForBoot{
			SuperDirt.start;
			// server.sync;
			// this.tidalExtra;
		};
	}

	*tidalExtra {
		// SynthDescLib.read;
		// (Platform.userHomeDir ++ "/Dropbox/loopier/setups/setup-superdirt-buffers.scd").load;
		// var b = (Platform.userHomeDir ++ "/Dropbox/loopier/setups/setup-sampleslist.scd").load; // loads ~samples variable
        var b = Loopier.loadSamplePathsList;
        b.do {|path|
            ~dirt.loadSoundFileFolder(path, PathName(path).folderName);
        };
		(Platform.userHomeDir ++ "/loopier/code/synthdefs/synthdef-tidal.scd").load;
	}

    // Genereates ~smaples variable to store a list of paths to samples and returns its contents.
    *loadSamplePathsList { arg pathToListFile;
        var b;
        if(pathToListFile == nil, b = (Platform.userHomeDir++"/loopier/setups/setup-sampleslist.scd").load);
        ^b;
    }


    // *loadSamplesFromPathsList { arg paths;
    //     paths.do {|path|
    //         Loopier.loadSampleFiles(path);
    //     }
    // }

	// loads samples from a folder.  Samples must be in subfolders within the given path
	// returns a Dictionary with name:buffer pairs.  To get samples directly from folder see loadSampleFiles below.
	*loadSampleDirectories { arg path, s = Server.default;
		var d = Dictionary.new;
		d.add(\foldernames -> PathName(path).entries);
		d[\foldernames].do({
			arg item, i;
			var tempdict = item.entries;
			tempdict.do({ arg sf, i; tempdict.put(i, Buffer.read(s, sf.fullPath)) });
			d.add(item.folderName -> tempdict);
			item.folderName.post; "(".post; item.entries.size.post; ")".postln;
		});

		d.removeAt(\foldernames);
		/*d[\foldernames].do {
		arg item, i;
		// i.post;": ".post;d[\foldernames][i].folderName.post;": ".post;item.folderName.postln;
		d.add(item.folderName -> item.entries.do({
		arg sf, i;
		var buf = Buffer.read(s, sf.fullPath);
		i.post; "...".post; sf.parent.name.postln;
		}));
		item.folderName.post; "(".post; item.files.size.post; ")".postln;
		};*/

		^d;
	}

	*dictionary { arg dict;
		dict[\foldernames].do({
			arg item, i;
			item.folderName.post; "(".post; item.entries.size.post; ")".postln;
		});
	}

	*listSamples { arg dict;
		dict = dict ? currentEnvironment;
		dict.keys.asSortedList.do {|k| (k++" ("++dict[k].size++")").postln};
	}

	*listLoadedSamples {
		currentEnvironment.keys.asArray.sort.do{|k|
			"% (%)".format(k, currentEnvironment[k].size).postln;
		}
	}

	*freeBuffers { arg dict;
		dict.do({ |item| item.collect(_.free); });
	}

	// loads samples from a folder.  Samples must be files within the given path
	// returns a Dictionary
	*loadSampleFiles { arg path = "samples/", s = Server.default;
		var d = ();
		PathName(path).entries.do({ |item, i|
			// item.fullPath.postln;
			d.put(i, Buffer.read(s, item.fullPath));
		});
		^d;
	}

	// loads samples from a folder.  Samples must be files within the given path
	// returns an Array
	*loadSamplesArray { arg path = "samples/", s = Server.default;
		var a = Array.new;
		PathName(path).entries.do({ |item, i|
			// item.fullPath.postln;
			a = a.add(Buffer.read(s, item.fullPath));
		});
		^a;
	}

	// loads samples from an array of paths
	// returns a Dictionary
	*loadSamples { arg paths = [], s = Server.default;
		var d = Dictionary.new;
		paths.do { |path|
			var name  = PathName(path).folderName;
			d.add(name -> Loopier.loadSamplesArray(path, s));
		};
		// Loopier.listSamples(d);
		^d;
	}

	// loads samples from an array of paths into the environment which
	// will make them available as variables with ~ (~folderName).
	// returns nothing, variables will be available in the environment.
	*loadSamplesAsSymbols { arg paths = [], s = Server.default;
		paths.do { |path|
			var name  = PathName(path).folderName;
			currentEnvironment.put(name.asSymbol, Loopier.loadSamplesArray(path, s));
		};
	}

	*allocBuffers  { arg numOfBuffers = 4,
		server = Server.default,
		seconds = 4,
		channels = 1;
		var buffers = ();
		numOfBuffers.do({|i| buffers.put(i, Buffer.alloc(server, server.sampleRate * seconds, channels))});
		^buffers;
	}

	// Return a list of all compiled SynthDef names
	*synthDefList {
		var names = SortedList.new;

		SynthDescLib.global.synthDescs.do { |desc|
			if(desc.def.notNil) {
				// Skip names that start with "system_"
				if ("^[^system_|pbindFx_]".matchRegexp(desc.name)) {
					names.add(desc.name);
				};
			};
		};

		^names;
	}

	// Prints a list of all compiled Synthdef names
	*listSynthDefs {
		"".postln;
		Loopier.synthDefList.do {|i|
			i.postln;
		}
	}

	*synths {
		Loopier.listSynthDefs;
	}

    *listSynthControls { |synth|
        "% controls".format(synth).postln;
        Loopier.synthControls(synth).collect(_.postln)
    }

	*controls { |synth|
		Loopier.listSynthControls(synth);
	}

    *synthControls { |synth|
        var controls = List();
        SynthDescLib.global.at(synth).controls.do{ |ctl|
            controls.add([ctl.name, ctl.defaultValue]);
        }
        ^controls
    }

	*addEventTypes {
		Event.addEventType(\sample, {
			~n = ~n ? 0;
			~sample;
			~type = \note;
		}, )
	}

	// UTILS ///////////////////////////////////////////////

	// Returns an integer from a given array of 0s and 1s
	// @param  array   Array[0,1]     An array representing a binary number
	//                                              WANRING!: Reads from right to left!
	//                                              e.g: [0,1] = 1; [1,0] = 2
	*binaryArrayAsInteger{ arg array;
		// (x + 2^0) + (x + 2^1) + (x + 2^2) + .. + (x + 2^n)
		// where x=(value[0||1]) and n=(digit position in binary form)
		^(array * 2.pow((0..array.size-1)).reverse).sum;
	}

	// Returns the degree of a letter
	// @param  char   String   A letter
	*charAsDegree{ arg char;
		^"abcdefghijklmnopqrstuvwxyz".find(char).mod(7);
	}

	// Returns a word as an array of degrees
	// @param  word   String   A  word
	*wordAsDegrees{ arg word;
		^Array.fill(word.size, {|i| Loopier.charAsDegree(word.at(i))});
	}

	// Returns a buffer
	*wavetable { |server|
		var buffer;
		var env, wt, numsegs;
		buffer = Buffer.alloc(server, 2048);
		numsegs = rrand(4,20);
		env = Env(
			(({rrand(0.0, 1.0)}!(numsegs+1)) * [1,-1]).scramble,
			{exprand(1,20)}!numsegs,
			{rrand(-20,20)}!numsegs
		);
		wt = env.asSignal(buffer.numFrames).asWavetable;
		buffer.loadCollection(wt);
		^buffer;
	}
}
