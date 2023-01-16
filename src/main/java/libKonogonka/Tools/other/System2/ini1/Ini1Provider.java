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
import libKonogonka.Tools.other.System2.System2Header;
import libKonogonka.ctraesclassic.InFileStreamClassicProducer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Ini1Provider extends ExportAble {
    private Ini1Header ini1Header;
    private List<KIP1Provider> kip1List;

    private final InFileStreamClassicProducer producer;

    public Ini1Provider(Path fileLocation) throws Exception{
        this.producer = new InFileStreamClassicProducer(fileLocation);
        this.stream = producer.produce();
        makeHeader();
        collectKips();
        this.stream.close();
    }

    public Ini1Provider(InFileStreamClassicProducer producer) throws Exception{
        this.producer = producer;
        this.stream = producer.produce();
        makeHeader();
        collectKips();
        this.stream.close();
    }

    public Ini1Provider(System2Header system2Header, String pathToFile, int kernelMapIni1Offset) throws Exception{
        Path filePath = Paths.get(pathToFile);
        this.producer = new InFileStreamClassicProducer(filePath,
                0x200 + kernelMapIni1Offset,
                0x200,
                Files.size(filePath), // size of system2
                system2Header.getKey(),
                system2Header.getSection0Ctr());
        this.stream = producer.produce();
        makeHeader();
        collectKips();
        this.stream.close();
    }

    private void makeHeader() throws Exception{
        byte[] headerBytes = new byte[0x10];
        if (0x10 != stream.read(headerBytes))
            throw new Exception("Unable to read header bytes");
        ini1Header = new Ini1Header(headerBytes);
    }

    private void collectKips() throws Exception{
        kip1List = new ArrayList<>();
        long skipTillNextKip1 = 0;
        long kip1StartOffset = 0;
        for (int i = 0; i < ini1Header.getKipNumber(); i++){
            skipLoop(skipTillNextKip1);
            byte[] kip1bytes = new byte[0x100];
            if (0x100 != stream.read(kip1bytes))
                throw new Exception("Unable to read KIP1 data ");
            KIP1Provider kip1 = new KIP1Provider(kip1bytes, kip1StartOffset, producer.getSuccessor(0x10, true));
            kip1List.add(kip1);
            KIP1Header kip1Header = kip1.getHeader();
            skipTillNextKip1 = kip1Header.getTextSegmentHeader().getSize() +
                    kip1Header.getRoDataSegmentHeader().getSize() +
                    kip1Header.getRwDataSegmentHeader().getSize() +
                    kip1Header.getBssSegmentHeader().getSize();
            kip1StartOffset = kip1.getEndOffset();
        }
    }
    private void skipLoop(long size) throws IOException {
        long mustSkip = size;
        long skipped = 0;
        while (mustSkip > 0){
            skipped += stream.skip(mustSkip);
            mustSkip = size - skipped;
        }
    }

    public Ini1Header getIni1Header() { return ini1Header; }
    public List<KIP1Provider> getKip1List() { return kip1List; }

    public boolean export(String saveTo) throws Exception{
        stream = producer.produce();
        return export(saveTo, "INI1.bin", 0, ini1Header.getSize());
    }
}
