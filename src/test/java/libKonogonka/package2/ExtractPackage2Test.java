package libKonogonka.package2;

import libKonogonka.Converter;
import libKonogonka.fs.NCA.NCAProvider;
import libKonogonka.fs.RomFs.FileSystemEntry;
import libKonogonka.fs.RomFs.RomFsProvider;
import org.junit.jupiter.api.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.CRC32;

import static libKonogonka.Converter.byteArrToHexStringAsLE;

/* ..::::::::::::::::::::: # 1 :::::::::::::::::::::..
* This test validates (encrypted) package2 CRC32 equality and sizes match between reference values and
* 1. package2 from RomFS exported root
* 2. package2 from RomFS exported as stand-alone file
*  */

public class ExtractPackage2Test extends LKonPackage2Test {

    private static final String SYSTEM2_FAT_NCA_PATTERN = "0100000000000819";
    private static final String SYSTEM2_EXFAT_NCA_PATTERN = "010000000000081b";

    private static final String REFERENCE_FILE_PATH = File.separator+"romfs"+File.separator+"nx"+File.separator+"package2";
    private static final String OWN_FILE1_PATH = File.separator+"ROOT"+File.separator+"nx"+File.separator+"package2";
    private static final String OWN_FILE2_PATH = File.separator+"package2";

    @Disabled
    @DisplayName("Extract package2 test")
    @Test
    void testSystem2() throws Exception{
        String[] ncaFileNames = collectNcaFileNames();
        List<NCAProvider> ncaProviders = makeNcaProviders(ncaFileNames);

        NCAProvider system2FatNcaProvider = ncaProviders.stream()
                .filter(ncaProv -> is(ncaProv.getTitleId(), SYSTEM2_FAT_NCA_PATTERN))
                .findFirst().get();

        NCAProvider system2ExFatNcaProvider = ncaProviders.stream()
                .filter(ncaProv -> is(ncaProv.getTitleId(), SYSTEM2_EXFAT_NCA_PATTERN))
                .findFirst().get();

        System.out.println("\n" +
            "FAT   " + system2FatNcaProvider.getFile().getName() + "\n" +
            byteArrToHexStringAsLE(system2FatNcaProvider.getDecryptedKey0()) + "\n" +
            byteArrToHexStringAsLE(system2FatNcaProvider.getDecryptedKey1()) + "\n" +
            byteArrToHexStringAsLE(system2FatNcaProvider.getDecryptedKey2()) + "\n" +
            byteArrToHexStringAsLE(system2FatNcaProvider.getDecryptedKey3()) + "\n~ ~ ~ ~ ~ ~ ~ ~ ~ ~\n" +
            "ExFAT " + system2ExFatNcaProvider.getFile().getName() + "\n" +
            byteArrToHexStringAsLE(system2ExFatNcaProvider.getDecryptedKey0()) + "\n" +
            byteArrToHexStringAsLE(system2ExFatNcaProvider.getDecryptedKey1()) + "\n" +
            byteArrToHexStringAsLE(system2ExFatNcaProvider.getDecryptedKey2()) + "\n" +
            byteArrToHexStringAsLE(system2ExFatNcaProvider.getDecryptedKey3()));

        Assertions.assertTrue(system2FatNcaProvider.getFile().getName().endsWith("1212c.nca"));
        Assertions.assertTrue(system2ExFatNcaProvider.getFile().getName().endsWith("cc081.nca"));

        testExportedFiles(system2FatNcaProvider, exportFat, REFERENCE_FAT);
        testExportedFiles(system2ExFatNcaProvider, exportExFat, REFERENCE_EXFAT);
    }

    String[] collectNcaFileNames(){
        String[] ncaFileNames = new File(PATH_TO_FIRMWARE)
                .list((File directory, String file) -> ( ! file.endsWith(".cnmt.nca") && file.endsWith(".nca")));
        Assertions.assertNotNull(ncaFileNames);
        return ncaFileNames;
    }
    List<NCAProvider> makeNcaProviders(String[] ncaFileNames) throws Exception{
        List<NCAProvider> ncaProviders = new ArrayList<>();
        for (String ncaFileName : ncaFileNames){
            File nca = new File(PATH_TO_FIRMWARE +File.separator+ncaFileName);
            NCAProvider provider = new NCAProvider(nca, keyChainHolder.getRawKeySet());
            ncaProviders.add(provider);
        }

        Assertions.assertNotEquals(0, ncaProviders.size());

        return ncaProviders;
    }

    void testExportedFiles(NCAProvider system2NcaProvider, String exportToFolder, String referenceFilesFolder) throws Exception{
        Path referenceFilePath = Paths.get(referenceFilesFolder+REFERENCE_FILE_PATH);
        Path ownFilePath1 = Paths.get(exportToFolder+OWN_FILE1_PATH);
        Path ownFilePath2 = Paths.get(exportToFolder+OWN_FILE2_PATH);

        System.out.printf("\nReference : %s\nOwn #1    : %s\nOwn #2    : %s\n",
                referenceFilePath, ownFilePath1, ownFilePath2);

        RomFsProvider romFsProvider = system2NcaProvider.getNCAContentProvider(0).getRomfs();

        romFsProvider.exportContent(exportToFolder, romFsProvider.getRootEntry());
        long referenceCrc32 = calcCRC32(referenceFilePath);
        long ownFile1Crc32 = calcCRC32(ownFilePath1);
        Assertions.assertEquals(ownFile1Crc32, referenceCrc32);
        Assertions.assertEquals(Files.size(referenceFilePath), Files.size(ownFilePath1));

        FileSystemEntry package2FileSystemEntry = romFsProvider.getRootEntry().getContent()
                .stream()
                .filter(e -> e.getName().equals("nx"))
                .collect(Collectors.toList())
                .get(0)
                .getContent()
                .stream()
                .filter(e -> e.getName().equals("package2"))
                .collect(Collectors.toList())
                .get(0);

        romFsProvider.exportContent(exportToFolder, package2FileSystemEntry);
        long ownFile2Crc32 = calcCRC32(ownFilePath2);
        Assertions.assertEquals(ownFile2Crc32, referenceCrc32);
        Assertions.assertEquals(Files.size(referenceFilePath), Files.size(ownFilePath2));
    }

    long calcCRC32(Path package2Path) throws Exception{
        byte[] package2Bytes = Files.readAllBytes(package2Path);
        CRC32 crc32 = new CRC32();
        crc32.update(package2Bytes, 0, package2Bytes.length);
        return crc32.getValue();
    }
}
