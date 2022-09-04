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
package libKonogonka.Tools.RomFs;

import libKonogonka.ctraes.AesCtrDecryptSimple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.PipedOutputStream;
import java.io.RandomAccessFile;

public class RomFsEncryptedContentRetrieve implements Runnable{
    private final static Logger log = LogManager.getLogger(RomFsEncryptedContentRetrieve.class);

    private final File parentFile;
    private final PipedOutputStream streamOut;
    private final long absoluteOffsetPosition;
    private final AesCtrDecryptSimple decryptor;
    private final long internalFileOffset;
    private final long internalFileSize;
    private final long level6Offset;
    private final long headersFileDataOffset;

    RomFsEncryptedContentRetrieve(File parentFile,
                                  PipedOutputStream streamOut,
                                  long absoluteOffsetPosition,
                                  AesCtrDecryptSimple decryptor,
                                  long internalFileOffset,
                                  long internalFileSize,
                                  long level6Offset,
                                  long headersFileDataOffset
                                  ){
        this.parentFile = parentFile;
        this.absoluteOffsetPosition = absoluteOffsetPosition;
        this.streamOut = streamOut;
        this.decryptor = decryptor;
        this.internalFileOffset = internalFileOffset;
        this.internalFileSize = internalFileSize;
        this.level6Offset = level6Offset;
        this.headersFileDataOffset = headersFileDataOffset;
    }

    @Override
    public void run() {
        log.trace("Executing thread");
        try {
            byte[] encryptedBlock;
            byte[] decryptedBlock;

            RandomAccessFile raf = new RandomAccessFile(parentFile, "r");

            //0
            long startBlock = (internalFileOffset + headersFileDataOffset) / 0x200;

            decryptor.skipNext(level6Offset / 0x200 + startBlock);

            // long absoluteOffsetPosition = romFsOffsetPosition + (mediaStartOffset * 0x200); // calculated in constructor

            raf.seek(absoluteOffsetPosition + level6Offset + startBlock * 0x200);

            //1
            long ignoreBytes = (internalFileOffset + headersFileDataOffset) - startBlock * 0x200;

            if (ignoreBytes > 0) {
                encryptedBlock = new byte[0x200];
                if (raf.read(encryptedBlock) == 0x200) {
                    decryptedBlock = decryptor.decryptNext(encryptedBlock);
                    // If we have extra-small file that is less than a block and even more
                    if ((0x200 - ignoreBytes) > internalFileSize){
                        streamOut.write(decryptedBlock, (int)ignoreBytes, (int) internalFileSize);    // safe cast
                        raf.close();
                        streamOut.close();
                        return;
                    }
                    else {
                        streamOut.write(decryptedBlock, (int) ignoreBytes, 0x200 - (int) ignoreBytes);
                    }
                }
                else {
                    throw new Exception("Unable to get 512 bytes from 1st bock");
                }
                startBlock++;
            }
            long endBlock = (internalFileSize + ignoreBytes) / 0x200 + startBlock;  // <- pointing to place where any data related to this media-block ends

            //2
            int extraData = (int) ((endBlock - startBlock)*0x200 - (internalFileSize + ignoreBytes));

            if (extraData < 0)
                endBlock--;
            //3
            while ( startBlock < endBlock ) {
                encryptedBlock = new byte[0x200];
                if (raf.read(encryptedBlock) == 0x200) {
                    decryptedBlock = decryptor.decryptNext(encryptedBlock);
                    streamOut.write(decryptedBlock);
                }
                else
                    throw new Exception("Unable to get 512 bytes from block");

                startBlock++;
            }

            //4
            if (extraData != 0){                 // In case we didn't get what we want
                encryptedBlock = new byte[0x200];
                if (raf.read(encryptedBlock) == 0x200) {
                    decryptedBlock = decryptor.decryptNext(encryptedBlock);
                    streamOut.write(decryptedBlock, 0, Math.abs(extraData));
                }
                else
                    throw new Exception("Unable to get 512 bytes from block");
            }
            raf.close();
            streamOut.close();
        } catch (Exception exception) {
            log.error("Unable to provide stream", exception);
        }
        log.trace("Thread died");
    }
}
