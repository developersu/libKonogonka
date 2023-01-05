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

import libKonogonka.Tools.other.System2.KernelMap;
import libKonogonka.Tools.other.System2.System2Header;
import libKonogonka.ctraesclassic.AesCtrClassicBufferedInputStream;
import libKonogonka.ctraesclassic.AesCtrDecryptClassic;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Ini1Provider {
    private final static Logger log = LogManager.getLogger(Ini1Provider.class);

    private final System2Header system2Header;
    private final String pathToFile;
    private final KernelMap kernelMap;
    private Ini1Header ini1Header;
    private List<Kip1> kip1List;

    private AesCtrClassicBufferedInputStream stream;

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
                Files.size(filePath),
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
        for (int i = 0; i < ini1Header.getKipNumber(); i++){
            if (skipTillNextKip1 != stream.skip(skipTillNextKip1))
                throw new Exception("Unable to skip bytes till next KIP1 header");
            byte[] kip1bytes = new byte[0x100];
            if (0x100 != stream.read(kip1bytes))
                throw new Exception("Unable to read KIP1 data ");
            Kip1 kip1 = new Kip1(kip1bytes);
            kip1List.add(kip1);
            skipTillNextKip1 = kip1.getTextSegmentHeader().getSizeAsDecompressed() +
                    kip1.getRoDataSegmentHeader().getSizeAsDecompressed() +
                    kip1.getDataSegmentHeader().getSizeAsDecompressed();
        }
    }

    public Ini1Header getIni1Header() { return ini1Header; }
    public List<Kip1> getKip1List() { return kip1List; }

    public boolean export(String saveTo) throws Exception{
        makeStream();
        File location = new File(saveTo);
        location.mkdirs();

        try (BufferedOutputStream extractedFileBOS = new BufferedOutputStream(
                Files.newOutputStream(Paths.get(saveTo+File.separator+"INI1.bin")))){

            long iniSize = ini1Header.getSize();

            int blockSize = 0x200;
            if (iniSize < 0x200)
                blockSize = (int) iniSize;

            long i = 0;
            byte[] block = new byte[blockSize];

            int actuallyRead;
            while (true) {
                if ((actuallyRead = stream.read(block)) != blockSize)
                    throw new Exception("Read failure. Block Size: "+blockSize+", actuallyRead: "+actuallyRead);
                extractedFileBOS.write(block);
                i += blockSize;
                if ((i + blockSize) > iniSize) {
                    blockSize = (int) (iniSize - i);
                    if (blockSize == 0)
                        break;
                    block = new byte[blockSize];
                }
            }
        }
        catch (Exception e){
            log.error("File export failure", e);
            return false;
        }
        return true;
    }
}
