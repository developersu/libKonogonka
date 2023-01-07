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
package libKonogonka.Tools.NSO;

import libKonogonka.Converter;
import libKonogonka.RainbowDump;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static libKonogonka.Converter.getLEint;

public class NSO0Header {
    private final static Logger log = LogManager.getLogger(NSO0Header.class);

    private String magic;
    private int version;
    private byte[] upperReserved;
    private int flags;
    private SegmentHeader textSegmentHeader;
    private int moduleNameOffset;   // note: could be unsigned int; consider 'long'
    private SegmentHeader rodataSegmentHeader;
    private int moduleNameSize;
    private SegmentHeader dataSegmentHeader;
    private int bssSize;            // note: could be unsigned int; consider 'long'
    private byte[] moduleId;
    private int textCompressedSize;
    private int rodataCompressedSize;
    private int dataCompressedSize;
    private byte[] bottomReserved;
    private SegmentHeaderRelative _api_infoRelative;
    private SegmentHeaderRelative _dynstrRelative;
    private SegmentHeaderRelative _dynsymRelative;
    private byte[] textHash;
    private byte[] rodataHash;
    private byte[] dataHash;

    public NSO0Header(byte[] headerBytes) throws Exception{
        if (headerBytes.length < 0x100)
            throw new Exception("Incorrect NSO0 header size");
        parse(headerBytes);
    }

    private void parse(byte[] knownStartingBytes) throws Exception{
        this.magic = new String(knownStartingBytes, 0x0, 0x4, StandardCharsets.US_ASCII);
        if (! magic.equals("NSO0")){
            throw new Exception("Bad magic");
        }

        this.version = getLEint(knownStartingBytes, 0x4);
        this.upperReserved = Arrays.copyOfRange(knownStartingBytes, 0x8, 0xC);
        this.flags = getLEint(knownStartingBytes, 0xC);
        this.textSegmentHeader = new SegmentHeader(knownStartingBytes, 0x10);
        this.moduleNameOffset = Converter.getLEint(knownStartingBytes, 0x1C);
        this.rodataSegmentHeader = new SegmentHeader(knownStartingBytes, 0x20);
        this.moduleNameSize = Converter.getLEint(knownStartingBytes, 0x2C);
        this.dataSegmentHeader = new SegmentHeader(knownStartingBytes, 0x30);
        this.bssSize = Converter.getLEint(knownStartingBytes, 0x3C);
        this.moduleId = Arrays.copyOfRange(knownStartingBytes, 0x40, 0x60);
        this.textCompressedSize = Converter.getLEint(knownStartingBytes, 0x60);
        this.rodataCompressedSize = Converter.getLEint(knownStartingBytes, 0x64);
        this.dataCompressedSize = Converter.getLEint(knownStartingBytes, 0x68);
        this.bottomReserved = Arrays.copyOfRange(knownStartingBytes, 0x6C, 0x88);
        this._api_infoRelative = new SegmentHeaderRelative(knownStartingBytes, 0x88);
        this._dynstrRelative = new SegmentHeaderRelative(knownStartingBytes, 0x90);
        this._dynsymRelative = new SegmentHeaderRelative(knownStartingBytes, 0x98);
        this.textHash = Arrays.copyOfRange(knownStartingBytes, 0xA0, 0xC0);
        this.rodataHash = Arrays.copyOfRange(knownStartingBytes, 0xC0, 0xE0);
        this.dataHash = Arrays.copyOfRange(knownStartingBytes, 0xE0, 0x100);
    }

    /* API */
    public String getMagic() { return magic; }
    public int getVersion() {return version; }
    public byte[] getUpperReserved() { return upperReserved; }
    public int getFlags() { return flags; }
    public boolean isTextCompressFlag(){ return (flags & 0b000001) == 1; }
    public boolean isRoCompressFlag(){ return (flags & 0b000010) >> 1 == 1; }
    public boolean isDataCompressFlag(){ return (flags & 0b000100 ) >> 2 == 1; }
    public boolean isTextHashFlag(){ return (flags & 0b001000 ) >> 3 == 1; }
    public boolean isRoHashFlag(){ return (flags & 0b010000 ) >> 4 == 1; }
    public boolean isDataHashFlag(){ return (flags & 0b100000 ) >> 5 == 1; }
    public SegmentHeader getTextSegmentHeader() { return textSegmentHeader; }
    public int getModuleNameOffset() { return moduleNameOffset; }
    public SegmentHeader getRodataSegmentHeader() { return rodataSegmentHeader; }
    public int getModuleNameSize() { return moduleNameSize; }
    public SegmentHeader getDataSegmentHeader() { return dataSegmentHeader; }
    public int getBssSize() { return bssSize; }
    public byte[] getModuleId() { return moduleId; }
    public int getTextCompressedSize() { return textCompressedSize; }
    public int getRodataCompressedSize() { return rodataCompressedSize; }
    public int getDataCompressedSize() { return dataCompressedSize; }
    public byte[] getBottomReserved() { return bottomReserved; }
    public SegmentHeaderRelative get_api_infoRelative() { return _api_infoRelative; }
    public SegmentHeaderRelative get_dynstrRelative() { return _dynstrRelative; }
    public SegmentHeaderRelative get_dynsymRelative() { return _dynsymRelative; }
    public byte[] getTextHash() { return textHash; }
    public byte[] getRodataHash() { return rodataHash; }
    public byte[] getDataHash() { return dataHash; }

    public void printDebug(){
        log.debug(".:: NSO0 Provider ::.\n" +
                " ============================================================= \n" +
                "Magic \"NSO0\"                                     " + magic + "\n" +
                "Version (always 0)                               " + version + "\n" +
                "Reserved                                         " + Converter.byteArrToHexStringAsLE(upperReserved) + "\n" +
                "Flags                                            " + Converter.intToBinaryString(flags)  + "\n" +
                "  |- 0.   .text Compress                         " + isTextCompressFlag() + "\n" +
                "  |- 1.   .rodata Compress                       " + isRoCompressFlag() + "\n" +
                "  |- 2.   .data Compress                         " + isDataCompressFlag() + "\n" +
                "  |- 3.   .text Hash                             " + isTextHashFlag() + "\n" +
                "  |- 4.   .rodata Hash                           " + isRoHashFlag() + "\n" +
                "  |- 5.   .data Hash                             " + isDataHashFlag() + "\n" +
                "                                     +++\n"+
                "SegmentHeader for .text\n" +
                "    |-  File Offset - - - - - - - - - - - - - -  "+ RainbowDump.formatDecHexString(textSegmentHeader.getSegmentOffset()) + "\n" +
                "    |-  Memory Offset - - - - - - - - - - - - -  "+ RainbowDump.formatDecHexString(textSegmentHeader.getMemoryOffset()) + "\n" +
                "    |-  Size As Decompressed  - - - - - - - - -  "+ RainbowDump.formatDecHexString(textSegmentHeader.getSize()) + "\n" +
                "ModuleNameOffset (calculated by sizeof(header))  " + RainbowDump.formatDecHexString(moduleNameOffset) + "\n" +
                "                                     +++\n"+
                "SegmentHeader for .rodata\n" +
                "    |-  File Offset - - - - - - - - - - - - - -  " + RainbowDump.formatDecHexString(rodataSegmentHeader.getSegmentOffset()) + "\n" +
                "    |-  Memory Offset - - - - - - - - - - - - -  " + RainbowDump.formatDecHexString(rodataSegmentHeader.getMemoryOffset()) + "\n" +
                "    |-  Size As Decompressed  - - - - - - - - -  " + RainbowDump.formatDecHexString(rodataSegmentHeader.getSize()) + "\n" +
                "ModuleNameSize                                   " + RainbowDump.formatDecHexString(moduleNameSize) + "\n" +
                "                                     +++\n"+
                "SegmentHeader for .data\n" +
                "    |-  File Offset - - - - - - - - - - - - - -  " + RainbowDump.formatDecHexString(dataSegmentHeader.getSegmentOffset()) + "\n" +
                "    |-  Memory Offset - - - - - - - - - - - - -  " + RainbowDump.formatDecHexString(dataSegmentHeader.getMemoryOffset()) + "\n" +
                "    |-  Size As Decompressed  - - - - - - - - -  " + RainbowDump.formatDecHexString(dataSegmentHeader.getSize()) + "\n" +
                "  .bss Size                                      " + RainbowDump.formatDecHexString(bssSize) + "\n" + // Block Started by Symbol
                "Module ID (aka Build ID)                         " + Converter.byteArrToHexStringAsLE(moduleId) + "\n" +
                "  .text Size (compressed)                        " + RainbowDump.formatDecHexString(textCompressedSize) + "\n" +
                "  .rodata Size (compressed)                      " + RainbowDump.formatDecHexString(rodataCompressedSize) + "\n" +
                "  .data Size (compressed)                        " + RainbowDump.formatDecHexString(dataCompressedSize) + "\n" +
                "Reserved                                         " + Converter.byteArrToHexStringAsLE(bottomReserved) + "\n" +
                "                                     xxx\n"+
                "SegmentHeaderRelative for .api_info\n" +
                "    |-  Offset  - - - - - - - - - - - - - - - -  " + RainbowDump.formatDecHexString(_api_infoRelative.getOffset()) + "\n" +
                "    |-  Size  - - - - - - - - - - - - - - - - -  " + RainbowDump.formatDecHexString(_api_infoRelative.getSize()) + "\n" +
                "                                     xxx\n"+
                "SegmentHeaderRelative for .dynstr\n" +
                "    |-  Offset  - - - - - - - - - - - - - - - -  " + RainbowDump.formatDecHexString(_dynstrRelative.getOffset()) + "\n" +
                "    |-  Size  - - - - - - - - - - - - - - - - -  " + RainbowDump.formatDecHexString(_dynstrRelative.getSize()) + "\n" +
                "                                     xxx\n"+
                "SegmentHeaderRelative for .dynsym\n" +
                "    |-  Offset  - - - - - - - - - - - - - - - -  " + RainbowDump.formatDecHexString(_dynsymRelative.getOffset()) + "\n" +
                "    |-  Size  - - - - - - - - - - - - - - - - -  " + RainbowDump.formatDecHexString(_dynsymRelative.getSize()) + "\n" +
                "                                     xxx\n"+
                ".text decompressed' SHA-256 hash                 " + Converter.byteArrToHexStringAsLE(textHash) + "\n" +
                ".rodata decompressed' SHA-256 hash               " + Converter.byteArrToHexStringAsLE(rodataHash) + "\n" +
                ".data decompressed' SHA-256 hash                 " + Converter.byteArrToHexStringAsLE(dataHash) + "\n" +
                " ============================================================= "
        );
    }
}
