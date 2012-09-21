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
			
			String proxyHost = "" , proxyPort = "" , destinationDir = "", manga = "";
			
			// GNU parser is better at parsing out options > 1 char long
			CommandLineParser parser = new GnuParser();
			
			// Create a new set of options
			Options options = new Options();
			
			// Add the required set of options to be tracked
			options.addOption("h","help",false,"show help");
			options.addOption("ph","proxyhost",true,"proxy host to pass through");
			options.addOption("pp","proxyport",true,"proxy port to pass through");
			options.addOption("m","manga",true,"manga to download (name should be same as in mangareader.net)");
			options.addOption("d","destinationdirectory",true,"destination directory to dump to");
			
			// Format the help out of the options defined
			HelpFormatter formatter = new HelpFormatter();
			
			// parse the command line arguments
		    CommandLine line = parser.parse( options, args );

			// If number of arguments are lesser than 6 (all args required) , display help and quit
		    
		    if (!line.hasOption("m") || !line.hasOption("d"))
		    {
		    	System.out.println("'m' and 'd' are mandatory arguments , usage is as below");
		    	formatter.printHelp("muz",options);
				return;
		    }
		    
		    if( line.hasOption( "h" ) ) {
				formatter.printHelp( "muz", options );
				return;
		    }
		    
		    // for each option , store the value inside the variables required
		    
		    if( line.hasOption("ph")) {
		    	proxyHost = line.getOptionValue( "ph" ).toString();
		    }
		    
		    if( line.hasOption( "pp" ) ) {
		    	proxyPort = line.getOptionValue( "pp" ).toString();
		    }
		    
		    if (line.hasOption("d")){
		    	destinationDir = line.getOptionValue("d").toString();
		    }
		    
		    if (line.hasOption("m")){
		    	manga = line.getOptionValue("m").toString();
		    }
		    
			System.setProperty("http.proxyHost", proxyHost);
			System.setProperty("http.proxyPort", proxyPort);

			String baseUrl = "http://www.mangareader.net";
			String saveBaseFolder = destinationDir;

			Document doc = Jsoup.connect(baseUrl + "/" + manga).get();

			System.out.println(doc.title());

			Elements chapters = doc.select("#listing td:lt(1) a");

			long start = System.currentTimeMillis();
			
			for (Element chapter : chapters)
			{
				String chapterName = chapter.text();
				
				String dirName = saveBaseFolder + "//" + chapterName;
				
				boolean success = (new File(dirName)).mkdir();
				
				if (!success)
				{
					throw new Exception("Problem with directories");
				}
				
				String chapterHref = chapter.attr("href");

				String chapterUrl = baseUrl + chapterHref;

				Document chapterDoc = Jsoup.connect(chapterUrl).get();

				Elements pages = chapterDoc.select("#pageMenu option");

				int pageNo = 1;
				
				for (Element page : pages)
				{
					String pageHref = page.attr("value");

					String imageHref = baseUrl + pageHref;

					Document imageDoc = Jsoup.connect(imageHref).get();

					Element image = imageDoc.select("#img").first();

					String imageUrl = image.attr("src");

					String fileName = dirName + "//" + pageNo + ".jpg";
					
					saveImage(imageUrl,fileName);
					
					pageNo++;

				}

				break;
			}
			
			long end = System.currentTimeMillis();
			
			long diff = end - start;
			
			System.out.println("Took "+(diff/1000)+" seconds");
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

}
