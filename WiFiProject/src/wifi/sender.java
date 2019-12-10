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
	private Packet theAck;
	
	public sender(RF theRF, ArrayBlockingQueue<Packet> output, ArrayBlockingQueue<Packet> acker) {
		rf = theRF;
		this.output = output;
		this.acker = acker;
	}

	@SuppressWarnings("static-access")
	private int sifs = rf.aSIFSTime;
	@SuppressWarnings("static-access")
	private int ifs = sifs + (rf.aSlotTime * 2);
	@SuppressWarnings("static-access")
	private int backoff = rf.aCWmin;
	private int slot = 0;
	@SuppressWarnings("static-access")
	private int slotTime = rf.aSlotTime;
	@SuppressWarnings("static-access")
	private int maxRetrys = rf.dot11RetryLimit;
	private int numRetrys = 0;
	@SuppressWarnings("static-access")
	private int maxBackoff = rf.aCWmax;
	
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
		 
		while(true) {
			transmit(); 

			if (packet.getDestAddress() != -1) {
				waitForAck();
			}
			 
	    	try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	    	 try {
				packet = output.take();
				curpack = packet.packet;
				gotAck = false;
				System.out.println("next packet");
				numRetrys = 0;
			} catch (InterruptedException e) {			
				e.printStackTrace();
			}
	     }
			
	}
	
	private void waitIfs() {
		try {
			Thread.sleep(ifs);
		} catch (InterruptedException e1) {			//wait ifs either sifs or difs
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
		 
		 if(state == 1) {
			 backoff();
		 }		 
		 
		 rf.transmit(curpack); 				//send current packet
		 state = 0;
	}
	
	private void waitForAck() {
		 numRetrys = 0;
		 while(!gotAck) {							 //wait for ack with the correct sequence number
			 try {
				theAck = this.acker.poll(timeout, TimeUnit.MILLISECONDS);         //start timer using poll to specify timeout
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if(theAck == null) {							//if ack equals null there was a timeout and we should retransmit the packet and increase the contention window
				packet.setReTry(1);
				System.out.println("resending");
				System.out.println(packet);
				numRetrys++;
				if(backoff * 2 + 1 >= maxBackoff) {
					backoff = maxBackoff;
				}
				else {					
				    backoff = (backoff * 2) + 1;
				}
				System.out.println("new boff" + backoff);
				transmit();
			}
			if(theAck != null) {
				if(theAck.getSeqNum() == packet.getSeqNum()) {    //check if ack has correct sequence number
					gotAck = true;
				}
			}
			else if(numRetrys == maxRetrys) { 						 //checks if retry limit is met and moves to next packet if it is
				gotAck = true;
				System.out.println("aborted packet");
			}
		 } 
	}
	
	private void backoff() {
		slot = rand.nextInt(backoff + 1);							//set slot to some random number
		 
		System.out.println(slot);
		 while (state == 1 && slot != 0) {							//if the channel was not idle wait additional time
			 System.out.println("exponential backoff");
			 System.out.println(state);
			 System.out.println("slot:" + slot);
			 try {
				Thread.sleep(slotTime);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			slot--;
			
			if (rf.inUse()) {
				waitWhileBusy();
				waitIfs();	
			}			
		 }
	}
	
}
