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
package libKonogonka.Tools.other.System2;

import libKonogonka.Converter;
import libKonogonka.RainbowDump;
import libKonogonka.ctraes.AesCtrDecrypt;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class System2Header {
    private final static Logger log = LogManager.getLogger(System2Header.class);

    private final byte[] headerCtr;
    private byte[] section0Ctr;
    private byte[] section1Ctr;
    private byte[] section2Ctr;
    private byte[] section3Ctr;
    private String magic;
    private int baseOffset;
    private byte[] zeroOrReserved; // Or base offset always been int8?
    private byte package2Version;
    private byte bootloaderVersion;
    private byte[] padding;
    private int section0size;
    private int section1size;
    private int section2size;
    private int section3size;
    private int section0offset;
    private int section1offset;
    private int section2offset;
    private int section3offset;
    private byte[] sha256overEncryptedSection0;
    private byte[] sha256overEncryptedSection1;
    private byte[] sha256overEncryptedSection2;
    private byte[] sha256overEncryptedSection3;

    private HashMap<String, String> package2Keys;
    private byte[] decodedHeaderBytes;

    public System2Header(byte[] headerBytes, HashMap<String, String> keys) throws Exception{
        this.headerCtr = Arrays.copyOfRange(headerBytes, 0, 0x10);
        collectKeys(keys);
        decodeEncrypted(headerBytes);
        buildHeader();
    }
    private void collectKeys(HashMap<String, String> keys){
        package2Keys = new HashMap<>();

        for (String key: keys.keySet()){
            if (key.matches("package2_key_[0-f][0-f]"))
                package2Keys.put(key, keys.get(key));
        }
    }
    private void decodeEncrypted(byte[] headerBytes) throws Exception{
        for (Map.Entry<String, String> entry: package2Keys.entrySet()){
            AesCtrDecrypt decrypt = new AesCtrDecrypt(entry.getValue(), headerCtr, 0x100);

            decodedHeaderBytes = decrypt.decryptNext(headerBytes);
            byte[] magicBytes = Arrays.copyOfRange(decodedHeaderBytes, 0x50, 0x54);
            magic = new String(magicBytes, StandardCharsets.US_ASCII);
            if (magic.equals("PK21")) {
                log.debug("Header key used "+entry.getKey() + " = " + entry.getValue());
                return;
            }
        }
        throw new Exception("Header hasn't been decoded. No appropriate package2_key_XX?");
    }
    private void buildHeader(){
        section0Ctr = Arrays.copyOfRange(decodedHeaderBytes, 0x10, 0x20);
        section1Ctr = Arrays.copyOfRange(decodedHeaderBytes, 0x20, 0x30);
        section2Ctr = Arrays.copyOfRange(decodedHeaderBytes, 0x30, 0x40);
        section3Ctr = Arrays.copyOfRange(decodedHeaderBytes, 0x40, 0x50);

        baseOffset = Converter.getLEint(decodedHeaderBytes, 0x54);
        zeroOrReserved = Arrays.copyOfRange(decodedHeaderBytes, 0x58, 0x5c);
        package2Version = decodedHeaderBytes[0x5c];
        bootloaderVersion = decodedHeaderBytes[0x5d];
        padding = Arrays.copyOfRange(decodedHeaderBytes, 0x5e, 0x60);
        section0size = Converter.getLEint(decodedHeaderBytes, 0x60);
        section1size = Converter.getLEint(decodedHeaderBytes, 0x64);
        section2size = Converter.getLEint(decodedHeaderBytes, 0x68);
        section3size = Converter.getLEint(decodedHeaderBytes, 0x6c);
        section0offset = Converter.getLEint(decodedHeaderBytes, 0x70);
        section1offset = Converter.getLEint(decodedHeaderBytes, 0x74);
        section2offset = Converter.getLEint(decodedHeaderBytes, 0x78);
        section3offset = Converter.getLEint(decodedHeaderBytes, 0x7c);
        sha256overEncryptedSection0 = Arrays.copyOfRange(decodedHeaderBytes, 0x80, 0xa0);
        sha256overEncryptedSection1 = Arrays.copyOfRange(decodedHeaderBytes, 0xa0, 0xc0);
        sha256overEncryptedSection2 = Arrays.copyOfRange(decodedHeaderBytes, 0xc0, 0xe0);
        sha256overEncryptedSection3 = Arrays.copyOfRange(decodedHeaderBytes, 0xe0, 0x100);
    }

    public void printDebug(){
        log.debug("== System2 Header ==\n" +
            "Header CTR          : " + Converter.byteArrToHexStringAsLE(headerCtr) + "\n" +
            "Section 0 CTR       : " + Converter.byteArrToHexStringAsLE(section0Ctr) + "\n" +
            "Section 1 CTR       : " + Converter.byteArrToHexStringAsLE(section1Ctr) + "\n" +
            "Section 2 CTR       : " + Converter.byteArrToHexStringAsLE(section2Ctr) + "\n" +
            "Section 3 CTR       : " + Converter.byteArrToHexStringAsLE(section3Ctr) + "\n" +
            "Magic PK21          : " + magic + "\n" +
            "Offset              : " + RainbowDump.formatDecHexString(baseOffset) + "\n" +
            "Zero/reserved       : " + Converter.byteArrToHexStringAsLE(zeroOrReserved) + "\n" +
            "Package2 version    : " + RainbowDump.formatDecHexString(package2Version) + "\n" +
            "Bootloader version  : " + RainbowDump.formatDecHexString(bootloaderVersion) + "\n" +
            "Padding             : " + Converter.byteArrToHexStringAsLE(padding) + "\n" +
            "Section 0 size      : " + RainbowDump.formatDecHexString(section0size) + "\n" +
            "Section 1 size      : " + RainbowDump.formatDecHexString(section1size) + "\n" +
            "Section 2 size      : " + RainbowDump.formatDecHexString(section2size) + "\n" +
            "Section 3 size      : " + RainbowDump.formatDecHexString(section3size) + "\n" +
            "Section 0 offset    : " + RainbowDump.formatDecHexString(section0offset) + "\n" +
            "Section 1 offset    : " + RainbowDump.formatDecHexString(section1offset) + "\n" +
            "Section 2 offset    : " + RainbowDump.formatDecHexString(section2offset) + "\n" +
            "Section 3 offset    : " + RainbowDump.formatDecHexString(section3offset) + "\n" +
            "SHA256 ov.enc.sec 0 : " + Converter.byteArrToHexStringAsLE(sha256overEncryptedSection0) + "\n" +
            "SHA256 ov.enc.sec 1 : " + Converter.byteArrToHexStringAsLE(sha256overEncryptedSection1) + "\n" +
            "SHA256 ov.enc.sec 2 : " + Converter.byteArrToHexStringAsLE(sha256overEncryptedSection2) + "\n" +
            "SHA256 ov.enc.sec 3 : " + Converter.byteArrToHexStringAsLE(sha256overEncryptedSection3) + "\n"
        );
    }
}
