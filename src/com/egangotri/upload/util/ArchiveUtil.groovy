package com.egangotri.upload.util

import com.egangotri.util.EGangotriUtil
import groovy.util.logging.Slf4j
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait

@Slf4j
class ArchiveUtil {
    static String ARCHIVE_LOGIN_URL = "https://archive.org/account/login.php"
    static String ARCHIVE_USER_ACCOUNT_URL = "https://archive.org/details/@ACCOUNT_NAME"

    static void getResultsCount(WebDriver driver, Boolean _startTime = true) {
        WebElement avatar = driver.findElementByClassName("avatar")
        String userName = avatar.getAttribute("alt")
        log.info("userName: ${userName}")
        String archiveUserAccountUrl = ARCHIVE_USER_ACCOUNT_URL.replace("ACCOUNT_NAME", userName.toLowerCase())
        if(!_startTime){
            UploadUtils.openNewTab()
            UploadUtils.switchToLastOpenTab(driver)
            driver.navigate().to(archiveUserAccountUrl)
        }
        driver.get(archiveUserAccountUrl)
        WebDriverWait webDriverWait = new WebDriverWait(driver, EGangotriUtil.TEN_TIMES_TIMEOUT_IN_SECONDS)
        webDriverWait.until(ExpectedConditions.elementToBeClickable(By.className("results_count")))
        WebElement resultsCount = driver.findElementByClassName("results_count")
        if (resultsCount) {
            log.info("Results Count at ${ _startTime ? "LoginTime": 'UploadCompletionTime'}: " + resultsCount.text)
            if(!_startTime){
                log.info("**Figure captured will update in a while. So not exctly accurate as upload are still happening")
            }
        }
    }

    static void navigateLoginLogic(WebDriver driver, Map metaDataMap, String archiveProfile) throws Exception{
        boolean loginSuccess = logInToArchiveOrg(driver, metaDataMap, archiveProfile)
        if (!loginSuccess) {
            log.info("Login failed once for ${archiveProfile}. will give it one more shot")
            loginSuccess = logInToArchiveOrg(driver, metaDataMap, archiveProfile)
        }
        if (!loginSuccess) {
            log.info("Login failed for Second Time for ${archiveProfile}. will now quit")
            throw new Exception("Not Continuing becuase of Login Failure twice")
        }
    }

    static void storeArchiveIdentifierInFile(String archiveProfile, String uploadLink, String fileNameWithPath,String fileName, String _identifier) {
        String appendable = "\"$archiveProfile\", \"$uploadLink\", \"$fileNameWithPath\", \"$fileName\", \"$_identifier\"\n"
        new File(EGangotriUtil.ARCHIVE_IDENTIFIER_FILE).append(appendable)
    }

    static void printUplodReport( Map<Integer, String> uploadSuccessCheckingMatrix){
        if (uploadSuccessCheckingMatrix) {
            log.info "Upload Report:\n"
            uploadSuccessCheckingMatrix.each { k, v ->
                log.info "$k) $v"
            }
            log.info "\n ***All Items put for upload implies all were attempted successfully for upload. But there can be errors still after attempted upload. best to check manually."
        }
    }

    static void loginToArchive(def metaDataMap, String archiveProfile) {
        logInToArchiveOrg(new ChromeDriver(), metaDataMap, archiveProfile)
    }

    static boolean logInToArchiveOrg(WebDriver driver, def metaDataMap, String archiveProfile) {
        boolean loginSucess = false
        try {
            driver.get(ArchiveUtil.ARCHIVE_LOGIN_URL)
            log.info("Login to Archive URL $ArchiveUtil.ARCHIVE_LOGIN_URL")
            //Login
            WebElement id = driver.findElement(By.name(UploadUtils.USERNAME_TEXTBOX_NAME))
            WebElement pass = driver.findElement(By.name(UploadUtils.PASSWORD_TEXTBOX_NAME))
            WebElement button = driver.findElement(By.name(UploadUtils.LOGIN_BUTTON_NAME))

            String username = metaDataMap."${archiveProfile}.username"
            id.sendKeys(username)
            String kuta = metaDataMap."${archiveProfile}.kuta" ?: metaDataMap."kuta"
            pass.sendKeys(kuta)
            //button.click doesnt work
            button.submit()
            //pass.click()
            EGangotriUtil.sleepTimeInSeconds(0.2)
            WebDriverWait wait = new WebDriverWait(driver, EGangotriUtil.TEN_TIMES_TIMEOUT_IN_SECONDS)
            wait.until(ExpectedConditions.elementToBeClickable(By.id(UploadUtils.USER_MENU_ID)))
            loginSucess = true
        }
        catch (Exception e) {
            log.info("Exeption in logInToArchiveOrg ${e.message}")
            e.printStackTrace()
            throw e
        }
        return loginSucess
    }


}