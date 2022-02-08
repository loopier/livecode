Pspeak : Pbind {

	// Linux-only.  Needs the text-to-speach
	// software Mimic Classic (https://mimic.mycroft.ai/).
	// It uses sytem audio, not controlled by supercollider
	// *new {arg say="uh", voice="kal";
	// 	// if (pairs.size.odd, { Error("Pbind should have even number of args.\n").throw; });
	// 	^Pbind(
	// 		// avoid all supercollider sounds
	// 		\amp, 0,
	// 		\say, say,
	// 		\voice, voice,
	// 		\unixcmd, Pfunc{ |e|
	// 			var cmd = "mimic -voice % -t '%'".format(e[\voice], e[\say]);
	// 			cmd.unixCmd;
	// 			cmd.postln;
	// 		}
	// 	);
	// }

	*new { arg ... pairs;
		// if (pairs.size.odd, { Error("Pbind should have even number of args.\n").throw; });
			// avoid all supercollider sounds
		pairs = pairs ++ [
			\amp, 0,
			\voice, Pfunc{ |e| e[\voice] ? "kal"},
			\unixcmd, Pfunc{ |e|
				var cmd = "mimic -voice % -t \"%\"".format(e[\voice], e[\say]);
				cmd.unixCmd;
			}
		];
		^super.newCopyArgs(pairs)
	}
}