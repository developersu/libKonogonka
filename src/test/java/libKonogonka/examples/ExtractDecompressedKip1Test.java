package libKonogonka.examples;

import libKonogonka.Converter;
import libKonogonka.KeyChainHolder;
import libKonogonka.fs.NCA.NCAProvider;
import libKonogonka.fs.RomFs.FileSystemEntry;
import libKonogonka.fs.RomFs.RomFsProvider;
import libKonogonka.fs.other.System2.System2Provider;
import libKonogonka.fs.other.System2.ini1.KIP1Provider;
import libKonogonka.aesctr.InFileStreamProducer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ExtractDecompressedKip1Test {
    final String KEYS_FILE_LOCATION = "FilesForTests"+ File.separator+"prod.keys";
    final String XCI_HEADER_KEYS_FILE_LOCATION = "FilesForTests"+File.separator+"xci_header_key.txt";

    final String pathToFirmwares = "/tmp";

    private static KeyChainHolder keyChainHolder;

    @Disabled
    @DisplayName("Extract FS.kip1")
    @Test
    void testSystem2() throws Exception{
        makeKeys();
        File firmwaresDir = new File(pathToFirmwares);
        Assertions.assertNotNull(firmwaresDir);
        File[] fwDirs = new File(pathToFirmwares).listFiles((file, s) -> (s.matches("^Firmware (9\\.|[0-9][0-9]\\.).*") && ! s.endsWith(".zip")));
        //File[] fwDirs = new File(pathToFirmwares).listFiles((file, s) -> s.equals("Firmware 14.1.2"));

        for (File fw : fwDirs) {
            if (fw.isFile())
                continue;
            System.out.println("\t\t\t==== "+fw.getName()+" ====");
            String fwAbsolutePath = fw.getAbsolutePath();
            iterate(fwAbsolutePath,
                    System.getProperty("java.io.tmpdir")+File.separator+fw.getName()+File.separator+"FAT",
                    System.getProperty("java.io.tmpdir")+File.separator+fw.getName()+File.separator+"ExFAT");
        }
    }
    void iterate(String pathToFirmware, String exportFat, String exportExFat){
        try {
            String[] ncaFileNames = collectNcaFileNames(pathToFirmware);
            List<NCAProvider> ncaProviders = makeNcaProviders(ncaFileNames, pathToFirmware);

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

            System.out.println("FAT   " + system2FatNcaProvider.getFile().getName() + "\t" + exportFat);
            System.out.println("ExFAT " + system2ExFatNcaProvider.getFile().getName() + "\t" + exportExFat);

            testExportedFiles(system2FatNcaProvider, exportFat);
            testExportedFiles(system2ExFatNcaProvider, exportExFat);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    void makeKeys() throws Exception{
        String keyValue = new String(Files.readAllBytes(Paths.get(XCI_HEADER_KEYS_FILE_LOCATION))).trim();
        Assertions.assertNotEquals(0, keyValue.length());
        keyChainHolder = new KeyChainHolder(KEYS_FILE_LOCATION, keyValue);
    }
    String[] collectNcaFileNames(String pathToFirmware){
        File firmware = new File(pathToFirmware);
        Assertions.assertTrue(firmware.exists());
        String[] ncaFileNames = firmware.list((File directory, String file) -> ( ! file.endsWith(".cnmt.nca") && file.endsWith(".nca")));
        Assertions.assertNotNull(ncaFileNames);
        return ncaFileNames;
    }
    List<NCAProvider> makeNcaProviders(String[] ncaFileNames, String pathToFirmware) throws Exception{
        List<NCAProvider> ncaProviders = new ArrayList<>();
        for (String ncaFileName : ncaFileNames){
            File nca = new File(pathToFirmware+File.separator+ncaFileName);
            NCAProvider provider = new NCAProvider(nca, keyChainHolder.getRawKeySet());
            ncaProviders.add(provider);
        }

        Assertions.assertNotEquals(0, ncaProviders.size());

        return ncaProviders;
    }

    void testExportedFiles(NCAProvider system2NcaProvider, String exportIntoFolder) throws Exception{
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

        InFileStreamProducer producer = romFsProvider.getStreamProducer(package2FileSystemEntry);
        System2Provider providerStream = new System2Provider(producer, keyChainHolder);
        for (KIP1Provider kip1Provider : providerStream.getIni1Provider().getKip1List()){
            String kip1Name = kip1Provider.getHeader().getName();
            if (kip1Name.equals("FS")) {
                System.out.println("Exported: "+kip1Provider.exportAsDecompressed(exportIntoFolder)+"\n"+exportIntoFolder);
                break;
            }
        }
    }
}
