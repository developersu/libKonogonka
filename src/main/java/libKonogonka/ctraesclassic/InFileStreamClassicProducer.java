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
import libKonogonka.ctraes.InFileStreamProducer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class InFileStreamClassicProducer implements IProducer {
    private boolean encrypted;

    private Path filePath;
    private InFileStreamProducer parentProducer;
    private long offset;
    private long encryptedStartOffset;
    private long encryptedEndOffset;
    private AesCtrDecryptClassic decryptor;
    private final long fileSize;

/** Reference AES-CTR stream producer.
 * @param filePath Path to file-container
 * @param offset Offset to skip (since file beginning).
 * @param encryptedStartOffset Offset since file beginning where encrypted section starts
 * @param encryptedEndOffset Offset since file beginning where encrypted section ends
 * @param key AES-CTR Key
 * @param iv CTR / IV (counter)
 */
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
        this.fileSize = Files.size(filePath);
    }
    private InFileStreamClassicProducer(Path filePath,
                                       long offset,
                                       long encryptedStartOffset,
                                       long encryptedEndOffset, //Files.size(filePath)
                                       AesCtrDecryptClassic decryptor) throws Exception{
        this.encrypted = true;
        this.filePath = filePath;
        this.offset = offset;
        this.encryptedStartOffset = encryptedStartOffset;
        this.encryptedEndOffset = encryptedEndOffset;
        this.decryptor = decryptor;
        this.fileSize = Files.size(filePath);
    }
    /** Stream producer for non-encrypted files.
     * @param filePath Path to file-container
     * */
    public InFileStreamClassicProducer(Path filePath) throws Exception{
        this.filePath = filePath;
        this.fileSize = Files.size(filePath);
    }
    /** Stream producer for non-encrypted files.
     * @param filePath Path to file-container
     * @param offset Offset to skip (since file beginning).
     * */
    public InFileStreamClassicProducer(Path filePath, long offset) throws Exception{
        this.filePath = filePath;
        this.offset = offset;
        this.fileSize = Files.size(filePath);
    }
    /** Reference AES-CTR stream producer that utilizes InFileStreamProducer instead of file.
     * @param parentProducer Producer of the stream
     * @param offset Offset to skip at parent stream
     * @param encryptedStartOffset Offset since parent stream start at stream where encrypted section starts
     * @param encryptedEndOffset Offset since parent stream start at stream where encrypted section ends
     * @param key AES-CTR Key
     * @param iv CTR / IV (counter)
     */
    public InFileStreamClassicProducer(InFileStreamProducer parentProducer,
                                       long offset,
                                       long encryptedStartOffset,
                                       long encryptedEndOffset,
                                       String key,
                                       byte[] iv,
                                       long fileSize) throws Exception{
        this.parentProducer = parentProducer;
        this.encrypted = true;
        this.offset = offset;
        this.encryptedStartOffset = encryptedStartOffset;
        this.encryptedEndOffset = encryptedEndOffset;
        this.decryptor = new AesCtrDecryptClassic(key, iv);
        this.fileSize = fileSize;
    }
    private InFileStreamClassicProducer(InFileStreamProducer parentProducer,
                                       long offset,
                                       long encryptedStartOffset,
                                       long encryptedEndOffset,
                                       AesCtrDecryptClassic decryptor,
                                       long fileSize){
        this.parentProducer = parentProducer;
        this.encrypted = true;
        this.offset = offset;
        this.encryptedStartOffset = encryptedStartOffset;
        this.encryptedEndOffset = encryptedEndOffset;
        this.decryptor = decryptor;
        this.fileSize = fileSize;
    }

    @Override
    public BufferedInputStream produce() throws Exception{
        if (encrypted)
            return produceAesCtr();
        return produceNotEncrypted();
    }

    private BufferedInputStream produceAesCtr() throws Exception{
        decryptor.reset();

        InputStream is;

        if (filePath == null)
            is = parentProducer.produce();
        else
            is = Files.newInputStream(filePath);

        AesCtrClassicBufferedInputStream stream = new AesCtrClassicBufferedInputStream(
                decryptor, encryptedStartOffset, encryptedEndOffset, is, fileSize);

        if (offset != stream.skip(offset))
            throw new Exception("Unable to skip offset: "+offset);

        return stream;
    }

    private BufferedInputStream produceNotEncrypted() throws Exception{
        BufferedInputStream stream;

        if (filePath == null)
            stream = new BufferedInputStream(parentProducer.produce());
        else
            stream = new BufferedInputStream(Files.newInputStream(filePath));

        if (offset != stream.skip(offset))
            throw new Exception("Unable to skip offset: "+offset);

        return stream;
    }
    @Override
    public InFileStreamClassicProducer getSuccessor(long offset) throws Exception{
        if (! encrypted)
            return new InFileStreamClassicProducer(filePath, offset);

        if (filePath == null)
            return new InFileStreamClassicProducer(parentProducer, offset, encryptedStartOffset, encryptedEndOffset, decryptor, fileSize);
        return new InFileStreamClassicProducer(filePath, offset, encryptedStartOffset, encryptedEndOffset, decryptor);
    }

    public InFileStreamClassicProducer getSuccessor(long offset, boolean incrementExisting) throws Exception{
        if (incrementExisting)
            return getSuccessor(this.offset + offset);
        return getSuccessor(offset);
    }

    @Override
    public boolean isEncrypted() {
        return encrypted;
    }
    @Override
    public File getFile(){
        if (filePath == null)
            return parentProducer.getFile();
        return filePath.toFile();
    }
    @Override
    public String toString(){
        if (filePath == null)
            return parentProducer.getFile().getAbsolutePath();
        return filePath.toString();
    }
}
