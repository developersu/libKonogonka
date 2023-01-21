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
package libKonogonka.Tools.NSO;

import libKonogonka.aesctr.InFileStreamProducer;

import java.io.BufferedInputStream;
import java.io.File;

public class NSO0Provider {
    private final InFileStreamProducer producer;

    private final NSO0Header header;

    /* Encrypted */
    public NSO0Provider(InFileStreamProducer producer) throws Exception {
        this.producer = producer;
        try (BufferedInputStream stream = producer.produce()){
            byte[] knownStartingBytes = new byte[0x100];
            if (0x100 != stream.read(knownStartingBytes))
                throw new Exception("Reading stream suddenly ended while trying to read starting 0x100 bytes");
            this.header = new NSO0Header(knownStartingBytes);
        }
    }

    /* Not Encrypted */
    public NSO0Provider(File file) throws Exception{
        this(file, 0);
    }
    public NSO0Provider(File file, long offsetPosition) throws Exception {
        this.producer = new InFileStreamProducer(file, offsetPosition);
        try (BufferedInputStream stream = producer.produce()){
            if (offsetPosition != stream.skip(offsetPosition))
                throw new Exception("Can't skip bytes prior NSO0 offset");

            byte[] knownStartingBytes = new byte[0x100];
            if (0x100 != stream.read(knownStartingBytes))
                throw new Exception("Reading stream suddenly ended while trying to read starting 0x100 bytes");
            this.header = new NSO0Header(knownStartingBytes);
        }
    }

    public NSO0Header getHeader() {
        return header;
    }

    public void exportAsDecompressedNSO0(String saveToLocation) throws Exception{
        NSO0Unpacker.unpack(header, producer, saveToLocation);
    }

    public NSO0Raw getAsDecompressedNSO0() throws Exception{
        return NSO0Unpacker.getNSO0Raw(header, producer);
    }

    /**
     * Prints header.printDebug()
     * */
    public void printDebug(){
        header.printDebug();
    }
}
