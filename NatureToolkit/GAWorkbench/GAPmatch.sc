GAPmatch
{
	var <gaInstance, <>synthName, <poolSize, <>synthName, <>sampleLoc,
	<>synthParams, <>paramSpace, <numThreads, <>tLatency;
	
	var <soundBuf, playerBus, <sOutBusses, responder, sGroup;
	
	var thrCondition, jobClumps, fitnessClumps, curRoundIndex, proceedToNextGen, <>lastGen,
	<shouldContinue;
	
	*new
	{|argPoolSize, argSynthName, argSampleLoc, argSynthParams, argParamSpace, argNumThreads|
		
		^super.new.init
			(
				argPoolSize, 
				argSynthName, 
				argSampleLoc, 
				argSynthParams, 
				argParamSpace, 
				argNumThreads
			);
	}
	
	init
	{|argPoolSize, argSynthName, argSampleLoc, argSynthParams, argParamSpace, argNumThreads|
	
		poolSize = argPoolSize ? 1;
		synthName = argSynthName ? "no_synth_name";
		sampleLoc = argSampleLoc ? "sounds/a11wlk01.wav".standardizePath;
		synthParams = argSynthParams ? [\noParamSet];
		paramSpace = argParamSpace ? [1.0.rand];
		numThreads = argNumThreads ? 1;
		thrCondition = Condition.new(false);
		shouldContinue = Condition.new(true);
		tLatency = 0.1;
		proceedToNextGen = true;
		
		gaInstance = GAWorkbench
			(
				poolSize, 
				paramSpace, 
				{ 1; },
				{|chrom|
					
					var tempChrom = paramSpace.value;
					var randIndex = (tempChrom.size - 1).rand;
					chrom[randIndex] = tempChrom[randIndex];
					chrom;
				}
			); 
		
		//gaInstance.allowClones = false;
		gaInstance.mutationProb = 0.2;
		gaInstance.randImmigrantProb = 0.1;
		
		sGroup = Group.new;
		soundBuf = Buffer.readChannel(Server.default, sampleLoc, channels: [0]).normalize;
		
		playerBus = Bus.audio(Server.default, 1);
		sOutBusses = { Bus.audio(Server.default, 1); } ! numThreads;
		
		SynthDef(\gapmatch_player,
		{
			Out.ar(playerBus, PlayBuf.ar(1, soundBuf, doneAction: 2));
		}).memStore;
		
		SynthDef(\gapmatch_comparator,
		{
			arg totalTime, inBus, cIndex1, cIndex2;
			
			var inSynth = In.ar(inBus, 1);
			var inReal = In.ar(playerBus, 1);
			var endTrig = Line.kr(0, 1, totalTime).floor;
			var chainA, chainB, chain;
			chainA = FFT(LocalBuf(2048), inSynth, wintype: 1);
			chainB = FFT(LocalBuf(2048), inReal, wintype: 1);
			chain = FrameCompare.kr(chainA, chainB, 0.5);
			SendReply.kr(endTrig, 'gapmatch_anafinito', [chain, cIndex1, cIndex2]);
			FreeSelf.kr(endTrig);
			//Out.ar(0, [inSynth, inReal]);
			
		}).memStore;
		
		responder = OSCresponderNode(nil, 'gapmatch_anafinito',
			{|t, r, msg|
				
				var fitness = msg[3];
				var index1 = msg[4];
				var index2 = msg[5];
				//msg.postln;
				//DPR.dpr("curRound", curRoundIndex);
				curRoundIndex = curRoundIndex - 1;
				fitnessClumps[index1][index2] = fitness.reciprocal;
				
				if(curRoundIndex == 0,
				{
					"curRound finished".postln;
					thrCondition.test = true;
					thrCondition.signal;
					if(fitnessClumps.flat.includes(nil).not,
					{
						"generation finished".postln;
						gaInstance.injectFitness(fitnessClumps.flat);
						fitnessClumps.flat.asCompileString;
						lastGen = [gaInstance.genePool, gaInstance.fitnessScores];
						gaInstance.crossover;
						jobClumps = gaInstance.genePool.clump(numThreads);
						fitnessClumps = Array.fill(gaInstance.genePool.size).clump(numThreads);
						thrCondition.test = false;
					});
				});
				
				
			});
	}
	
	startJob
	{
		proceedToNextGen = true;
		shouldContinue.test = true;
		
		fork
		({
			
			while({ proceedToNextGen; },
			{
				jobClumps = gaInstance.genePool.clump(numThreads);
				fitnessClumps = Array.fill(gaInstance.genePool.size).clump(numThreads);
				thrCondition.test = false;
				if(responder.notNil, { responder.remove; });
				responder.add;
				jobClumps.do
				({|jItem, jCount|
				
					curRoundIndex = jItem.size;
					thrCondition.test = false;
					Server.default.makeBundle(tLatency,
						{
							Synth(\gapmatch_player, target: sGroup);
							jItem.do
							({|gParam, gCount|
								
								Synth
								(
									synthName, 
									([synthParams, gParam].flop.flat ++ 
										[\outBus, sOutBusses[gCount]]), 
									target: sGroup
								);
								Synth(\gapmatch_comparator,
									[
										\totalTime,
										soundBuf.duration,
										\inBus,
										sOutBusses[gCount],
										\cIndex1,
										jCount,
										\cIndex2,
										gCount
									], target: sGroup, addAction: \addToTail);
							});
						});
						
					thrCondition.wait;
					
					sGroup.freeAll;
					Server.default.sync;
					shouldContinue.wait;
				});
			});
			
		});
	}
	
	stopJob
	{
		proceedToNextGen = false;
	}
	
	pauseJob
	{
		shouldContinue.test = false;
	}
	
	resumeJob
	{
		shouldContinue.test = true;
		shouldContinue.signal;
	}
	
	playBest
	{
		var synth;
		fork
		{
			synth = Synth(synthName, [synthParams, lastGen[0][0]].flop.flat ++ [\outBus, 0]);
			soundBuf.duration.wait;
			synth.free;
		};
	}
	
	cleanUp
	{
		sGroup.free;
		sOutBusses.do(_.free);
		playerBus.free;
		soundBuf.free;
		responder.remove;
	}
}