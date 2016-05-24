package edu.upenn.cis.cis455.webserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;

import javax.servlet.http.HttpServlet;

import org.apache.log4j.Logger;
import org.apache.tools.ant.taskdefs.condition.Http;

public class ThreadPooledServer implements Runnable {

	protected int serverPort = 0;
	static final Logger logger = Logger.getLogger(ThreadPooledServer.class);
	protected static volatile ServerSocket serverSocket = null;
	protected Thread runningThread = null;
	protected static ArrayList<Thread> threadPool = new ArrayList<Thread>();
	int threadPoolLimit = 10;
	int maxSocketLimit = 10000;
	private List<Socket> taskQueue = new LinkedList<Socket>();

	public ThreadPooledServer(int port) throws Exception {
		this.serverPort = port;

		try {
			serverSocket = new ServerSocket(this.serverPort);
			Thread thrd = new Thread(this);
			thrd.setPriority(10);
			thrd.start();
		} catch (IOException e) {
			logger.error(e.getMessage());
		}

		Worker worker = new Worker(taskQueue, maxSocketLimit);
		for (int i = 0; i < threadPoolLimit; i++) {
			Thread handler = new Thread(worker);
			threadPool.add(handler);
		}
		int j = 0;
		for (Thread thread : threadPool) {
			thread.setName(j + "");
			thread.start();
			j++;
		}
		SessionTimer timer = new SessionTimer();
		Thread daemon = new Thread(timer);
		daemon.setDaemon(true);
		daemon.start();

	}

	/*
	 * Run method.
	 */
	public void run() {
		while (!HttpServer.isStopped) {
			Socket clientSocket;
			try {
				clientSocket = serverSocket.accept();
				synchronized (taskQueue) {
					while (taskQueue.size() == this.maxSocketLimit) {

						taskQueue.wait();

					}
					taskQueue.add(clientSocket);
					taskQueue.notify();

				}
			} catch (InterruptedException e) {
				logger.error(e.getMessage());
				e.printStackTrace();
			} catch (SocketException e) {
				logger.error(e.getMessage());
			} catch (IOException e) {
				logger.error(e.getMessage());
			}
		}
		for (Thread t : threadPool) {
			if (t.getState() == Thread.State.RUNNABLE)
				try {
					t.join();
				} catch (InterruptedException e) {
					logger.error(e.getMessage());
					e.printStackTrace();
				}
		}

		for (HttpServlet servlet : TestHarness.servlets.values()) {
			servlet.destroy();
		}

		synchronized (taskQueue) {
			taskQueue.notifyAll(); // notify all threads so they can shutdown
		}

	}
}