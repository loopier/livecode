+ String {
	digits { |octaves=1, degreesPerOctave=7|
		// replace numbers for letters to keep their right value when converting
		var str = this.letters;
		// replace nil by \r
		// "a" default value is 10, the whole sequence needs to be
		// offset so "a" === 0
		var digits = str.digit.collect(_ ?? {\r}) - 10 ;
		var modulo = degreesPerOctave ?? digits.copy.sort.last;
		^digits.mod(modulo * octaves);
	}

	letters {
		^this
		.tr($0, $a)
		.tr($1, $b)
		.tr($2, $c)
		.tr($3, $d)
		.tr($4, $e)
		.tr($5, $f)
		.tr($6, $g)
		.tr($7, $h)
		.tr($8, $i)
		.tr($9, $j);
	}

	degrees { |octaves=1, degreesPerOctave=7|
		^this.digits(octaves, degreesPerOctave);
	}

	notes { |octaves=1, degreesPerOctave=7|
		^this.digits(octaves, degreesPerOctave);
	}

	ixi { |octaves=1, degreesPerOctave=7|
		^this.digits(octaves, degreesPerOctave);
	}

	pixi { arg octaves=1, repeats=inf;
		// ^Pixi(this, repeats);
		^this.digits(octaves).pseq(repeats);
	}

	pseq{ arg repeats=1, offset=0;
		^Pseq(this, repeats, offset);
	}

	// doesNotUnderstand { |selector ... args|
	// 	^("this.asArray."++selector++"(args)").interpret ?? { ^super.doesNotUnderstand(selector, args) }
	// }
}