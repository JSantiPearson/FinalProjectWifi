//reader class

package wifi;

import java.io.PrintWriter;
import java.util.concurrent.ArrayBlockingQueue;

import rf.RF;

public class reader implements Runnable {
	
	private RF rf;
	LinkLayer link;
	ArrayBlockingQueue<Packet> input;
	ArrayBlockingQueue<Packet> acker;
	private short ourMAC;
	private Packet ack;
	private byte[] data = new byte[0];
	@SuppressWarnings("static-access")
	private int sifs = rf.aSIFSTime;
	private static int debug;
	private PrintWriter writer;
	
	public reader(RF theRF, ArrayBlockingQueue<Packet> input, ArrayBlockingQueue<Packet> acker, short ourMAC, int debug, PrintWriter writer) {
		this.rf = theRF;
		this.input = input;
		this.acker = acker;
		this.ourMAC = ourMAC;
		this.debug = debug;
		this.writer = writer;
	}
	 
	 private void unpackIt(Packet packet) {
		 long time = rf.clock();
		 if (packet.getDestAddress() == ourMAC) {
			 try {
				 if (packet.getType() == 1) {
						this.acker.put(packet);
				 }
				 else {
					 System.out.println("giving data up");
					 this.input.put(packet);
				 }
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		 }
		 
		 if(packet.getDestAddress() == -1 && packet.getSourceAddress() != ourMAC) {
			 try {
				this.input.put(packet);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		 }
		 
		 if (packet.getDestAddress() == ourMAC && packet.getType() == 0) {	
				ack = new Packet(1, 0, packet.getSeqNum(), ourMAC, packet.getSourceAddress(), data);
				System.out.println(ack);
				byte[] ackp = ack.packet;
				waitSifs();
				rf.transmit(ackp);
				long diff = rf.clock() - time;
				System.out.println(diff);
		}
	}

	@Override
	public void run() {
		System.out.println("Reader is alive and well");
		while(true) {
			byte[] packetBytes = rf.receive();
			Packet packet = new Packet(packetBytes);
			this.unpackIt(packet);
		}		
	}
	
	private void waitSifs() {
		try {
			Thread.sleep(sifs);
		} catch (InterruptedException e1) {			//wait ifs either sifs or difs
			e1.printStackTrace();
		}
		roundTo50(rf.clock(), rf);
	}
	
	private static void roundTo50(long time, RF rf) {
		long offset = time % 50;
		long off = Math.abs(50 - offset);
		//long newTime = time + off;
		
		 try {
			Thread.sleep(off);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public synchronized static void setDebug(int debugger) {
		debug = debugger;
	}
}