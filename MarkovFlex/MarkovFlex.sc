/*
	n-dimensional Markov Chain implementation
	Batuhan Bozkurt 2009
	
	This class stores the actual values instead of their 
	weightings so can be a memory hog, but it's relatively fast.
	In higher order chains, if uncertainity occurs, this class
	can fall back to lower orders and choose one from a lower
	set and keep continuity relatively consistent.
	
	Notes to self (and to other devs) about the implementation 
	details:
	
	mDict is the base Dictionary. When the probability table is
	being populated, for each value, a new dictionary inside the
	parent dictionary is created, and except for the base dict,
	each dictionary has a '!mdb' item which holds all the past
	transition values  in a List. A new value is chosen from inside
	that list. The nested dictionaries go as deep as the order of the
	chain goes. The data laid out in this way. It is a bit confusing
	so I'm keeping a note as a self reminder.
*/

MarkovFlex
{
	var <maxOrder, <mDict, <lastItems, <>verbose, <>allowNilResult, <latestOutVals;
	
	*new
	{|argMaxOrder|
	
		^super.new.init(argMaxOrder);
	}
	
	init
	{|argMaxOrder|
	
		maxOrder = argMaxOrder ? 1;
		lastItems = nil ! (maxOrder + 1);
		mDict = Dictionary.new;
		verbose = false;
		allowNilResult = true;
		latestOutVals = nil ! (maxOrder + 1);
		
	}
	
	pushItem
	{|argItem, isInternalCall|
	
		lastItems.pop;
		lastItems = [argItem] ++ lastItems;
		this.injectToMDict(this.giveOrdersArray);
		if(verbose and: { isInternalCall.isNil }, { "MarkovFlex: Item pushed into probability table.".postln; });
	}
	
	giveOrdersArray
	{
		var tempList = List.new;
		var tempAddItem;
		var tempLastItems = lastItems.reverse.reject({|item| item == nil; });
		
		(lastItems.size-1).do
		({|cnt|
		
			cnt = lastItems.size - 2 - cnt;
			tempAddItem = tempLastItems[cnt..maxOrder + 1];
			if(tempAddItem.size > 1,
			{
				tempList.add(tempAddItem);
			});
		});
	
		
		^tempList;
	}
	
	injectToMDict
	{|argOrdersArray|
	
		var ord = argOrdersArray;
		var currentDict;
		
		if(ord.size > 0,
		{
			ord.do
			({|ordItem|
				
				currentDict = mDict; //anchor base dict
				(ordItem.size - 1).do
				({|cnt|
					
					if(currentDict.at(ordItem[cnt]).isNil,
					{//if a dict is not already created, create it. we will work in it.
						currentDict.put(ordItem[cnt], Dictionary.new.put('!mdb', List.new));
					});
					
					if(cnt == (ordItem.size - 2),
					{//if ordItem is the 2nd item from the end, insert the superseding
					 //item do the !mdb of the Dictionary at this level.
						currentDict.at(ordItem[cnt]).at('!mdb').add(ordItem.last);
					});
					currentDict = currentDict.at(ordItem[cnt]);
				});
				
				if(currentDict.at(ordItem.last).isNil,
				{
					currentDict.put(ordItem.last, Dictionary.new.put('!mdb', List.new));
				});
			});
		});
	}
	
	pushSequence
	{|argSequence|
	
		argSequence.do
		({|item|
			
			if(item.notNil,
			{
				this.pushItem(item, true);
			});
		});
		
		if(verbose, { "MarkovFlex: Sequence pushed into probability table.".postln; });
	}
	
	nextFor
	{|argSequence|
	
		var finalVal = this.calculateNext(argSequence);

		latestOutVals.removeAt(0);
		latestOutVals = latestOutVals ++ [finalVal];
		
		^finalVal;
	}
	
	autoAdvance
	{
		^this.nextFor(latestOutVals);
	}
	
	calculateNext
	{|argSequence, argCurrentOrder|
	
		var currentDict = mDict;
		var finalVal;
		var tempSequence;
		var currentOrder = argCurrentOrder ? argSequence.size.clip(1, maxOrder);
		
		tempSequence = argSequence[(argSequence.size-maxOrder)..(argSequence.size-1)];
		
		block
		({|break|
			tempSequence.reject({|item| item.isNil; }).do
			({|item|
				
				if(currentDict.at(item).notNil,
				{
					
					currentDict = currentDict.at(item);
				},
				{
					if(verbose, 
					{ 
						"MarkovFlex: No match found for order %, going a level higher.".format(currentOrder).postln; 
					});
					
					if(tempSequence.size > 1,
					{
						currentOrder = currentOrder - 1;
						^this.calculateNext(tempSequence[1..(tempSequence.size-1)], currentOrder);
					},
					{
						currentDict = nil;
						break.value;
					});
				});
			});
		});
		
		if(currentDict.notNil,
		{
			finalVal = currentDict.at('!mdb').choose;
			
			if(finalVal.notNil,
			{
				if(verbose, { "MarkovFlex: Item found for given order.".postln; });
				^finalVal;
			},
			{
				^this.handleUncertainity;
			});
		},
		{
			^this.handleUncertainity;
		});
	}
	
	handleUncertainity
	{
		if(allowNilResult,
		{
			if(verbose, { "MarkovFlex: No match found. allowNilResult = true so returning nil.".postln; });
			^nil;
		},
		{
			if(verbose, { "MarkovFlex: No match found. allowNilResult = false so returning a random value.".postln; });
			^this.returnRandom;
		});
	}
	
	returnRandom
	{ 
		^mDict.keys.choose;
	}

}