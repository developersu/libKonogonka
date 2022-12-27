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
package libKonogonka.Tools.other.System2;

import libKonogonka.KeyChainHolder;
import libKonogonka.RainbowDump;
import libKonogonka.ctraes.AesCtrClassic;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class System2Provider {
    private final static Logger log = LogManager.getLogger(System2Provider.class);

    private byte[] Rsa2048signature;
    private System2Header header;
    // ...

    private final String pathToFile;
    private final KeyChainHolder keyChainHolder;

    public System2Provider(String pathToFile, KeyChainHolder keyChainHolder) throws Exception{
        this.pathToFile = pathToFile;
        this.keyChainHolder = keyChainHolder;

        readHeaderCtr();


    }

    private void readHeaderCtr() throws Exception{
        try (BufferedInputStream stream = new BufferedInputStream(Files.newInputStream(Paths.get(pathToFile)))){
            if (0x100 != stream.skip(0x100))
                throw new Exception("Can't skip RSA-2048 signature offset (0x100)");
            byte[] headerBytes = new byte[0x100];
            if (0x100 != stream.read(headerBytes))
                throw new Exception("System2 header is too small");
            this.header = new System2Header(headerBytes, keyChainHolder.getRawKeySet());
        }
    }

    public boolean exportKernel(String saveTo) throws Exception{
        AesCtrClassic aesCtrClassic = new AesCtrClassic(header.getKey(), header.getSection0Ctr()); // TODO: DELETE

        File location = new File(saveTo);
        location.mkdirs();

        try (BufferedInputStream stream = new BufferedInputStream(Files.newInputStream(Paths.get(pathToFile)));
                BufferedOutputStream extractedFileBOS = new BufferedOutputStream(
                Files.newOutputStream(Paths.get(saveTo+File.separator+"Kernel.bin")))){

            long kernelSize = header.getSection0size();

            long toSkip = 0x200;
            if (toSkip != stream.skip(toSkip))
                throw new Exception("Unable to skip offset: "+toSkip);
            int blockSize = 0x200;
            if (kernelSize < 0x200)
                blockSize = (int) kernelSize;

            long i = 0;
            byte[] block = new byte[blockSize];

            int actuallyRead;
            while (true) {
                if ((actuallyRead = stream.read(block)) != blockSize)
                    throw new Exception("Read failure. Block Size: "+blockSize+", actuallyRead: "+actuallyRead);
                byte[] decrypted = aesCtrClassic.decryptNext(block);
                if (i > 0 && i <= 0x201){
                    RainbowDump.hexDumpUTF8(decrypted);
                }
                if (i == 0){
                    RainbowDump.hexDumpUTF8(decrypted);
                }
                extractedFileBOS.write(decrypted);
                i += blockSize;
                if ((i + blockSize) > kernelSize) {
                    blockSize = (int) (kernelSize - i);
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

    public System2Header getHeader() {
        return header;
    }
}
