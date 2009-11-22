//GUI for FreeSound Quark
//Batuhan Bozkurt 2009
FreeSoundSearch
{
	var <>doneFunc, <win, tagText, infoView, infoText, srcDescBut, srcTagBut, srcFNameBut,
	srcUsernameBut, durMinNum, durMaxNum, startIndexNum, limitNum, orderMenu, uNameText,
	passText, specsText, evalMenu, curFSInstance, srcRoutine, contCondition, fileBucket,
	sampKeys, reqSpecs, runButton, uncheckedSamps;
	
	*new
	{|argDoneFunc|
	
		^super.new.init(argDoneFunc);
	}
	
	init
	{|argDoneFunc|
		
		doneFunc = argDoneFunc ? {|argInstance, samps| samps.postln;};
		contCondition = Condition.new.test_(false);
		fileBucket = List.new;
		
		win = Window("FreeSound Search", Rect((Window.screenBounds.width / 2) - 150, (Window.screenBounds.height / 2) - 260, 300, 520), false);
		infoView = CompositeView(win, Rect(0, win.bounds.height - 25, win.bounds.width, 20))
			.background_(Color.gray(0.7));
		infoText = StaticText(win, Rect(5, infoView.bounds.top, win.bounds.width, 20))
			.string_("Ready...");
		
		StaticText(win, Rect(15, 10, 275, 20)).string_("Comma separated tags:");
		tagText = TextField(win, Rect(10, 30, 280, 20)).string_("train,guitar");
		
		srcDescBut = Button(win, Rect(10, 60, 280, 20))
			.states_
			([
				["Search in Descriptions"], 
				["Search in Descriptions", Color.black, Color(0.5, 0.7, 0.5)]
			])
			.focusColor_(Color.gray(alpha: 0));
			
		srcTagBut = Button(win, Rect(10, 80, 280, 20))
			.states_
			([
				["Search in Tags"], 
				["Search in Tags", Color.black, Color(0.5, 0.7, 0.5)]
			])
			.focusColor_(Color.gray(alpha: 0))
			.value_(1);
		
		srcFNameBut = Button(win, Rect(10, 100, 280, 20))
			.states_
			([
				["Search in Filenames"], 
				["Search in Filenames", Color.black, Color(0.5, 0.7, 0.5)]
			])
			.focusColor_(Color.gray(alpha: 0))
			.value_(1);
		
		srcUsernameBut = Button(win, Rect(10, 120, 280, 20))
			.states_
			([
				["Search in Usernames"], 
				["Search in Usernames", Color.black, Color(0.5, 0.7, 0.5)]
			])
			.focusColor_(Color.gray(alpha: 0))
			.value_(0);
			
		StaticText(win, Rect(15, 150, 80, 20)).string_("Min. Duration:");
		durMinNum = NumberBox(win, Rect(105, 150, 35, 20)).value_(1);
		
		StaticText(win, Rect(win.view.bounds.width - 140, 150, 80, 20)).string_("Max. Duration:");
		durMaxNum = NumberBox(win, Rect(win.view.bounds.width - 50, 150, 35, 20)).value_(20);
		
		StaticText(win, Rect(15, 180, 80, 20)).string_("Start Index:");
		startIndexNum = NumberBox(win, Rect(105, 180, 35, 20)).value_(0);
		
		StaticText(win, Rect(win.view.bounds.width - 140, 180, 80, 20)).string_("Result Limit:");
		limitNum = NumberBox(win, Rect(win.view.bounds.width - 50, 180, 35, 20)).value_(100);
		
		orderMenu = PopUpMenu(win, Rect(10, 210, win.view.bounds.width - 20, 20))
			.items_
			([
				"Default Order",
				"Downloads Descending",
				"Downloads Ascending",
				"Username Descending",
				"Username Ascending",
				"Date Descending",
				"Date Ascending",
				"Duration Descending",
				"Duration Ascending",
				"File Format Descending",
				"File Format Ascending"
			]);
			
		StaticText(win, Rect(15, 240, 275, 20)).string_("Username / Pass:");
		uNameText = TextField(win, Rect(30, 265, 100, 20));
		passText = TextField(win, Rect(win.view.bounds.width - 130, 265, 100, 20))
			.background_(Color.black);
		
		StaticText(win, Rect(15, 290, 275, 20)).string_("Acceptable File Specs:");
		specsText = TextView(win, Rect(10, 315, win.view.bounds.width - 20, 100))
			.usesTabToFocusNextView_(false)
			.enterInterpretsSelection_(false)
			.string_("(\n\tnumDownloads: \\any,\n\textension:\n\t{|ext|\n\t\t(ext == \\wav).or(ext == \\aiff);\n\t},\n\tsampleRate:\n\t{|sr|\n\t\tsr == 44100;\n\t},\n\tbitRate: \\any,\n\tbitDepth: \\any,\n\tnumChannels: \\any,\n\tduration: \\any,\n\tfileSize: \\any\n)");
		specsText.tryPerform(\syntaxColorize);
		
		evalMenu = PopUpMenu(win, Rect(10, 430, win.view.bounds.width - 20, 20))
			.items_
			([
				"Evaluate Sequentially",
				"Evaluate in Random Order"
			])
			.value_(1);
			
		runButton = Button(win, Rect(10, 460, 80, 20))
			.states_
			([
				["Start", Color.black, Color(0.5, 0.7, 0.5)], 
				["Stop", Color.black, Color(0.7, 0.5, 0.5)]
			])
			.action_
			({|btn|
			
				btn.value.switch
				(
					0,
					{
						srcRoutine.stop;
					},
					1,
					{
						srcRoutine.reset;
						sampKeys = tagText.string.split($,).collect({|item| item.asSymbol; });
						reqSpecs = specsText.string.interpret.as(Dictionary);
						reqSpecs.keys.do
							({|item|
							
								if(reqSpecs.at(item) == \any,
								{
									reqSpecs.put(item, { true; });
								})
							});
						fileBucket = List.new;
						srcRoutine.play(AppClock);
					}
				);
			});
			
		srcRoutine = 
			Routine
			({
				var curSelected;
				
				//lots of duplicate code, meh.
				
				sampKeys.do
				({|curKey|
					contCondition.test = false;
					curFSInstance =
						FreeSound
						(
							(
								\keyword: curKey,
								\searchDescriptions: srcDescBut.value,
								\searchTags: srcTagBut.value,
								\searchFileNames: srcFNameBut.value,
								\searchUserNames: srcUsernameBut.value,
								\durationMin: durMinNum.value,
								\durationMax: durMaxNum.value,
								\startFrom: startIndexNum.value,
								\limit: limitNum.value,
								\order: orderMenu.value
							),
							[uNameText.string, passText.string]
						).callbackFunc_
						({|fs, argStat, argInfo|
						
							{//begin fork (needed for waits)
							argStat.switch
							(
								1,
								{
									this.displayStatus("Logging in...");
								},
								2,
								{
									this.displayStatus("Login succesful!", 1);
								},
								-2,
								{
									this.displayStatus("Login failed. Check credentials.", -1);
									runButton.valueAction_(0);
								},
								3,
								{
									this.displayStatus("Performing search for %...".format(curKey.asString));
								},
								4,
								{
									this.displayStatus
									(
										"Search complete for %! % results returned.".format(curKey.asString, argInfo),
										1
									);
									uncheckedSamps = ({|c|c;} ! fs.numSamples).asList;
									if(uncheckedSamps.size > 0,
									{
										if(evalMenu.value == 0,
										{
											curSelected = uncheckedSamps[0];
											uncheckedSamps.removeAt(0);
											fs.getSampleInfo(curSelected);
										},
										{
											curSelected = uncheckedSamps.choose;
											uncheckedSamps.remove(curSelected);
											fs.getSampleInfo(curSelected);
										});
									},
									{
										this.displayStatus("No matching sound for keyword %. Skipping.".format(curKey.asString), -1);
										//fatal
										1.wait;
										contCondition.test = true;
										contCondition.signal;
									});
								},
								-4,
								{
									this.displayStatus("There was an error in search...", -1);
									runButton.valueAction_(0);
								},
								5,
								{
									this.displayStatus("Got sample info for index: %".format(curSelected));
									if
									(
										argInfo.keys.collect
										({|infoItem|
											
											if(infoItem != \index,
											{
												reqSpecs.at(infoItem).value(argInfo.at(infoItem));
											},
											{
												true;
											});
										}).every({|eItem| eItem == true; }),
									{//upper if - true block
										this.displayStatus("Sample % matches criterias. Downloading.".format(curSelected), 1);
										fs.downloadSample(argInfo.at(\index));
									},
									{
										this.displayStatus("Sample % didn't match the criterias.".format(curSelected), 1);
										if(uncheckedSamps.size > 0,
										{
											if(evalMenu.value == 0,
											{
												curSelected = uncheckedSamps[0];
												uncheckedSamps.removeAt(0);
												fs.getSampleInfo(curSelected);
											},
											{
												curSelected = uncheckedSamps.choose;
												uncheckedSamps.remove(curSelected);
												fs.getSampleInfo(curSelected);
											});
										},
										{
											this.displayStatus("Failed to find a fit sample. Skipping.".format(curKey.asString), -1);
											//fatal
											1.wait;
											contCondition.test = true;
											contCondition.signal;
										});
									});
									
								},
								-5,
								{
									this.displayStatus("Failed to get sample info.", -1);
									if(uncheckedSamps.size > 0,
									{
										if(evalMenu.value == 0,
										{
											curSelected = uncheckedSamps[0];
											uncheckedSamps.removeAt(0);
											fs.getSampleInfo(curSelected);
										},
										{
											curSelected = uncheckedSamps.choose;
											uncheckedSamps.remove(curSelected);
											fs.getSampleInfo(curSelected);
										});
									},
									{
										this.displayStatus("Failed to find a fit sample. Skipping.".format(curKey.asString), -1);
										//fatal
										1.wait;
										contCondition.test = true;
										contCondition.signal;
									});		
								},
								6,
								{
									this.displayStatus("Downloaded a sample for keyword: %!".format(curKey.asString), 1);
									fileBucket.add(argInfo);
									1.wait;
									contCondition.test = true;
									contCondition.signal;
								},
								-6,
								{
									this.displayStatus("Downloaded failed.", -1);
									1.wait;
									if(uncheckedSamps.size > 0,
									{
										if(evalMenu.value == 0,
										{
											curSelected = uncheckedSamps[0];
											uncheckedSamps.removeAt(0);
											fs.getSampleInfo(curSelected);
										},
										{
											curSelected = uncheckedSamps.choose;
											uncheckedSamps.remove(curSelected);
											fs.getSampleInfo(curSelected);
										});
									},
									{
										this.displayStatus("No other samples to check. Skipping.".format(curKey.asString), -1);
										//fatal
										1.wait;
										contCondition.test = true;
										contCondition.signal;
									});
									
								}				
							)
							}.fork(AppClock)		
						});
							
					curFSInstance.doSearch;	
					contCondition.wait;
				});
				
				this.displayStatus("Tasks finished!", 1);
				doneFunc.value(this, fileBucket.as(Array));
				runButton.valueAction_(0);
			});
			
		win.front;
	}
	
	displayStatus
	{|argString, argCond|
	
		{
			infoText.string_(argString);
			
			argCond.switch
			(
				-1,
				{
					infoView.background_(Color(1, 0.2, 0.2));
					0.5.wait;
					if(win.isClosed.not,
					{
						infoView.background_(Color.gray(0.7));
					});
				},
				1,
				{
					infoView.background_(Color(0.2, 1, 0.2));
					0.5.wait;
					if(win.isClosed.not,
					{
						infoView.background_(Color.gray(0.7));
					});
				}
			);
		}.fork(AppClock);
	}
	
	hideGui
	{
		win.visible_(false);
	}
	
	showGui
	{
		win.visible_(true);
	}
}