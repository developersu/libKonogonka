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
import libKonogonka.Tools.other.System2.KernelMap;
import libKonogonka.Tools.other.System2.System2Header;
import libKonogonka.ctraesclassic.AesCtrClassicBufferedInputStream;
import libKonogonka.ctraesclassic.AesCtrDecryptClassic;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Ini1Provider extends ExportAble {
    private final System2Header system2Header;
    private final String pathToFile;
    private final KernelMap kernelMap;
    private Ini1Header ini1Header;
    private List<Kip1> kip1List;

    public Ini1Provider(System2Header system2Header, String pathToFile, KernelMap kernelMap) throws Exception{
        this.system2Header = system2Header;
        this.pathToFile = pathToFile;
        this.kernelMap = kernelMap;

        makeStream();
        makeHeader();
        collectKips();
    }

    private void makeStream() throws Exception{
        Path filePath = Paths.get(pathToFile);
        long toSkip = 0x200 + kernelMap.getIni1Offset();
        AesCtrDecryptClassic decryptor = new AesCtrDecryptClassic(system2Header.getKey(), system2Header.getSection0Ctr());
        stream = new AesCtrClassicBufferedInputStream(decryptor,
                0x200,
                Files.size(filePath), // size of system2
                Files.newInputStream(filePath),
                Files.size(filePath));

        if (toSkip != stream.skip(toSkip))
            throw new Exception("Unable to skip offset: "+toSkip);
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
            if (skipTillNextKip1 != stream.skip(skipTillNextKip1))
                throw new Exception("Unable to skip bytes till next KIP1 header");
            byte[] kip1bytes = new byte[0x100];
            if (0x100 != stream.read(kip1bytes))
                throw new Exception("Unable to read KIP1 data ");
            Kip1 kip1 = new Kip1(kip1bytes, kip1StartOffset);
            kip1List.add(kip1);
            skipTillNextKip1 = kip1.getTextSegmentHeader().getSizeAsDecompressed() +
                    kip1.getRoDataSegmentHeader().getSizeAsDecompressed() +
                    kip1.getRwDataSegmentHeader().getSizeAsDecompressed() +
                    kip1.getBssSegmentHeader().getSizeAsDecompressed();
            kip1StartOffset = kip1.getEndOffset();
        }
    }

    public Ini1Header getIni1Header() { return ini1Header; }
    public List<Kip1> getKip1List() { return kip1List; }

    public boolean exportIni1(String saveTo) throws Exception{
        makeStream();
        return export(saveTo, "INI1.bin", 0, ini1Header.getSize());
    }

    public boolean exportKip1(String saveTo, Kip1 kip1) throws Exception{
        makeStream();
        return export(saveTo,
                kip1.getName()+".kip1",
                0x10 + kip1.getStartOffset(),
                kip1.getEndOffset()-kip1.getStartOffset());
    }
}
