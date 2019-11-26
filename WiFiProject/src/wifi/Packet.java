package wifi;

import java.util.zip.CRC32;

public class Packet {

	public static int DATA = 0;
    public static int ACK = 1;
    public static int BEACON = 2;
    public static int CTS = 4;
    public static int RTS = 5;
    public byte[] packet;
    
    public Packet(byte[] packetBytes) {
        final int dataSize = packetBytes.length - 10;
        if (dataSize > 2038) {
            throw new IllegalArgumentException("Too much data in the packet!");
        }
        if (dataSize < 0) {
            throw new IllegalArgumentException("Negative data in your packet, that's impossible.");
        }
        this.packet = new byte[packetBytes.length];
        for (int i = 0; i < packetBytes.length; ++i) {
            this.packet[i] = packetBytes[i];
        }
    }
    
    public Packet(int type, int reTry, short seq, short src, short dest, byte[] data) {
    	if (data.length > 2038) {
    		throw new IllegalArgumentException("Packet size too large.");
    	}
    	this.packet = new byte[10 + data.length];
    	this.setType(type);
        this.setReTry(reTry);
        this.setSeqNum(seq);
        this.setDestAddress(dest);
        this.setSourceAddress(src);
        this.setData(data);
        this.setChecksum();
    }
    
    public Packet(int type, short seq, short src, short dest, byte[] data) {
    	 if (data.length > 2038) {
             throw new IllegalArgumentException("Packet size too large.");
         }
    	this.packet = new byte[10 + data.length];
    	//1 := its a retransmission and 0 := not a retrans.
        this.setType(type);
        // this.setReTry(0); // Sets retry bit to 0 for default case.
        this.setSeqNum(seq);
        this.setDestAddress(dest);
        this.setSourceAddress(src);
        this.setData(data);
        this.setChecksum();
    }
    
    public static void main(String args[]) {
    	String str = "Hello world!";
    	byte[] data = str.getBytes();
    	short seq = 0;
    	short src = 564;
    	short dest = 312;
    	Packet packet = new Packet(DATA, seq, src, dest, data);
    	Packet packet2 = new Packet(packet.packet);
    	System.out.println(packet2);
    }

    public void setReTry(int retry) {
    	if (retry == 1) {
    		this.packet[0] = (byte) (this.packet[0] << 1 | 1);
    		return;
    	}
    	else if (retry == 0) {
    		this.packet[0] = (byte) (this.packet[0] << 1);
    		
    	}
    	throw new IllegalArgumentException("Not a valid retry bit.");
    }
    
	public void setType(int type) {
        if (type >= DATA && type <= RTS && type != 3) {
            byte[] packet = this.packet;
            packet[0] &= 0x1F;
            packet[0] |= (byte)(type << 5);
            return;
        }
        throw new IllegalArgumentException("Unknown message type");
    }
	
	public int getType() {
        return (this.packet[0] & 0xE0) >>> 5;
    }
    
    public void setSeqNum(short seqNum) {
        if (seqNum != (seqNum & 0xFFF)) {
            throw new IllegalArgumentException("Sequence number is not 12 bits.");
        }
        this.packet[1] = (byte)(seqNum & 0xFF);
    }
    
    public short getSeqNum() {
        return this.translatorByteShort((byte)(this.packet[0] & 0xF), this.packet[1]);
    }
    
    public void setDestAddress(short destAddress) {
        this.packet[2] = (byte)(destAddress >>> 8 & 0xFF);
        this.packet[3] = (byte)(destAddress & 0xFF);
    }
    
    public short getDestAddress() {
        return this.translatorByteShort(this.packet[2], this.packet[3]);
    }
    
    public void setSourceAddress(short sourceAddress) {
        this.packet[4] = (byte)(sourceAddress >>> 8 & 0xFF);
        this.packet[5] = (byte)(sourceAddress & 0xFF);
    }
    
    public short getSourceAddress() {
        return this.translatorByteShort(this.packet[4], this.packet[5]);
    }
    
    public void setData(byte[] data) {
        if (data.length > 2038) {
            throw new IllegalArgumentException("Too much data to put in the packet.");
            }
        for (int i = 0; i < data.length; ++i) {
            this.packet[6 + i] = data[i];
        }
    }
    
    public byte[] getData() {
        final byte[] data = new byte[this.packet.length - 10];
        for (int i = 0; i < data.length; ++i) {
            data[i] = this.packet[6 + i];
        }
        return data;
    }
    
    @Override
    public String toString() {
    	this.setChecksum();
    	String packetString = "";
        switch (this.getType()) {
            case 0: {
               packetString += "Type: DATA";
                break;
            }
            case 1: {
               packetString += "Type: ACK";
                break;
            }
            case 2: {
               packetString += "Type: BEACON";
                break;
            }
            case 4: {
               packetString += "Type: CTS";
                break;
            }
            case 5: {
               packetString += "Type: RTS";
                break;
            }
            default: {
               packetString += "Not a valid type.";
                break;
            }
        }
       packetString += "\nSequence Number:" + this.getSeqNum();
//        if (this.isRetry()) {
//           packetString += "\nIs a retry.";
//        }
       packetString += "\nSource Address: " + this.getSourceAddress();
       packetString += "\nDestination Address: " + this.getDestAddress();
       packetString += "\nData: [" + new String(this.getData()) + "]";
       packetString += "\nChecksum: (" + this.getChecksum() + ")";
        return "____________\n" + packetString + "\n____________";
    }
	
    public void setChecksum() {
    	CRC32 checksum = new CRC32();
        checksum.update(this.packet, 0, this.packet.length - 4);
    	int checkSumValue = (int) checksum.getValue();
        int checksumIndex = this.packet.length - 4;
        this.packet[checksumIndex + 0] = (byte)(checkSumValue >>> 24 & 0xFF);
        this.packet[checksumIndex + 1] = (byte)(checkSumValue >>> 16 & 0xFF);
        this.packet[checksumIndex + 2] = (byte)(checkSumValue >>> 8 & 0xFF);
        this.packet[checksumIndex + 3] = (byte)(checkSumValue & 0xFF);
    }
    
    public int getChecksum() {
        int checksumIndex = this.packet.length - 4;
        int incomingCRC = this.packet[checksumIndex + 3] & 0xFF;
        incomingCRC |= (this.packet[checksumIndex + 2] << 8 & 0xFF00);
        incomingCRC |= (this.packet[checksumIndex + 1] << 16 & 0xFF0000);
        return incomingCRC;
    }
    
    private short translatorByteShort(byte half1, byte half2) {
        int tempShort = half1 & 0xFF;
        tempShort = (tempShort << 8 | (half2 & 0xFF));
        return (short)tempShort;
    }
    
}
