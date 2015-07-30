SFZ {
	var <lineNo, curHeader, context, curRegion, curGroup;
	var <groups;
	var <opcodes;

	var opcodeSpecs, specialOpcodes;

	*new {
		^super.new.init;
	}

	init {
		groups = [];

		opcodes = ();

		opcodeSpecs = (
			default_path: [\string, nil],
			octave_offset: [\int, 0],
			note_offset: [\int, 0]
		);

		specialOpcodes = ();

		opcodeSpecs.keysValuesDo { |opcode, spec|
			opcodes[opcode] = spec[1];
		};
	}

	// specs take on the form [type, default, lo, hi]
	// type can be \string, \int, \float, or \note.
	// default is ignored in this method, but it is used in initialization.
	// lo and hi are optional ranges for the parameter.

	*validate { |spec, value, lineNo=0|

		value = switch (spec[0])
			{ \string } { value }
			{ \int } {
				if ("^[-+]?\\d+$".matchRegexp(value).not) {
					^Error("Bad opcode value '%' on line %. Expected an integer.".format(value, lineNo)).throw;
				};
				value.asInteger;
			}
			{ \float } {
				if ("^[-+]?\\d+(\\.\\d*)?$".matchRegexp(value).not) {
					^Error("Bad opcode value '%' on line %. Expected a float.".format(value, lineNo)).throw;
				};
				value.asFloat;
			}
			{ \note } {
				var match;
				match = value.findRegexp("^([a-g])([b#]?)([-+]?\\d+)$");
				if (match.isEmpty) {
					if ("^[-+]?\\d+$".matchRegexp(value)) {
						value.asInteger;
					} {
						^Error("Bad opcode value '%' on line %. Expected a MIDI note.".format(value, lineNo)).throw;
					};
				} {
					var noteName, alteration, octave;
					noteName = [0, 2, 4, 5, 7, 9, 11]["cdefgab".indexOf(match[1][1][0])];
					alteration = match[2][1][0];
					alteration = if(alteration.notNil, { if(alteration == $b, -1, 1) }, 0);
					octave = match[3][1].asInteger;
					octave + 1 * 12 + noteName + alteration;
				};
			};

		if (spec[2].notNil) {
			if (value < spec[2] or: { value > spec[3] }) {
				^Error("Opcode value '%' out of range on line %. Expected range: % to %".format(value, lineNo, spec[2], spec[3])).throw;
			};
		};

		^value;
	}

	addGroup { |group|
		groups = groups.add(group);
	}

	parse { |sfzString|

		// Split into lines
		sfzString.split($\n).do { |line, i|

			lineNo = i + 1;

			// Remove comments
			if (line.find("//").notNil) {
				line = line[..line.find("//") - 1];
			};

			line = line.trim;

			if (line.notEmpty) {

				if ((line.first == $<) and: { line.last == $> }) {
					var header = line[1..line.size - 2];
					this.parseHeader(header.asSymbol);
				} {

					if (line.find("=").notNil) {
						var pos = line.find("=");
						var opcode = line[..pos - 1];
						var value = line[pos + 1..];
						this.parseOpcode(opcode, value);
					} {
						^Error("Syntax error on line %".format(lineNo)).throw;
					};

				};

			};

		};

	}

	parseHeader { |header|

		if ([\control, \group, \region].includes(header)) {

			switch (header)
			{ \control } {
				context = this;
				curGroup = nil;
				curRegion = nil;
			}

			{ \group } {
				context = SFZGroup(this);
				this.addGroup(context);
				curGroup = context;
				curRegion = nil;
			}

			{ \region } {
				if (curGroup.isNil) {
					curGroup = SFZGroup(this);
					this.addGroup(curGroup);
				};
				context = SFZRegion(this);
				curRegion = context;
				curGroup.addRegion(context);
			};

			curHeader = header;

		} {
			^Error("Unrecognized header <%> on line %".format(header, lineNo)).throw;
		};

	}

	parseOpcode { |opcode, value|
		if (context.notNil) {
			context.setOpcode(opcode.asSymbol, value);
		}
	}

	setOpcode { |opcode, value|
		if (opcodeSpecs[opcode].notNil) {
			value = SFZ.validate(opcodeSpecs[opcode], value, lineNo);
			opcodes[opcode] = value;
		} {
			if (specialOpcodes[opcode].notNil) {
				specialOpcodes[opcode].value(value);
			} {
				^Error("Unrecognized control opcode '%' on line %.".format(opcode, lineNo)).throw;
			};
		};
	}
}

SFZRegion {

	var <parent;
	var <opcodes;

	var opcodeSpecs, specialOpcodes;

	*new { |parent|
		^super.new.init(parent);
	}

	init { |argParent|

		parent = argParent;

		opcodes = ();

		opcodeSpecs = (
			sample: [\string, nil],
			lochan: [\int, 1, 1, 16],
			hichan: [\int, 16, 1, 16],
			lokey: [\note, 0, 0, 127],
			hikey: [\note, 127, 0, 127],
			pitch_keycenter: [\note, 60, 0, 127],
			lovel: [\int, 0, 0, 127],
			hivel: [\int, 127, 0, 127]
		);

		specialOpcodes = (
			key: { |value|
				value = SFZ.validate([\note, nil, 0, 127], value, parent.lineNo);
				opcodes.lokey = value;
				opcodes.hikey = value;
				opcodes.pitch_keycenter = value;
			}
		);

		opcodeSpecs.keysValuesDo { |opcode, spec|
			opcodes[opcode] = spec[1];
		};
	}

	setOpcode { |opcode, value|

		if (opcodeSpecs[opcode].notNil) {
			value = SFZ.validate(opcodeSpecs[opcode], value, parent.lineNo);
			opcodes[opcode] = value;
		} {
			if (specialOpcodes[opcode].notNil) {
				specialOpcodes[opcode].value(value);
			} {
				^Error("Unrecognized region opcode '%' on line %.".format(opcode, parent.lineNo)).throw;
			};
		};
	}

}

SFZGroup : SFZRegion {

	var <regions;

	init { |argParent|
		parent = argParent;
		regions = [];
	}

	addRegion { |region|
		regions = regions.add(region);
	}

}