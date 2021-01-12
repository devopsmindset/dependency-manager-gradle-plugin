package org.devopsmindset.gradle.compress;

import net.lingala.zip4j.ZipFile;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.*;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;

import static org.apache.commons.compress.utils.FileNameUtils.getExtension;

public class CompressionUtils {

    public static final String ZIP = "zip";
    public static final String TAR_GZ = "tar.gz";
    public static final String TGZ = "tgz";

    public static void unzipFolderZip4j(Path source, Path target) throws IOException {
        new ZipFile(source.toFile()).extractAll(target.toString());
    }

    public static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }

    public static void unTarFile(File tarFile, File destFile) {
        try (
                FileInputStream fis = new FileInputStream(tarFile);
                // .gz
                GZIPInputStream gzipInputStream = new GZIPInputStream(new BufferedInputStream(fis));
                //.tar.gz
                TarArchiveInputStream tis = new TarArchiveInputStream(gzipInputStream)
        ) {
            TarArchiveEntry tarEntry;
            while ((tarEntry = tis.getNextTarEntry()) != null) {
                if (!tarEntry.isDirectory()) {
                    // In case entry is for file ensure parent directory is in place
                    // and write file content to Output Stream
                    File outputFile = new File(destFile, tarEntry.getName());
                    outputFile.getParentFile().mkdirs();
                    FileOutputStream fos = new FileOutputStream(outputFile);
                    IOUtils.copy(tis, fos);
                    fos.close();
                }
            }
        } catch (IOException ex) {
            System.out.println("Error while untarring a file- " + ex.getMessage());
        }
    }

    public static void extract(File origin, File destination) throws Exception {
        if (getExtension(origin.toString()).equalsIgnoreCase(ZIP)) {
            CompressionUtils.unzipFolderZip4j(origin.toPath(), destination.toPath());
        } else {
            if (getExtension(origin.toString()).equalsIgnoreCase(TAR_GZ) ||
                    getExtension(origin.toString()).equalsIgnoreCase(TGZ)) {
                CompressionUtils.unTarFile(origin, destination);
            }
        }
    }

}
