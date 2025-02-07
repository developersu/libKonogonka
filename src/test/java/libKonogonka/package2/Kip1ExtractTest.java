package libKonogonka.package2;

import libKonogonka.Converter;
import libKonogonka.fs.NCA.NCAProvider;
import libKonogonka.fs.RomFs.FileSystemEntry;
import libKonogonka.fs.RomFs.RomFsProvider;
import libKonogonka.fs.other.System2.System2Provider;
import libKonogonka.fs.other.System2.ini1.Ini1Provider;
import libKonogonka.fs.other.System2.ini1.KIP1Provider;
import libKonogonka.aesctr.InFileStreamProducer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.CRC32;

/* ..::::::::::::::::::::: # 4 :::::::::::::::::::::..
* This test validates KIP1 CRC32 equality and sizes match between reference values and
* 1. KIP1 extracted from INI1.bin file
* 2. KIP1 extracted from NCA file via streams
*  */

public class Kip1ExtractTest extends LKonPackage2Test {
    private static final String SYSTEM2_FAT_NCA_PATTERN = "0100000000000819";
    private static final String SYSTEM2_EXFAT_NCA_PATTERN = "010000000000081b";

    private static final String REFERENCE_FILE_PATH = File.separator+"ini1_extracted"+File.separator+"FS.kip1";
    private static final String OWN_FILE_PATH = File.separator+"FS.kip1";

    @DisplayName("KIP1 extract test (case 'FS')")
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

        System.out.printf("FAT   %s\nExFAT %s\n",
                system2FatNcaProvider.getFile().getName(), system2ExFatNcaProvider.getFile().getName());

        Assertions.assertTrue(system2FatNcaProvider.getFile().getName().endsWith("1212c.nca"));
        Assertions.assertTrue(system2ExFatNcaProvider.getFile().getName().endsWith("cc081.nca"));

        testExportedFiles(system2FatNcaProvider, exportFat, REFERENCE_FAT);
        testExportedFiles(system2ExFatNcaProvider, exportExFat, REFERENCE_EXFAT);
    }

    String[] collectNcaFileNames(){
        File firmware = new File(PATH_TO_FIRMWARE);
        Assertions.assertTrue(firmware.exists());
        String[] ncaFileNames = firmware.list((File directory, String file) -> ( ! file.endsWith(".cnmt.nca") && file.endsWith(".nca")));
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

    void testExportedFiles(NCAProvider system2NcaProvider, String exportIntoFolder, String referenceFilesFolder) throws Exception{
        RomFsProvider romFsProvider = system2NcaProvider.getNCAContentProvider(0).getRomfs();

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

        Path referenceFilePath = Paths.get(referenceFilesFolder+REFERENCE_FILE_PATH);
        Path ownFilePath = Paths.get(exportIntoFolder+OWN_FILE_PATH);

        System.out.printf("\nReference : %s\nOwn       : %s\n", referenceFilePath, ownFilePath);
        long referenceCrc32 = calcCRC32(referenceFilePath);

        romFsProvider.exportContent(exportIntoFolder, package2FileSystemEntry);
        System2Provider kernelProviderFile = new System2Provider(exportIntoFolder+File.separator+"package2", keyChainHolder);
        kernelProviderFile.getIni1Provider().export(exportIntoFolder);
        Ini1Provider ini1Provider = new Ini1Provider(Paths.get(exportIntoFolder+File.separator+"INI1.bin"));
        for (KIP1Provider kip1Provider : ini1Provider.getKip1List())
            kip1Provider.export(exportIntoFolder);
        long ownCrc32 = calcCRC32(ownFilePath);
        Assertions.assertEquals(ownCrc32, referenceCrc32);
        Assertions.assertEquals(Files.size(referenceFilePath), Files.size(ownFilePath));

        InFileStreamProducer producer = romFsProvider.getStreamProducer(package2FileSystemEntry);
        System2Provider providerStream = new System2Provider(producer, keyChainHolder);
        for (KIP1Provider kip1Provider : providerStream.getIni1Provider().getKip1List())
            kip1Provider.export(exportIntoFolder);
        ownCrc32 = calcCRC32(ownFilePath);
        Assertions.assertEquals(ownCrc32, referenceCrc32);
        Assertions.assertEquals(Files.size(referenceFilePath), Files.size(ownFilePath));
    }
    long calcCRC32(Path package2Path) throws Exception{
        byte[] package2Bytes = Files.readAllBytes(package2Path);
        CRC32 crc32 = new CRC32();
        crc32.update(package2Bytes, 0, package2Bytes.length);
        return crc32.getValue();
    }
}
