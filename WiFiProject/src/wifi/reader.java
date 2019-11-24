//reader class

package wifi;

import java.util.concurrent.ArrayBlockingQueue;

import rf.RF;

public class reader implements Runnable {
	
	private RF rf;
	LinkLayer link;
	ArrayBlockingQueue<Packet> input;
	private short ourMAC;
	
	public reader(RF theRF, ArrayBlockingQueue<Packet> input, short ourMAC) {
		this.rf = theRF;
		this.input = input;
		this.ourMAC = ourMAC;
	}
	 
	 private void unpackIt(Packet packet) {
		 if (packet.getDestAddress() == -1 || packet.getDestAddress() == ourMAC) {
			 try {
				 this.input.put(packet);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
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