An unfinished sample player for the SFZ format written in SuperCollider. Currently, it implements some of SFZ v1, containing enough critical features to handle a good number of simple sample packs.

I don't develop this anymore since I stopped using SFZ in my work, but I'm willing to offer some technical support. I would very much appreciate an adoption.

If you're using this software and you need something that isn't currently supported, please file an issue. If you need something more stable, try [LinuxSampler](http://www.linuxsampler.org/).

    x = SFZ("/path/to/sfz/file.sfz");
    // load buffers
    x.prepare { "done".postln };

    x.noteOn(64, 60);
    x.noteOff(64, 60);
