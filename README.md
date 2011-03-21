# Backup all your posterous blogs #

## Requirements ##
* Java v1.5 / v1.6+
* Groovy

## How to use ##
	# groovy posterous.groovy

	This is posterous export script v 0.1
	error: Missing required options: u, p, o
	usage: groovy posterous.groovy -u email@posterous -p password -o outputFolder
	 -h,--help provides full help and usage information
	 -o,--output  An eventually existing output folder, where all data will be output. Beware, if some data exists in that folder, it may be overwritten.
	 -p,--password  Unfortunatly one have to give its password to this little script
	 -u,--username  Sets posterous mail address here

On the first run grape (groovy package manager) will download and install all required dependencies.



## File Structure ##
* One folder for each site.
* In each folder, entries keep the file name they have in posterous, followed by a nice .xml extension.
* Each media associated to an entry uses the entry name, followed by _#anumer, where the #number is the media number.

>
	+---knackfx.posterous.com
	|   all-your-bases-are-belong-to-rest.xml
	|	   cant-wait-for-the-671.xml
	|   knack-it.xml
	|   netbeans-671-available-for-download.xml
	|   rest-in-game.xml
	|   serve-me-good-games.xml
	|   whats-new-today.xml
	| 
	\---riduidel.posterous.com
	   il-mavait-prevenu-le-bougre.xml
	   il-mavait-prevenu-le-bougre_0.png
	   il-mavait-prevenu-le-bougre_1.jpg
	   tester-lutilisation-de-la-memo.xml
	   tester-lutilisation-de-la-memo_0.gif	
	   tester-lutilisation-de-la-memo_1.jpg
	   the-big-band-theory.xml
	   the-gaf-collection-collected.xml
	   the-gaf-collection-collected_0.jpg
	   the-gaf-collection-collected_1.jpg
	   xkcd-movie-narrative-charts-0.xml


## XML Structure ##
	<post>
	  <title><!-- posterous title--></title>
	  <date>2008-12-31T17:48:00.000+0100<!-- posterous post date in a quite standard form --></date>
	<author>
	  <name><!-- author name --></name>
	  <pic><!-- author pic --></pic>
	</author>
	<body><![CDATA[
	<!-- post body, protected from XML interpreting by the CDATA section
	   ]]></body>
	<comments>
	<comment>
	<author>
	  <name></name>
	  <pic></pic>
	</author>
	  <date></date>
	<body><![CDATA[
	   ]]></body>
	</comment>
	</comments>
	</post>

## License ##
creative commons license : [paternity, share-alike, no commercial use](http://creativecommons.org/licenses/by-nc-sa/3.0/).
