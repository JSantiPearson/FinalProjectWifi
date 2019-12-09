//reader class

package wifi;

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
	
	public reader(RF theRF, ArrayBlockingQueue<Packet> input, ArrayBlockingQueue<Packet> output, ArrayBlockingQueue<Packet> acker, short ourMAC) {
		this.rf = theRF;
		this.input = input;
		this.output = output;
		this.acker = acker;
		this.ourMAC = ourMAC;
	}
	 
	 private void unpackIt(Packet packet) {
		 if (packet.getDestAddress() == -1 || packet.getDestAddress() == ourMAC) {
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
				ack = new Packet(1, packet.getSeqNum(), ourMAC, packet.getSourceAddress(), data);
				byte[] ackp = ack.packet;
				waitSifs();
				System.out.println("waited");
				rf.transmit(ackp);
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
	}
	
}