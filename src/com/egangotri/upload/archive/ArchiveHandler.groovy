package com.egangotri.upload.archive

import com.egangotri.upload.util.UploadUtils
import com.egangotri.util.EGangotriUtil
import com.egangotri.util.FileUtil
import groovy.util.logging.Slf4j
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.Select
import org.openqa.selenium.support.ui.WebDriverWait

import java.awt.Robot
import java.awt.event.KeyEvent

@Slf4j
class ArchiveHandler {
    static final int ARCHIVE_WAITING_PERIOD = 200
    static final int DEFAULT_SLEEP_TIME = 1000

    static String ARCHIVE_URL = "https://archive.org/account/login.php"
    static final String baseUrl = "https://archive.org/upload/?"
    static final String ampersand = "&"

     static void loginToArchive(def metaDataMap, String archiveUrl, String archiveProfile) {
        logInToArchiveOrg(new ChromeDriver(), metaDataMap, archiveUrl, archiveProfile)
    }

     static int uploadToArchive(def metaDataMap, String archiveUrl, String archiveProfile) {
        return uploadToArchive(metaDataMap, archiveUrl, archiveProfile, true)
    }

     static void logInToArchiveOrg(WebDriver driver, def metaDataMap, String archiveUrl, String archiveProfile) {
        try {
            driver.get(archiveUrl)
            log.info("Login to Archive URL $archiveUrl")
            //Login
            WebElement id = driver.findElement(By.name("username"))
            WebElement pass = driver.findElement(By.name("password"))
            WebElement button = driver.findElement(By.name("submit-to-login"))

            id.sendKeys(metaDataMap."${archiveProfile}.username")
            pass.sendKeys(metaDataMap."kuta")
            log.info("before click")
            //button.click doesnt work
            button.submit()
            //pass.click()
            log.info("after click")
        }
        catch (Exception e) {
            log.info("Exeption in logInToArchiveOrg ${e.message}")
            e.printStackTrace()
            throw e
        }

    }


     static int uploadToArchive(
            def metaDataMap, String archiveUrl, String archiveProfile, boolean upload, List<String> uploadables) {
        int countOfUploadedItems = 0
        Thread.sleep(4000)
        // HashMap<String,String> mapOfArchiveIdAndFileName = [:]
        try {

            WebDriver driver = new ChromeDriver()

            logInToArchiveOrg(driver, metaDataMap, archiveUrl, archiveProfile)
            if (upload) {
                if (uploadables) {
                    log.info "Ready to upload ${uploadables.size()} Pdf(s) for Profile $archiveProfile"
                    //Get Upload Link
                    String uploadLink = generateURL(archiveProfile, uploadables[0])

                    //Start Upload of First File in Root Tab
                    log.info "Uploading: ${uploadables[0]}"
                    Thread.sleep(ARCHIVE_WAITING_PERIOD)
                    ArchiveHandler.upload(driver, uploadables[0], uploadLink)
                    countOfUploadedItems++
                    // mapOfArchiveIdAndFileName.put(archiveIdentifier, uploadables[0])
                    // Upload Remaining Files by generating New Tabs
                    if (uploadables.size() > 1) {
                        int tabIndex = 1
                        for (uploadableFile in uploadables.drop(1)){
                            log.info "Uploading: $uploadableFile @ tabNo:$tabIndex"
                            openNewTab(DEFAULT_SLEEP_TIME)

                            //Switch to new Tab
                            ArrayList<String> chromeTabsList = new ArrayList<String>(driver.getWindowHandles())
                            //there is a bug in retrieving the size of chromeTabsList in Selenium.
                            //use of last() instead of chromeTabsList.get(tabIndex+1) saves the issue
                            println "${chromeTabsList.last()} chromeTabsList.size(): ${chromeTabsList.size()} , tabIndex:$tabIndex"
                            try {
                                driver.switchTo().window(chromeTabsList.last())
                                tabIndex++
                            }
                            catch (Exception e) {
                                log.info("Exception while switching to new Tab", e)
                            }
                            uploadLink = generateURL(archiveProfile, uploadableFile)

                            //Start Upload
                            String rchvIdntfr = ArchiveHandler.upload(driver, uploadableFile, uploadLink)
                            countOfUploadedItems++
                            // mapOfArchiveIdAndFileName.put(rchvIdntfr, fileName)
                        }
                    }
                } else {
                    log.info "No File uploadable for profile $archiveProfile"
                }
            }

        }
        catch (Exception e) {
            e.printStackTrace()
        }

        return countOfUploadedItems
        //WriteToExcel.toCSV(mapOfArchiveIdAndFileName)
    }

     static int checkForMissingUploadsInArchive(String archiveUrl, List<String> fileNames) {
        int countOfUploadedItems = 0
        Thread.sleep(4000)
        try {

            WebDriver driver = new ChromeDriver()
            driver.get(archiveUrl)
            def results = []
            if (fileNames) {
                fileNames.each { fileName ->
                    log.info("Check for $fileName")
                    //Go to URL
                    driver.get(archiveUrl)

                    WebElement searchList = driver.findElement(By.className("searchlist"))
                    searchList.sendKeys(fileName)
                    searchList.submit()

                    //new WebDriverWait(driver, ARCHIVE_WAITING_PERIOD).until(ExpectedConditions.textToBePresentInElement(By.cssSelector("h3.co-top-row")))
                    String numOfUploads = driver.findElement(By.cssSelector("h3.co-top-row")).text
                    println "$numOfUploads $fileName"
                    results << [numOfUploads, fileName]
                }

            } else {
                log.info "No Filenames"
            }

        }
        catch (Exception e) {
            e.printStackTrace()
        }

        return countOfUploadedItems
        //WriteToExcel.toCSV(mapOfArchiveIdAndFileName)
    }


     static void generateAllUrls(String archiveProfile, List<String> uploadables) {
        uploadables.eachWithIndex { fileName, tabIndex ->
            String uploadLink = generateURL(archiveProfile, fileName)
            log.info("$tabIndex) $uploadLink")
        }
    }


    static int uploadToArchive(def metaDataMap, String archiveUrl, String archiveProfile, boolean upload) {
        List<String> uploadables = UploadUtils.getUploadablePdfsForProfile(archiveProfile)

        int uploadCount = 0
        if(EGangotriUtil.PARTITIONING_ENABLED && uploadables.size > EGangotriUtil.PARTITION_SIZE ){
            def partitions = UploadUtils.partition(uploadables, EGangotriUtil.PARTITION_SIZE)
            log.info("uploadables will be uploaded in ${partitions.size} # of Browsers: ")

            for( List<String> partitionedUploadables : partitions){
                log.info("Batch of partitioned Items Count ${partitionedUploadables.size} sent for uploads")
                uploadCount += uploadToArchive(metaDataMap, archiveUrl, archiveProfile, upload, partitionedUploadables)
            }
        }
        else {
            log.info("No partitioning")
            uploadCount = uploadToArchive(metaDataMap, archiveUrl, archiveProfile, upload, uploadables)
        }
        uploadCount
    }


     static String upload(WebDriver driver, String fileNameWIthPath, String uploadLink) {
        log.info("fileNameWIthPath:$fileNameWIthPath ready for upload")
         //Go to URL
         driver.navigate().to(uploadLink);
         driver.get(uploadLink);

         WebDriverWait waitForFileButtonInitial = new WebDriverWait(driver, ARCHIVE_WAITING_PERIOD)
         waitForFileButtonInitial.until(ExpectedConditions.elementToBeClickable(By.id("file_button_initial")))

        WebElement fileButtonInitial = driver.findElement(By.id("file_button_initial"))
        //((RemoteWebElement) fileButtonInitial).setFileDetector(new LocalFileDetector())
        fileButtonInitial.click()
        UploadUtils.pasteFileNameAndCloseUploadPopup(fileNameWIthPath)

        new WebDriverWait(driver, ARCHIVE_WAITING_PERIOD).until(ExpectedConditions.elementToBeClickable(By.id("license_picker_row")))

        WebElement licPicker = driver.findElement(By.id("license_picker_row"))
        licPicker.click()

        WebElement radioBtn = driver.findElement(By.id("license_radio_CC0"))
        radioBtn.click()

         if(!fileNameWIthPath.endsWith(EGangotriUtil.PDF) && !uploadLink.contains("collection=")){
             WebElement collectionSpan = driver.findElement(By.id("collection"))
             new WebDriverWait(driver, ARCHIVE_WAITING_PERIOD).until(ExpectedConditions.elementToBeClickable(By.id("collection")))

             collectionSpan.click()
             Select collDropDown = new Select(driver.findElement(By.name("mediatypecollection")))
             collDropDown.selectByValue("data:opensource_media");
         }

        //remove this junk value that pops-up for profiles with Collections
        /* if (uploadLink.contains("collection=")) {
             driver.findElement(By.className("additional_meta_remove_link")).click()
         }*/


        WebDriverWait wait = new WebDriverWait(driver, ARCHIVE_WAITING_PERIOD)
        wait.until(ExpectedConditions.elementToBeClickable(By.id("upload_button")))

        String identifier = ""
//driver.findElement( By.id("page_url")).getText() //By.xpath("//span[contains(@class, 'gray') and @id='page_url']"))
        println "identifier: $identifier"

        WebElement uploadButton = driver.findElement(By.id("upload_button"))
        uploadButton.click()
        return identifier
    }

     static String generateURL(String archiveProfile, String fileNameToBeUsedAsUniqueDescription = "") {
        boolean isPDF = fileNameToBeUsedAsUniqueDescription.endsWith(EGangotriUtil.PDF)
        if (isPDF) {
            fileNameToBeUsedAsUniqueDescription = fileNameToBeUsedAsUniqueDescription.replace(EGangotriUtil.PDF, "")
        }

        log.info "uniqueDescription:$fileNameToBeUsedAsUniqueDescription"

        def metaDataMap = UploadUtils.loadProperties(EGangotriUtil.ARCHIVE_METADATA_PROPERTIES_FILE)
        String fullURL = baseUrl + metaDataMap."${archiveProfile}.subjects" + ampersand + metaDataMap."${archiveProfile}.language" + ampersand + metaDataMap."${archiveProfile}.description" + ", '${removeAmpersand(fileNameToBeUsedAsUniqueDescription)}'" + ampersand + metaDataMap."${archiveProfile}.creator"
        if (metaDataMap."${archiveProfile}.collection") {
            fullURL += ampersand + metaDataMap."${archiveProfile}.collection"
        }
        /* else {
            if (!isPDF) {
                fullURL += ampersand + "collection=Community%20data"
            }
        }*/

        log.info "generateURL($archiveProfile):  \n$fullURL"
        return fullURL
    }

     static String removeAmpersand(String title) {
        title = title.replaceAll(ampersand, "")
        return title.drop(title.lastIndexOf(File.separator) + 1)
    }

     static List<String> pickFolderBasedOnArchiveProfile(String archiveProfile) {
        List folderName = []

        if (EGangotriUtil.isAPreCutOffProfile(archiveProfile)) {
            folderName = FileUtil.ALL_FOLDERS.values().toList() - FileUtil.ALL_FOLDERS."DT"
        } else {
            folderName = [FileUtil.ALL_FOLDERS."${archiveProfile.toUpperCase()}"]
        }

        log.info "pickFolderBasedOnArchiveProfile($archiveProfile): $folderName"
        return folderName
    }

     static void openNewTab(int sleepTime = 0) {
        Robot r = new Robot();
        r.keyPress(KeyEvent.VK_CONTROL);
        r.keyPress(KeyEvent.VK_T);
        r.keyRelease(KeyEvent.VK_T);
        r.keyRelease(KeyEvent.VK_CONTROL);
        if (sleepTime > 0) {
            Thread.sleep(sleepTime)
        }
    }

}