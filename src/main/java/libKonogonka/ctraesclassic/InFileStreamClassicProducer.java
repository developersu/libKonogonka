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
package libKonogonka.ctraesclassic;

import libKonogonka.IProducer;

import java.io.BufferedInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class InFileStreamClassicProducer implements IProducer {
    private boolean encrypted;

    private final Path filePath;
    private long offset;
    private long encryptedStartOffset;
    private long encryptedEndOffset;
    private AesCtrDecryptClassic decryptor;

    public InFileStreamClassicProducer(Path filePath,
                                       long offset,
                                       long encryptedStartOffset,
                                       long encryptedEndOffset, //Files.size(filePath)
                                       String key,
                                       byte[] iv) throws Exception{
        this.encrypted = true;
        this.filePath = filePath;
        this.offset = offset;
        this.encryptedStartOffset = encryptedStartOffset;
        this.encryptedEndOffset = encryptedEndOffset;
        this.decryptor = new AesCtrDecryptClassic(key, iv);
    }
    public InFileStreamClassicProducer(Path filePath,
                                       long offset,
                                       long encryptedStartOffset,
                                       long encryptedEndOffset, //Files.size(filePath)
                                       AesCtrDecryptClassic decryptor){
        this.encrypted = true;
        this.filePath = filePath;
        this.offset = offset;
        this.encryptedStartOffset = encryptedStartOffset;
        this.encryptedEndOffset = encryptedEndOffset;
        this.decryptor = decryptor;
    }

    public InFileStreamClassicProducer(Path filePath){
        this.filePath = filePath;
    }
    public InFileStreamClassicProducer(Path filePath, long offset){
        this.filePath = filePath;
        this.offset = offset;
    }

    @Override
    public BufferedInputStream produce() throws Exception{
        if (encrypted)
            return produceAesCtr();
        else
            return produceNotEncrypted();
    }

    private BufferedInputStream produceAesCtr() throws Exception{
        decryptor.reset();
        AesCtrClassicBufferedInputStream stream = new AesCtrClassicBufferedInputStream(decryptor,
                encryptedStartOffset,
                encryptedEndOffset,
                Files.newInputStream(filePath),
                Files.size(filePath));

        if (offset != stream.skip(offset))
            throw new Exception("Unable to skip offset: "+offset);

        return stream;
    }

    private BufferedInputStream produceNotEncrypted() throws Exception{
        BufferedInputStream stream = new BufferedInputStream(Files.newInputStream(filePath));
        if (offset != stream.skip(offset))
            throw new Exception("Unable to skip offset: "+offset);
        return stream;
    }
    @Override
    public InFileStreamClassicProducer getSuccessor(long offset){
        if (encrypted)
            return new InFileStreamClassicProducer(filePath, offset, encryptedStartOffset, encryptedEndOffset, decryptor);
        return new InFileStreamClassicProducer(filePath, offset);
    }
    public InFileStreamClassicProducer getSuccessor(long offset, boolean incrementExisting){
        if (incrementExisting)
            return getSuccessor(this.offset + offset);
        return getSuccessor(offset);
    }

    @Override
    public boolean isEncrypted() {
        return encrypted;
    }
    @Override
    public File getFile(){ return filePath.toFile(); }
    @Override
    public String toString(){
        return filePath.toString();
    }
}
