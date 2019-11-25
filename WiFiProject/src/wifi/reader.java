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
					 this.input.put(packet);
				 }
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		 }
		 
		 if (packet.getDestAddress() == ourMAC) {													
				ack = new Packet(1, packet.getSeqNum(), ourMAC, packet.getSourceAddress(), data);
				output.add(ack);
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
}