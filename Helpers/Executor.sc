//helper for opening help files (h ClassName) source files (s ClassName) and executing commands
//Batuhan Bozkurt 2009

Executor
{
	*new
	{
		^super.new.init;
	}
	
	init
	{
		var bnd, cmWin, tBox;
		bnd = Window.screenBounds;
		cmWin = Window("Command:", Rect((bnd.width/2) - 100, (bnd.height/2), 200, 40), resizable: false);
		tBox = TextField(cmWin, Rect(10, 10, 180, 20)).focus(true)
			.action_
			({|txt|
			
				if(txt.string[0..1] == "h ",
				{
					txt.string.split($ )[1].interpret.openHelpFile;
				},
				{
					if(txt.string[0..1] == "s ",
					{
						txt.string.split($ )[1].interpret.openCodeFile;
					},
					{
						(">>Executor:" + txt.string.interpret).postln;
					});
				});
				
				cmWin.close;
			});
		cmWin.front;
	}
}