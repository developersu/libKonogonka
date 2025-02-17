/*
    Copyright 2019-2025 Dmitry Isaenko
     
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
package libKonogonka.fs.other.System2;

import libKonogonka.Converter;
import libKonogonka.KeyChainHolder;
import libKonogonka.fs.ExportAble;
import libKonogonka.fs.other.System2.ini1.Ini1Provider;
import libKonogonka.aesctr.InFileStreamProducer;
import libKonogonka.aesctr.InFileStreamClassicProducer;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class System2Provider extends ExportAble {
    private byte[] rsa2048signature;
    private System2Header header;
    private KernelMap kernelMap;
    private Ini1Provider ini1Provider;

    private final KeyChainHolder keyChainHolder;
    private InFileStreamClassicProducer producer;

    public System2Provider(String pathToFile, KeyChainHolder keyChainHolder) throws Exception{
        this.keyChainHolder = keyChainHolder;

        Path filePath = Paths.get(pathToFile);
        this.stream = new BufferedInputStream(Files.newInputStream(filePath));
        readSignatures();
        readHeader();
        this.stream.close();
        createProducerOfFile(filePath);
        findIni1KernelMap();
        this.stream.close();
    }

    public System2Provider(InFileStreamProducer producer, KeyChainHolder keyChainHolder) throws Exception{
        this.keyChainHolder = keyChainHolder;

        this.stream = producer.produce();
        readSignatures();
        readHeader();
        this.stream.close();
        createProducerOfStream(producer);
        findIni1KernelMap();
        this.stream.close();
    }

    private void readSignatures() throws Exception{
        rsa2048signature = new byte[0x100];
        if (0x100 != stream.read(rsa2048signature))
            throw new Exception("Unable to read System2 RSA-2048 signature bytes");
    }

    private void readHeader() throws Exception{
        byte[] headerBytes = new byte[0x100];
        if (0x100 != stream.read(headerBytes))
            throw new Exception("Unable to read System2 header bytes");
        this.header = new System2Header(headerBytes, keyChainHolder.getRawKeySet());
    }

    private void createProducerOfFile(Path filePath) throws Exception{
        this.producer = new InFileStreamClassicProducer(filePath,
                0,
                0x200,
                header.getPackageSize(),
                header.getKey(),
                header.getSection0Ctr());
        this.stream = producer.produce();
    }

    private void createProducerOfStream(InFileStreamProducer parentProducer) throws Exception{
        producer = new InFileStreamClassicProducer(parentProducer,
                0,
                0x200,
                header.getPackageSize(),
                header.getKey(),
                header.getSection0Ctr(),
                header.getPackageSize());
        this.stream = producer.produce();
    }

    private void findIni1KernelMap() throws Exception{
        if (0x200 != stream.skip(0x200))
            throw new Exception("Unable to skip offset of 0x200");

        ByteBuffer byteBuffer = ByteBuffer.allocate(0x1000);

        for (int j = 0; j < 8; j++) {
            byte[] block = new byte[0x200];
            int actuallyRead;
            if ((actuallyRead = stream.read(block)) != 0x200)
                throw new Exception("Read failure " + actuallyRead);
            byteBuffer.put(block);
        }
        byte[] searchField = byteBuffer.array();

        if (Converter.getLEint(searchField, 3) == 0x14){ // If FW 17.0.0+
            // Calculate new location of the 'kernel beginning'
            int branchTarget = (Converter.getLEint(searchField, 0) & 0x00FFFFFF) << 2;

            int toSkip = branchTarget - 0x1000;
            if (toSkip != stream.skip(toSkip))
                throw new Exception("Unable to skip offset of " + toSkip);

            byteBuffer.clear();

            for (int j = 0; j < 8; j++) {
                byte[] block = new byte[0x200];
                int actuallyRead;
                if ((actuallyRead = stream.read(block)) != 0x200)
                    throw new Exception("Read failure " + actuallyRead);
                byteBuffer.put(block);
            }

            searchField = byteBuffer.array();

            for (int i = 0; i < 0x1000; i += 4) {
                kernelMap = KernelMap.constructKernelMap17(searchField, i, branchTarget, header.getSection0size());
                if (kernelMap != null)
                    return;
            }
            System.out.println();
        }
        else {
            for (int i = 0; i < 0x1000; i += 4) {
                kernelMap = KernelMap.constructKernelMap(searchField, i, header.getSection0size());
                if (kernelMap != null)
                    return;
            }
        }
        throw new Exception("Kernel map not found");
    }

    public boolean exportKernel(String saveTo) throws Exception{
        stream = producer.produce();
        return export(saveTo, "Kernel.bin", 0x200, header.getSection0size());
    }

    public byte[] getRsa2048signature() { return rsa2048signature; }
    public System2Header getHeader() { return header; }
    public KernelMap getKernelMap() { return kernelMap; }
    public Ini1Provider getIni1Provider() throws Exception{
        if (ini1Provider == null)
            ini1Provider = new Ini1Provider(
                    producer.getSuccessor(0x200 + kernelMap.getIni1Offset(), true));
        return ini1Provider;
    }
}
