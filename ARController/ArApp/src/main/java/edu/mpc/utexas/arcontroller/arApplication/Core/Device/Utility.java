package edu.mpc.utexas.arcontroller.arApplication.Core.Device;

public class Utility {

    public static byte[] macStr2Byte(String addr) {
        String[] macAddressParts = addr.split(":");
        byte[] macAddressBytes = new byte[6];
        for(int i=0; i<6; i++){
            Integer hex = Integer.parseInt(macAddressParts[i], 16);
            macAddressBytes[i] = hex.byteValue();
        }
        return macAddressBytes;
    }
}
