//reader class

package wifi;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;

import rf.RF;

public class reader implements Runnable {
	
	private RF rf;
	ArrayBlockingQueue<Packet> input;
	private Transmission test;
	private byte[] in;
	
	public reader(RF theRF, ArrayBlockingQueue inputx) {
		rf = theRF;
		input = inputx;
	}

	@Override
	public void run() {
		System.out.println("Reader is alive and well");
		while(true) {
			int type = 0;
			short seq = 0;
			short destination = 0;
			short src = 0;
			byte[] data = null;
			
			in = rf.receive();
			
			int shifter = 0;
			int temp = 0;
			int temp2 = 0;
			while(true) {
				in = rf.receive();
				//test = in;
				//input
				//System.out.println("Recieved: " + Arrays.toString(in));
				/*for(int i = 6; i >= 0; i--) {
					data = in[i];
				}
				for(int j = 9; j >= 2; j--) {
					temp2 = j - 2;
					temp = in[j];
					if(in[j] <= 0) {
						temp = temp + 256;	
					}
					shifter = 8 * temp2;
					temp = temp << shifter;
				//	incomingMac += temp;
				}*/
			
			Packet pack = new Packet(0, seq, src, destination, data);
			input.add(pack); 
			}
		}
		
	}

}