A work-in-progress sample player for the SFZ format written in SuperCollider. Currently, it implements some of SFZ v1, containing enough critical features to handle a good number of simple sample packs.

    x = SFZ("/path/to/sfz/file.sfz");
    x.prepare { "done".postln };

    x.noteOn(64, 60);
    x.noteOff(64, 60);