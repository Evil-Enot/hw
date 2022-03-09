package client.protocol;

import java.util.ArrayList;
import java.util.Arrays;

public class DHCPMessage {
    public static final byte[] COOKIE = Tools.toBytes(new int[]{99, 130, 83, 99});
    private static final int MINLENGTH = 240;

    // Types of DHTPMessage
    public final static byte DHCPDISCOVER = 1;
    public final static byte DHCPOFFER = 2;
    public final static byte DHCPREQUEST = 3;
    public final static byte DHCPDECLINE = 4;
    public final static byte DHCPACK = 5;
    public final static byte DHCPNAK = 6;
    public final static byte DHCPRELEASE = 7;
    public final static byte DHCPINFORM = 8;
    public final static byte LEASEQUERY = 13;

    public byte op;
    public byte hType;
    public byte hLen;
    public byte hOps;
    public byte[] xId = new byte[4];
    public byte[] secs = new byte[2];
    public byte[] flags = new byte[2];
    public byte[] ciAddr = new byte[4];
    public byte[] yiAddr = new byte[4];
    public byte[] siAddr = new byte[4];
    public byte[] giAddr = new byte[4];
    public byte[] chAddr = new byte[16];
    public byte[] sName = new byte[64];
    public byte[] file = new byte[128];
    public byte[] magicCookie = new byte[4];
    private ArrayList<Options> options = new ArrayList<>();

    public DHCPMessage() { }

    public DHCPMessage(byte[] buffer) {
        op = buffer[0];
        hType = buffer[1];
        hLen = buffer[2];
        hOps = buffer[3];

        int count = 4;

        System.arraycopy(buffer, count, xId, 0, xId.length);
        count += xId.length;

        System.arraycopy(buffer, count, secs, 0, secs.length);
        count += secs.length;

        System.arraycopy(buffer, count, flags, 0, flags.length);
        count += flags.length;

        System.arraycopy(buffer, count, ciAddr, 0, ciAddr.length);
        count += ciAddr.length;

        System.arraycopy(buffer, count, yiAddr, 0, yiAddr.length);
        count += yiAddr.length;

        System.arraycopy(buffer, count, siAddr, 0, siAddr.length);
        count += siAddr.length;

        System.arraycopy(buffer, count, giAddr, 0, giAddr.length);
        count += giAddr.length;

        System.arraycopy(buffer, count, chAddr, 0, chAddr.length);
        count += chAddr.length;

        System.arraycopy(buffer, count, sName, 0, sName.length);
        count += sName.length;

        System.arraycopy(buffer, count, file, 0, file.length);
        count += file.length;

        System.arraycopy(buffer, count, magicCookie, 0, magicCookie.length);
        count += magicCookie.length;

        if (count < buffer.length - 1) {
            int k = buffer.length - count;
            byte[] bufferedOptions = new byte[k];
            System.arraycopy(buffer, count, bufferedOptions, 0, k);
            createOptions(bufferedOptions);
        }
    }

    public void addOption(byte code, byte length, byte[] data) {
        Options newOptions = new Options(code, length, data);
        options.add(newOptions);
    }

    private void createOptions(byte[] buffer) {
        if (buffer[0] == (byte) 255) {
            byte opCode = (byte) 255;
            byte length = (byte) 0;
            byte[] data = {0};
            addOption(opCode, length, data);
            return;
        }
        byte option = buffer[0];
        byte length = buffer[1];
        int k = length + 2;
        byte[] data = new byte[length];
        System.arraycopy(buffer, 2, data, 0, length);
        addOption(option, length, data);
        int l = buffer.length - k;
        byte[] toPass = new byte[l];
        System.arraycopy(buffer, k, toPass, 0, l);
        createOptions(toPass);
    }

    public byte[] getMessage() {
        byte[] resultOptions = new byte[getLength() + 1];

        resultOptions[0] = op;
        resultOptions[1] = hType;
        resultOptions[2] = hLen;
        resultOptions[3] = hOps;

        int count = 4;

        System.arraycopy(xId, 0, resultOptions, count, xId.length);
        count += xId.length;

        System.arraycopy(secs, 0, resultOptions, count, secs.length);
        count += secs.length;

        System.arraycopy(flags, 0, resultOptions, count, flags.length);
        count += flags.length;

        System.arraycopy(ciAddr, 0, resultOptions, count, ciAddr.length);
        count += ciAddr.length;

        System.arraycopy(yiAddr, 0, resultOptions, count, yiAddr.length);
        count += yiAddr.length;

        System.arraycopy(siAddr, 0, resultOptions, count, siAddr.length);
        count += siAddr.length;

        System.arraycopy(giAddr, 0, resultOptions, count, giAddr.length);
        count += giAddr.length;

        System.arraycopy(chAddr, 0, resultOptions, count, chAddr.length);
        count += chAddr.length;

        System.arraycopy(sName, 0, resultOptions, count, sName.length);
        count += sName.length;

        System.arraycopy(file, 0, resultOptions, count, file.length);
        count += file.length;

        System.arraycopy(magicCookie, 0, resultOptions, count, magicCookie.length);
        count += magicCookie.length;

        for (Options option : options) {
            System.arraycopy(option.getBytes(), 0, resultOptions, count, option.getTotalLength());
            count += option.getTotalLength();
        }

        return resultOptions;
    }

    public int getLength() {
        return MINLENGTH + getOptionsLength();
    }

    private int getOptionsLength() {
        int length = 0;
        for (Options option : options) {
            length += option.getTotalLength();
        }
        return length;
    }

    public void clearOptions() {
        options.clear();
    }

    public byte getType() {
        for (Options options : options)
            if (options.getCode() == 53 && options.getLength() == 1) return options.getData()[0];
        return 0;
    }

    public String toString() {
        StringBuilder toStringOptions = new StringBuilder("\n");

        for (Options opt : options)
            toStringOptions.append(opt.toString()).append("\n");

        return "<===============================================================>"
                + "\nop: " + Tools.unsignedByte(op)
                + "\nhType: " + Tools.unsignedByte(hType)
                + "\nhLen: " + Tools.unsignedByte(hLen)
                + "\nhOps: " + Tools.unsignedByte(hOps)
                + "\nxId:  " + Arrays.toString(Tools.unsignedBytes(xId))
                + "\nSecs: " + Arrays.toString(Tools.unsignedBytes(secs))
                + "\nFlags: " + Arrays.toString(Tools.unsignedBytes(flags))
                + "\nciAddr: " + Arrays.toString(Tools.unsignedBytes(ciAddr))
                + "\nyiAddr: " + Arrays.toString(Tools.unsignedBytes(yiAddr))
                + "\nsiAddr: " + Arrays.toString(Tools.unsignedBytes(siAddr))
                + "\ngiAddr: " + Arrays.toString(Tools.unsignedBytes(giAddr))
                + "\nchAddr: " + Arrays.toString(Tools.unsignedBytes(chAddr))
                + "\nsName: " + Arrays.toString(Tools.unsignedBytes(sName))
                + "\nfile :" + Arrays.toString(Tools.unsignedBytes(file))
                + "\nmagicCookie: " + Arrays.toString(Tools.unsignedBytes(magicCookie))
                + toStringOptions
                + "<===============================================================>\n";
    }
}
