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
import libKonogonka.ctraes.AesCtrStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.CipherInputStream;
import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;


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
        File location = new File(saveTo);
        location.mkdirs();

        InputStream fis = Files.newInputStream(Paths.get(pathToFile));
        // Encrypted section comes next
        long toSkip = 0x200;
        if (toSkip != fis.skip(toSkip))
            throw new Exception("Unable to skip offset: "+toSkip);

        try (CipherInputStream stream = AesCtrStream.getStream(header.getKey(), header.getSection0Ctr(), fis);
             BufferedOutputStream extractedFileBOS = new BufferedOutputStream(
                     Files.newOutputStream(Paths.get(saveTo+File.separator+"Kernel.bin")))){

            long kernelSize = header.getSection0size();

            int blockSize = 0x200;
            if (kernelSize < 0x200)
                blockSize = (int) kernelSize;

            long i = 0;
            byte[] block = new byte[blockSize];

            int actuallyRead;
            while (true) {
                if ((actuallyRead = stream.read(block)) != blockSize)
                    throw new Exception("Read failure. Block Size: "+blockSize+", actuallyRead: "+actuallyRead);
                extractedFileBOS.write(block);
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
    public boolean exportIni1(String saveTo) throws Exception{
        File location = new File(saveTo);
        location.mkdirs();
        InputStream fis = Files.newInputStream(Paths.get(pathToFile));
        // Encrypted section comes next
        long toSkip = 0x200 + header.getSection0offset();
        if (toSkip != fis.skip(toSkip))
            throw new Exception("Unable to skip offset: "+toSkip);

        try (CipherInputStream stream = AesCtrStream.getStream(header.getKey(), calcIni1Ctr(), fis);
             BufferedOutputStream extractedFileBOS = new BufferedOutputStream(
                     Files.newOutputStream(Paths.get(saveTo+File.separator+"INI1.bin")))){

            long iniSize = header.getSection0size()-header.getSection0offset();

            int blockSize = 0x200;
            if (iniSize < 0x200)
                blockSize = (int) iniSize;

            long i = 0;
            byte[] block = new byte[blockSize];

            boolean skipMode = true;
            final byte[] zeroes = new byte[blockSize];

            int actuallyRead;
            while (true) {
                if ((actuallyRead = stream.read(block)) != blockSize)
                    throw new Exception("Read failure. Block Size: "+blockSize+", actuallyRead: "+actuallyRead);
                if (skipMode && Arrays.equals(block, zeroes))
                    ;
                else {
                    skipMode = false;
                    extractedFileBOS.write(block);
                }
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
    private byte[] calcIni1Ctr(){
        BigInteger ctr = new BigInteger(header.getSection0Ctr());
        BigInteger updateTo = BigInteger.valueOf(header.getSection0offset() / 0x10L);
        return ctr.add(updateTo).toByteArray();
    }

    public System2Header getHeader() {
        return header;
    }
}
