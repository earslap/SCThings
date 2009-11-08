MultiTouchPad
{
	var responder, <fingersDict, <activeBlobs, <>setAction, <>touchAction, <>untouchAction,
		guiOn, guiWin;
	
	*new
	{
		^super.new.init;
	}
	
	init
	{
		responder = OSCresponderNode(nil, "/tuio/2Dobj", {|...args| this.processOSC(*args); });
		fingersDict = Dictionary.new;
		activeBlobs = List.new;
		guiOn = false;
	}
	
	start
	{
		responder.add;
	}
	
	stop
	{
		responder.remove;
	}
	
	processOSC
	{|time, responder, msg|
	
		//msg.postln;
		if(msg[1] == 'alive',
		{
			var toRemove = List.new;
			
			activeBlobs = msg[2..];
			fingersDict.keys.do
			({|item|
				
				if(activeBlobs.includes(item).not,
				{
					toRemove.add(item);
				});
			});
			
			toRemove.do
			({|item| 
				
				fingersDict.removeAt(item); 
				untouchAction.value(item);
				if(guiOn, { { guiWin.refresh; }.defer; });
			});
			
			activeBlobs.do
			({|item|
			
				if(fingersDict.at(item).isNil,
				{
					fingersDict.put(item, -1); //-1 means xy not initialized
				});
			});
			
			^this;
		});
		
		if(msg[1] == 'set',
		{
			var curID = msg[2];
			var xys = msg[4..6];
			if(fingersDict.at(curID).isNil, { "this should never happen".postln; });
			if(fingersDict.at(curID) == -1, { touchAction.value(curID, xys); });
			fingersDict.put(curID, xys);
			setAction.value(curID, xys);
			if(guiOn, { { guiWin.refresh; }.defer; });
			^this;
		});
	}
	
	gui
	{
		var view;
		guiWin = Window("MultiTouchPad", Rect(100, 100, 525, 375)).onClose_({ guiOn = false; });
		view = UserView(guiWin, guiWin.view.bounds).background_(Color.white).resize_(5);
		view.drawFunc_
			({
				var fItem;
				
				fingersDict.keys.do
				({|key|
					
					fItem = fingersDict.at(key);
					Pen.color = Color.red;
					Pen.fillOval
					(
						Rect
						(
							guiWin.view.bounds.width * fItem[0], 
							guiWin.view.bounds.height * fItem[1],
							20 * fItem[2],
							20 * fItem[2]
						)
					);
				});
			});
		guiOn = true;
		guiWin.front;
	}
}