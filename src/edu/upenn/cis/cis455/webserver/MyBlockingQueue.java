package edu.upenn.cis.cis455.webserver;

import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

public class MyBlockingQueue {
	private List<Socket> queue = new LinkedList<Socket>();
	  private int  limit = 10;

	  public MyBlockingQueue(int limit){
	    this.limit = limit;
	  }


	  public synchronized void enqueue(Socket item)
	  throws InterruptedException  {
	    while(this.queue.size() == this.limit) {
	      wait();
	    }
	    if(this.queue.size() == 0) {
	      notifyAll();
	    }
	    this.queue.add(item);
	  }


	  public synchronized Socket dequeue()
	  throws InterruptedException{
	    while(this.queue.size() == 0){
	      wait();
	    }
	    if(this.queue.size() == this.limit){
	      notifyAll();
	    }

	    return this.queue.remove(0);
	  }
}
