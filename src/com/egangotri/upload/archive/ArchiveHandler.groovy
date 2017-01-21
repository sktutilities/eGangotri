package com.egangotri.upload.archive

import com.egangotri.upload.util.UploadUtils
import com.egangotri.util.EGangotriUtil
import com.egangotri.util.FileUtil
import geb.Browser
import groovy.util.logging.Slf4j
import org.openqa.selenium.By
import org.openqa.selenium.Keys
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait

import java.awt.Robot
import java.awt.event.KeyEvent

@Slf4j
class ArchiveHandler {
    static ARCHIVE_WAITING_PERIOD = 50

    static String ARCHIVE_URL = "http://archive.org/account/login.php"
    static final String baseUrl = "http://archive.org/upload/?"
    static final String ampersand = "&"

    public static void loginToArchive(def metaDataMap, String archiveUrl, String archiveProfile) {
        uploadToArchive(metaDataMap, archiveUrl, archiveProfile, false)
    }

    public static int uploadToArchive(def metaDataMap, String archiveUrl, String archiveProfile) {
        return uploadToArchive(metaDataMap, archiveUrl, archiveProfile, true)
    }

    public static int uploadToArchive(def metaDataMap, String archiveUrl, String archiveProfile, boolean upload) {
        int countOfUploadedItems = 0
        Thread.sleep(4000)
        // HashMap<String,String> mapOfArchiveIdAndFileName = [:]
        try {

            WebDriver driver = new ChromeDriver()
            driver.get(archiveUrl)

            //Login
            WebElement id = driver.findElement(By.id("username"))
            WebElement pass = driver.findElement(By.id("password"))
            WebElement button = driver.findElement(By.id("submit"))

            id.sendKeys(metaDataMap."${archiveProfile}.username")
            pass.sendKeys(metaDataMap."kuta")
            log.info("before click")
            button.click()
            log.info("after click")
            if (upload) {
                List<String> uploadables = UploadUtils.getUploadablePdfsForProfile(archiveProfile)
                if (uploadables) {
                    log.info "Ready to upload ${uploadables.size()} Pdf(s) for Profile $archiveProfile"
                    //Get Upload Link
                    String uploadLink = generateURL(archiveProfile)

                    //Start Upload of First File in Root Tab
                    ArchiveHandler.upload(driver, uploadables[0], uploadLink)
                    countOfUploadedItems++
                    // mapOfArchiveIdAndFileName.put(archiveIdentifier, uploadables[0])
                    // Upload Remaining Files by generating New Tabs
                    if (uploadables.size() > 1) {
                        uploadables.drop(1).eachWithIndex { fileName, tabNo ->
                            log.info "Uploading: $fileName @ tabNo:$tabNo"
                            // Open new tab
                            //driver.findElement(By.cssSelector("body")).sendKeys)
                            def ele = driver.findElement(By.id("wrap"))

                            println "ele $ele"
                            println driver.findElement(By.cssSelector("body"))
                            println driver.findElement(By.id("wrap"))
                            //ele.sendKeys(Keys.CONTROL + "t")
                            //driver.findElement(By.cssSelector("body")).sendKeys(Keys.CONTROL + "t");

                            openNewTab()

                            //Switch to new Tab
                            ArrayList<String> tabs = new ArrayList<String>(driver.getWindowHandles())
                            println "tabs.size(): ${tabs.size()}"


                            driver.switchTo().window(tabs.get(tabNo + 1))

                            //Start Upload
                            String rchvIdntfr = ArchiveHandler.upload(driver, fileName, uploadLink)
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


    public static String upload(WebDriver driver, String fileNameWIthPath, String uploadLink) {
        log.info("$fileNameWIthPath goes to $uploadLink")
        //Go to URL
        driver.get(uploadLink)

        WebElement fileButtonInitial = driver.findElement(By.id("file_button_initial"))
        //((RemoteWebElement) fileButtonInitial).setFileDetector(new LocalFileDetector())
        fileButtonInitial.click()
        UploadUtils.pasteFileNameAndCloseUploadPopup(fileNameWIthPath)

        new WebDriverWait(driver, ARCHIVE_WAITING_PERIOD).until(ExpectedConditions.elementToBeClickable(By.id("license_picker_row")))

        WebElement licPicker = driver.findElement(By.id("license_picker_row"))
        licPicker.click()

        WebElement radioBtn = driver.findElement(By.id("license_radio_CC0"))
        radioBtn.click()

        //remove this junk value that pops-up for profiles with Collections
        if (uploadLink.contains("collection=")) {
            driver.findElement(By.className("additional_meta_remove_link")).click()
        }

        WebDriverWait wait = new WebDriverWait(driver, ARCHIVE_WAITING_PERIOD)
        wait.until(ExpectedConditions.elementToBeClickable(By.id("upload_button")))

        String identifier = ""
//driver.findElement( By.id("page_url")).getText() //By.xpath("//span[contains(@class, 'gray') and @id='page_url']"))
        println "identifier: $identifier"

        WebElement uploadButton = driver.findElement(By.id("upload_button"))
        uploadButton.click()
        return identifier
    }

    public static String generateURL(String archiveProfile) {
        def metaDataMap = UploadUtils.loadProperties(EGangotriUtil.ARCHIVE_METADATA_PROPERTIES_FILE)
        String fullURL = baseUrl + metaDataMap."${archiveProfile}.subjects" + ampersand + metaDataMap."${archiveProfile}.language" + ampersand + metaDataMap."${archiveProfile}.description" + ampersand + metaDataMap."${archiveProfile}.creator"
        if (metaDataMap."${archiveProfile}.collection") {
            fullURL += ampersand + metaDataMap."${archiveProfile}.collection"
        }

        log.info "generateURL($archiveProfile):fullURL"
        return fullURL
    }

    public static List<String> pickFolderBasedOnArchiveProfile(String archiveProfile) {
        List folderName = []

        if (EGangotriUtil.isAPreCutOffProfile(archiveProfile)) {
            folderName = FileUtil.ALL_FOLDERS.values().toList() - FileUtil.ALL_FOLDERS."DT"
        } else {
            folderName = [FileUtil.ALL_FOLDERS."${archiveProfile.toUpperCase()}"]
        }

        log.info "pickFolderBasedOnArchiveProfile($archiveProfile): $folderName"
        return folderName
    }


    public static void openNewTab() {
        Robot r = new Robot();
        r.keyPress(KeyEvent.VK_CONTROL);
        r.keyPress(KeyEvent.VK_T);
        r.keyRelease(KeyEvent.VK_CONTROL);
        r.keyRelease(KeyEvent.VK_T);
    }

}