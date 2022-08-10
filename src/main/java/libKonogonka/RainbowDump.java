/*
    Copyright 2019-2020 Dmitry Isaenko

    This file is part of Konogonka.

    Konogonka is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Konogonka is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Konogonka.  If not, see <https://www.gnu.org/licenses/>.
*/
package libKonogonka;

import java.nio.charset.StandardCharsets;

/**
 * Debug tool like hexdump <3
 */
public class RainbowDump {
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BLACK = "\u001B[30m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_PURPLE = "\u001B[35m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_WHITE = "\u001B[37m";


    public static void hexDumpUTF8(byte[] byteArray){
        if (byteArray == null || byteArray.length == 0)
            return;
        System.out.print(ANSI_BLUE);
        for (int i=0; i < byteArray.length; i++)
            System.out.print(String.format("%02d-", i%100));
        System.out.println(">"+ANSI_RED+byteArray.length+ANSI_RESET);
        for (byte b: byteArray)
            System.out.print(String.format("%02x ", b));
        System.out.println();
        System.out.print(new String(byteArray, StandardCharsets.UTF_8)+"\n");
    }

    public static void octDumpInt(int value){
        System.out.println(String.format("%32s", Integer.toBinaryString( value )).replace(' ', '0')+" | "+value);
    }

    public static void octDumpLong(long value){
        System.out.println(String.format("%64s", Long.toBinaryString( value )).replace(' ', '0')+" | "+value);
    }

    public static String formatDecHexString(long value){
        return String.format("%-20d 0x%x", value, value);
    }
}
