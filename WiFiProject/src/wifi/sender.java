//sender class

package wifi;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;

import rf.RF;

public class sender implements Runnable {
	
	private RF rf = null;
	ArrayBlockingQueue<String> output;
	private String packet;
	Random rand = new Random();
	
	public sender(RF theRF, ArrayBlockingQueue<String> output) {
		rf = theRF;
		this.output = output;
	}

	@Override
	public void run() {
		System.out.println("Writer is alive and well");
		try {
			packet = output.take();
		} catch (InterruptedException e1) {
			
			e1.printStackTrace();
		}
		int sleeper = rand.nextInt(69) + 1;
		 while(true)
	      {
			 while(rf.inUse()) {
				 try {
						Thread.sleep(20);
				 } catch (InterruptedException e) {
					e.printStackTrace();
				 }
			 }
			// rf.transmit(packet); 
	    	 try {
				Thread.sleep(sleeper);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	    	 try {
				packet = output.take();
			} catch (InterruptedException e) {
				
				e.printStackTrace();
			}
	      }
		
	}

	
	
}
