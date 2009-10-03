XSes
{
	classvar <bar, <ctl, responder, <>spit, factoryMap;
	
	*initClass
	{
		ctl = Array.fill(16, {0});
		bar = 0;
		spit = false;
		factoryMap = 
		(16:9, 17:10, 18:11, 19:12, 20:13, 21:14, 22:15, 23:16, 
		24:1, 25:2, 26:3, 27:4, 28:5, 29:6, 30:7, 31:8, 10: 0);
	}
	
	*attach
	{
		responder = CCResponder
		({|src, chan, num, vel| 
			if(spit, { [src, chan, factoryMap.at(num), vel].postln; });
			if(num != 10, { ctl[factoryMap.at(num)] = vel/127; }, { bar = vel/127; });
		});
	}
	
	*detach
	{
		responder.remove;
		ctl = Array.fill(16, {0});
	}
	
	*reset
	{
		ctl = Array.fill(16, {0});
	}
}