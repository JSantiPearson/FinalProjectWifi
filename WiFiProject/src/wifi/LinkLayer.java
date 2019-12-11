package wifi;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;

import rf.RF;

/**
 * Use this layer as a starting point for your project code.  See {@link Dot11Interface} for more
 * details on these routines.
 * @author richards
 */
public class LinkLayer implements Dot11Interface 
{
	private RF theRF;           // You'll need one of these eventually
	private short ourMAC;       // Our MAC address
	private PrintWriter output; // The output stream we'll write to
	
	public boolean maxCollisionWindow;
	private int debug;
	
	public reader read;
	HashMap<Short, Short> destSeqNums;

    // create object of ArrayBlockingQueue 
    ArrayBlockingQueue<Packet> packetHolder = new ArrayBlockingQueue<Packet>(1000);   
    ArrayBlockingQueue<Packet> packetHolderIn = new ArrayBlockingQueue<Packet>(1000);
    ArrayBlockingQueue<Packet> ackHolder = new ArrayBlockingQueue<Packet>(1000);
    ArrayBlockingQueue<Packet> limiter = new ArrayBlockingQueue<Packet>(1000);
    
	/**
	 * Constructor takes a MAC address and the PrintWriter to which our output will
	 * be written.
	 * @param ourMAC  MAC address
	 * @param output  Output stream associated with GUI
	 */
	public LinkLayer(short ourMAC, PrintWriter output) {
		this.ourMAC = ourMAC;
		this.output = output;
		output.println("LinkLayer: Constructor ran.");
		this.destSeqNums = new HashMap<Short, Short>();
		theRF = new RF(null, null);
		sender send = new sender(theRF, packetHolder, ackHolder, limiter);
		(new Thread(send)).start();
		read = new reader(theRF, packetHolderIn, packetHolder, ackHolder, ourMAC);
		(new Thread(read)).start();
	}
	
	
	  public short calcNextSeqNum(short i) {
	        short seqNum;
	        if (this.destSeqNums.containsKey(i)) {
	            seqNum = this.destSeqNums.get(i);
	        }
	        else {
	        	seqNum = 0;
	        }
	        this.destSeqNums.put(i, (short)(seqNum + 1));
	        return seqNum;
	    }

	/**
	 * Send method takes a destination, a buffer (array) of data, and the number
	 * of bytes to send.  See docs for full description.
	 */
	public int send(short dest, byte[] data, int len) {
		if (limiter.size() < 4) {
			output.println("LinkLayer: Sending "+len+" bytes to "+dest); 
			Packet pack = new Packet(0, 0, calcNextSeqNum(dest), ourMAC, dest, data);
			
			// Packet beacon = new Packet(1, , ourMac, dest, data);
			
			packetHolder.add(pack);
			limiter.add(pack);
			return len;
		}
		else {
			return 0;
		}
	}

	/**
	 * Recv method blocks until data arrives, then writes it an address info into
	 * the Transmission object.  See docs for full description.
	 */
	public int recv(Transmission t) {
		output.println("LinkLayer: Pretending to block on recv()");
		while(true) {
			 try {
				 Packet packet = this.read.input.take();
		         byte[] data = packet.getData();
		         t.setSourceAddr(packet.getSourceAddress());
		         t.setDestAddr(packet.getDestAddress());
		         t.setBuf(data);
		         System.out.println("Test packet :" + packet);
		         return data.length;
			 } catch (InterruptedException e) {
				 e.printStackTrace();
			 }  
	      }
	 } // <--- This is a REALLY bad way to wait.  Sleep a little each time through.
		// return 0;

	/**
	 * Returns a current status code.  See docs for full description.
	 */
	public int status() {
		output.println("LinkLayer: Faking a status() return value of 0");
		return 0;
	}

	/**
	 * Passes command info to your link layer.  See docs for full description.
	 */
	public int command(int cmnd, int value) {
		switch (cmnd) {
			case 0: {
				System.out.println("------------ Commands and Settings -------------");
				System.out.println("Cmd #0: Display command options and current settings");
				System.out.println("Cmd #1: Set debug level.  Currently at " + this.debug + "\n		  Use -1 for full debug output, 0 for no output");
				String collision;
				if (this.maxCollisionWindow) {
					collision = "max";
				}
				else {
					collision = "random";
				}
				System.out.println("Cmd #2: Set slot selection method.  Currently " + collision + "\n		  Use 0 for random slot selection, any other value to use maxCW");
				System.out.println("Cmd #3: Set beacon interval.  Currently at " + "*beacon interval goes here*" + " seconds" + "\n		  Value specifies seconds between the start of beacons; -1 disables");
				System.out.println("------------------------------------------------");
				return 0;
			}
			case 1: {
				int prevDebug = this.debug;
				this.debug = value;
				System.out.println("Setting debug to " + value);
				return prevDebug;
			}
			case 2: {
				if (value != 0) {
					this.maxCollisionWindow = true;
				}
				else {
					this.maxCollisionWindow = false;
				}
				if (this.maxCollisionWindow) {
					System.out.println("Using the maximum Collision Window value each time");
					return 0;
				}
				System.out.println("Using a random Collision Window value each time");
				return 0;
			}
			case 3: {
				if (value < 0) {
					System.out.println("Beacon frames will never be sent");
					//disable beacon frames here
					return 0;
				}
				System.out.println("Beacon frames will be sent every " + value + " seconds");
				//set beacon frames here
				return 0;
			}
		}
		System.out.println("Unknown command: " + cmnd);
		return 0;
	}
}
