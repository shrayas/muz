package com.thuthuri;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ThuthuriMain
{

	public static void main(String[] args)
	{
		try
		{
			
			String proxyHost = "" , proxyPort = "" , proxyUsername = "", proxyPassword = "", destinationDir = "", manga = "";
			int from = 1, to = -1;
			
			// GNU parser is better at parsing out options > 1 char long
			CommandLineParser parser = new GnuParser();
			
			// Create a new set of options
			Options options = new Options();
			
			// Add the required set of options to be tracked
			options.addOption("h","help",false,"show help");
			options.addOption("ph","proxy-host",true,"proxy host to pass through");
			options.addOption("pp","proxy-port",true,"proxy port to pass through");
			options.addOption("pu","proxy-username",true,"username to use to authenticate proxy");
			options.addOption("pw","proxy-password",true,"proxy password");
			options.addOption("m","manga",true,"manga to download (name should be same as in mangareader.net)");
			options.addOption("f","from-chapter",true,"which chapter to download from");
			options.addOption("t","to-chapter",true,"which chapter to download till");
			//options.addOption("c","chapters",true,"comma seperated values of chapters to get");
			options.addOption("d","destination-directory",true,"destination directory to dump to");
			
			// Format the help out of the options defined
			HelpFormatter formatter = new HelpFormatter();
			
			// parse the command line arguments
		    CommandLine line = parser.parse( options, args );

			// If *m* and *d* aren't among the arguments , then do not proceed
		    if (!line.hasOption("m") || !line.hasOption("d"))
		    {
		    	System.out.println("'m' and 'd' are mandatory arguments , usage is as below");
		    	formatter.printHelp("muz [options]",options);
				return;
		    }
		    
		    // display help
		    if( line.hasOption( "h" ) ) {
				formatter.printHelp( "muz", options );
				return;
		    }
		    
		    // for each option , store the value inside the variables required
		    // Proxy Host
		    if( line.hasOption("ph")) {
		    	proxyHost = line.getOptionValue( "ph" ).toString();
		    }
		    
		    // Proxy Port
		    if( line.hasOption( "pp" ) ) {
		    	proxyPort = line.getOptionValue( "pp" ).toString();
		    }
		    
		    // Proxy username
		    if( line.hasOption( "pu" ) ) {
		    	proxyUsername = line.getOptionValue( "pu" ).toString();
		    }
		    
		    // Proxy password
		    if( line.hasOption( "pw" ) ) {
		    	proxyPassword = line.getOptionValue( "pw" ).toString();
		    }
		    
		    // destination directory
		    if (line.hasOption("d")){
		    	destinationDir = line.getOptionValue("d").toString();
		    }
		    
		    // manga
		    if (line.hasOption("m")){
		    	manga = line.getOptionValue("m").toString();
		    }
		    
		    // from
		    if (line.hasOption("f")){
		    	from = Integer.parseInt(line.getOptionValue("f").toString());
		    }
		    
		    // to
		    if (line.hasOption("t")){
		    	to = Integer.parseInt(line.getOptionValue("t").toString());
		    }
		    
		    // Set the proxy details
			System.setProperty("http.proxyHost", proxyHost);
			System.setProperty("http.proxyPort", proxyPort);
			System.setProperty("http.proxyUser", proxyPort);
			System.setProperty("http.proxyPassword", proxyPort);

			// Set the base url - Only downloads from mangareader
			String baseUrl = "http://www.mangareader.net";
			
			// set the destination directory
			String saveBaseFolder = destinationDir;
			
			System.out.println("Downloading "+manga+" from "+(baseUrl + "/" + manga));

			/*
			 *  connect and download the page for the manga
			 *  Eg: http://www.mangareader.net/naruto 
			 */
			Document doc = Jsoup.connect(baseUrl + "/" + manga).get();

			/*
			 * Get the list of chapters and links
			 * In a table specified with the id *listing* take the first *td* 
			 */
			Elements chapters = doc.select("#listing td:lt(1)");
			
			// Print the amount of chapters
			int noOfChapters = chapters.size();
			
			if (to == -1)
				to = noOfChapters;

			// Log the start time
			long start = System.currentTimeMillis();
			
			// chapter count
			int currentChapterIndex = 1;
			
			System.out.println("Downloading chapters from " +from+ " to "+to);

			
			// Iterate over the list of chapters
			for (int i=(from-1); i<to;i++)
			{
				
				Element chapter = chapters.get(i);

				/*
				// the boundary logic
				if (currentChapterIndex < from)
					continue;
				else if (currentChapterIndex > to)
					break;
				*/
				
				System.out.println("\n("+(currentChapterIndex)+"/"+noOfChapters+")");

				// pick up the chapter text
				String chapterText = chapter.text();
				
				String[] chapterTextParts = chapterText.trim().split(":");
				
				String chapterName = "";
				
				if (chapterTextParts.length > 1)
				{
					chapterName = " [ "+ chapterTextParts[1].trim() + " ]";
					
					if (chapterName.contains("?"))
						chapterName.replace("?", "");
				}
				
				//String mangaNameChapterNumber = chapterTextParts[0];
				
				//String chapterNumber = mangaNameChapterNumber.trim().split(" ")[1];
				
				String chapterNumber = ""+(i+1);
				
				// construct the directory name
				String directoryName = chapterNumber + chapterName;
				
				// get the absolute directory name
				String dirName = saveBaseFolder + "//" + directoryName;
				
				// create the directory
				boolean success = (new File(dirName)).mkdir();
				
				// if creation was a failure 
				if (!success)
				{
					// throw an exception 
					throw new Exception("Problem with directories");
				}
				
				// get the chapter link
				String chapterHref = chapter.select("a").attr("href");

				// construct the absolute url
				String chapterUrl = baseUrl + chapterHref;

				// get the chapter document
				Document chapterDoc = Jsoup.connect(chapterUrl).get();

				// in the document , get the select specified by the *pageMenu* id and get the list of options under it
				Elements pages = chapterDoc.select("#pageMenu option");
				
				int noOfPages = pages.size();
				
				// initialize the page number
				int pageNo = 1;
				
				// set a page percent count
				int pageI = 0;
				
				int pagePercent = 0;
				
				// Iterate on the list of pages
				for (Element page : pages)
				{
					
					// calculate percent of chapter downloaded
					pagePercent = (int)((pageI / (double) noOfPages) * 100.00);
					
					// print a progress bar
					printProgressBar(pagePercent);
					
					// get the page url
					String pageHref = page.attr("value");

					// create the absolute url
					String imageHref = baseUrl + pageHref;

					// get the image document
					Document imageDoc = Jsoup.connect(imageHref).get();

					// get the image element ( represented by the tag with *img* id ) 
					Element image = imageDoc.select("#img").first();

					// get the source of the image
					String imageUrl = image.attr("src");

					// construct the filename
					String fileName = dirName + "//" + pageNo + ".jpg";
					
					// save the image
					saveImage(imageUrl,fileName);
					
					// increment the page number
					pageNo++;
					
					// increment the page count
					pageI++;

				}
				
				pagePercent = (int)((pageI / (double) noOfPages) * 100.00);
				
				printProgressBar(pagePercent);
				
				
				// Increment the chapter count
				currentChapterIndex++;

				/*
				if (chapI == 5)
					break;
				 */
			}
			
			// log the end
			long end = System.currentTimeMillis();
			
			// get the difference 
			long diff = end - start;
			
			System.out.println("\n"+(diff/1000)+" seconds");
		}

		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	private static void saveImage(String imageUrl,String fileName)
	{
		try
		{
			URL url = new URL(imageUrl);
			InputStream in = new BufferedInputStream(url.openStream());
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			
			byte[] buf = new byte[1024];
			int n = 0;
			
			while (-1 != (n = in.read(buf)))
			{
				out.write(buf, 0, n);
			}
			
			out.close();
			in.close();
			byte[] response = out.toByteArray();
	
			FileOutputStream fos = new FileOutputStream(fileName);
			fos.write(response);
			fos.close();
		}
		
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	private static void printProgressBar(int percent)
	{
	    StringBuilder bar = new StringBuilder("[");

	    for(int i = 0; i < 50; i++)
	    {
	        if( i < (percent/2))
	        {
	            bar.append("=");
	        }
	        
	        else if( i == (percent/2))
	        {
	            bar.append(">");
	        }
	        
	        else
	        {
	            bar.append(" ");
	        }
	    }

	    bar.append("]   " + percent + "%");
	    System.out.print("\r" + bar.toString());
	}

}
