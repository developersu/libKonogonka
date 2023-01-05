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
package libKonogonka.Tools.NCA;

import libKonogonka.Tools.NCA.NCASectionTableBlock.NcaFsHeader;
import libKonogonka.Tools.PFS0.PFS0Provider;
import libKonogonka.Tools.RomFs.RomFsProvider;
import libKonogonka.ctraes.AesCtrBufferedInputStream;
import libKonogonka.ctraes.AesCtrDecryptForMediaBlocks;
import libKonogonka.ctraes.InFileStreamProducer;
import libKonogonka.exceptions.EmptySectionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class NCAContent {
    private final static Logger log = LogManager.getLogger(NCAContent.class);

    private final File file;
    private final long ncaOffsetPosition;
    private final NcaFsHeader ncaFsHeader;
    private final NCAHeaderTableEntry ncaHeaderTableEntry;
    private final byte[] decryptedKey;

    private PFS0Provider pfs0;
    private RomFsProvider romfs;

    // TODO: if decryptedKey is empty, throw exception?
    public NCAContent(File file,
                      long ncaOffsetPosition,
                      NcaFsHeader ncaFsHeader,
                      NCAHeaderTableEntry ncaHeaderTableEntry,
                      byte[] decryptedKey) throws Exception
    {
        this.file = file;
        this.ncaOffsetPosition = ncaOffsetPosition;
        this.ncaFsHeader = ncaFsHeader;
        this.ncaHeaderTableEntry = ncaHeaderTableEntry;
        this.decryptedKey = decryptedKey;
        // If nothing to do
        if (ncaHeaderTableEntry.getMediaEndOffset() == 0)
            throw new EmptySectionException("Empty section");
        // If it's PFS0Provider
        if (ncaFsHeader.getSuperBlockPFS0() != null)
            this.proceedPFS0();
        else if (ncaFsHeader.getSuperBlockIVFC() != null)
            this.proceedRomFs();
        else
            throw new Exception("NCAContent(): Not supported. PFS0 or RomFS supported only.");
    }

    private void proceedPFS0() throws Exception {
        switch (ncaFsHeader.getCryptoType()){
            case 0x01:   // IF NO ENCRYPTION
                proceedPFS0NotEncrypted();
                break;
            case 0x03:
                proceedPFS0Encrypted();
                break;
            default:
                throw new Exception("'Crypto type' not supported: "+ncaFsHeader.getCryptoType());
        }
    }
    private void proceedPFS0NotEncrypted() throws Exception{
        InFileStreamProducer producer = new InFileStreamProducer(file); // no need to bypass ncaOffsetPosition!
        pfs0 = new PFS0Provider(producer,
                makeOffsetPositionInFile(),
                ncaFsHeader.getSuperBlockPFS0(),
                ncaHeaderTableEntry.getMediaStartOffset());
    }

    private void proceedPFS0Encrypted() throws Exception{
        pfs0 = new PFS0Provider(makeEncryptedProducer(), makeOffsetPositionInFile(), ncaFsHeader.getSuperBlockPFS0(),
                ncaHeaderTableEntry.getMediaStartOffset());
    }

    private void proceedRomFs() throws Exception{
        switch (ncaFsHeader.getCryptoType()){
            case 0x01:
                proceedRomFsNotEncrypted();
                break;
            case 0x03:
                proceedRomFsEncrypted();
                break;
            default:
                throw new Exception("Non-supported 'Crypto type' "+ncaFsHeader.getCryptoType());
        }
    }
    private void proceedRomFsNotEncrypted(){   // TODO: Clarify, implement if needed
        log.error("proceedRomFs() -> proceedRomFsNotEncrypted() is not implemented :(");
    }
    private void proceedRomFsEncrypted() throws Exception{
        if (decryptedKey == null)
            throw new Exception("CryptoSection03: unable to proceed. No decrypted key provided.");

        this.romfs = new RomFsProvider(makeEncryptedProducer(), ncaFsHeader.getSuperBlockIVFC().getLvl6Offset(),
                makeOffsetPositionInFile(), ncaHeaderTableEntry.getMediaStartOffset());
    }
    public PFS0Provider getPfs0() { return pfs0; }
    public RomFsProvider getRomfs() { return romfs; }

    private InFileStreamProducer makeEncryptedProducer() throws Exception{
        AesCtrDecryptForMediaBlocks decryptor = new AesCtrDecryptForMediaBlocks(decryptedKey, ncaFsHeader.getSectionCTR(),
                ncaHeaderTableEntry.getMediaStartOffset() * 0x200);
        return new InFileStreamProducer(file, ncaOffsetPosition, 0, decryptor,
                ncaHeaderTableEntry.getMediaStartOffset(), ncaHeaderTableEntry.getMediaEndOffset());
    }
    private long makeOffsetPositionInFile(){
        return ncaOffsetPosition + ncaHeaderTableEntry.getMediaStartOffset() * 0x200;
    }
    /**
     * Export NCA content AS IS.
     * Not so good for PFS0 since there are SHAs list that discourages but good for 'romfs' and things like that
     * */
    public boolean exportMediaBlock(String saveToLocation){
        File location = new File(saveToLocation);
        location.mkdirs();
        BufferedInputStream stream;

        long mediaStartOffset = ncaHeaderTableEntry.getMediaStartOffset();
        long mediaEndOffset = ncaHeaderTableEntry.getMediaEndOffset();
        long mediaBlocksCount = mediaEndOffset - mediaStartOffset;

        try (BufferedOutputStream extractedFileBOS = new BufferedOutputStream(
                Files.newOutputStream(Paths.get(saveToLocation+File.separator+file.getName()+"_MediaBlock.bin")))){
            if(ncaFsHeader.getCryptoType()==0x01){
                stream = new BufferedInputStream(Files.newInputStream(file.toPath()));
            }
            else if(ncaFsHeader.getCryptoType()==0x03) {
                AesCtrDecryptForMediaBlocks decryptor = new AesCtrDecryptForMediaBlocks(decryptedKey,
                        ncaFsHeader.getSectionCTR(),
                        mediaStartOffset * 0x200);

                stream = new AesCtrBufferedInputStream(decryptor,
                        ncaOffsetPosition,
                        mediaStartOffset,
                        mediaEndOffset,
                        Files.newInputStream(file.toPath()),
                        Files.size(file.toPath()));
            }
            else
                throw new Exception("Crypto type not supported");

            for (int i = 0; i < mediaBlocksCount; i++){
                byte[] block = new byte[0x200];
                if (0x200 != stream.read(block))
                    throw new Exception("Read failure");
                extractedFileBOS.write(block);
            }
            stream.close();
        } catch (Exception e) {
            log.error("Failed to export MediaBlock", e);
            return false;
        }
        return true;
    }

    public long getRawDataContentSize(){
        return (ncaHeaderTableEntry.getMediaEndOffset() - ncaHeaderTableEntry.getMediaStartOffset()) * 0x200;
    }
    public String getFileName(){
        return file.getName();
    }
}