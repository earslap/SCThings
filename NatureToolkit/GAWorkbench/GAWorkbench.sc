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
	var <genePool, <fitnessScores;
	
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
			this.internalCrossover;
		});
	}
	
	internalCrossover
	{
		var tempGenePool = List.new;
		var tempChromosome, splitPoint;
		var tempParent1, tempParent2;
		var offspring;
		
		if(isElitist, { tempGenePool.add(genePool[0]); });
		
		while({ tempGenePool.size < poolSize; },
		{
			if(randImmigrantProb.coin,
			{
				tempGenePool.add(randomChromosomeFunc.value);
			},
			{
				splitPoint = chromosomeSize.rand;
				//kind of roulette whel selection. fitters have more chance.
				tempParent1 = genePool[(exprand(1, poolSize) - 1).floor.asInteger];
				tempParent2 = genePool[(exprand(1, poolSize) - 1).floor.asInteger];
				offspring = tempParent1[0..splitPoint] ++ 
					tempParent2[(splitPoint+1)..chromosomeSize];
				
				if(mutationProb.coin, { offspring = mutationFunc.value(offspring); });
				
				tempGenePool.add(offspring);
				
				//hellotta slower
				if(allowClones.not, { tempGenePool = tempGenePool.asSet.asList; });
			});
		});
		
		genePool = tempGenePool;
		fitnessScores = nil ! poolSize;
			
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

