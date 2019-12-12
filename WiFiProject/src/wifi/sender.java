//sender class

package wifi;
import java.io.PrintWriter;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import rf.RF;

public class sender implements Runnable {
	
	private RF rf;
	ArrayBlockingQueue<Packet> output;
	ArrayBlockingQueue<Packet> acker;
	ArrayBlockingQueue<Packet> limit;
	private Packet packet;
	Random rand = new Random();
	private byte[] curpack;
	private boolean gotAck = false;
	private int state = 0;
	private Packet theAck;
	private static boolean max;
	private static int debug;
	private static PrintWriter writer;
	
	public sender(RF theRF, ArrayBlockingQueue<Packet> output, ArrayBlockingQueue<Packet> acker, ArrayBlockingQueue<Packet> limiter, boolean maxCollisionWindow, int debug, PrintWriter writer) {
		rf = theRF;
		this.output = output;
		this.acker = acker;
		this.limit = limiter;
		this.max = maxCollisionWindow;
		this.debug = debug;
		this.writer = writer;
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
	
	private long timeout = 1120 + sifs + slotTime;					//computed using the average time it takes to send an ack plus 
	
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
			
			long time = rf.clock();
			
			if (packet.getDestAddress() != -1) {
				waitForAck();
			}
			
			long diff = rf.clock() - time;
			
			limit.remove();                //if packet was acked remove from limiter
			
	    	try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	    	try {
	    		
	    		if(debug == 1) {
	   			    writer.println("Moving to AWAIT_PACKET after broadcasting DATA");   
	   		    }
	    		
				packet = output.take();
				curpack = packet.packet;
				gotAck = false;
				state = 0;
				System.out.println("next packet");
				numRetrys = 0;

			} catch (InterruptedException e) {			
				e.printStackTrace();
			}
	     }
			
	}
	
	private void waitIfs() {
		if(debug == 1) {	  
			if (state == 0) {
				writer.println("Moving to IDLE_DIFS_WAIT with pending DATA");
			}
			else {
				writer.println("Moving to BUSY_DIFS_WAIT after ACK timeout.  (slotCount = " + slot + ")");
			}
		}
		
		if(debug == 1 && state == 1) {
			 writer.println("Waiting for DIFS to elapse after current Tx...");   
		 }

		try {
			Thread.sleep(ifs);
		} catch (InterruptedException e1) {			//wait difs
			e1.printStackTrace();
		}
		roundTo50(rf.clock(), rf);					//round up to nearest 50 ms
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
		
		if(debug == 1 && numRetrys <= 1) {
			writer.println("Starting collision window at [0..." + backoff + "]");   
		}
		
		if(max) {
			slot = backoff;												//if command is in 2 set slot to max value
		}
		else {
			slot = rand.nextInt(backoff + 1);							//set slot to some random number
		}
		
		if(rf.inUse()) {  					  //if channel is busy change state to one
			 state = 1;
		 }
		 
		 waitWhileBusy();						//wait while busy

		 waitIfs();
		 
		 if(rf.inUse()) {  					  //if channel is busy change state to one
			 state = 1;
		 }		 

		 waitWhileBusy();	
		 
		 if(state == 1) {
			 if(debug == 1) {
				 writer.println("DIFS wait is over, starting slot countdown (" + slot + ")");   
			 }
			 backoff();
		 }		 
		 
		 if(debug == 1 && state == 0) {
			 writer.println("Transmitting DATA after simple DIFS wait at " + rf.clock());   
		 }
		 if(debug == 1 && state == 1) {
			 writer.println("Transmitting DATA after DIFS+SLOTs wait at " + rf.clock());   
		 }		 
		 
		 rf.transmit(curpack); 				//send current packet
	
	}
	
	private void waitForAck() {
		
		if(debug == 1) {
			 writer.println("Moving to AWAIT_ACK after sending DATA");   
		 }
		
		 numRetrys = 0;
		 while(!gotAck) {							 //wait for ack with the correct sequence number
			 try {
				theAck = this.acker.poll(timeout, TimeUnit.MILLISECONDS);         //start timer using poll to specify timeout
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if(theAck == null) {							//if ack equals null there was a timeout and we should retransmit the packet and increase the contention window
				
				if(debug == 1) {
					 writer.println("Ack timer expired at " + rf.clock());   
				}
				
				packet.setReTry(1);
				System.out.println("resending");
				System.out.println(packet);
				numRetrys++;
				state = 1;
				
				if(numRetrys != 1) {
					
					if(backoff * 2 + 1 >= maxBackoff) {			//increase the collision window
						backoff = maxBackoff;
					}
					else {					
					    backoff = (backoff * 2) + 1;
					}
					
					if(debug == 1) {
						 writer.println("Doubled collision window -- is now [0..." + backoff + "]");   
					}
				}

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
			 
			roundTo50(rf.clock(), rf);
			slot--;
			
			if (rf.inUse()) {
				waitWhileBusy();
				waitIfs();	
			}			
		 }
	}
	
	private static void roundTo50(long time, RF rf) {				//code for rounding to nearest 50 ms
		long offset = time % 50;
		long off = Math.abs(50 - offset);
		//long newTime = time + off;
		
		 try {
			Thread.sleep(off);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(debug == 1) {
			 writer.println("Idle waited until " + rf.clock());   
		}
	}
	
	public synchronized static void setCollisionWindow(int maxCol) {
		if (maxCol != 0) {
			max = true;
		}else {
			max = false;
		}
	}
	
	public synchronized static void setDebug(int debugger) {
		debug = debugger;
	}
}
