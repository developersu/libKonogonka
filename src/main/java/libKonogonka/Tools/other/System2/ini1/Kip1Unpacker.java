/*
    Copyright 2019-2023 Dmitry Isaenko

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
package libKonogonka.Tools.other.System2.ini1;

import libKonogonka.Tools.NSO.SegmentHeader;
import libKonogonka.blz.BlzDecompress;
import libKonogonka.aesctr.InFileStreamClassicProducer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import static libKonogonka.Tools.other.System2.ini1.KIP1Provider.HEADER_SIZE;

public class Kip1Unpacker {
    private static final String DECOMPRESSED_FILE_POSTFIX = "_decompressed";

    private final KIP1Header kip1Header;
    private final InFileStreamClassicProducer producer;
    private byte[] header;
    private byte[] _textDecompressedSection;
    private byte[] _roDataDecompressedSection;
    private byte[] _rwDataDecompressedSection;
    private int textFileOffsetNew;
    private int roDataFileOffsetNew;
    private int rwDataFileOffsetNew;

    private Kip1Unpacker(KIP1Header kip1Header, InFileStreamClassicProducer producer) throws Exception{
        this.kip1Header = kip1Header;
        this.producer = producer;

        decompressSections();
        makeHeader();
    }

    static boolean unpack(KIP1Header kip1Header, InFileStreamClassicProducer producer, String saveToLocation) throws Exception{
        if (! kip1Header.isTextCompressFlag() && ! kip1Header.isRoDataCompressFlag() && ! kip1Header.isRwDataCompressFlag())
            throw new Exception("This file is not compressed. Use 'export(location)' method instead.");
        Kip1Unpacker instance = new Kip1Unpacker(kip1Header, producer);

        instance.writeFile(saveToLocation);
        return true;
    }

    static KIP1Raw getKIP1Raw(KIP1Header kip1Header, InFileStreamClassicProducer producer) throws Exception{
        Kip1Unpacker instance = new Kip1Unpacker(kip1Header, producer);

        return new KIP1Raw(instance.header,
                instance._textDecompressedSection,
                instance._roDataDecompressedSection,
                instance._rwDataDecompressedSection);
    }

    private void decompressSections() throws Exception{
        decompressTextSection();
        decompressRoDataSection();
        decompressRwDataSection();
    }
    private void decompressTextSection() throws Exception{
        if (kip1Header.isTextCompressFlag())
            _textDecompressedSection = decompressSection(kip1Header.getTextSegmentHeader(), HEADER_SIZE);
        else
            _textDecompressedSection = duplicateSection(kip1Header.getTextSegmentHeader(), HEADER_SIZE);
    }
    private void decompressRoDataSection() throws Exception{
        int offset = HEADER_SIZE + kip1Header.getTextSegmentHeader().getSize();
        if (kip1Header.isRoDataCompressFlag())
            _roDataDecompressedSection = decompressSection(kip1Header.getRoDataSegmentHeader(), offset);
        else
            _roDataDecompressedSection = duplicateSection(kip1Header.getRoDataSegmentHeader(), offset);
    }
    private void decompressRwDataSection() throws Exception{
        int offset = HEADER_SIZE + kip1Header.getTextSegmentHeader().getSize() + kip1Header.getRoDataSegmentHeader().getSize();
        if (kip1Header.isRwDataCompressFlag())
            _rwDataDecompressedSection = decompressSection(kip1Header.getRwDataSegmentHeader(), offset);
        else
            _rwDataDecompressedSection = duplicateSection(kip1Header.getRwDataSegmentHeader(), offset);
    }

    private byte[] decompressSection(SegmentHeader segmentHeader, int offset) throws Exception{
        try (BufferedInputStream stream = producer.produce()) {
            int sectionDecompressedSize = segmentHeader.getMemoryOffset();
            byte[] compressed = new byte[segmentHeader.getSize()];
            if (offset != stream.skip(offset))
                throw new Exception("Failed to skip " + offset + " bytes till section");

            if (segmentHeader.getSize() != stream.read(compressed))
                throw new Exception("Failed to read entire section");

            BlzDecompress decompressor = new BlzDecompress();
            byte[] restored = new byte[sectionDecompressedSize];
            int decompressedLength = decompressor.decompress(compressed, restored);

            if (decompressedLength != sectionDecompressedSize)
                throw new Exception("Decompression failure. Expected vs. actual decompressed sizes mismatch: " +
                        sectionDecompressedSize + " / " + decompressedLength);
            return restored;
        }
    }

    private byte[] duplicateSection(SegmentHeader segmentHeader, int offset) throws Exception{
        int size = segmentHeader.getSize();
        byte[] content = new byte[size];

        try (BufferedInputStream stream = producer.produce()) {
            if (offset != stream.skip(offset))
                throw new Exception("Failed to skip header bytes");

            int blockSize = Math.min(size, 0x200);

            long i = 0;
            byte[] block = new byte[blockSize];

            int actuallyRead;
            while (true) {
                if ((actuallyRead = stream.read(block)) != blockSize)
                    throw new Exception("Read failure. Block Size: " + blockSize + ", actuallyRead: " + actuallyRead);
                System.arraycopy(block, 0, content, (int) i, blockSize);
                i += blockSize;
                if ((i + blockSize) > size) {
                    blockSize = (int) (size - i);
                    if (blockSize == 0)
                        break;
                    block = new byte[blockSize];
                }
            }
        }
        return content;
    }

    private void makeHeader(){
        textFileOffsetNew = kip1Header.getTextSegmentHeader().getMemoryOffset();
        roDataFileOffsetNew = kip1Header.getRoDataSegmentHeader().getMemoryOffset();
        rwDataFileOffsetNew = kip1Header.getRwDataSegmentHeader().getMemoryOffset();
        byte flags = kip1Header.getFlags();
        flags &= ~0b111; //mark .text .ro .rw as 'not compress'

        ByteBuffer resultingHeader = ByteBuffer.allocate(HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        resultingHeader.put("KIP1".getBytes(StandardCharsets.US_ASCII))
                .put(kip1Header.getName().getBytes(StandardCharsets.US_ASCII));
        resultingHeader.position(0x10);
        resultingHeader.put(kip1Header.getProgramId())
                .putInt(kip1Header.getVersion())
                .put(kip1Header.getMainThreadPriority())
                .put(kip1Header.getMainThreadCoreNumber())
                .put(kip1Header.getReserved1())
                .put(flags)
                .putInt(kip1Header.getTextSegmentHeader().getSegmentOffset())
                .putInt(textFileOffsetNew)
                .putInt(textFileOffsetNew)
                .putInt(kip1Header.getThreadAffinityMask())
                .putInt(kip1Header.getRoDataSegmentHeader().getSegmentOffset())
                .putInt(roDataFileOffsetNew)
                .putInt(roDataFileOffsetNew)
                .putInt(kip1Header.getMainThreadStackSize())
                .putInt(kip1Header.getRwDataSegmentHeader().getSegmentOffset())
                .putInt(rwDataFileOffsetNew)
                .putInt(rwDataFileOffsetNew)
                .put(kip1Header.getReserved2())
                .putInt(kip1Header.getBssSegmentHeader().getSegmentOffset())
                .putInt(kip1Header.getBssSegmentHeader().getMemoryOffset())
                .putInt(kip1Header.getBssSegmentHeader().getSize())
                .put(kip1Header.getReserved3())
                .put(kip1Header.getKernelCapabilityData().getRaw());

        header = resultingHeader.array();
    }

    private void writeFile(String saveToLocation) throws Exception{
        File location = new File(saveToLocation);
        location.mkdirs();

        try (RandomAccessFile raf = new RandomAccessFile(
                saveToLocation+File.separator+kip1Header.getName()+DECOMPRESSED_FILE_POSTFIX+".kip1", "rw")){
            raf.write(header);
            raf.seek(HEADER_SIZE);
            raf.write(_textDecompressedSection);
            raf.seek(HEADER_SIZE + textFileOffsetNew);
            raf.write(_roDataDecompressedSection);
            raf.seek(HEADER_SIZE + textFileOffsetNew + roDataFileOffsetNew);
            raf.write(_rwDataDecompressedSection);
        }
    }
}
