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
import libKonogonka.ctraesclassic.InFileStreamClassicProducer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

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

    static KIP1Raw getNSO0Raw(KIP1Header kip1Header, InFileStreamClassicProducer producer) throws Exception{
        Kip1Unpacker instance = new Kip1Unpacker(kip1Header, producer);

        return new KIP1Raw(instance.header,
                instance._textDecompressedSection,
                instance._roDataDecompressedSection,
                instance._rwDataDecompressedSection);
    }

    private void decompressSections() throws Exception{
        decompressTextSection();
        decompressRodataSection();
        decompressDataSection();
    }
    private void decompressTextSection() throws Exception{

        if (kip1Header.isTextCompressFlag())
            _textDecompressedSection = decompressSection(kip1Header.getTextSegmentHeader(), kip1Header.getTextSegmentHeader().getSize());
        else
            _textDecompressedSection = duplicateSection(kip1Header.getTextSegmentHeader());
    }
    private void decompressRodataSection() throws Exception{
        if (kip1Header.isRoDataCompressFlag())
            _roDataDecompressedSection = decompressSection(kip1Header.getRoDataSegmentHeader(), kip1Header.getRoDataSegmentHeader().getSize());
        else
            _roDataDecompressedSection = duplicateSection(kip1Header.getRoDataSegmentHeader());
    }
    private void decompressDataSection() throws Exception{
        if (kip1Header.isRwDataCompressFlag())
            _rwDataDecompressedSection = decompressSection(kip1Header.getRwDataSegmentHeader(), kip1Header.getRwDataSegmentHeader().getSize());
        else
            _rwDataDecompressedSection = duplicateSection(kip1Header.getRwDataSegmentHeader());
    }

    private byte[] decompressSection(SegmentHeader segmentHeader, int compressedSectionSize) throws Exception{
        // TODO
        return new byte[1];
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

    private void makeHeader() throws Exception{
        try (BufferedInputStream stream = producer.produce()) {
            byte[] headerBytes = new byte[0x100];

            if (0x100 != stream.read(headerBytes))
                throw new Exception("Unable to read initial 0x100 bytes needed for export.");
            //TODO
            //textFileOffsetNew = kip1Header.getTextSegmentHeader().getMemoryOffset()+0x100;
            //roDataFileOffsetNew = kip1Header.getRoDataSegmentHeader().getMemoryOffset()+0x100;
            //rwDataFileOffsetNew = kip1Header.getRwDataSegmentHeader().getMemoryOffset()+0x100;

            ByteBuffer resultingHeader = ByteBuffer.allocate(0x100).order(ByteOrder.LITTLE_ENDIAN);
            resultingHeader.put("KIP1".getBytes(StandardCharsets.US_ASCII));
                    //.putInt(kip1Header.getVersion())
                    //.put(kip1Header.getUpperReserved())


            header = resultingHeader.array();
        }
    }

    private void writeFile(String saveToLocation) throws Exception{
        File location = new File(saveToLocation);
        location.mkdirs();

        try (RandomAccessFile raf = new RandomAccessFile(
                saveToLocation+File.separator+kip1Header.getName()+DECOMPRESSED_FILE_POSTFIX+".kip1", "rw")){
            raf.write(header);
            raf.seek(textFileOffsetNew);
            raf.write(_textDecompressedSection);
            raf.seek(roDataFileOffsetNew);
            raf.write(_roDataDecompressedSection);
            raf.seek(roDataFileOffsetNew);
            raf.write(_rwDataDecompressedSection);
        }
    }
}
