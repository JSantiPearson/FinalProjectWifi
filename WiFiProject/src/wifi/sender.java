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
		 while(true)
	      {
			 if(rf.inUse()) {  					  //if channel is busy change state to one
				 state = 1;
			 }
			 
			 while(rf.inUse()) {                  //sleep while channel is busy
				 try {
						Thread.sleep(20);
				 } catch (InterruptedException e) {
					e.printStackTrace();
				 }
			 }
			 
			 try {
				Thread.sleep(ifs);
			} catch (InterruptedException e1) {			//wait ifs depending 
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			 
			 if(rf.inUse()) {  					  //if channel is busy change state to one
				 state = 1;
			 }
			 
			 while(rf.inUse()) {                  //sleep while channel is busy
				 try {
						Thread.sleep(20);
				 } catch (InterruptedException e) {
					e.printStackTrace();
				 }
			 }
			 
			 if (state == 1) {							//if the channel was not idle wait additional time
				 
			 }
			 
			 rf.transmit(curpack); 						 //send current packet and set state to 2
			 state = 2;
			 
			 if (!isAck || packet.getDestAddress() != -1) {								 //if not sending ack wait for ack
				 while(!gotAck) {							 //wait for ack with the correct sequence number
					 try {
						theAck = this.acker.poll(timeout, TimeUnit.MILLISECONDS);         //start timer using poll to specify timeout
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if(theAck == null) {							//if ack equals null there was a timeout and we should retransmit the packet and increase the contention window
						rf.transmit(curpack);
					}
					if(theAck.getSeqNum() == packet.getSeqNum()) {    //check if ack has correct sequence number
						gotAck = true;
						System.out.println("got");
					}
				 }
			 }
			 
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
}
