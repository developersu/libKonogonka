package libKonogonka.package2;

import libKonogonka.Converter;
import libKonogonka.fs.NCA.NCAProvider;
import libKonogonka.fs.RomFs.FileSystemEntry;
import libKonogonka.fs.RomFs.RomFsProvider;
import libKonogonka.fs.other.System2.System2Provider;
import libKonogonka.aesctr.InFileStreamProducer;
import org.junit.jupiter.api.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.CRC32;

/* ..::::::::::::::::::::: # 2 :::::::::::::::::::::..
* This test validates Kernel.bin CRC32 equality and sizes match between reference values and
* 1. Kernel.bin extracted from 'package2' file
* 2. Kernel.bin extracted from NCA file via streams
*  */

public class KernelBinExtractTest extends LKonPackage2Test {
    private static final String SYSTEM2_FAT_NCA_PATTERN = "0100000000000819";
    private static final String SYSTEM2_EXFAT_NCA_PATTERN = "010000000000081b";

    @DisplayName("Kernel.bin extract test")
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

        Path referenceFilePath = Paths.get(referenceFilesFolder+File.separator+"package2"+File.separator+"Kernel.bin");
        Path myFilePath = Paths.get(exportIntoFolder+File.separator+"Kernel.bin");

        System.out.println("\n" +
            "\nReference : " + referenceFilePath +
            "\nOwn       : " + myFilePath);
        long referenceCrc32 = calculateReferenceCRC32(referenceFilePath);

        romFsProvider.exportContent(exportIntoFolder, package2FileSystemEntry);
        System2Provider providerFile = new System2Provider(exportIntoFolder+File.separator+"package2", keyChainHolder);
        providerFile.exportKernel(exportIntoFolder);
        validateChecksums(myFilePath, referenceCrc32);
        validateSizes(referenceFilePath, myFilePath);

        InFileStreamProducer producer = romFsProvider.getStreamProducer(package2FileSystemEntry);
        System2Provider providerStream = new System2Provider(producer, keyChainHolder);
        providerStream.exportKernel(exportIntoFolder);
        validateChecksums(myFilePath, referenceCrc32);
        validateSizes(referenceFilePath, myFilePath);
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
