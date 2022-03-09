package client;

import client.protocol.DHCPMessage;
import client.protocol.Tools;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;

public class DHCPClient extends Thread {
    private final static Integer clientPort = 67;
    private final static int serverPort = 68;
    private final static byte[] clientHost = new byte[]{0, 0, 0, 0};
    private final static byte[] serverHost = new byte[]{(byte) 255, (byte) 255, (byte) 255, (byte) 255};
    private DatagramSocket clientSocket;

    private final byte[] mac = {(byte) 0xFF, (byte) 0xF2, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

    public DHCPClient() {
        try {
            clientSocket = new DatagramSocket(clientPort, InetAddress.getByAddress(clientHost));
        } catch (SocketException e) {
            System.out.println("Error while creating clientSocket: " + e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        byte[] buffer = new byte[576];
        int length = buffer.length;
        DatagramPacket receivePacketOffer = new DatagramPacket(buffer, length);
        DatagramPacket receivePacketAck = new DatagramPacket(buffer, length);

        DHCPDiscover();

        try {
            clientSocket.setSoTimeout(3000);
            clientSocket.receive(receivePacketOffer);
            System.out.println("Receive offer response from server");
        } catch (IOException e) {
            System.out.println("Error while receive offer response: " + e.getMessage());
            System.exit(-1);
        }

        DHCPRequest(receivePacketOffer);

        try {
            clientSocket.receive(receivePacketAck);
            System.out.println("Receive ack response from server");
            System.out.println();
        } catch (IOException e) {
            System.out.println("Error while receive ack response: " + e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        }

        DHCPMessage acknowledge = new DHCPMessage(receivePacketAck.getData());

        switch (acknowledge.getType()) {
            case DHCPMessage.DHCPACK:
                System.out.print("Allocated ip: ");
                for (int i = 0; i < acknowledge.yiAddr.length; i++) {
                    byte b = acknowledge.yiAddr[i];
                    if (i != acknowledge.yiAddr.length - 1) {
                        System.out.print((b & 0xFF) + ".");
                    } else {
                        System.out.print((b & 0xFF));
                    }
                }
                System.out.println();
                break;
            case DHCPMessage.DHCPNAK:
                System.out.println("No IP allocated to this client");
                break;
        }
        clientSocket.close();
    }

    private void DHCPDiscover() {
        System.out.println("\nStart discover phase");
        DHCPMessage message = new DHCPMessage();
        message.op = 1;
        message.hType = 1;
        message.hLen = 6;
        message.hOps = 0;

        ByteBuffer hash = ByteBuffer.allocate(4);
        hash.putInt(message.hashCode());
        message.xId = hash.array();

        message.secs = new byte[]{0, 0};
        message.flags = new byte[]{0, 0};
        message.ciAddr = new byte[]{0, 0, 0, 0};
        message.yiAddr = new byte[]{0, 0, 0, 0};
        message.siAddr = new byte[]{0, 0, 0, 0};
        message.giAddr = new byte[]{0, 0, 0, 0};
        message.chAddr = mac;
        message.sName = new byte[64];
        message.file = new byte[128];
        message.magicCookie = DHCPMessage.COOKIE;
        message.addOption((byte) 53, (byte) 1, new byte[]{DHCPMessage.DHCPDISCOVER});
        message.addOption((byte) 51, (byte) 4, Tools.toByteArray(10));
        message.addOption((byte) 255, (byte) 0, new byte[]{0});

        try {
            DatagramPacket discoverPacket = new DatagramPacket(message.getMessage(), message.getLength(), InetAddress.getByAddress(serverHost), serverPort);
            clientSocket.send(discoverPacket);
            System.out.println("DHCPDiscover sent");
            System.out.println();
        } catch (IOException e) {
            System.out.println("Error while sending discover packet: " + e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private void DHCPRequest(DatagramPacket offer) {
        System.out.println("\nStart request phase");

        DHCPMessage message = new DHCPMessage(offer.getData());
        message.clearOptions();

        message.op = 1;

        message.addOption((byte) 53, (byte) 1, new byte[]{DHCPMessage.DHCPREQUEST});
        message.addOption((byte) 50, (byte) 4, message.yiAddr);
        message.addOption((byte) 54, (byte) 4, message.siAddr);
        message.addOption((byte) 51, (byte) 4, Tools.toByteArray(10));
        message.addOption((byte) 255, (byte) 0, new byte[]{0});

        try {
            int portServer = offer.getPort();
            DatagramPacket discoverPacket = new DatagramPacket(message.getMessage(), message.getLength(), InetAddress.getByAddress(message.siAddr), portServer);
            clientSocket.send(discoverPacket);
            System.out.println("DHCPRequest sent");
            System.out.println();
        } catch (IOException e) {
            System.out.println("Error while sending request packet: " + e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
