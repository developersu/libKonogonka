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
package libKonogonka.Tools.NCA.NCASectionTableBlock;

import libKonogonka.RainbowDump;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;

import static libKonogonka.Converter.byteArrToHexString;
import static libKonogonka.Converter.getLElong;

public class NcaFsHeader {
    private final static Logger log = LogManager.getLogger(NcaFsHeader.class);
    
    private final byte[] version;
    private final byte fsType;
    private final byte hashType;
    private final byte cryptoType;
    private final byte metaDataHashType;
    private final byte[] padding;
    private SuperBlockIVFC superBlockIVFC;
    private SuperBlockPFS0 superBlockPFS0;
    // BKTR extended
    private final long PatchInfoOffsetSection1;
    private final long PatchInfoSizeSection1;
    private final BucketTreeHeader BktrSection1;

    private final long PatchInfoOffsetSection2;
    private final long PatchInfoSizeSection2;
    private final BucketTreeHeader BktrSection2;

    private final byte[] generation;
    private final byte[] secureValue;
    private final byte[] sectionCTR;
    private final SparseInfo sparseInfo;
    private final CompressionInfo compressionInfo;
    private final MetaDataHashDataInfo metaDataHashDataInfo;
    private final byte[] unknownEndPadding;
    
    public NcaFsHeader(byte[] tableBlockBytes) throws Exception{
        if (tableBlockBytes.length != 0x200)
            throw new Exception("Table Block Section size is incorrect.");
        version = Arrays.copyOfRange(tableBlockBytes, 0, 0x2);
        fsType = tableBlockBytes[0x2];
        hashType = tableBlockBytes[0x3];
        cryptoType = tableBlockBytes[0x4];
        metaDataHashType = tableBlockBytes[0x6];
        padding = Arrays.copyOfRange(tableBlockBytes, 0x6, 0x8);
        byte[] superBlockBytes = Arrays.copyOfRange(tableBlockBytes, 0x8, 0xf8);

        if ((fsType == 0) && (hashType == 0x3))
            superBlockIVFC = new SuperBlockIVFC(superBlockBytes);
        else if ((fsType == 0x1) && (hashType == 0x2))
            superBlockPFS0 = new SuperBlockPFS0(superBlockBytes);

        PatchInfoOffsetSection1 = getLElong(tableBlockBytes, 0x100);
        PatchInfoSizeSection1 = getLElong(tableBlockBytes, 0x108);
        BktrSection1 = new BucketTreeHeader(Arrays.copyOfRange(tableBlockBytes, 0x110, 0x120));

        PatchInfoOffsetSection2 = getLElong(tableBlockBytes, 0x120);
        PatchInfoSizeSection2 = getLElong(tableBlockBytes, 0x128);
        BktrSection2 = new BucketTreeHeader(Arrays.copyOfRange(tableBlockBytes, 0x130, 0x140));

        generation = Arrays.copyOfRange(tableBlockBytes, 0x140, 0x144);
        secureValue = Arrays.copyOfRange(tableBlockBytes, 0x144, 0x148);

        sectionCTR = Arrays.copyOfRange(tableBlockBytes, 0x140, 0x148);

        sparseInfo = new SparseInfo(Arrays.copyOfRange(tableBlockBytes, 0x148, 0x178));
        compressionInfo = new CompressionInfo(Arrays.copyOfRange(tableBlockBytes,  0x178, 0x1a0));
        metaDataHashDataInfo = new MetaDataHashDataInfo(Arrays.copyOfRange(tableBlockBytes,   0x1a0, 0x1d0));

        unknownEndPadding = Arrays.copyOfRange(tableBlockBytes, 0x1d0, 0x200);
    }

    public byte[] getVersion() { return version; }
    public byte getFsType() { return fsType; }
    public byte getHashType() { return hashType; }
    public byte getCryptoType() { return cryptoType; }
    public byte getMetaDataHashType() { return metaDataHashType; }
    public byte[] getPadding() { return padding; }
    public SuperBlockIVFC getSuperBlockIVFC() { return superBlockIVFC; }
    public SuperBlockPFS0 getSuperBlockPFS0() { return superBlockPFS0; }

    public long getPatchInfoOffsetSection1() { return PatchInfoOffsetSection1; }
    public long getPatchInfoSizeSection1() { return PatchInfoSizeSection1; }
    public String getPatchInfoMagicSection1() { return BktrSection1.getMagic(); }
    public int getPatchInfoVersionSection1() { return BktrSection1.getVersion(); }
    public int getEntryCountSection1() { return BktrSection1.getEntryCount(); }
    public byte[] getPatchInfoUnknownSection1() { return BktrSection1.getUnknown(); }
    public long getPatchInfoOffsetSection2() { return PatchInfoOffsetSection2; }
    public long getPatchInfoSizeSection2() { return PatchInfoSizeSection2; }
    public String getPatchInfoMagicSection2() { return BktrSection2.getMagic(); }
    public int getPatchInfoVersionSection2() { return BktrSection2.getVersion(); }
    public int getEntryCountSection2() { return BktrSection2.getEntryCount(); }
    public byte[] getPatchInfoUnknownSection2() { return BktrSection2.getUnknown(); }
    public byte[] getGeneration() {return generation;}
    public byte[] getSecureValue() {return secureValue;}
    /**
     * Used for Aes Ctr decryption in IV context.
     * */
    public byte[] getSectionCTR() { return sectionCTR; }
    public SparseInfo getSparseInfo() {return sparseInfo;}
    public CompressionInfo getCompressionInfo() {return compressionInfo;}
    public MetaDataHashDataInfo getMetaDataHashDataInfo() {return metaDataHashDataInfo;}
    public byte[] getUnknownEndPadding() { return unknownEndPadding; }
    
    public void printDebug(){
        String hashTypeDescription;
        switch (hashType){
            case 0 :
                hashTypeDescription = "Auto";
                break;
            case 1 :
                hashTypeDescription = "None";
                break;
            case 2 :
                hashTypeDescription = "HierarchicalSha256Hash";
                break;
            case 3 :
                hashTypeDescription = "HierarchicalIntegrityHash";
                break;
            case 4 :
                hashTypeDescription = "AutoSha3";
                break;
            case 5 :
                hashTypeDescription = "HierarchicalSha3256Hash";
                break;
            case 6 :
                hashTypeDescription = "HierarchicalIntegritySha3Hash";
                break;
            default:
                hashTypeDescription = "???";
        }
        String cryptoTypeDescription;
        switch (cryptoType){
           case 0 :
                cryptoTypeDescription = "Auto";
                break;
            case 1 :
                cryptoTypeDescription = "None";
                break;
            case 2 :
                cryptoTypeDescription = "AesXts";
                break;
            case 3 :
                cryptoTypeDescription = "AesCtr";
                break;
            case 4 :
                cryptoTypeDescription = "AesCtrEx";
                break;
            case 5 :
                cryptoTypeDescription = "AesCtrSkipLayerHash";
                break;
            case 6 :
                cryptoTypeDescription = "AesCtrExSkipLayerHash";
                break;
            default:
                cryptoTypeDescription = "???";
        }
        
        log.debug("NCASectionBlock:\n" +
                "Version                          : " + byteArrToHexString(version) + "\n" +
                "FS Type                          : " + fsType +(fsType == 0?" (RomFS)":fsType == 1?" (PartitionFS)":" (Unknown)")+ "\n" +
                "Hash Type                        : " + hashType +" ("+ hashTypeDescription + ")\n" +
                "Crypto Type                      : " + cryptoType + " (" + cryptoTypeDescription + ")\n" +
                "Meta Data Hash Type              : " + metaDataHashType + "\n" +
                "Padding                          : " + byteArrToHexString(padding) + "\n" +
                "Super Block IVFC                 : " + (superBlockIVFC == null ? "-\n": "YES\n") +
                "Super Block PFS0                 : " + (superBlockPFS0 == null ? "-\n": "YES\n") +
                "================================================================================================\n" +
                (((fsType == 0) && (hashType == 0x3))?
                ("|                   Hash Data - RomFS\n" +
                "| Magic                          : " + superBlockIVFC.getMagic() + "\n" +
                "| Version                        : " + superBlockIVFC.getVersion() + "\n" +
                "| Master Hash Size               : " + superBlockIVFC.getMasterHashSize() + "\n" +
                "|   Total Number of Levels       : " + superBlockIVFC.getTotalNumberOfLevels() + "\n\n" +
                        
                "|     Level 1 Offset             : " + RainbowDump.formatDecHexString(superBlockIVFC.getLvl1Offset()) + "\n" +
                "|     Level 1 Size               : " + RainbowDump.formatDecHexString(superBlockIVFC.getLvl1Size()) + "\n" +
                "|     Level 1 Block Size (log2)  : " + RainbowDump.formatDecHexString(superBlockIVFC.getLvl1SBlockSize()) + "\n" +
                "|     Level 1 reserved           : " + byteArrToHexString(superBlockIVFC.getReserved1()) + "\n\n" +

                "|     Level 2 Offset             : " + RainbowDump.formatDecHexString(superBlockIVFC.getLvl2Offset()) + "\n" +
                "|     Level 2 Size               : " + RainbowDump.formatDecHexString(superBlockIVFC.getLvl2Size()) + "\n" +
                "|     Level 2 Block Size (log2)  : " + RainbowDump.formatDecHexString(superBlockIVFC.getLvl2SBlockSize()) + "\n" +
                "|     Level 2 reserved           : " + byteArrToHexString(superBlockIVFC.getReserved2()) + "\n\n" +

                "|     Level 3 Offset             : " + RainbowDump.formatDecHexString(superBlockIVFC.getLvl3Offset()) + "\n" +
                "|     Level 3 Size               : " + RainbowDump.formatDecHexString(superBlockIVFC.getLvl3Size()) + "\n" +
                "|     Level 3 Block Size (log2)  : " + RainbowDump.formatDecHexString(superBlockIVFC.getLvl3SBlockSize()) + "\n" +
                "|     Level 3 reserved           : " + byteArrToHexString(superBlockIVFC.getReserved3()) + "\n\n" +

                "|     Level 4 Offset             : " + RainbowDump.formatDecHexString(superBlockIVFC.getLvl4Offset()) + "\n" +
                "|     Level 4 Size               : " + RainbowDump.formatDecHexString(superBlockIVFC.getLvl4Size()) + "\n" +
                "|     Level 4 Block Size (log2)  : " + RainbowDump.formatDecHexString(superBlockIVFC.getLvl4SBlockSize()) + "\n" +
                "|     Level 4 reserved           : " + byteArrToHexString(superBlockIVFC.getReserved4()) + "\n\n" +

                "|     Level 5 Offset             : " + RainbowDump.formatDecHexString(superBlockIVFC.getLvl5Offset()) + "\n" +
                "|     Level 5 Size               : " + RainbowDump.formatDecHexString(superBlockIVFC.getLvl5Size()) + "\n" +
                "|     Level 5 Block Size (log2)  : " + RainbowDump.formatDecHexString(superBlockIVFC.getLvl5SBlockSize()) + "\n" +
                "|     Level 5 reserved           : " + byteArrToHexString(superBlockIVFC.getReserved5()) + "\n\n" +

                "|     Level 6 Offset             : " + RainbowDump.formatDecHexString(superBlockIVFC.getLvl6Offset()) + "\n" +
                "|     Level 6 Size               : " + RainbowDump.formatDecHexString(superBlockIVFC.getLvl6Size()) + "\n" +
                "|     Level 6 Block Size (log2)  : " + RainbowDump.formatDecHexString(superBlockIVFC.getLvl6SBlockSize()) + "\n" +
                "|     Level 6 reserved           : " + byteArrToHexString(superBlockIVFC.getReserved6()) + "\n\n" +
                
                "|   SignatureSalt                : " + byteArrToHexString(superBlockIVFC.getSignatureSalt()) + "\n" +
                "| Master Hash                    : " + byteArrToHexString(superBlockIVFC.getMasterHash()) + "\n" +
                "| Reserved (tail)                : " + byteArrToHexString(superBlockIVFC.getReservedTail()) + "\n"
                )
                :(((fsType == 0x1) && (hashType == 0x2))?
                ("|                   Hash Data - PFS0\n" +
                "| SHA256 hash                    : " + byteArrToHexString(superBlockPFS0.getSHA256hash()) + "\n" +
                "| Block Size (bytes)             : " + superBlockPFS0.getBlockSize() + "\n" +
                "| Layer Count (2)                : " + superBlockPFS0.getLayerCount() + "\n" +
                "| Hash table offset              : " + RainbowDump.formatDecHexString(superBlockPFS0.getHashTableOffset()) + "\n" +
                "| Hash table size                : " + RainbowDump.formatDecHexString(superBlockPFS0.getHashTableSize()) + "\n" +
                "| PFS0 header offset             : " + RainbowDump.formatDecHexString(superBlockPFS0.getPfs0offset()) + "\n" +
                "| PFS0 header size               : " + RainbowDump.formatDecHexString(superBlockPFS0.getPfs0size()) + "\n" +
                "| Unknown (reserved)             : " + byteArrToHexString(superBlockPFS0.getZeroes()) + "\n"
                )
                :
                "               //    Hash Data - EMPTY     \\\\ \n"
                )) +
                "================================================================================================\n" +
                "                    PatchInfo\n" +
                "================================================================================================\n" +
                "Indirect Offset                  : " + PatchInfoOffsetSection1 + "\n" +
                "Indirect Size                    : " + PatchInfoSizeSection1 + "\n" +
                "Magic ('BKTR')                   : " + BktrSection1.getMagic() + "\n" +
                "Version                          : " + BktrSection1.getVersion() + "\n" +
                "EntryCount                       : " + BktrSection1.getEntryCount() + "\n" +
                "Unknown (reserved)               : " + byteArrToHexString(BktrSection1.getUnknown()) + "\n" +
                "------------------------------------------------------------------------------------------------\n" +
                "AesCtrEx Offset                  : " + PatchInfoOffsetSection2 + "\n" +
                "AesCtrEx Size                    : " + PatchInfoSizeSection2 + "\n" +
                "Magic ('BKTR')                   : " + BktrSection2.getMagic() + "\n" +
                "Version                          : " + BktrSection2.getVersion() + "\n" +
                "EntryCount                       : " + BktrSection2.getEntryCount() + "\n" +
                "Unknown (reserved)               : " + byteArrToHexString(BktrSection2.getUnknown()) + "\n" +
                "================================================================================================\n" +
                "Generation                       : " + byteArrToHexString(generation) + "\n" +
                "Section CTR                      : " + byteArrToHexString(sectionCTR) + "\n" +
                "================================================================================================\n" +
                "                    Sparse Info\n" +
                "Table Offset                     : " + sparseInfo.getOffset() + "\n" +
                "Table Size                       : " + sparseInfo.getSize() + "\n" +
                "Magic ('BKTR')                   : " + sparseInfo.getBktrMagic() + "\n" +
                "Version                          : " + sparseInfo.getBktrVersion() + "\n" +
                "EntryCount                       : " + sparseInfo.getBktrEntryCount() + "\n" +
                "Unknown (BKTR)                   : " + byteArrToHexString(sparseInfo.getBktrUnknown()) + "\n" +
                "PhysicalOffset                   : " + sparseInfo.getPhysicalOffset() + "\n" +
                "Generation                       : " + byteArrToHexString(sparseInfo.getGeneration()) + "\n" +
                "Unknown (reserved)               : " + byteArrToHexString(sparseInfo.getUnknown()) + "\n" +
                "================================================================================================\n" +
                "                    Compression Info\n" +
                "Table Offset                     : " + compressionInfo.getOffset() + "\n" +
                "Table Size                       : " + compressionInfo.getSize() + "\n" +
                "Magic ('BKTR')                   : " + compressionInfo.getBktrMagic() + "\n" +
                "Version                          : " + compressionInfo.getBktrVersion() + "\n" +
                "EntryCount                       : " + compressionInfo.getBktrEntryCount() + "\n" +
                "Unknown (reserved)               : " + byteArrToHexString(compressionInfo.getBktrUnknown()) + "\n" +
                "Reserved                         : " + byteArrToHexString(compressionInfo.getUnknown()) + "\n" +
                "================================================================================================\n" +
                "                    Meta Data Hash Data Info\n" +
                "Table Offset                     : " + metaDataHashDataInfo.getOffset() + "\n" +
                "Table Size                       : " + metaDataHashDataInfo.getSize() + "\n" +
                "Unknown (reserved)               : " + byteArrToHexString(metaDataHashDataInfo.getTableHash()) + "\n" +
                "================================================================================================\n" +
                "Unknown End Padding              : " + byteArrToHexString(unknownEndPadding) + "\n" +
                "################################################################################################\n"
        );
    }
}

