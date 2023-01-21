/*
    Copyright 2019-2022 Dmitry Isaenko

    This file is part of libKonogonka.

    libKonogonka is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    libKonogonka is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with libKonogonka.  If not, see <https://www.gnu.org/licenses/>.
*/
package libKonogonka;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Converter {
    private final static Logger log = LogManager.getLogger(Converter.class);

    public static int getLEint(byte[] bytes, int fromOffset){
        if (fromOffset < 0 || fromOffset >= bytes.length)
            log.debug("\tLen =" + bytes.length + "\tFrom =" + fromOffset);
        return ByteBuffer.wrap(bytes, fromOffset, 0x4).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    public static long getLElong(byte[] bytes, int fromOffset){
        return ByteBuffer.wrap(bytes, fromOffset, 0x8).order(ByteOrder.LITTLE_ENDIAN).getLong();
    }
    /**
     * Convert (usually unsigned) int to long. Workaround to store unsigned int
     * @param bytes original array
     * @param fromOffset start position of the 4-bytes value
     * */
    public static long getLElongOfInt(byte[] bytes, int fromOffset){
        final byte[] holder = new byte[8];
        System.arraycopy(bytes, fromOffset, holder, 0, 4);
        return ByteBuffer.wrap(holder).order(ByteOrder.LITTLE_ENDIAN).getLong();
    }

    public static String byteArrToHexStringAsLE(byte[] array){
        return byteArrToHexStringAsLE(array, false);
    }
    public static String byteArrToHexStringAsLE(byte[] array, boolean upperCase){
        if (upperCase)
            return byteArrToHexStringAsLE(array, "%02X");
        return byteArrToHexStringAsLE(array, "%02x");
    }
    private static String byteArrToHexStringAsLE(byte[] array, String format){
        if (array == null)
            return "";
        StringBuilder sb = new StringBuilder();
        for (byte b: array)
            sb.append(String.format(format, b));
        return sb.toString();
    }

    public static String intToBinaryString(int value){
        return String.format("%32s", Integer.toBinaryString( value )).replace(' ', '0');
    }

    public static String byteToBinaryString(byte value){
        String str = String.format("%8s", Integer.toBinaryString( value )).replace(' ', '0');
        int decrease = 0;
        if (str.length() > 8)
            decrease = str.length()-8;
        return str.substring(decrease);
    }

    public static String shortToBinaryString(short value){
        String str = String.format("%16s", Integer.toBinaryString( value )).replace(' ', '0');
        int decrease = 0;
        if (str.length() > 16)
            decrease = str.length()-16;
        return str.substring(decrease);
    }

    public static String longToOctString(long value){
        return String.format("%64s", Long.toBinaryString( value )).replace(' ', '0');
    }

    public static byte[] hexStringToByteArray(String string){
        int len = string.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(string.charAt(i), 16) << 4)
                    + Character.digit(string.charAt(i+1), 16));
        }
        return data;
    }

    public static byte[] flip(byte[] bytes){
        int size = bytes.length;
        byte[] ret = new byte[size];
        for (int i = 0; i < size; i++){
            ret[size-i-1] = bytes[i];
        }
        return ret;
    }
}
