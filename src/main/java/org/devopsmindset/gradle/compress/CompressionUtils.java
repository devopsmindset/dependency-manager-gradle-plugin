package org.devopsmindset.gradle.compress;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class CompressionUtils {

    public static void unZipFile(File fileZip, File destDir) throws IOException {
        destDir.mkdirs();

        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(new FileInputStream(fileZip.toString()));
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            File newFile = newFile(destDir, zipEntry);
            FileOutputStream fos = new FileOutputStream(newFile);
            int len;
            while ((len = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
            fos.close();
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();
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

    public static void unTarFile(File tarFile, File destFile) throws Exception {
        TarArchiveInputStream tis = null;
        try {
            FileInputStream fis = new FileInputStream(tarFile);
            // .gz
            GZIPInputStream gzipInputStream = new GZIPInputStream(new BufferedInputStream(fis));
            //.tar.gz
            tis = new TarArchiveInputStream(gzipInputStream);
            TarArchiveEntry tarEntry = null;
            while ((tarEntry = tis.getNextTarEntry()) != null) {
                if (tarEntry.isDirectory()) {
                    continue;
                } else {
                    // In case entry is for file ensure parent directory is in place
                    // and write file content to Output Stream
                    File outputFile = new File(destFile,tarEntry.getName());
                    outputFile.getParentFile().mkdirs();
                    FileOutputStream fos = new FileOutputStream(outputFile);
                    IOUtils.copy(tis, fos);
                    fos.close();
                }
            }
        }catch(IOException ex) {
            System.out.println("Error while untarring a file- " + ex.getMessage());
        }finally {
            if (tis != null) {
                try {
                    tis.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

}
