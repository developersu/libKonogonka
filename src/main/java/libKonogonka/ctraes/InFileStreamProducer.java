/*
    Copyright 2018-2022 Dmitry Isaenko
     
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
package libKonogonka.ctraes;

import java.io.BufferedInputStream;
import java.io.File;
import java.nio.file.Files;

// TODO: rework, simplify
public class InFileStreamProducer {
    private final boolean encrypted;
    private final long size;

    private final File file;
    private final long initialOffset;
    private long subOffset;
    private AesCtrDecryptSimple decryptor;
    private long mediaStartOffset;
    private long mediaEndOffset;

    public InFileStreamProducer(
            File file,
            long size,
            long initialOffset,
            long subOffset,
            AesCtrDecryptSimple decryptor,
            long mediaStartOffset,
            long mediaEndOffset
    ){
        this.encrypted = (decryptor != null);
        this.file = file;
        this.size = size;
        this.initialOffset = initialOffset;
        this.subOffset = subOffset;
        this.decryptor = decryptor;
        this.mediaStartOffset = mediaStartOffset;
        this.mediaEndOffset = mediaEndOffset;
    }
    public InFileStreamProducer(File file, long size, long initialOffset, long subOffset){
        this.encrypted = false;
        this.file = file;
        this.size = size;
        this.initialOffset = 0;
        this.subOffset = initialOffset+subOffset;
    }
    public InFileStreamProducer(File file, long size){
        this.encrypted = false;
        this.file = file;
        this.size = size;
        this.initialOffset = 0;
        this.subOffset = 0;
    }
    public InFileStreamProducer(File file, long size, long subOffset){
        this.encrypted = false;
        this.file = file;
        this.size = size;
        this.initialOffset = 0;
        this.subOffset = subOffset;
    }

    public BufferedInputStream produce() throws Exception{
        if (encrypted) {
            return produceAesCtr();
        }
        return produceNotEncrypted();
    }
    private AesCtrBufferedInputStream produceAesCtr() throws Exception{
        decryptor.reset();
        AesCtrBufferedInputStream stream = new AesCtrBufferedInputStream(
                decryptor,
                initialOffset,
                mediaStartOffset,
                mediaEndOffset,
                Files.newInputStream(file.toPath()));
        if (subOffset != stream.skip(subOffset))
            throw new Exception("Unable to skip offset: " + subOffset);
        return stream;
    }
    private BufferedInputStream produceNotEncrypted() throws Exception{
        BufferedInputStream stream = new BufferedInputStream(Files.newInputStream(file.toPath()));
        if (subOffset != stream.skip(subOffset))
            throw new Exception("Unable to skip offset: " + subOffset);
        return stream;
    }

    public InFileStreamProducer getSuccessor(long subOffset, long size){
        this.subOffset = subOffset;
        return new InFileStreamProducer(file, size, initialOffset, subOffset, decryptor, mediaStartOffset, mediaEndOffset);
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public long getSize() {
        return size;
    }

    @Override
    public String toString(){
        return file.getName();
    }
}
