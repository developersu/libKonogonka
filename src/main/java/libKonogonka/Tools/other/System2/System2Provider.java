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
import libKonogonka.Tools.other.System2.ini1.Ini1Provider;
import libKonogonka.ctraesclassic.AesCtrStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.CipherInputStream;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;


public class System2Provider {
    private final static Logger log = LogManager.getLogger(System2Provider.class);

    private byte[] rsa2048signature;
    private System2Header header;
    private KernelMap kernelMap;
    private Ini1Provider ini1Provider;

    private final String pathToFile;
    private final KeyChainHolder keyChainHolder;

    public System2Provider(String pathToFile, KeyChainHolder keyChainHolder) throws Exception{
        this.pathToFile = pathToFile;
        this.keyChainHolder = keyChainHolder;

        try (BufferedInputStream stream = new BufferedInputStream(Files.newInputStream(Paths.get(pathToFile)))) {
            readSignatures(stream);
            readHeader(stream);
            findIni1KernelMap();
        }
    }

    private void readSignatures(BufferedInputStream stream) throws Exception{
        rsa2048signature = new byte[0x100];
        if (0x100 != stream.read(rsa2048signature))
            throw new Exception("Unable to read System2 RSA-2048 signature bytes");
    }

    private void readHeader(BufferedInputStream stream) throws Exception{
        byte[] headerBytes = new byte[0x100];
        if (0x100 != stream.read(headerBytes))
            throw new Exception("Unable to read System2 header bytes");
        this.header = new System2Header(headerBytes, keyChainHolder.getRawKeySet());
    }

    private void findIni1KernelMap() throws Exception{
        try (InputStream fis = Files.newInputStream(Paths.get(pathToFile))){
            // Encrypted section comes next
            long toSkip = 0x200;
            if (toSkip != fis.skip(toSkip))
                throw new Exception("Unable to skip offset: " + toSkip);

            ByteBuffer byteBuffer = ByteBuffer.allocate(0x1000);
            try (CipherInputStream stream = AesCtrStream.getStream(header.getKey(), header.getSection0Ctr(), fis);) {
                for (int j = 0; j < 8; j++) {
                    byte[] block = new byte[0x200];
                    int actuallyRead;
                    if ((actuallyRead = stream.read(block)) != 0x200)
                        throw new Exception("Read failure " + actuallyRead);
                    byteBuffer.put(block);
                }
            }

            byte[] searchField = byteBuffer.array();
            for (int i = 0; i < 1024; i += 4) {
                kernelMap = new KernelMap(searchField, i);
                if (kernelMap.isValid(header.getSection0size()))
                    return;
            }
            throw new Exception("Kernel map not found");
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

    public byte[] getRsa2048signature() { return rsa2048signature; }
    public System2Header getHeader() { return header; }
    public KernelMap getKernelMap() { return kernelMap; }
    public Ini1Provider getIni1Provider() throws Exception{
        if (ini1Provider == null)
            ini1Provider = new Ini1Provider(header, pathToFile, kernelMap);
        return ini1Provider;
    }
}
