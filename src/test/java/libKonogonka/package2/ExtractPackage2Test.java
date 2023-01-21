package libKonogonka.package2;

import libKonogonka.Converter;
import libKonogonka.KeyChainHolder;
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

/* ..::::::::::::::::::::: # 1 :::::::::::::::::::::..
* This test validates (encrypted) package2 CRC32 equality and sizes match between reference values and
* 1. package2 from RomFS exported root
* 2. package2 from RomFS exported as stand-alone file
*  */

public class ExtractPackage2Test {
    final String KEYS_FILE_LOCATION = "FilesForTests"+File.separator+"prod.keys";
    final String XCI_HEADER_KEYS_FILE_LOCATION = "FilesForTests"+File.separator+"xci_header_key.txt";

    final String pathToFirmware = "FilesForTests"+File.separator+"Firmware 14.1.0";

    private static KeyChainHolder keyChainHolder;

    final String referenceFat = "FilesForTests"+File.separator+"reference_for_system2"+File.separator+"FAT";
    final String referenceExFat = "FilesForTests"+File.separator+"reference_for_system2"+File.separator+"ExFAT";
    final String exportFat = System.getProperty("java.io.tmpdir")+File.separator+"Exported_FAT"+File.separator+getClass().getSimpleName();
    final String exportExFat = System.getProperty("java.io.tmpdir")+File.separator+"Exported_ExFAT"+File.separator+getClass().getSimpleName();

    @DisplayName("Extract package2 test")
    @Test
    void testSystem2() throws Exception{
        makeKeys();
        String[] ncaFileNames = collectNcaFileNames();
        List<NCAProvider> ncaProviders = makeNcaProviders(ncaFileNames);

        NCAProvider system2FatNcaProvider = null;
        NCAProvider system2ExFatNcaProvider = null;

        for (NCAProvider ncaProvider : ncaProviders) {
            String titleId = Converter.byteArrToHexStringAsLE(ncaProvider.getTitleId());
            if (titleId.equals("0100000000000819"))
                system2FatNcaProvider = ncaProvider;
            else if (titleId.equals("010000000000081b"))
                system2ExFatNcaProvider = ncaProvider;
        }

        Assertions.assertNotNull(system2FatNcaProvider);
        Assertions.assertNotNull(system2ExFatNcaProvider);

        System.out.println("FAT   " + system2FatNcaProvider.getFile().getName());
        System.out.println("ExFAT " + system2ExFatNcaProvider.getFile().getName());

        Assertions.assertTrue(system2FatNcaProvider.getFile().getName().endsWith("1212c.nca"));
        Assertions.assertTrue(system2ExFatNcaProvider.getFile().getName().endsWith("cc081.nca"));

        testExportedFiles(system2FatNcaProvider, exportFat, referenceFat);
        testExportedFiles(system2ExFatNcaProvider, exportExFat, referenceExFat);
    }
    void makeKeys() throws Exception{
        String keyValue = new String(Files.readAllBytes(Paths.get(XCI_HEADER_KEYS_FILE_LOCATION))).trim();
        Assertions.assertNotEquals(0, keyValue.length());
        keyChainHolder = new KeyChainHolder(KEYS_FILE_LOCATION, keyValue);
    }
    String[] collectNcaFileNames(){
        File firmware = new File(pathToFirmware);
        Assertions.assertTrue(firmware.exists());
        String[] ncaFileNames = firmware.list((File directory, String file) -> ( ! file.endsWith(".cnmt.nca") && file.endsWith(".nca")));
        Assertions.assertNotNull(ncaFileNames);
        return ncaFileNames;
    }
    List<NCAProvider> makeNcaProviders(String[] ncaFileNames) throws Exception{
        List<NCAProvider> ncaProviders = new ArrayList<>();
        for (String ncaFileName : ncaFileNames){
            File nca = new File(pathToFirmware+File.separator+ncaFileName);
            NCAProvider provider = new NCAProvider(nca, keyChainHolder.getRawKeySet());
            ncaProviders.add(provider);
        }

        Assertions.assertNotEquals(0, ncaProviders.size());

        return ncaProviders;
    }

    void testExportedFiles(NCAProvider system2NcaProvider, String exportIntoFolder, String referenceFilesFolder) throws Exception{
        RomFsProvider romFsProvider = system2NcaProvider.getNCAContentProvider(0).getRomfs();

        Path referenceFilePath = Paths.get(referenceFilesFolder+File.separator+"romfs"+File.separator+"nx"+File.separator+"package2");
        Path myFilePath1 = Paths.get(exportIntoFolder+File.separator+"ROOT"+File.separator+"nx"+File.separator+"package2");
        Path myFilePath2 = Paths.get(exportIntoFolder+File.separator+"package2");

        System.out.println();
        System.out.println("Reference : " + referenceFilePath);
        System.out.println("Own #1    : " + myFilePath1);
        System.out.println("Own #2    : " + myFilePath2);

        romFsProvider.exportContent(exportIntoFolder, romFsProvider.getRootEntry());
        long referenceCrc32 = calculateReferenceCRC32(referenceFilePath);
        validateChecksums(myFilePath1, referenceCrc32);
        validateSizes(referenceFilePath, myFilePath1);

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

        romFsProvider.exportContent(exportIntoFolder, package2FileSystemEntry);
        validateChecksums(myFilePath2, referenceCrc32);
        validateSizes(referenceFilePath, myFilePath2);
    }
    long calculateReferenceCRC32(Path refPackage2Path) throws Exception{
        byte[] refPackage2Bytes = Files.readAllBytes(refPackage2Path);
        CRC32 crc32 = new CRC32();
        crc32.update(refPackage2Bytes, 0, refPackage2Bytes.length);
        return crc32.getValue();
    }

    void validateChecksums(Path myPackage2Path, long refPackage2Crc32) throws Exception{
        // Check CRC32 for package2 file only
        byte[] myPackage2Bytes = Files.readAllBytes(myPackage2Path);
        CRC32 crc32 = new CRC32();
        crc32.update(myPackage2Bytes, 0, myPackage2Bytes.length);
        long myPackage2Crc32 = crc32.getValue();
        Assertions.assertEquals(myPackage2Crc32, refPackage2Crc32);
    }

    void validateSizes(Path a, Path b) throws Exception{
        Assertions.assertEquals(Files.size(a), Files.size(b));
    }
}
