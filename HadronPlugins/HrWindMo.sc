//requires Tartini, BBMacros
//Batuhan Bozkurt 2009
//Physical model is based on SteamPipe1 from Reaktor Library

HrWindMo : HadronPlugin
{
	var synthInstance, sDef, responder, breathView, threshLed, resetVals, wiNumBoxes;
	
	*initClass
	{
		this.addHadronPlugin;
	}
	
	*new
	{|argParentApp, argIdent, argUniqueID, argExtraArgs, argCanvasXY|
		
		^super.new
		(
			argParentApp, 
			"HrWindMo", 
			argIdent, 
			argUniqueID, 
			argExtraArgs, 
			Rect(100, 100, 300, 450), 
			1, 2, 
			argCanvasXY
		).init;
	}
	
	init
	{		
		var wiSliders = List.new, wiSpecs = List.new, resetVals = List.new;
		var tags = [\selDCN, \pressure, \mCor, \srCor, \pTune, \kTrack, \damp, \rt, \presHpLo, \presHpHi, \presHpTrack, \presLpLo, \presLpHi, \presLpTrack, \push, \offset, \ampThresh];
		wiNumBoxes = List.new;
			
		window.background = Color.gray;
		
		StaticText(window, Rect(10, 20, 80, 20)).string_("DC/Wind:");
		StaticText(window, Rect(10, 40, 80, 20)).string_("Max. Pres.:");
		StaticText(window, Rect(10, 60, 80, 20)).string_("Blow:");
		StaticText(window, Rect(10, 80, 80, 20)).string_("SR Cor.:");
		StaticText(window, Rect(10, 100, 80, 20)).string_("Tune:");
		StaticText(window, Rect(10, 120, 80, 20)).string_("P. Track1:");
		StaticText(window, Rect(10, 140, 80, 20)).string_("Damp:");
		StaticText(window, Rect(10, 160, 80, 20)).string_("In Rev:");
		StaticText(window, Rect(10, 180, 80, 20)).string_("HP Lo:");
		StaticText(window, Rect(10, 200, 80, 20)).string_("HP Hi:");
		StaticText(window, Rect(10, 220, 80, 20)).string_("Hp track:");
		StaticText(window, Rect(10, 240, 80, 20)).string_("LP Lo:");
		StaticText(window, Rect(10, 260, 80, 20)).string_("LP Hi:");
		StaticText(window, Rect(10, 280, 80, 20)).string_("LP Track:");
		StaticText(window, Rect(10, 300, 80, 20)).string_("Push");
		StaticText(window, Rect(10, 320, 80, 20)).string_("Offset:");
		StaticText(window, Rect(10, 340, 80, 20)).string_("Amp Thresh:");
		
		StaticText(window, Rect(10, 380, 80, 20)).string_("Breath:");
		
		threshLed = Button(window, Rect(90, 365, 10, 10))
			.states_([["", Color.black, Color.white], ["", Color.black, Color.red]]).enabled_(false);
		
		breathView = CompositeView(window, Rect(90, 385, 200, 10)).background_(Color.blue);
			
		HrButton(window, Rect(10, 410, 60, 20)).states_([["Reset"]])
			.action_
			({
				wiNumBoxes.do({|item, cnt| item.valueAction_(resetVals[cnt]); });
			});
		
		HrButton(window, Rect(80, 410, 60, 20)).states_([["Panic"]])
			.action_
			({
				fork
				{
				synthInstance.free;
				synthInstance = Synth("windMo"++uniqueID, [\inBus0, inBusses[0], \outBus0, outBusses[0], \outBus1, outBusses[1]], target: group);
				Server.default.sync;
				{ wiNumBoxes.do({|item, cnt| item.valueAction_(resetVals[cnt]); }); }.defer;
				};
			});
		
		wiSpecs.add([0, 1, \lin, 0.01, 0.5].asSpec);
		wiSpecs.add([0, 2, \lin, 0.01, 0.4].asSpec);
		wiSpecs.add([-2, 2, \lin, 0.01, -1.2].asSpec);
		wiSpecs.add([-5, 0, \lin, 0.01, -1.5].asSpec);
		wiSpecs.add([-60, 60, \lin, 0.01, 1].asSpec);
		wiSpecs.add([0, 2, \lin, 0.01, 1].asSpec);
		wiSpecs.add([0, 120, \lin, 1, 33].asSpec);
		wiSpecs.add([-90, 30, \lin, 1, -60].asSpec);
		wiSpecs.add([0, 120, \lin, 0.01, 12].asSpec);
		wiSpecs.add([0, 120, \lin, 0.01, 63].asSpec);
		wiSpecs.add([0, 2, \lin, 0.01, 0.26].asSpec);
		wiSpecs.add([0, 120, \lin, 0.01, 65].asSpec);
		wiSpecs.add([0, 120, \lin, 0.01, 98].asSpec);
		wiSpecs.add([0, 2, \lin, 0.01, 0.92].asSpec);
		wiSpecs.add([0, 2, \lin, 0.01, 1].asSpec);
		wiSpecs.add([-1, 1, \lin, 0.01, -0.5].asSpec);
		wiSpecs.add([0, 1, \lin, 0.0001, 0.001].asSpec);
		
		resetVals = wiSpecs.collect({|item| item.default; });
		
		tags.size.do
		({|cnt| 
			 
			wiSliders.add
			(
				HrSlider(window, Rect(90, 20 * (cnt + 1), 150, 20))
					.value_(wiSpecs[cnt].unmap(wiSpecs[cnt].default))
					.action_
					({|sld|
						
						wiNumBoxes[cnt].valueAction_(wiSpecs[cnt].map(sld.value));
					}); 
			);
		});
		
		tags.size.do
		({|cnt| 
			
			wiNumBoxes.add
			(
				NumberBox(window, Rect(240, 20 * (cnt + 1), 50, 20))
					.value_(wiSpecs[cnt].default)
					.clipLo_(wiSpecs[cnt].minval)
					.clipHi_(wiSpecs[cnt].maxval)
					.action_
					({|nmb|
					
						wiSliders[cnt].value_(wiSpecs[cnt].unmap(nmb.value));
						synthInstance.set(tags[cnt], nmb.value);
					}); 
			);
		});
		
		fork
		{
			sDef = 
				SynthDef("windMo"++uniqueID,
				{
					arg selDCN = 0.7, pressure = 0.4, mCor = -1.2, srCor = -1.5, pTune = 1,
						kTrack = 1, damp = 33, rt = -60, presHpLo = 12, presHpHi = 63, 
						presHpTrack = 0.26, presLpLo = 65, presLpHi = 98, presLpTrack = 0.92,
						push = 1, offset = -0.5, ampThresh = 0.001, inBus0, outBus0, outBus1;
						
					var in = InFeedback.ar(inBus0);
					var wind, wEnv, delTune, fb, pushPull, fbMul, delayed, presFiltered;
					var note = Tartini.kr(LPF.ar(in, 50.midicps))[0].cpsmidi;
					var amp = Amplitude.ar(in);
					var ampKill;
					
					pressure = pressure + (0.6 * amp.lag(0.1));
					
					fb = LocalIn.ar(1);
					delTune = (note + pTune + (pressure * mCor)).midicps.reciprocal + 
						(srCor * SampleRate.ir.reciprocal) - (SampleDur.ir * 64);
					
					wEnv = amp;
					wind = SelectX.ar(selDCN, [wEnv, OpLPF.ar(WhiteNoise.ar(wEnv), note.midicps)]);
					pushPull = ((fb * push) + offset) * wind;
					fbMul = -60 * ((delTune * 1000) / (1000 / (((note - 60) * kTrack) + (-2 * damp + rt)).midicps));
					delayed = (DelayC.ar(pushPull + (fb * fbMul.dbamp * 1), 1, delTune) * 1).tanh;
					presFiltered = 
						OpLPF.ar
						(
							OpHPF.ar
							(
								delayed, 
								(((note-60) * presHpTrack) + ((pressure * (presHpHi - presHpLo)) + presHpLo))
							), 
							(((note-60) * presLpTrack) + ((pressure * (presLpHi - presLpLo)) + presLpLo)).midicps
						);
					
					LocalOut.ar(BadNanny.ar(presFiltered));
					
					ampKill = amp.lag(0.1) > ampThresh;
					
					SendReply.ar(Impulse.ar(30), "windMo_OSC_"++uniqueID, [ampKill, pressure]);
					
					Out.ar(outBus0, presFiltered * ampKill.lag(0.1));
					Out.ar(outBus1, presFiltered * ampKill.lag(0.1));
				});
			
			sDef.memStore;
			
			Server.default.sync;
			
			synthInstance = Synth("windMo"++uniqueID, [\inBus0, inBusses[0], \outBus0, outBusses[0], \outBus1, outBusses[1]], target: group);
		};
		
		responder = OSCresponderNode(Server.default.addr, "windMo_OSC_"++uniqueID,
			{
				arg time, responder, msg;
				//[time, responder, msg].postln;
				{
					if(msg[3] > 0,
					{
						if(threshLed.value != 1,
						{
							threshLed.value_(1);
						});
						
						if(breathView.bounds.width > 1,
						{
							breathView.bounds = breathView.bounds.width_((breathView.bounds.width - (msg[4] * 0.5)).clip(1, 200));
						});
					},
					{
						if(threshLed.value != 0,
						{
							threshLed.value_(0);
						});
						
						if(breathView.bounds.width < 200,
						{
							breathView.bounds= breathView.bounds.width_((breathView.bounds.width + 5).clip(1, 200));
						});
					});
				}.defer;
			}).add;
			
			saveGets = wiNumBoxes.collect({|item| { item.value; }; });
				
			saveSets = wiNumBoxes.collect({|item, cnt| {|argg| item.valueAction_(argg); resetVals[cnt] = argg; }; });
				

			
	}
	
	updateBusConnections
	{
		synthInstance.set(\inBus0, inBusses[0], \outBus0, outBusses[0], \outBus1, outBusses[1]);
	}
	
	cleanUp
	{
		synthInstance.free;
		responder.remove;
	}
}