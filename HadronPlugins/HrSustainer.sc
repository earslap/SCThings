HrSustainer : HadronPlugin
{

	var numAttTime, numRelTime, numAttCurve, numRelCurve, numLPLo, numLPHi, numDetectThresh, 
	curveMenu, slSusTime, numSlSusTime, releaseAllBtn, susSwitchBtn, slGain, numGainSl, onsetLed, 
	holdBtn, susSpec, sustainGroup, onsetResponder, isSustaining, listenerSynth, autoReleaseBtn,
	stickAutoRelButton;

	*initClass
	{
		this.addHadronPlugin;
	}
	
	*new
	{|argParentApp, argIdent, argUniqueID, argExtraArgs, argCanvasXY|
		
		var numIns = 1;
		var numOuts = 1;
		var bounds = Rect((Window.screenBounds.width - 250).rand, rrand(100, Window.screenBounds.height), 275, 250);
		var name = "HrSustainer";
		^super.new(argParentApp, name, argIdent, argUniqueID, argExtraArgs, bounds, numIns, numOuts, argCanvasXY).init;
	}
	
	init
	{
		window.background_(Color(0.8, 1, 0.8));
		
		susSpec = [0.1, 20, \lin, 0.01, 0.1].asSpec;
		
		sustainGroup = Group.new(target: group, addAction: \addToTail);
		isSustaining = false;
		
		StaticText(window, Rect(10, 10, 125, 20)).string_("Attack/Release times:");
		StaticText(window, Rect(10, 35, 125, 20)).string_("Attack/Release curves:");
		StaticText(window, Rect(10, 60, 125, 20)).string_("Lowpass max/min freq:");
		StaticText(window, Rect(10, 85, 125, 20)).string_("Detection threshold:");
		StaticText(window, Rect(10, 110, 125, 20)).string_("Controller curve:");
		StaticText(window, Rect(10, 135, 125, 20)).string_("Sustain time control:");
		StaticText(window, Rect(10, 160, 125, 20)).string_("Gain:");
		
		numAttTime = NumberBox(window, Rect(140, 10, 40, 20)).value_(0.2);
		numRelTime = NumberBox(window, Rect(185, 10, 40, 20)).value_(20).clipLo_(0.1)
			.action_
			({|nmb|
			
				var tmpCurve = if(curveMenu.value == 0, { \lin; }, { \exponential });
				susSpec = [0.1, nmb.value, tmpCurve, 0.01, 0.1].asSpec;
				numSlSusTime.value_(susSpec.map(slSusTime.value));
			});
		
		numAttCurve = NumberBox(window, Rect(140, 35, 40, 20)).value_(-2);
		numRelCurve = NumberBox(window, Rect(185, 35, 40, 20)).value_(-2);
		
		numLPLo = NumberBox(window, Rect(140, 60, 40, 20)).value_(200);
		numLPHi = NumberBox(window, Rect(185, 60, 40, 20)).value_(8000);
		
		numDetectThresh = NumberBox(window, Rect(140, 85, 40, 20)).value_(0.05)
			.action_
			({|nmb|
				listenerSynth.set(\thresh, nmb.value);
			});
			
		onsetLed = Button(window, Rect(200, 90, 10, 10))
			.states_([["", Color.black, Color.white], ["", Color.black, Color.red]]).enabled_(false);
		
		curveMenu = PopUpMenu(window, Rect(140, 110, 85, 20)).items_(["Linear", "Exponential"])
			.action_
			({|mnu|
			
				var tmpCurve = if(mnu.value == 0, { \lin; }, { \exponential });
				susSpec = [0.1, susSpec.maxval, tmpCurve, 0.01, 0.1].asSpec;
				numSlSusTime.value_(susSpec.map(slSusTime.value));
			});
		
		slSusTime = HrSlider(window, Rect(140, 135, 80, 20)).value_(0)
			.action_
			({|sld|
			
				numSlSusTime.value_(susSpec.map(sld.value));
			});
		
		numSlSusTime = NumberBox(window, Rect(225, 135, 40, 20)).value_(susSpec.map(0)).enabled_(false);
		
		slGain = HrSlider(window, Rect(140, 160, 80, 20)).value_(0.5).action_({|sld| numGainSl.value_(sld.value); });
		numGainSl = NumberBox(window, Rect(225, 160, 40, 20)).value_(0.5);
		
		autoReleaseBtn = HrButton(window, Rect(10, 195, 10, 10))
			.states_([["", Color.black, Color.white], ["", Color.black, Color.red]]);
		
		StaticText(window, Rect(30, 190, 240, 20)).string_("Auto release all in next event. (Stick:     )");
		
		stickAutoRelButton = HrButton(window, Rect(230, 195, 10, 10))
			.states_([["", Color.black, Color.white], ["", Color.black, Color.red]]);
		
		holdBtn = HrButton(window, Rect(10, 215, 80, 20)).states_([["Hold this"]])
			.action_
			({
				this.spawnSustainer;
			});
		
		releaseAllBtn = HrButton(window, Rect(97, 215, 80, 20)).states_([["Release All"]])
			.action_
			({
				sustainGroup.set(\t_release, 1);
			});
		
		susSwitchBtn = HrButton(window, Rect(184, 215, 80, 20))
			.states_
			([
				["Auto Hold Off", Color.white, Color(0.7, 0.5, 0.5)],
				["Auto Hold On", Color.white, Color(0.5, 0.7, 0.5)],
			])
			.action_
			({|btn|
			
				if(btn.value == 1, { isSustaining = true; }, { isSustaining = false; });
			});
		
		
		fork
		{
			SynthDef("hrsustainer_playtrig"++uniqueID,
			{
				arg inBus0, thresh = 0.05;
				var in = InFeedback.ar(inBus0);
				var trig = Coyote.kr(in, thresh: thresh);
				SendReply.kr(trig, ("hrsustainer_onset_occured"++uniqueID).asSymbol, 0, 1000);
			}).memStore;
			
			SynthDef("hrsustainer_freezer"++uniqueID,
			{
				arg inBus0, outBus0, attTime = 0.2, susTime = 20, attCurve = -2, relCurve = -2, t_release = 0, mul = 0.5,
				lopLo = 200, lopHi = 8000;
				var in, chain, reader,env, relEnv, out;
				
				in = InFeedback.ar(inBus0);
				env = EnvGen.ar(Env([0,0,mul,0], [0.1, attTime, susTime - attTime - 0.1], [0, attCurve, relCurve]), 1, doneAction: 2);
				relEnv = EnvGen.ar(Env([1, 0], [1], [-9]), t_release, doneAction: 2);
				
				chain = FFT(LocalBuf(4096), in, 0.25, 1);
				chain = PV_Freeze(chain, Line.kr(0, 1, 0.2).floor);
				
				out = LPF.ar(IFFT(chain, 1), env.range(lopLo, lopHi));
				
				Out.ar(outBus0, relEnv * env * out);
			}).memStore;
			
			Server.default.sync;
			
			onsetResponder = 
				OSCresponder(nil, ("hrsustainer_onset_occured"++uniqueID).asSymbol,
				{|t, r, msg|
				
				
					{
						onsetLed.value_(1);
						0.05.wait;
						onsetLed.value_(0);
					}.fork(AppClock);
					
					if(isSustaining,
					{
						{ this.spawnSustainer; }.defer;
					});
				}).add;
				
			listenerSynth = Synth("hrsustainer_playtrig"++uniqueID, target: group, addAction: \addToHead);
		};
		
		saveGets =
			[
				{ numAttTime.value; },
				{ numRelTime.value; },
				{ numAttCurve.value; },
				{ numRelCurve.value; },
				{ numLPLo.value; },
				{ numLPHi.value; },
				{ numDetectThresh.value; },
				{ curveMenu.value; },
				{ slSusTime.boundMidiArgs; },
				{ slSusTime.value; },
				{ releaseAllBtn.boundOnMidiArgs; },
				{ releaseAllBtn.boundOffMidiArgs; },
				{ susSwitchBtn.boundOnMidiArgs; },
				{ susSwitchBtn.boundOffMidiArgs; },
				{ susSwitchBtn.value; },
				{ slGain.boundMidiArgs; },
				{ slGain.value; },
				{ holdBtn.boundOnMidiArgs; },
				{ holdBtn.boundOffMidiArgs; },
				{ autoReleaseBtn.boundOnMidiArgs; },
				{ autoReleaseBtn.boundOffMidiArgs; },
				{ autoReleaseBtn.value; },
				{ stickAutoRelButton.boundOnMidiArgs; },
				{ stickAutoRelButton.boundOffMidiArgs; },
				{ stickAutoRelButton.value; }
			];
			
		saveSets =
			[
				{|argg| numAttTime.valueAction_(argg); },
				{|argg| numRelTime.valueAction_(argg); },
				{|argg| numAttCurve.valueAction_(argg); },
				{|argg| numRelCurve.valueAction_(argg); },
				{|argg| numLPLo.valueAction_(argg); },
				{|argg| numLPHi.valueAction_(argg); },
				{|argg| numDetectThresh.valueAction_(argg); },
				{|argg| curveMenu.valueAction_(argg); },
				{|argg| slSusTime.boundMidiArgs_(argg); },
				{|argg| slSusTime.valueAction_(argg); },
				{|argg| releaseAllBtn.boundOnMidiArgs_(argg); },
				{|argg| releaseAllBtn.boundOffMidiArgs_(argg); },
				{|argg| susSwitchBtn.boundOnMidiArgs_(argg); },
				{|argg| susSwitchBtn.boundOffMidiArgs_(argg); },
				{|argg| susSwitchBtn.valueAction_(argg); },
				{|argg| slGain.boundMidiArgs_(argg); },
				{|argg| slGain.valueAction_(argg); },
				{|argg| holdBtn.boundOnMidiArgs_(argg); },
				{|argg| holdBtn.boundOffMidiArgs_(argg); },
				{|argg| autoReleaseBtn.boundOnMidiArgs_(argg); },
				{|argg| autoReleaseBtn.boundOffMidiArgs_(argg); },
				{|argg| autoReleaseBtn.valueAction_(argg); },
				{|argg| stickAutoRelButton.boundOnMidiArgs_(argg); },
				{|argg| stickAutoRelButton.boundOffMidiArgs_(argg); },
				{|argg| stickAutoRelButton.valueAction_(argg); }
			];
	}
	
	spawnSustainer
	{
		if(autoReleaseBtn.value == 1, 
		{ 
			sustainGroup.set(\t_release, 1); 
			if(stickAutoRelButton.value == 0,
			{
				autoReleaseBtn.value_(0); 
			});
		});
		
		Synth("hrsustainer_freezer"++uniqueID, 
			[
				\inBus0, inBusses[0],
				\outBus0, outBusses[0],
				\attTime, numAttTime.value,
				\susTime, numSlSusTime.value,
				\attCurve, numAttCurve.value,
				\relCurve, numRelCurve.value,
				\mul, slGain.value,
				\lopLo, numLPLo.value,
				\lopHi, numLPHi.value
			], target: sustainGroup); 
	}
	
	updateBusConnections
	{
		sustainGroup.set(\inBus0, inBusses[0], \outBus0, outBusses[0]);
		listenerSynth.set(\inBus0, inBusses[0], \outBus0, outBusses[0]);
		
	}
	
	cleanUp
	{
		onsetResponder.remove;
	}
	
	
}