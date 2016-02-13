package com.egangotri.upload.util

import com.egangotri.filter.DirectoryFileFilter
import com.egangotri.filter.PdfFileFilter
import com.egangotri.filter.NonPre57DirectoryFilter
import com.egangotri.upload.archive.ArchiveHandler
import com.egangotri.util.FileUtil
import org.apache.commons.logging.LogFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.awt.Robot
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.KeyEvent

/**
 * Created by user on 1/16/2016.
 */
class UploadUtils {
    final static org.slf4j.Logger Log = LoggerFactory.getLogger(this.class);

    static final String HOME = System.getProperty('user.home')
    static final String PDF = ".pdf"

    public static Hashtable loadProperties(String fileName) {
        Properties properties = new Properties()
        File propertiesFile = new File(fileName)
        propertiesFile.withInputStream {
            properties.load(it)
        }

        Hashtable<String, String> metaDataMap = [:]
        for (Enumeration e = properties.keys(); e.hasMoreElements();) {
            String key = (String) e.nextElement();
            String val = new String(properties.get(key).getBytes("ISO-8859-1"), "UTF-8")
            metaDataMap.put(key, val);
        }
        metaDataMap.each { k, v ->
            // Log.info "$k $v"
        }
        return metaDataMap
    }

    static boolean hasAtleastOneUploadablePdfForProfile(String archiveProfile) {
        List folders = ArchiveHandler.pickFolderBasedOnArchiveProfile(archiveProfile)
        folders.each {
            if (hasAtleastOnePdf(it.toString())) {
                return true
            }
        }
    }

    static boolean hasAtleastOnePdf(File directory, boolean excludePre57 = true) {
        Log.info("hasAtleastOnePdf($directory)")
        if (hasAtleastOnePdf(directory.absolutePath)) {
            return true
        }
        //Check Recursively
        return hasAtleastOnePdfRec(directory.listFiles(excludePre57 ? new NonPre57DirectoryFilter() : new DirectoryFileFilter()))
    }


    static boolean hasAtleastOnePdfRec(File[] directories, boolean excludePre57 = true) {
        boolean hasPdf = false
        //Check One Level Deep
        if(directories){
            directories.each {
                if (hasAtleastOnePdf(it.absolutePath)) {
                    hasPdf = true
                } else {
                    hasPdf = hasAtleastOnePdfRec(it.listFiles(excludePre57 ? new NonPre57DirectoryFilter() : new DirectoryFileFilter()))
                }
            }
        }
        return hasPdf
    }

    static boolean hasAtleastOnePdf(String dirName) {
        File directory = new File(dirName)
        return (directory && directory.list(new PdfFileFilter()));
    }

    // pre57's inside will be automatically ignored. but not a pre57 folder itself
    static List<String> getFiles(List<String> folderPaths) {
        List<String> uploadables = []
        folderPaths.each { String folder ->
            if (hasAtleastOnePdf(new File(folder))) {
                uploadables.addAll(getPdfFiles(folder))
            }
        }
        Log.info "uploadables:$uploadables"
        return uploadables
    }

    //Goes One Level Deep
    static List<String> getPdfFiles(String folderAbsolutePath) {
        File directory = new File(folderAbsolutePath)
        File[] pdfs = directory.listFiles(new PdfFileFilter())

        List<String> uploadables = []
        if (pdfs) {
            uploadables.addAll(pdfs.toList()*.absolutePath)
        }

        File[] deepDir1 = directory.listFiles(new NonPre57DirectoryFilter())

        deepDir1.each { File deepDir2 ->
            File[] deepDir2List = deepDir2.listFiles(new PdfFileFilter())
            if (deepDir2List) {
                uploadables.addAll(deepDir2List.toList()*.absolutePath)
            }
        }
        Log.info "***Total Files uploadables in $folderAbsolutePath: ${uploadables.size()}"
        return uploadables
    }


    public static void pasteFileNameAndCloseUploadPopup(String fileName) {
        // A short pause, just to be sure that OK is selected
        Thread.sleep(1000);
        setClipboardData(fileName);
        //native key strokes for CTRL, V and ENTER keys
        Robot robot = new Robot();
        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_V);
        robot.keyRelease(KeyEvent.VK_V);
        robot.keyRelease(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_ENTER);
        robot.keyRelease(KeyEvent.VK_ENTER);
    }

    public static void tabPasteFolderNameAndCloseUploadPopup(String fileName) {
        Log.info "$fileName  being pasted"
        // A short pause, just to be sure that OK is selected
        Thread.sleep(1000);
        setClipboardData(fileName);
        //native key strokes for CTRL, V and ENTER keys
        Robot robot = new Robot();
        robot.keyPress(KeyEvent.VK_TAB);
        robot.keyRelease(KeyEvent.VK_TAB);

        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_V);
        robot.keyRelease(KeyEvent.VK_V);
        robot.keyRelease(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_ENTER);
        robot.keyRelease(KeyEvent.VK_ENTER);
    }

    public static void setClipboardData(String string) {
        StringSelection stringSelection = new StringSelection(string);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
    }

    static List<String> pre57SubFolders(File directory) {
        List<String> subFolders = []
        File[] subDirs = directory.listFiles(new NonPre57DirectoryFilter(false))
        if (subDirs) {
            subFolders << subDirs[0].absolutePath
        }

        //Go One Level Deep
        File[] oneLevelDeepDirs = directory.listFiles(new NonPre57DirectoryFilter())?.toList()
        for (int i = 0; i < oneLevelDeepDirs.size(); i++) {
            File[] level2Files = oneLevelDeepDirs[i].listFiles(new NonPre57DirectoryFilter(false))
            if (level2Files) {
                subFolders << level2Files[0].absolutePath
            }
        }
        Log.info "pre57SubFolders in Directory : $directory: $subFolders"
        return subFolders
    }

    static List pre57SubFolders(String folderName) {
        return pre57SubFolders(new File(folderName))
    }
}
