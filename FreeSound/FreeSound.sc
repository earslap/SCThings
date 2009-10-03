//Batuhan Bozkurt 2009
FreeSound
{
	classvar <aliveInstances;
	
	var <>keyword, credentials, searchDescriptions, searchTags, searchFileNames, searchUserNames, 
	durationMin, durationMax, order, startFrom, limit, <>verbose, >callbackFunc, <uniqueID, sampleIDBucket, numSamples;
	
	*initClass
	{
		aliveInstances = List.new;
	}
	
	*new
	{|argSearchOptions, argUserPass, argCallbackFunc|
	
		if(argUserPass.isNil, 
		{ 
			"You need to fill in your FreeSound username and password.".error;
			this.halt;
		});
		
		
		^super.new.init(argSearchOptions, argUserPass, argCallbackFunc);
	}
	
	init
	{|argSearchOptions, argUserPass, argCallbackFunc|
	
		credentials = argUserPass;
		keyword = argSearchOptions.at(\keyword) ? "train";
		searchDescriptions = argSearchOptions.at(\searchDescriptions) ? 1;
		searchTags = argSearchOptions.at(\searchTags) ? 1;
		searchFileNames = argSearchOptions.at(\searchFileNames) ? 0;
		searchUserNames = argSearchOptions.at(\searchUserNames) ? 0;
		durationMin = argSearchOptions.at(\durationMin) ? 1;
		durationMax = argSearchOptions.at(\durationMax) ? 20;
		order = argSearchOptions.at(\order) ? 1;
		startFrom = argSearchOptions.at(\startFrom) ? 0;
		limit = argSearchOptions.at(\limit) ? 100;
		
		sampleIDBucket = List.new;
		
		FreeSound.aliveInstances.add(this);
		uniqueID = this.getUniqueID;
		
		//callbackFunc args:
		//1: trying to login
		//2: login successful
		//-2: login failed
		//3: performing search
		//4: search complete, second arg is number of results returned
		//-4: there was an error in searching.
		//5: returning sample info, second argument is info dictionary.
		//-5: there was an error getting sample info.
		//6: file downloaded. secong argument is file path.
		//-6: there was an error in downloading the file.
		
		callbackFunc = argCallbackFunc ? {};
		
		verbose = false;
	}
	
	getUniqueID
	{
		var tempRand = 16777216.rand;
		var tempPool = FreeSound.aliveInstances.collect({|item| item.uniqueID; });
		
		while({ tempPool.detectIndex({|item| item == tempRand; }).notNil },
		{
			tempRand = 16777216.rand;
		});
		
		^tempRand;
	}
	
	doSearch
	{
		this.prTryLogin;
	}
	
	prTryLogin
	{
		var response, loginCheckFunc;
		
		if(verbose, { "Trying to log in...".postln; });
		callbackFunc.value(this, 1);
		
		("curl -c /tmp/cookies"++uniqueID++".txt -d \"username=" ++ credentials[0] ++ "&password=" ++ credentials[1] ++ "&redirect=../index.php&login=login&autologin=0\" http://www.freesound.org/forum/login.php > /dev/null").unixCmd
		({|res|
			if(res == 1, { if(verbose, { "There was an error logging in.".error; }); callbackFunc.value(this, -2); this.halt; });
			loginCheckFunc.value;		
		});
		
		loginCheckFunc = 
		{
			("curl -b /tmp/cookies"++uniqueID++".txt -I http://www.freesound.org/searchTextXML.php  > /tmp/scloginoutput"++uniqueID).unixCmd
			({|res|
				if(res == 1, { if(verbose, { "There was an error logging in.".error; }); callbackFunc.value(this, -2); this.halt; });
				response = String.readNew(File("/tmp/scloginoutput"++uniqueID, "r"));
				response = response.replace(" ", "").replace("\n", "");
				if(response.find("text/xml").notNil, 
				{
					if(verbose, { "Login was successful...".postln; });
					callbackFunc.value(this, 2);
					this.prPerformSearch.value;
				},
				{
					if(verbose, { "Login failed, check your username and password...".error; });
					callbackFunc.value(this, -2);
					this.halt;
				});
			});
		};
	}
	
	prPerformSearch
	{
		var response;
		
		if(verbose, { "Performing search...".postln; });		callbackFunc.value(this, 3);
		("curl -b /tmp/cookies"++uniqueID++".txt -d \"search="++keyword++"&start="++startFrom++"&searchDescriptions="++searchDescriptions++"&searchTags="++searchTags++"&searchFilenames="++searchFileNames++"&searchUsernames="++searchUserNames++"&durationMin="++durationMin++"&durationMax="++durationMax++"&order="++order++"&limit="++limit++"\" http://www.freesound.org/searchTextXML.php > /tmp/scsearchresult"++uniqueID).unixCmd
		({|res|
			
			if(res == 1, { if(verbose, { "There was an error in search.".error; }); callbackFunc.value(this, -4); this.halt; });
			
			response = DOMDocument.new("/tmp/scsearchresult"++uniqueID).getElementsByTagName("sample");
			if(response.size == 0,
			{
				if(verbose, { "No results returned...".postln; });
				callbackFunc.value(this, 4, 0);
			},
			{
				sampleIDBucket = List.new;
				response.do
				({|item|
				
					sampleIDBucket.add(item.getAttribute("id"));
				});
				if(verbose, { (response.size.asString+"sample(s) returned...").postln; });
				callbackFunc.value(this, 4, response.size);
			});
			
		});
		
	}
	
	numSamples
	{
		^sampleIDBucket.size;
	}
	
	getSampleInfo
	{|argIndex|
	
		var response, infoDict = Dictionary.new;
		
		if(argIndex > (sampleIDBucket.size - 1),
		{
			"Sample index out of range.".error;
			callbackFunc.value(this, -5);
		},
		{
			if(verbose, { "Getting sample info...".postln; });
			("curl -b /tmp/cookies"++uniqueID++".txt http://www.freesound.org/samplesViewSingleXML.php?id="++sampleIDBucket[argIndex]++" > /tmp/scsampleinfo"++sampleIDBucket[argIndex]++uniqueID).unixCmd
			({|res|
			
				if(res == 1, { if(verbose, { "There was an error in getting sample info.".error; }); callbackFunc.value(this, -5); this.halt; });
				response = DOMDocument.new("/tmp/scsampleinfo"++sampleIDBucket[argIndex]++uniqueID);
				infoDict.put(\numDownloads, response.getElementsByTagName("statistics")[0].getElementsByTagName("downloads")[0].getFirstChild.getText.asInteger);
				//infoDict.put(\rating, response.getElementsByTagName("statistics")[0].getElementsByTagName("rating")[0].getFirstChild.getText.asInteger);
				infoDict.put(\extension, response.getElementsByTagName("extension")[0].getFirstChild.getText.asSymbol);
				infoDict.put(\sampleRate, response.getElementsByTagName("samplerate")[0].getFirstChild.getText.asInteger);
				infoDict.put(\bitRate, response.getElementsByTagName("bitrate")[0].getFirstChild.getText.asInteger);
				infoDict.put(\bitDepth, response.getElementsByTagName("bitdepth")[0].getFirstChild.getText.asInteger);
				infoDict.put(\numChannels, response.getElementsByTagName("channels")[0].getFirstChild.getText.asInteger);
				infoDict.put(\duration, response.getElementsByTagName("duration")[0].getFirstChild.getText.asFloat);
				infoDict.put(\fileSize, response.getElementsByTagName("filesize")[0].getFirstChild.getText.asInteger);
				
				infoDict.put(\index, argIndex);
				callbackFunc.value(this, 5, infoDict);
			});
		});
	}
	
	downloadSample
	{|argIndex, argPath|
	
		var responseHeader, targetPath, downloadFunc, fileName;
		
		if(verbose, { "Getting sample location...".postln; });
		
		argPath = argPath ? "/tmp/";
		if(argPath[argPath.size-1].asSymbol != '/', { argPath = argPath ++ "/" });
		
		("curl -b /tmp/cookies"++uniqueID++".txt -I http://www.freesound.org/samplesDownload.php?id="++sampleIDBucket[argIndex]++" > /tmp/scdlresponseheader"++sampleIDBucket[argIndex]++uniqueID).unixCmd
		({|res|
		
			if(res == 1, { if(verbose, { "There was an error while trying to download file.".error; }); callbackFunc.value(this, -6); this.halt; });
			responseHeader = String.readNew(File("/tmp/scdlresponseheader"++sampleIDBucket[argIndex]++uniqueID, "r")).replace(" ", "").replace("\n", "");
			responseHeader = responseHeader[responseHeader.find("Location:")+9..responseHeader.find("Keep-Alive:")-2];
			fileName = responseHeader[responseHeader.findBackwards("/")+1..responseHeader.size-1];
			downloadFunc.value;
		});
		
		downloadFunc = 
		{
			if(verbose, { "Downloading file...".postln; });
			("curl -b /tmp/cookies"++uniqueID++".txt "++responseHeader++" > "++argPath++fileName).unixCmd
			({|res|
				if(res == 1, { if(verbose, { "There was an error while trying to download file.".error; }); callbackFunc.value(this, -6); this.halt; });
				
				if(verbose, { ("File "++fileName++" downloaded...").postln; });
				callbackFunc.value(this, 6, argPath++fileName);
			});
		}
	}

}