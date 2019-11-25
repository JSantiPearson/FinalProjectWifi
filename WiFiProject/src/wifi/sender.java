//sender class

package wifi;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import rf.RF;

public class sender implements Runnable {
	
	private RF rf;
	ArrayBlockingQueue<Packet> output;
	ArrayBlockingQueue<Packet> acker;
	private Packet packet;
	Random rand = new Random();
	private byte[] curpack;
	private boolean gotAck = false;
	private long timeout = 6000;
	private int state = 0;
	private boolean isAck = false;
	private Packet theAck;
	
	public sender(RF theRF, ArrayBlockingQueue<Packet> output, ArrayBlockingQueue<Packet> acker) {
		rf = theRF;
		this.output = output;
		this.acker = acker;
	}

	private int sifs = rf.aSIFSTime;
	private int difs = sifs + (rf.aSlotTime * 2);
	private int ifs = 0;
	private int backoff = rf.aCWmin;
	private int slot = 0;
	private int slotTime = rf.aSlotTime;
	
	
	@Override
	public void run() {
		System.out.println("Writer is alive and well");
		try {
			packet = output.take();
			System.out.println(packet);
			curpack = packet.packet;
		} catch (InterruptedException e1) {
			
			e1.printStackTrace();
		}
		
		if (packet.getType() == 1) {			 //if of type ack set ifs to to sifs, else set to difs
			ifs = sifs;
			isAck = true;
		}
		else {
			ifs = difs;
		}
			
		int sleeper = rand.nextInt(69) + 1;
		 
		while(true) {
			transmit(); 
			waitForAck();
			 
	    	try {
				Thread.sleep(sleeper);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	    	 try {
				packet = output.take();
				curpack = packet.packet;
			} catch (InterruptedException e) {
				
				e.printStackTrace();
			}
	     }
			
	}
	
	private void waitIfs() {
		try {
			Thread.sleep(ifs);
		} catch (InterruptedException e1) {			//wait ifs either sifs or difs
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	private void waitWhileBusy() {
		while(rf.inUse()) {                  //sleep while channel is busy
			 try {
					Thread.sleep(20);
			 } catch (InterruptedException e) {
				e.printStackTrace();
			 }
		 }
	}
	
	private void transmit() {
		if(rf.inUse()) {  					  //if channel is busy change state to one
			 state = 1;
		 }
		 
		 waitWhileBusy();						
		 
		 waitIfs();
		 
		 if(rf.inUse()) {  					  //if channel is busy change state to one
			 state = 1;
		 }
		 
		 waitWhileBusy();
		 
		 while (state == 1 && slot != 0) {							//if the channel was not idle wait additional time
			 slot = rand.nextInt(backoff);
			 try {
				Thread.sleep(slotTime);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			slot--;
			
			waitWhileBusy();
			
			waitIfs();	
			
		 }
		 
		 rf.transmit(curpack); 				//send current packet and begin wait for an ack
	}
	
	private void waitForAck() {
		 if (!isAck || packet.getDestAddress() != -1) {								 //if not sending ack wait for ack
			 while(!gotAck) {							 //wait for ack with the correct sequence number
				 try {
					theAck = this.acker.poll(timeout, TimeUnit.MILLISECONDS);         //start timer using poll to specify timeout
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if(theAck == null) {							//if ack equals null there was a timeout and we should retransmit the packet and increase the contention window
					//curpack.
					
					transmit();
					backoff = backoff *2;
				}
				if(theAck.getSeqNum() == packet.getSeqNum()) {    //check if ack has correct sequence number
					gotAck = true;
					System.out.println("got");
				}
			 }
		 }
	}
	
}
