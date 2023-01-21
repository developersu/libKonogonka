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
import libKonogonka.aesctr.AesCtrDecryptClassic;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class System2Header {
    private final static Logger log = LogManager.getLogger(System2Header.class);

    private final byte[] headerCtr;
    private final long packageSize;
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

    private String key;

    private HashMap<String, String> package2Keys;
    private byte[] decodedHeaderBytes;

    public System2Header(byte[] headerBytes, HashMap<String, String> keys) throws Exception{
        this.headerCtr = Arrays.copyOfRange(headerBytes, 0, 0x10);
        this.packageSize = Converter.getLEint(headerCtr, 0) ^ Converter.getLEint(headerCtr, 0x8) ^ Converter.getLEint(headerCtr, 0xC);
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
            AesCtrDecryptClassic ctrClassic = new AesCtrDecryptClassic(entry.getValue(), headerCtr);

            decodedHeaderBytes = ctrClassic.decryptNext(headerBytes);
            byte[] magicBytes = Arrays.copyOfRange(decodedHeaderBytes, 0x50, 0x54);
            magic = new String(magicBytes, StandardCharsets.US_ASCII);
            if (magic.equals("PK21")) {
                key = entry.getValue();
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

        if (packageSize != 0x200 + section0size)
            log.error("'Package size' doesn't match 'Header Size' + 'Section 0 size'!");
    }

    public byte[] getHeaderCtr() { return headerCtr; }
    public long getPackageSize() { return packageSize; }
    public byte[] getSection0Ctr() { return section0Ctr; }
    public byte[] getSection1Ctr() { return section1Ctr; }
    public byte[] getSection2Ctr() { return section2Ctr; }
    public byte[] getSection3Ctr() { return section3Ctr; }
    public String getMagic() { return magic; }
    public int getBaseOffset() { return baseOffset; }
    public byte[] getZeroOrReserved() { return zeroOrReserved; }
    public byte getPackage2Version() { return package2Version; }
    public byte getBootloaderVersion() { return bootloaderVersion; }
    public byte[] getPadding() { return padding; }
    public int getSection0size() { return section0size; }
    public int getSection1size() { return section1size; }
    public int getSection2size() { return section2size; }
    public int getSection3size() { return section3size; }
    public int getSection0offset() { return section0offset; }
    public int getSection1offset() { return section1offset; }
    public int getSection2offset() { return section2offset; }
    public int getSection3offset() { return section3offset; }
    public byte[] getSha256overEncryptedSection0() { return sha256overEncryptedSection0; }
    public byte[] getSha256overEncryptedSection1() { return sha256overEncryptedSection1; }
    public byte[] getSha256overEncryptedSection2() { return sha256overEncryptedSection2; }
    public byte[] getSha256overEncryptedSection3() { return sha256overEncryptedSection3; }
    public String getKey() { return key; }

    public void printDebug(){
        log.debug("== System2 Header ==\n" +
            "Header CTR          : " + Converter.byteArrToHexStringAsLE(headerCtr) + "\n" +
            "  Package size      : " + RainbowDump.formatDecHexString(packageSize) + "\n" +
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
