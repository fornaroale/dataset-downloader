package it.fornaro;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * The FileDownloader class implements a simple compressed dataset downloader
 * which expects two arguments, the URL of a .zip file (containing the dataset
 * and an MD5 checksum of the .zip file).
 *
 */
class DatasetDownloader {
    /**
     * Application entry point
     *
     * @param args Two arguments are expected: URL of dataset .zip file; URL of dataset zip .md5 file
     */
    public static void main(String[] args) {
        System.out.println("* *  Dataset Downloader  * *");

        // Check if the user has provided the required args (url + checksum):
        // if ok, check that the provided args are valid (URL & MD5)
        if(args.length != 2) {
            System.out.println("Please provide the required args: .zip URL, .zip checksum URL");
        } else if( isURLValid(args[0]) && isURLValid(args[1]) ) {
            String url = args[0];
            String md5URL = args[1];

            System.out.println(" -> Dataset .zip URL: " + url);
            System.out.println(" -> Dataset .zip MD5 URL: " + md5URL);

            // Download the file
            String filename = "dataset";
            if( downloadFile(url, false, filename) && downloadFile(md5URL, true, filename) ) {

                // Once files are downloaded, compare checksums (up to five attempts)
                boolean checksumsMatch = false;
                int counter = 0;

                while( !checksumsMatch && counter < 5 ){
                    checksumsMatch = compareChecksum(filename);

                    if ( !checksumsMatch ) {
                        if(counter == 4) {
                            System.out.println("Could not download dataset .zip: MD5 checksums are not corresponding.");
                            System.exit(1);
                        } else {
                            System.out.println("Error while downloading the file/checksum. Retrying... (attempt no. " + (counter+2) + ")");
                            counter++;
                        }
                    } else {
                        // List files contained in dataset .zip
                        listZipFiles(filename);
                    }
                }
            } else {
                System.out.println("Error while downloading the file/checksum. Please retry.");
            }
        } else {
            System.exit(1);
        }
    }

    /**
     * This method is intended to check if the URL is valid or malformed;
     * malformed URLs will throw an exception
     *
     * @param urlString URL address to be checked
     * @return true if URL format is correct; false otherwise
     */
    static private boolean isURLValid(String urlString){
        try {
            new URL(urlString).toURI();
            return true;
        } catch (MalformedURLException | URISyntaxException e) {
            System.out.println("Exception: URL '" + urlString + "' is not valid\n" + e);
            return false;
        }
    }

    /**
     * This method is intended to check if the MD5 checksum is syntactically valid
     *
     * @param md5 MD5 checksum to be validated
     * @return true if checksum format is correct; false otherwise
     */
    static private boolean isChecksumValid(String md5) {
        if ( md5.matches("^[a-fA-F0-9]{32}$") ) {
            return true;
        } else {
            System.out.println("Exception: the provided MD5 checksum is not valid");
            return false;
        }
    }

    /**
     * This method downloads the file at the given URL
     *
     * @param url URL address pointing to the file
     * @param md5DL true if asking to download the MD5 .zip, false otherwise
     * @return true if download was successful; false otherwise
     */
    static private boolean downloadFile(String url, boolean md5DL, String filename) {

        String datasetFilename = filename + ".zip";
        String datasetChecksum = filename + ".zip.md5";

        // Use Apache Commons IO to download the file
        try {
            // Start downloading the file
            if ( !md5DL ) {
                // Check if file already exists (already downloaded)
                File datasetFile = new File(datasetFilename);
                if ( datasetFile.exists() && !datasetFile.isDirectory() ) {
                    System.out.println(" -> File dataset .zip was already downloaded! Skipping...");
                } else {
                    // If not already present, download it
                    System.out.println(" -> Download of dataset .zip started! Please wait...");
                    FileUtils.copyURLToFile(new URL(url), datasetFile);
                    System.out.println(" -> Download of dataset .zip finished!");
                }
            } else {
                // MD5 gets always downloaded
                System.out.println(" -> Download of MD5 checksum started! Please wait...");
                FileUtils.copyURLToFile(new URL(url), new File(datasetChecksum));
                System.out.println(" -> Download of MD5 checksum finished!");
            }

        } catch (IOException e) {
            System.out.println("Exception: no permission to write file!\n" + e);
            System.exit(1);
        }

        return true;
    }

    /**
     * This method is intended to compare the MD5 checksum of the downloaded file
     * with the one provided via the second downloaded file
     *
     * @param filename Name of the file to be checked (two files are expected:
     *                 filename.zip; filename.zip.md5)
     * @return true if checksum of downloaded file matches; false otherwise
     */
    static private boolean compareChecksum(String filename){
        // Verify checksum
        try (InputStream is = Files.newInputStream(Paths.get(filename + ".zip"))) {
            // Compute .zip file MD5
            String md5 = DigestUtils.md5Hex(is);

            // Fetch the provided Md5
            String providedMd5 = new String(Files.readAllBytes(Paths.get(filename + ".zip.md5")));
            providedMd5 = providedMd5.substring(0, providedMd5.indexOf(' '));

            // Verify if the provided checksum is valid
            if( !isChecksumValid(providedMd5) )
                return false;   // If invalid, return false

            // Compare checksums
            System.out.println(" -> Comparing checksum (" + providedMd5 + ") with downloaded file: " + md5);
            return providedMd5.equals(md5);   // Return true if the checksum matches; false otherwise
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * This method is intended to list the files contained in the compressed .zip
     * file passed as parameter
     *
     * @param zipFilename Name of the .zip file containing entries to be listed
     */
    static private void listZipFiles(String zipFilename) {
        try {
            ZipFile zipFile = new ZipFile(zipFilename + ".zip");
            Enumeration zipEntries = zipFile.entries();
            System.out.println(" -> Dataset .zip content (names of the files):");
            while (zipEntries.hasMoreElements()) {
                // Get single filename
                String fname = ((ZipEntry)zipEntries.nextElement()).getName();

                // Remove parent filename from actual filename
                fname = fname.substring(fname.indexOf('/') + 1);
                if(!fname.isEmpty()) System.out.println("    - " + fname);
            }
        } catch (IOException e) {
            System.out.println("Error while reading file names of .zip file! Is the provided .zip file valid?");
        }
    }
}