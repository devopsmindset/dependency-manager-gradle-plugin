package org.devopsmindset.gradle.compress;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.*;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;

import static org.apache.commons.compress.utils.FileNameUtils.getExtension;

/**
 * Compression utils class
 */
@UtilityClass
@Slf4j
public class CompressionUtils {

    /**
     * ZIP
     */
    public static final String ZIP = "zip";
    /**
     * TAR GZ
     */
    public static final String TAR_GZ = "tar.gz";
    /**
     * TGZ
     */
    public static final String TGZ = "tgz";

    /**
     * Unzips a folder with Zip4j
     * @param source source path
     * @param target target path
     * @throws IOException exception when file is not found
     */
    public static void unzipFolderZip4j(Path source, Path target) throws IOException {
        new ZipFile(source.toFile()).extractAll(target.toString());
    }

    /**
     * Creates a new file in the destination folder
     * @param destinationDir destination folder
     * @param zipEntry zip file
     * @return the newly created file
     * @throws IOException throws an exception if the destination is outside of the target directory.
     */
    public static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }

    /**
     * Unzips a file with tar
     * @param tarFile tar file
     * @param destFile destination file
     */
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
            log.error("Error while untarring a file- {}", ex.getMessage());
        }
    }

    /**
     * Unzips a file depending on the extension
     * @param origin source file
     * @param destination destination folder
     * @throws Exception throws an exception if the file is not found
     */
    public static void extract(File origin, File destination) throws Exception {
        if (getExtension(origin.toString()).equalsIgnoreCase(ZIP)) {
            CompressionUtils.unzipFolderZip4j(origin.toPath(), destination.toPath());
        } else   {
            if (getExtension(origin.toString()).equalsIgnoreCase(TAR_GZ) ||
                    getExtension(origin.toString()).equalsIgnoreCase(TGZ)) {
                CompressionUtils.unTarFile(origin, destination);
            }
        }
    }

}
