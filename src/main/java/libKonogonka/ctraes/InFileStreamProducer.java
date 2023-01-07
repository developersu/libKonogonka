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
package libKonogonka.ctraes;

import libKonogonka.IProducer;

import java.io.BufferedInputStream;
import java.io.File;
import java.nio.file.Files;

public class InFileStreamProducer implements IProducer {
    private boolean encrypted;

    private final File file;
    private final long initialOffset;
    private long subOffset;
    private AesCtrDecryptForMediaBlocks decryptor;
    private long mediaStartOffset;
    private long mediaEndOffset;

    public InFileStreamProducer(File file){
        this.file = file;
        this.initialOffset = 0;
        this.subOffset = 0;
    }
    public InFileStreamProducer(File file, long subOffset){
        this.file = file;
        this.initialOffset = 0;
        this.subOffset = subOffset;
    }
    public InFileStreamProducer(
            File file,
            long initialOffset,
            long subOffset,
            AesCtrDecryptForMediaBlocks decryptor,
            long mediaStartOffset,
            long mediaEndOffset){
        this.encrypted = (decryptor != null);
        this.file = file;
        this.initialOffset = initialOffset;
        this.subOffset = subOffset;
        this.decryptor = decryptor;
        this.mediaStartOffset = mediaStartOffset;
        this.mediaEndOffset = mediaEndOffset;
    }
    @Override
    public BufferedInputStream produce() throws Exception{
        if (encrypted)
            return produceAesCtr();
        return produceNotEncrypted();
    }
    private AesCtrBufferedInputStream produceAesCtr() throws Exception{
        decryptor.reset();
        AesCtrBufferedInputStream stream = new AesCtrBufferedInputStream(
                decryptor,
                initialOffset,
                mediaStartOffset,
                mediaEndOffset,
                Files.newInputStream(file.toPath()),
                Files.size(file.toPath()));
        skipBytesTillBeginning(stream, subOffset);
        return stream;
    }
    private BufferedInputStream produceNotEncrypted() throws Exception{
        BufferedInputStream stream = new BufferedInputStream(Files.newInputStream(file.toPath()));
        skipBytesTillBeginning(stream, subOffset);
        return stream;
    }
    @Override
    public InFileStreamProducer getSuccessor(long subOffset){
        this.subOffset = subOffset;
        return new InFileStreamProducer(file, initialOffset, subOffset, decryptor, mediaStartOffset, mediaEndOffset);
    }
    @Override
    public boolean isEncrypted() {
        return encrypted;
    }

    private void skipBytesTillBeginning(BufferedInputStream stream, long size) throws Exception{
        long mustSkip = size;
        long skipped = 0;
        while (mustSkip > 0){
            skipped += stream.skip(mustSkip);
            mustSkip = size - skipped;
        }
    }
    @Override
    public File getFile(){ return file; }
    @Override
    public String toString(){
        return file.getName();
    }
}
