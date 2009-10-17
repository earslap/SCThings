//Batuhan Bozkurt 2009
GAWorkbench
{
	//flags
	var <>isElitist, <>allowClones;
	
	//probabilities
	var <>mutationProb, <>randImmigrantProb;
	
	//feats
	var <>randomChromosomeFunc, <>fitnessFunc, <>mutationFunc, poolSize, chromosomeSize;
	
	//internal state
	var <>genePool, <fitnessScores;
	
	//pluggable crossover function
	var <>userCrossover, <>externalCrossover;
	
	var <>lowerBetter;
	
	*new
	{|argPoolSize, argRandomChromosomeFunc, argFitnessFunc, argMutationFunc|
	
		^super.new.init(argPoolSize, argRandomChromosomeFunc, argFitnessFunc, argMutationFunc);
	}
	
	init
	{|argPoolSize, argRandomChromosomeFunc, argFitnessFunc, argMutationFunc|
		
		poolSize = argPoolSize ? 100;
		
		randomChromosomeFunc = argRandomChromosomeFunc ? { { 100.0.rand; } ! 100; };
		fitnessFunc = argFitnessFunc ? {|chromosome| chromosome.sum / 100; };
		mutationFunc = 
			argMutationFunc ? 
				{|chromosome| 
					
					chromosome[(chromosome.size - 1).rand] = 100.0.rand; 
					chromosome; 
				};
		
		randImmigrantProb = 0;
		mutationProb = 0.08;
		
		externalCrossover = false;
		
		genePool = List.new;
		fitnessScores = nil ! poolSize;
		
		isElitist = true;
		allowClones = true;
		
		lowerBetter = false;
		
		this.initGenePool;
		this.rateFitness;
		
	}
	
	initGenePool
	{
		poolSize.do
		({
			genePool.add(randomChromosomeFunc.value);
		});
		
		chromosomeSize = genePool[0].size;
	}
	
	rateFitness
	{
		var tempOrder;
		
		poolSize.do
		({|cnt|
			
			fitnessScores[cnt] = fitnessFunc.value(genePool[cnt]);
		});
		
		tempOrder = fitnessScores.order;
		fitnessScores = fitnessScores[tempOrder];
		genePool = genePool[tempOrder];
		
		if(lowerBetter == false,
		{
			fitnessScores = fitnessScores.reverse;
			genePool = genePool.reverse;
		});
	}
	
	crossover
	{
		if(externalCrossover,
		{
			genePool = userCrossover.value
				(
					genePool, 
					isElitist, 
					mutationProb, 
					randImmigrantProb, 
					allowClones
				);
		},
		{
			//fitnessScores[0].reciprocal.postln;
			this.internalCrossover;
		});
	}
	
	internalCrossover
	{
		var tempGenePool = List.new;
		var tempChromosome, splitPoint;
		var tp1, tp2, tour, tempParent1, tempParent2;
		var offspring1, offspring2;
		
		if(isElitist, { tempGenePool.add(genePool[0]); });
		
		while({ tempGenePool.size < poolSize; },
		{			
			//splitPoint = chromosomeSize.rand;
			//tournament selection with tournament size 2
			tp1 = rrand(0, poolSize - 1);
			tp2 = rrand(0, poolSize - 1);
			tour = fitnessScores[[tp1, tp2]];
			
			if(tour[0] > tour[1], { tempParent1 = genePool[tp1]; }, { tempParent1 = genePool[tp2]; });
			
			tp1 = rrand(0, poolSize - 1);
			tp2 = rrand(0, poolSize - 1);
			tour = fitnessScores[[tp1, tp2]];
			
			if(tour[0] > tour[1], { tempParent2 = genePool[tp1]; }, { tempParent2 = genePool[tp2]; });
			
			#offspring1, offspring2 = this.mateParents(tempParent1, tempParent2);
			
			tempGenePool.add(offspring1);
			if(tempGenePool.size < poolSize, { tempGenePool.add(offspring2); });
			if(randImmigrantProb.coin and: { tempGenePool.size < poolSize }, { tempGenePool.add(randomChromosomeFunc.value); });
			
			//hellotta slower
			if(allowClones.not, { tempGenePool = tempGenePool.asSet.asList; });
		
		});
		
		genePool = tempGenePool;
		fitnessScores = nil ! poolSize;
			
	}
	
	mateParents
	{|p1, p2|
	
		var relief = { 2.rand; } ! chromosomeSize;
		var offspring1 = List.new, offspring2 = List.new;
		
		relief.do
		({|bump, cnt|
			
			if(bump == 1,
			{
				offspring1.add(p1[cnt]);
				offspring2.add(p2[cnt]);
			},
			{
				offspring1.add(p2[cnt]);
				offspring2.add(p1[cnt]);
			});
		});
		
		if(mutationProb.coin, { offspring1 = mutationFunc.value(offspring1); });
		if(mutationProb.coin, { offspring2 = mutationFunc.value(offspring2); });
		
		^[offspring1.asArray, offspring2.asArray];	
	}
	
	injectFitness
	{|argFitness|
	
		var tempOrder;
		
		if(argFitness.size != poolSize,
		{
			"poolSize is % but supplied fitness array has a size of %. Can't use these values.".format(poolSize, argFitness.size).error;
		},
		{
			fitnessScores = argFitness;
			tempOrder = fitnessScores.order;
			fitnessScores = fitnessScores[tempOrder];
			genePool = genePool[tempOrder];
			
			if(lowerBetter == false,
			{
				fitnessScores = fitnessScores.reverse;
				genePool = genePool.reverse;
			});
		});
	}

}

