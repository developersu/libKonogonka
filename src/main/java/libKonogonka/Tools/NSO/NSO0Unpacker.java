/*
    Copyright 2018-2022 Dmitry Isaenko
     
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

import libKonogonka.ctraes.InFileStreamProducer;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4SafeDecompressor;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class NSO0Unpacker {
    private static final String DECOMPRESSED_FILE_NAME = "main_decompressed";
    private final MessageDigest digest = MessageDigest.getInstance("SHA-256");

    private NSO0Provider provider;
    private InFileStreamProducer producer;

    private byte[] _textDecompressedSection;
    private byte[] _rodataDecompressedSection;
    private byte[] _dataDecompressedSection;
    private byte[] header;
    private int textFileOffsetNew;
    private int rodataFileOffsetNew;
    private int dataFileOffsetNew;

    private NSO0Unpacker() throws NoSuchAlgorithmException { }

    public static void unpack(NSO0Provider provider, InFileStreamProducer producer, String saveToLocation) throws Exception{
        if (! provider.isTextCompressFlag() && ! provider.isRoCompressFlag() && ! provider.isDataCompressFlag())
            throw new Exception("This file is not compressed");

        NSO0Unpacker instance = new NSO0Unpacker();
        instance.provider = provider;
        instance.producer = producer;

        instance.decompressSections();
        instance.validateHashes();
        instance.makeHeader();
        instance.writeFile(saveToLocation);
    }

    private void decompressSections() throws Exception{
        decompressTextSection();
        decompressRodataSection();
        decompressDataSection();
    }
    private void decompressTextSection() throws Exception{
        if (provider.isTextCompressFlag())
            _textDecompressedSection = decompressSection(provider.getTextSegmentHeader(), provider.getTextCompressedSize());
        else
            _textDecompressedSection = duplicateSection(provider.getTextSegmentHeader());
    }
    private void decompressRodataSection() throws Exception{
        if (provider.isRoCompressFlag())
            _rodataDecompressedSection = decompressSection(provider.getRodataSegmentHeader(), provider.getRodataCompressedSize());
        else
            _rodataDecompressedSection = duplicateSection(provider.getRodataSegmentHeader());
    }
    private void decompressDataSection() throws Exception{
        if (provider.isDataCompressFlag())
            _dataDecompressedSection = decompressSection(provider.getDataSegmentHeader(), provider.getDataCompressedSize());
        else
            _dataDecompressedSection = duplicateSection(provider.getDataSegmentHeader());
    }

    private byte[] decompressSection(SegmentHeader segmentHeader, int compressedSectionSize) throws Exception{
        try (BufferedInputStream stream = producer.produce()) {
            int sectionDecompressedSize = segmentHeader.getSizeAsDecompressed();

            byte[] compressed = new byte[compressedSectionSize];
            if (segmentHeader.getSegmentOffset() != stream.skip(segmentHeader.getSegmentOffset()))
                throw new Exception("Failed to skip " + segmentHeader.getSegmentOffset() + " bytes till section");

            if (compressedSectionSize != stream.read(compressed))
                throw new Exception("Failed to read entire section");

            LZ4Factory factory = LZ4Factory.fastestInstance();
            LZ4SafeDecompressor decompressor = factory.safeDecompressor();
            byte[] restored = new byte[sectionDecompressedSize];
            int decompressedLength = decompressor.decompress(compressed, 0, compressedSectionSize, restored, 0);

            if (decompressedLength != sectionDecompressedSize)
                throw new Exception("Decompression failure. Expected vs. actual decompressed sizes mismatch: " +
                        decompressedLength + " / " + sectionDecompressedSize);
            return restored;
        }
    }

    private byte[] duplicateSection(SegmentHeader segmentHeader) throws Exception{
        try (BufferedInputStream stream = producer.produce()) {
            int size = segmentHeader.getSizeAsDecompressed();

            byte[] sectionContent = new byte[size];
            if (segmentHeader.getSegmentOffset() != stream.skip(segmentHeader.getSegmentOffset()))
                throw new Exception("Failed to skip " + segmentHeader.getSegmentOffset() + " bytes till section");

            if (size != stream.read(sectionContent))
                throw new Exception("Failed to read entire section");

            return sectionContent;
        }
    }

    private void validateHashes() throws Exception{
        if ( ! Arrays.equals(provider.getTextHash(), digest.digest(_textDecompressedSection)) ) {
            throw new Exception(".text hash mismatch for .text section");
        }
        if ( ! Arrays.equals(provider.getRodataHash(), digest.digest(_rodataDecompressedSection)) ) {
            throw new Exception(".rodata hash mismatch for .text section");
        }
        if ( ! Arrays.equals(provider.getDataHash(), digest.digest(_dataDecompressedSection)) ) {
            throw new Exception(".data hash mismatch for .text section");
        }
    }

    private void makeHeader() throws Exception{
        try (BufferedInputStream stream = producer.produce()) {
            byte[] headerBytes = new byte[0x100];

            if (0x100 != stream.read(headerBytes))
                throw new Exception("Unable to read initial 0x100 bytes needed for export.");
/*  Simplified for computing however made hard for updating. Consider removing.
            ByteBuffer resultingHeader = ByteBuffer.wrap(headerBytes).order(ByteOrder.LITTLE_ENDIAN);
            ((Buffer) resultingHeader).position(0xC);
            resultingHeader.putInt(provider.getFlags() & 0b111000)
                    .putInt(provider.getTextSegmentHeader().getMemoryOffset()+0x100);
            ((Buffer) resultingHeader).position(0x1C);
            resultingHeader.putInt(0x100)
                    .putInt(provider.getRodataSegmentHeader().getMemoryOffset()+0x100);
            ((Buffer) resultingHeader).position(0x2C);
            resultingHeader.putInt(0)
                    .putInt(provider.getDataSegmentHeader().getMemoryOffset()+0x100);
            ((Buffer) resultingHeader).position(0x60);
            resultingHeader.putInt(provider.getTextSegmentHeader().getSizeAsDecompressed())
                    .putInt(provider.getRodataSegmentHeader().getSizeAsDecompressed())
                    .putInt(provider.getDataSegmentHeader().getSizeAsDecompressed());
            ((Buffer) resultingHeader).flip();

            header = resultingHeader.array();
*/

            textFileOffsetNew = provider.getTextSegmentHeader().getMemoryOffset()+0x100;
            rodataFileOffsetNew = provider.getRodataSegmentHeader().getMemoryOffset()+0x100;
            dataFileOffsetNew = provider.getDataSegmentHeader().getMemoryOffset()+0x100;

            ByteBuffer resultingHeader = ByteBuffer.allocate(0x100).order(ByteOrder.LITTLE_ENDIAN);
            resultingHeader.put("NSO0".getBytes(StandardCharsets.US_ASCII))
                    .putInt(provider.getVersion())
                    .put(provider.getUpperReserved())
                    .putInt(provider.getFlags() & 0b111000)
                    .putInt(textFileOffsetNew)
                    .putInt(provider.getTextSegmentHeader().getMemoryOffset())
                    .putInt(provider.getTextSegmentHeader().getSizeAsDecompressed())
                    .putInt(0x100)
                    .putInt(rodataFileOffsetNew)
                    .putInt(provider.getRodataSegmentHeader().getMemoryOffset())
                    .putInt(provider.getRodataSegmentHeader().getSizeAsDecompressed())
                    .putInt(0)
                    .putInt(dataFileOffsetNew)
                    .putInt(provider.getDataSegmentHeader().getMemoryOffset())
                    .putInt(provider.getDataSegmentHeader().getSizeAsDecompressed())
                    .putInt(provider.getBssSize())
                    .put(provider.getModuleId())
                    .putInt(provider.getTextSegmentHeader().getSizeAsDecompressed())
                    .putInt(provider.getRodataSegmentHeader().getSizeAsDecompressed())
                    .putInt(provider.getDataSegmentHeader().getSizeAsDecompressed())
                    .put(provider.getBottomReserved())
                    .putInt(provider.get_api_infoRelative().getOffset())
                    .putInt(provider.get_api_infoRelative().getSize())
                    .putInt(provider.get_dynstrRelative().getOffset())
                    .putInt(provider.get_dynstrRelative().getSize())
                    .putInt(provider.get_dynsymRelative().getOffset())
                    .putInt(provider.get_dynsymRelative().getSize())
                    .put(provider.getTextHash())
                    .put(provider.getRodataHash())
                    .put(provider.getDataHash());

            header = resultingHeader.array();
        }
    }

    private void writeFile(String saveToLocation) throws Exception{
        File location = new File(saveToLocation);
        location.mkdirs();

        try (RandomAccessFile raf = new RandomAccessFile(saveToLocation+File.separator+DECOMPRESSED_FILE_NAME,
                "rw")){
            raf.write(header);
            raf.seek(textFileOffsetNew);
            raf.write(_textDecompressedSection);
            raf.seek(rodataFileOffsetNew);
            raf.write(_rodataDecompressedSection);
            raf.seek(dataFileOffsetNew);
            raf.write(_dataDecompressedSection);
        }
    }
}
