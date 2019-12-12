//reader class

package wifi;

import java.io.PrintWriter;
import java.util.concurrent.ArrayBlockingQueue;

import rf.RF;

public class reader implements Runnable {
	
	private RF rf;
	LinkLayer link;
	ArrayBlockingQueue<Packet> input;
	ArrayBlockingQueue<Packet> output;
	ArrayBlockingQueue<Packet> acker;
	private short ourMAC;
	private Packet ack;
	private byte[] data = new byte[0];
	@SuppressWarnings("static-access")
	private int sifs = rf.aSIFSTime;
	private int debug;
	private PrintWriter writer;
	
	public reader(RF theRF, ArrayBlockingQueue<Packet> input, ArrayBlockingQueue<Packet> output, ArrayBlockingQueue<Packet> acker, short ourMAC, int debug, PrintWriter writer) {
		this.rf = theRF;
		this.input = input;
		this.output = output;
		this.acker = acker;
		this.ourMAC = ourMAC;
		this.debug = debug;
		this.writer = writer;
	}
	 
	 private void unpackIt(Packet packet) {
		 long time = rf.clock();
		 System.out.println(time);
		 if ((packet.getDestAddress() == -1 && packet.getSourceAddress() != ourMAC) || packet.getDestAddress() == ourMAC) {
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
		 
		 if (packet.getDestAddress() == ourMAC && packet.getType() == 0) {	
				ack = new Packet(1, 0, packet.getSeqNum(), ourMAC, packet.getSourceAddress(), data);
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
		System.out.println(rf.clock());
	}
}