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

import libKonogonka.Tools.ExportAble;
import libKonogonka.ctraesclassic.InFileStreamClassicProducer;

import java.nio.file.Paths;

public class KIP1Provider extends ExportAble {
    public static final int HEADER_SIZE = 0x100;
    
    private KIP1Header header;
    private final InFileStreamClassicProducer producer;

    private long startOffset;
    private long endOffset;

    public KIP1Provider(String fileLocation) throws Exception{
        this.producer = new InFileStreamClassicProducer(Paths.get(fileLocation));
        this.stream = producer.produce();
        byte[] kip1HeaderBytes = new byte[HEADER_SIZE];
        if (HEADER_SIZE != stream.read(kip1HeaderBytes))
            throw new Exception("Unable to read KIP1 file header");

        makeHeader(kip1HeaderBytes);
        calculateOffsets(0);
    }

    public KIP1Provider(byte[] kip1HeaderBytes, long kip1StartOffset, InFileStreamClassicProducer producer) throws Exception{
        makeHeader(kip1HeaderBytes);
        calculateOffsets(kip1StartOffset);
        this.producer = producer;
        this.stream = producer.produce();
    }

    private void makeHeader(byte[] kip1HeaderBytes) throws Exception{
        this.header = new KIP1Header(kip1HeaderBytes);
    }
    private void calculateOffsets(long kip1StartOffset){
        this.startOffset = kip1StartOffset;
        this.endOffset = HEADER_SIZE + kip1StartOffset +
                header.getTextSegmentHeader().getSize() + header.getRoDataSegmentHeader().getSize() +
                header.getRwDataSegmentHeader().getSize() + header.getBssSegmentHeader().getSize();
    }

    public KIP1Header getHeader() { return header; }

    public long getStartOffset() { return startOffset; }
    public long getEndOffset() { return endOffset; }

    public boolean export(String saveTo) throws Exception{
        stream = producer.produce();
        return export(saveTo, header.getName()+".kip1", startOffset, endOffset - startOffset);
    }
    public boolean exportAsDecompressed(String saveToLocation) throws Exception{
        return Kip1Unpacker.unpack(header, producer.getSuccessor(startOffset, true), saveToLocation);
    }

    public KIP1Raw getAsDecompressed() throws Exception{
        return Kip1Unpacker.getKIP1Raw(header, producer.getSuccessor(startOffset, true));
    }

    public void printDebug(){
        header.printDebug();
    }
}
