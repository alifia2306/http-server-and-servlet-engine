package edu.upenn.cis.cis455.webserver;

import org.apache.log4j.Logger;

class HttpServer {
	static int portNumber;
	static String rootDirectory;
	static String webXml;
	static Logger logger = Logger.getLogger(HttpServer.class);
	static boolean isStopped = false; // boolean for stopping the server

	public static void main(String args[]) {

		// Reading command line arguments and handling errors.
		if (args.length == 0) {
			System.out.println("Name : Alifia Haidry");
			System.out.println("SEAS Login Name : ahaidry");
			System.exit(0);
		}

		else if (args.length == 3) {
			try {
				portNumber = Integer.parseInt(args[0]);
			} catch (NumberFormatException e) {
				logger.error(e.getMessage());
				System.out.println("Invalid Port Number!");
				System.exit(0);
			}

			rootDirectory = args[1];
			webXml = args[2];

		}

		else {
			logger.error("Wrong number of Arguments!");
			System.out.println("Wrong number of Arguments!");
			System.exit(0);
		}

		if (portNumber > 65535) {
			logger.error("Invalid Port Number!");
			System.out.println("Invalid Port Number!");
			System.exit(0);
		}

		// Starting ThreadPoolServer class
		try {
			new ThreadPooledServer(portNumber);
		} catch (Exception e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
	}

}
