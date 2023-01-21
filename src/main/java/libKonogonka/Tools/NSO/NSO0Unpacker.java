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

import libKonogonka.aesctr.InFileStreamProducer;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4SafeDecompressor;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

class NSO0Unpacker {
    private static final String DECOMPRESSED_FILE_NAME = "main_decompressed";
    private final MessageDigest digest = MessageDigest.getInstance("SHA-256");

    private final NSO0Header nso0Header;
    private final InFileStreamProducer producer;

    private byte[] _textDecompressedSection;
    private byte[] _rodataDecompressedSection;
    private byte[] _dataDecompressedSection;
    private byte[] header;
    private int textFileOffsetNew;
    private int rodataFileOffsetNew;
    private int dataFileOffsetNew;

    private NSO0Unpacker(NSO0Header nso0Header, InFileStreamProducer producer) throws Exception {
        this.nso0Header = nso0Header;
        this.producer = producer;

        decompressSections();
        validateHashes();
        makeHeader();
    }

    static NSO0Raw getNSO0Raw(NSO0Header nso0Header, InFileStreamProducer producer) throws Exception{
        NSO0Unpacker instance = new NSO0Unpacker(nso0Header, producer);

        return new NSO0Raw(instance.header,
                instance._textDecompressedSection,
                instance._rodataDecompressedSection,
                instance._dataDecompressedSection);
    }

    static void unpack(NSO0Header nso0Header, InFileStreamProducer producer, String saveToLocation) throws Exception{
        if (! nso0Header.isTextCompressFlag() && ! nso0Header.isRoCompressFlag() && ! nso0Header.isDataCompressFlag())
            throw new Exception("This file is not compressed");
        NSO0Unpacker instance = new NSO0Unpacker(nso0Header, producer);

        instance.writeFile(saveToLocation);
    }

    private void decompressSections() throws Exception{
        decompressTextSection();
        decompressRodataSection();
        decompressDataSection();
    }
    private void decompressTextSection() throws Exception{
        if (nso0Header.isTextCompressFlag())
            _textDecompressedSection = decompressSection(nso0Header.getTextSegmentHeader(), nso0Header.getTextCompressedSize());
        else
            _textDecompressedSection = duplicateSection(nso0Header.getTextSegmentHeader());
    }
    private void decompressRodataSection() throws Exception{
        if (nso0Header.isRoCompressFlag())
            _rodataDecompressedSection = decompressSection(nso0Header.getRodataSegmentHeader(), nso0Header.getRodataCompressedSize());
        else
            _rodataDecompressedSection = duplicateSection(nso0Header.getRodataSegmentHeader());
    }
    private void decompressDataSection() throws Exception{
        if (nso0Header.isDataCompressFlag())
            _dataDecompressedSection = decompressSection(nso0Header.getDataSegmentHeader(), nso0Header.getDataCompressedSize());
        else
            _dataDecompressedSection = duplicateSection(nso0Header.getDataSegmentHeader());
    }

    private byte[] decompressSection(SegmentHeader segmentHeader, int compressedSectionSize) throws Exception{
        try (BufferedInputStream stream = producer.produce()) {
            int sectionDecompressedSize = segmentHeader.getSize();

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
            int size = segmentHeader.getSize();

            byte[] sectionContent = new byte[size];
            if (segmentHeader.getSegmentOffset() != stream.skip(segmentHeader.getSegmentOffset()))
                throw new Exception("Failed to skip " + segmentHeader.getSegmentOffset() + " bytes till section");

            if (size != stream.read(sectionContent))
                throw new Exception("Failed to read entire section");

            return sectionContent;
        }
    }

    private void validateHashes() throws Exception{
        if ( ! Arrays.equals(nso0Header.getTextHash(), digest.digest(_textDecompressedSection)) ) {
            throw new Exception(".text hash mismatch for .text section");
        }
        if ( ! Arrays.equals(nso0Header.getRodataHash(), digest.digest(_rodataDecompressedSection)) ) {
            throw new Exception(".rodata hash mismatch for .text section");
        }
        if ( ! Arrays.equals(nso0Header.getDataHash(), digest.digest(_dataDecompressedSection)) ) {
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

            textFileOffsetNew = nso0Header.getTextSegmentHeader().getMemoryOffset()+0x100;
            rodataFileOffsetNew = nso0Header.getRodataSegmentHeader().getMemoryOffset()+0x100;
            dataFileOffsetNew = nso0Header.getDataSegmentHeader().getMemoryOffset()+0x100;

            ByteBuffer resultingHeader = ByteBuffer.allocate(0x100).order(ByteOrder.LITTLE_ENDIAN);
            resultingHeader.put("NSO0".getBytes(StandardCharsets.US_ASCII))
                    .putInt(nso0Header.getVersion())
                    .put(nso0Header.getUpperReserved())
                    .putInt(nso0Header.getFlags() & 0b111000)
                    .putInt(textFileOffsetNew)
                    .putInt(nso0Header.getTextSegmentHeader().getMemoryOffset())
                    .putInt(nso0Header.getTextSegmentHeader().getSize())
                    .putInt(0x100)
                    .putInt(rodataFileOffsetNew)
                    .putInt(nso0Header.getRodataSegmentHeader().getMemoryOffset())
                    .putInt(nso0Header.getRodataSegmentHeader().getSize())
                    .putInt(0)
                    .putInt(dataFileOffsetNew)
                    .putInt(nso0Header.getDataSegmentHeader().getMemoryOffset())
                    .putInt(nso0Header.getDataSegmentHeader().getSize())
                    .putInt(nso0Header.getBssSize())
                    .put(nso0Header.getModuleId())
                    .putInt(nso0Header.getTextSegmentHeader().getSize())
                    .putInt(nso0Header.getRodataSegmentHeader().getSize())
                    .putInt(nso0Header.getDataSegmentHeader().getSize())
                    .put(nso0Header.getBottomReserved())
                    .putInt(nso0Header.get_api_infoRelative().getOffset())
                    .putInt(nso0Header.get_api_infoRelative().getSize())
                    .putInt(nso0Header.get_dynstrRelative().getOffset())
                    .putInt(nso0Header.get_dynstrRelative().getSize())
                    .putInt(nso0Header.get_dynsymRelative().getOffset())
                    .putInt(nso0Header.get_dynsymRelative().getSize())
                    .put(nso0Header.getTextHash())
                    .put(nso0Header.getRodataHash())
                    .put(nso0Header.getDataHash());

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
