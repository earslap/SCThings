FingerNeedle
{
	var <win, <>images, <>buffers, <blobs, <blobIDList, <selectionList, <>widthMul, <>heightMul, imagesPixels,
	<imgWidth, <imgHeight, rfrRoutine, shouldRefresh, <activeLayer, <>addAction, <>removeAction, <>muteAction,
	<>moveAction, heldKeys, infoView, infoText, numCurTouch, <layerSpeeds, layerNames, busses, rsrc;
	
	*new
	{|argWinSize, argSPaths, argBusses|
		
		^super.new.init(argWinSize, argSPaths, argBusses);
	}
	
	init
	{|argWinSize, argSPaths, argBusses|
	
		if(argWinSize.isNil or: { argSPaths.isNil; } or: { argBusses.isNil; },
		{
			"You must supply all three arguments. See help.".error;
			^this.halt;
		});
				
		images = argSPaths.collect({|item| SCImage.fromSound(argWinSize, argWinSize, item.standardizePath); });
		busses = argBusses;
		activeLayer = 0;
		blobs = Dictionary.new;
		blobIDList = List.fill(images.size, { List.new; });
		selectionList = List.fill(images.size, { -1; }); //no selection in any layers
		layerSpeeds = Array.fill(images.size, { 1.0; });
		layerNames = argSPaths.collect({|item| PathName(item.standardizePath).fileName; });
		widthMul = 40;
		heightMul = 40;
		imgWidth = images[0].width;
		imgHeight = images[0].height;
		shouldRefresh = false;
		numCurTouch = 0; //number of currently touching fingers
		addAction = removeAction = moveAction = muteAction = {};
		heldKeys = Set.new;
		rsrc = Dictionary.new;
		
		SynthDef(\touchPlay,
		{
			arg buf, imgWidth, imgHeight, rectX, rectY, rectW, rectH, t_decTrig, amp = 1, outBus;
			
			var needle, snd, env;
			var lAhead = 0.01;
			env = EnvGen.ar(Env([1, 0, 1], [lAhead, lAhead]), t_decTrig);
			needle = 
				NeedleRect.ar
					(
						44100, imgWidth, imgHeight, 
						rectX.clip(0, imgWidth), 
						rectY.clip(0, imgHeight), 
						rectW.clip(0, imgWidth), 
						rectH.clip(0, imgHeight)
					);
			snd = BufRd.ar(1, buf, DelayN.ar(needle, 1, lAhead)) * env * amp.lag(1);
			snd = Pan2.ar(snd, LFDNoise1.ar(LFNoise1.ar(0.5).range(1, 20)));
			Out.ar(outBus, snd);
		}).memStore;
		
		win = Window.new("...", Rect(100, 100, imgWidth, imgHeight + 30), false)
			.onClose_
			({
				MultiTouchPad.stop;
				this.stopAnim;
			});
			
		infoView = CompositeView(win, Rect(0, win.view.bounds.height - 25, win.view.bounds.width, 20))
			.background_(Color.gray(0.7));
		infoText = StaticText(infoView, Rect(5, 0, infoView.bounds.width, 20));
		win.name = layerNames[0];
		win.drawHook_
			({
				images[activeLayer].drawAtPoint(0@0, nil, 'sourceOver', 1);
				
				
				blobs.do
				({|item|
					if(item.parentLayer == activeLayer,
					{
						if(item.isOnPlayback,
						{
							if(item.isSelected,
							{
								Pen.color = Color(0, 0, 1.0, 0.5);
							},
							{
								if(item.isMuted,
								{
									Pen.color = Color(1, 0, 0, 0.5);
								},
								{
									Pen.color = Color(0, 1, 0, 0.5);
								});
							});
						},
						{
							Pen.color = Color(1, 1, 0, 0.5);
						});
						
						Pen.fillRect(item.bounds);
					});
				});
			});
			
		win.view.keyDownAction_
		({|...args| 
			
			var tempWin;
			//args.postln;
			
			if((args[2] != 262145) and: { args[2] != 262401 },
			{
				args[3].switch
				(
					63232, //up arrow
					{
						this.moveSelect(1);
					},
					63233, //down arrow
					{
						this.moveSelect(-1);
					},
					63234, //left arrow
					{
						if(numCurTouch == 0,
						{
							this.switchToLayer((activeLayer - 1) % images.size);
							this.updInfoText;
						});
					},
					63235, //right arrow
					{
						if(numCurTouch == 0,
						{
							this.switchToLayer((activeLayer + 1) % images.size);
							this.updInfoText;
						});
					},
					127, //del pressed
					{
						if(selectionList[activeLayer] != -1,
						{
							this.removeSelected;
						});
					},
					107, //k
					{
						heightMul = (heightMul - 5).wrap(1, 1000);
						this.updInfoText;
					},
					105, //i
					{
						heightMul = (heightMul + 5).wrap(1, 1000);
						this.updInfoText;
					},
					111, //o
					{
						widthMul = (widthMul - 5).wrap(1, 4000);
						this.updInfoText;
					},
					112, //p
					{
						widthMul = (widthMul + 5).wrap(1, 4000);
						this.updInfoText;
					},
					117, //u
					{
						layerSpeeds[activeLayer] = (layerSpeeds[activeLayer] + 0.01).clip(0, 1);
						this.updInfoText;
					},
					106, //j
					{
						layerSpeeds[activeLayer] = (layerSpeeds[activeLayer] - 0.01).clip(0, 1);
						this.updInfoText;
					},
					109, //m
					{
						muteAction.value(selectionList[activeLayer], blobs.at(selectionList[activeLayer]).isMuted.binaryValue);
						blobs.at(selectionList[activeLayer]).isMuted = blobs.at(selectionList[activeLayer]).isMuted.not;					}
				);
				heldKeys.add(args[1]); 
			},
			{
				args[3].switch
				(
					19, //ctrl + s
					{
						tempWin = Window("Enter new speed:", Window.centerRect(200, 40)).front;
						TextField(tempWin, Rect(10, 10, 180, 20))
							.action_
							({|txt|
							
								layerSpeeds[activeLayer] = txt.string.asFloat.clip(0, 1);
								this.updInfoText;
								tempWin.close;
							}).focus;
					},
					23, //ctrl + w
					{
						tempWin = Window("Enter new bWidth:", Window.centerRect(200, 40)).front;
						TextField(tempWin, Rect(10, 10, 180, 20))
							.action_
							({|txt|
							
								widthMul = txt.string.asFloat.clip(1, 4000);
								this.updInfoText;
								tempWin.close;
							}).focus;
					},
					8, //ctrl + h
					{
						tempWin = Window("Enter new bHeight:", Window.centerRect(200, 40)).front;
						TextField(tempWin, Rect(10, 10, 180, 20))
							.action_
							({|txt|
							
								heightMul = txt.string.asFloat.clip(1, 1000);
								this.updInfoText;
								tempWin.close;
							}).focus;
					}
				);
			});
		});
		win.view.keyUpAction_({|...args| heldKeys.remove(args[1]); });
		
		this.initBuffers(argSPaths);
		
		rfrRoutine = 
			Routine
			({ 
				loop
				({ 
					if(shouldRefresh == true, { win.refresh; shouldRefresh = false; }); 
					blobs.do
					({|item|
					
						if(item.isOnPlayback.not,
						{
							item.recordFrame;
						},
						{
						
							if(item.advance, //true if bounds are actually changing
							{
								moveAction.value(item.id, item.xys);
								if(item.parentLayer == activeLayer,
								{
									shouldRefresh = true;
								});
							});
							
						});
					});
					
					0.016666666666667.wait; 
				}); 
			});
		
		this.updInfoText;
		
		MultiTouchPad.resetActions.start(\force);
		MultiTouchPad.touchAction = {|...args| this.addBlob(args[0], args[1]); };
		MultiTouchPad.untouchAction = {|...args| this.removeBlob(args[0]); };
		MultiTouchPad.setAction = {|...args| this.moveBlob(args[0], args[1]); };
		
		addAction =
		{|...args|

			rsrc.put
			(
				args[0],  
				Synth(\touchPlay, 
					[
						\buf, buffers[activeLayer],
						\outBus, busses[activeLayer],
						\imgWidth, imgWidth,
						\imgHeight, imgHeight,
						\rectX, blobs.at(args[0]).bounds.left,
						\rectY, blobs.at(args[0]).bounds.top,
						\rectW, blobs.at(args[0]).bounds.width,
						\rectH, blobs.at(args[0]).bounds.height
					])			
			); 
		};
		
		removeAction =
		{|...args|
			
			rsrc.at(args[0]).free;
			rsrc.removeAt(args[0]);
		};
		
		moveAction =
		{|...args| 
			
			rsrc.at(args[0]).set
				(
					\rectX, blobs.at(args[0]).bounds.left,
					\rectY, blobs.at(args[0]).bounds.top,
					\rectW, blobs.at(args[0]).bounds.width,
					\rectH, blobs.at(args[0]).bounds.height,
					\t_decTrig, 1
				);
		};
		
		muteAction =
		{|...args|

			rsrc.at(args[0]).set(\amp, args[1]);		
		};

		win.front;
	}
	
	addBlob
	{|argID, argXYS|
	
		var x, y, w, h;
		
		numCurTouch = numCurTouch + 1;
		x = (argXYS[0] * imgWidth);
		y = (argXYS[1] * imgHeight);
		w = (widthMul * argXYS[2]).round(1);
		h = (heightMul * argXYS[2]).round(1);
		x = (x - (w * 0.5)).round(1);
		y = (y - (h * 0.5)).round(1);
		
		blobs.put(argID, FNBlob(Rect(x, y, w, h), activeLayer, argID, this));
		blobIDList[activeLayer].add(argID);
		shouldRefresh = true;
		addAction.value(argID, argXYS);
		this.updInfoText;
	}
	
	removeBlob
	{|argID|
		
		shouldRefresh = true;
		numCurTouch = numCurTouch - 1;
		if(heldKeys.includes($s).not, //if we are not saving movement
		{
			blobs.removeAt(argID);
			blobIDList[activeLayer].remove(argID);
			removeAction.value(argID);
		},
		{
			blobs.at(argID).isOnPlayback = true;
			if(selectionList[activeLayer] == -1,
			{
				selectionList[activeLayer] = argID;
				blobs.at(argID).isSelected = true;
			});
		});
		
		this.updInfoText;
	}
	
	removeSelected
	{
		var selected = selectionList[activeLayer];
	
		if(blobIDList[activeLayer].size > 1,
		{
			this.moveSelect(1);
		},
		{
			selectionList[activeLayer] = -1;
		});
		
		blobs.removeAt(selected);
		blobIDList[activeLayer].remove(selected);
		removeAction.value(selected);
		shouldRefresh = true;
		this.updInfoText;
	}
	
	moveBlob
	{|argID, argXYS|
	
		var x, y, w, h;
		
		x = (argXYS[0] * imgWidth);
		y = (argXYS[1] * imgHeight);
		w = (widthMul * argXYS[2]).round(1);
		h = (heightMul * argXYS[2]).round(1);
		x = (x - (w * 0.5)).round(1);
		y = (y - (h * 0.5)).round(1);
		
		blobs.at(argID).bounds_(Rect(x, y, w, h));
		shouldRefresh = true;
		moveAction.value(argID, argXYS);
	}
	
	initBuffers
	{|argSPaths|
	
		var numBuffers = images.size;
		
		if(images.collect({|item| item.width@item.height; }).asSet.size != 1,
		{
			"All images must have the same width and height.".error;
			^this.halt;
		});
		
		/*
		imagesPixels = images.collect({|item| item.pixels; });
		buffers = 
			imagesPixels.collect
			({|item| 
				
				Buffer.loadCollection
				(
					Server.default, 
					item.collect({|samp| (samp.asColor.red * 2) - 1; })
				);
			});
		
		imagesPixels = nil; //send to garbage collector we don't need them.
		*/
		this.updInfoText("Filling in buffers...");
		buffers = argSPaths.collect({|item| Buffer.readChannel(Server.default, item.standardizePath, 0, imgWidth * imgHeight, [0],
			{ numBuffers = numBuffers - 1; if(numBuffers == 0, { this.updInfoText("Buffers ready..."); }); }); });
		
	}
	
	startAnim
	{
		rfrRoutine.reset;
		rfrRoutine.play(AppClock);
	}
	
	stopAnim
	{
		rfrRoutine.stop;
	}
	
	resetActions
	{
		addAction = removeAction = moveAction = {};
	}
	
	moveSelect
	{|argDir|
	
		var newInd;
		
		newInd = (blobIDList[activeLayer].indexOf(selectionList[activeLayer]) + argDir) % blobIDList[activeLayer].size;
		blobs.at(selectionList[activeLayer]).isSelected = false;
		selectionList[activeLayer] = blobIDList[activeLayer][newInd];
		blobs.at(selectionList[activeLayer]).isSelected = true;
	}
	
	switchToLayer
	{|argLayerIndex|
	
		activeLayer = argLayerIndex;
		shouldRefresh = true;
	}
	
	updInfoText
	{|argExtra|
		
		var text;
		text = blobIDList.collect({|item, cnt| "%/%".format(cnt, item.size); });
		text[activeLayer] = "[" ++ text[activeLayer] ++ "]";
		text = text.collect({|item| item ++ " "; }).join ++ "wMul: %, hMul: %".format(widthMul, heightMul);
		text = text + "lSpd: %".format(layerSpeeds[activeLayer].round(0.01));
		if(argExtra.notNil, { text = text + argExtra; });
		{
			infoText.string = text;
			win.name = layerNames[activeLayer];
			infoView.refresh;
		}.defer;
	}
}

FNBlob
{
	var <bounds, <xys, <image, <parentLayer, <history, <historyXYS, <currentFrame, <>isOnPlayback, <id,
	<>isSelected, parentApp, <>isMuted;
	
	*new
	{|argBounds, argLayer, argID, argParent|
		
		^super.new.init(argBounds, argLayer, argID, argParent);
	}
	
	init
	{|argBounds, argLayer, argID, argParent|
	
		parentApp = argParent;
		bounds = argBounds;
		history = List.new;
		historyXYS = List.new;
		isOnPlayback = false;
		isMuted = false;
		parentLayer = argLayer;
		id = argID;
		isSelected = false;
		currentFrame = 0;
	}
	
	bounds_
	{|argNewBound, argXYS|
		
		bounds = argNewBound;
		xys = argXYS;
	}
	
	recordFrame
	{
		history.add(bounds);
		historyXYS.add(xys);
	}
	
	advance
	{
		var now;
		currentFrame = (currentFrame + parentApp.layerSpeeds[parentLayer]) % history.size;
		now = currentFrame.floor;
		
		if((bounds != history[now]) or: { xys != historyXYS[now] },
		{
			bounds = history[now];
			xys = historyXYS[now];
			^true;
		},
		{
			^false;
		});
		
	}
}