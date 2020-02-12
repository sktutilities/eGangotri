package com.egangotri.upload.util


import com.egangotri.util.EGangotriUtil
import com.egangotri.util.FileUtil
import groovy.io.FileType
import groovy.util.logging.Slf4j
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver

import java.awt.Robot
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.KeyEvent

@Slf4j
class UploadUtils {

    static final String USERNAME_TEXTBOX_NAME = "username"
    static final String PASSWORD_TEXTBOX_NAME = "password"
    static final String LOGIN_BUTTON_NAME = "submit-to-login"
    static final String USER_MENU_ID = "user-menu" // only created when User is Signed In
    static final String CHOOSE_FILES_TO_UPLOAD_BUTTON = "file_button_initial"
    static final String UPLOAD_AND_CREATE_YOUR_ITEM_BUTTON = "upload_button"
    static final String LICENSE_PICKER_DIV = "license_picker_row"
    static final String LICENSE_PICKER_RADIO_OPTION = "license_radio_CC0"

    static final int DEFAULT_SLEEP_TIME = 1000
    static Map<String,String> SUPPLEMENTARY_URL_FOR_EACH_PROFILE_MAP = [:]
    static Map<String,List<String>> RANDOM_CREATOR_BY_PROFILE_MAP = [:]
    static final String ARCHIVE_UPLOAD_URL = "https://archive.org/upload?"
    static final String AMPERSAND = "&"

    static readTextFileAndDumpToList(String fileName){
        List list = []
            File file = new File(fileName)
            def line = ""
            file.withReader { reader ->
                while ((line = reader.readLine()) != null) {
                    list << line
                }
            }
            return list
    }

    static Hashtable<String, String> loadProperties(String fileName) {
        Properties properties = new Properties()
        File propertiesFile = new File(fileName)
        Hashtable<String, String> metaDataMap = [:]

        if(propertiesFile.exists()){
            propertiesFile.withInputStream {
                properties.load(it)
            }


            properties.entrySet().each { entry ->
                String key = entry.key
                String val = new String(entry.value.getBytes("ISO-8859-1"), "UTF-8")
                if (key.endsWith(".description")) {
                    val = encodeString(val)
                }
                metaDataMap.put(key, val);
            }

            metaDataMap.each {
                k, v ->
                    //log.info "$k $v"
            }
        }
        return metaDataMap
    }

    def static encodeString(def stringToEncode) {

        def reservedCharacters = [32: 1, 33: 1, 42: 1, 34: 1, 39: 1, 40: 1, 41: 1, 59: 1, 58: 1, 64: 1, 38: 1, /*61:1,*/ 43: 1, 36: 1, 33: 1, 47: 1, 63: 1, 37: 1, 91: 1, 93: 1, 35: 1]

        def encoded = stringToEncode.collect { letter ->
            reservedCharacters[(int) letter] ? "%" + Integer.toHexString((int) letter).toString().toUpperCase() : letter
        }
        return encoded.join("")
    }

    static boolean hasAtleastOneUploadablePdfForProfile(String archiveProfile) {
        List<File> folders = pickFolderBasedOnArchiveProfile(archiveProfile).collect { new File(it) }
        boolean atlestOne = false
        println "folders: $folders"
        if (EGangotriUtil.isAPreCutOffProfile(archiveProfile) && hasAtleastOnePdfInPreCutOffFolders(folders)) {
            atlestOne = true
        } else if (hasAtleastOnePdfExcludePreCutOff(folders)) {
            atlestOne = true
        }
        log.info "atlestOne[$archiveProfile]: $atlestOne"
        return atlestOne
    }

    static List<String> getUploadablePdfsForProfile(String archiveProfile) {
        List<File> folders = pickFolderBasedOnArchiveProfile(archiveProfile).collect { String fileName -> fileName? new File(fileName): null }
        List<String> pdfs = []
        println "getUploadablePdfsForProfile: $archiveProfile"
        if (EGangotriUtil.isAPreCutOffProfile(archiveProfile)) {
            pdfs = getPdfsInPreCutOffFolders(folders)
        } else {
            pdfs = getAllPdfsExceptPreCutOff(folders)
        }
        return pdfs
    }

    static int getCountOfUploadablePdfsForProfile(String archiveProfile) {
        return getUploadablePdfsForProfile(archiveProfile)?.size()
    }


    static boolean hasAtleastOnePdf(File folder) {
        return hasAtleastOnePdf(folder, false)
    }

    static boolean hasAtleastOnePdf(File folder, boolean excludePreCutOff) {
        return getAllPdfs(folder, excludePreCutOff)?.size()
    }

    static boolean hasAtleastOnePdfExcludePreCutOff(File folder) {
        return hasAtleastOnePdf(folder, true)
    }

    static boolean hasAtleastOnePdfExcludePreCutOff(List<File> folders) {
        boolean atlestOne = false
        folders.each { folder ->
            if (hasAtleastOnePdfExcludePreCutOff(folder)) {
                atlestOne = true
            }
        }
        return atlestOne
    }

    static boolean hasAtleastOnePdfInPreCutOffFolders(List<File> folders) {
        boolean atlestOne = false
        if (getPdfsInPreCutOffFolders(folders)) {
            atlestOne = true
        }
        return atlestOne
    }

    static List<String> getAllPdfsExceptPreCutOff(File folder) {
        getAllPdfs(folder, true)
    }

    static List<String> getAllPdfsExceptPreCutOff(List<File> folders) {
        List<String> pdfs = []
        folders.each { folder ->
            pdfs.addAll(getAllPdfsExceptPreCutOff(folder))
        }
        return pdfs
    }

    static List<String> getAllPdfs(File folder, Boolean excludePreCutOff) {
        List<String> pdfs = []
        Map optionsMap = [type      : FileType.FILES,
                          nameFilter: ~(FileUtil.PDF_REGEX)
        ]
        if (excludePreCutOff) {
            optionsMap.put("excludeFilter", { it.absolutePath.toLowerCase().contains(FileUtil.PRE_CUTOFF) || it.absolutePath.toLowerCase().contains(FileUtil.UPLOAD_KEY_WORD)})
        }
        if(!folder.exists()){
            log.error("$folder doesnt exist. returning")
            return []
        }
        folder.traverse(optionsMap) {
            //log.info "getAllPdfs>>" + it
            pdfs << it.absolutePath
        }
        return pdfs.sort()
    }

    static List<String> getAllPdfs(File folder) {
        return getAllPdfs(folder, false)
    }

    static List<String> getPdfsInPreCutOffFolder(File folder) {
        List<String> pdfs = []
        Map optionsMap = [type  : FileType.FILES,
                          filter: {
                              it.absolutePath.contains(FileUtil.PRE_CUTOFF) /*&&
                                      it.name.endsWith(EGangotriUtil.PDF)*/
                          }
        ]
        if(!folder.exists()){
            log.error("$folder doesnt exist. returning")
            return []
        }
        folder.traverse(optionsMap) {
            log.info ">>>" + it
            log.info "${it.absolutePath.contains(FileUtil.PRE_CUTOFF)}"
            pdfs << it.absolutePath
        }
        return pdfs
    }

    static List<String> getPdfsInPreCutOffFolders(List<File> folders) {
        List<String> pdfs = []
        folders.each { folder ->
            pdfs.addAll(getPdfsInPreCutOffFolder(folder))
        }
        return pdfs
    }

    static void pasteFileNameAndCloseUploadPopup(String fileName) {
        // A short pause, just to be sure that OK is selected
        EGangotriUtil.sleepTimeInSeconds(1)
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

    static void hitEscapeKey() {
        Robot robot = new Robot();
        robot.keyPress(KeyEvent.VK_ESCAPE);
        robot.keyRelease(KeyEvent.VK_ESCAPE);
    }

    static void hitEnterKey() {
        Robot robot = new Robot();
        robot.keyPress(KeyEvent.VK_ENTER);
        robot.keyRelease(KeyEvent.VK_ENTER);
    }

    static void clickChooseFilesToUploadButton(WebDriver driver, String fileNameWithPath){
        WebElement fileButtonInitial = driver.findElement(By.id(CHOOSE_FILES_TO_UPLOAD_BUTTON))
        fileButtonInitial.click()
        pasteFileNameAndCloseUploadPopup(fileNameWithPath)
    }

    static boolean openNewTab(ChromeDriver driver, List chromeTabsList){
        try {
            driver.switchTo().window(chromeTabsList.last())
        }
        catch (Exception e) {
            log.info("Exception while switching to new Tab", e)
            return false
        }
        return true
    }

    static void tabPasteFolderNameAndCloseUploadPopup(String fileName) {
        log.info "$fileName  being pasted"
        // A short pause, just to be sure that OK is selected
        EGangotriUtil.sleepTimeInSeconds(1);
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

    static void setClipboardData(String string) {
        StringSelection stringSelection = new StringSelection(string);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
    }

    static List getAStashOfFilesForUpload(String src) {
        List<String> filterables = getFilterables()
        List<String> allPdfs = getAllPdfs(new File(src))
        if (filterables) {
            log.info("filterables.size():${filterables.size()} '${filterables[0]}'")
            log.info("allPdfs2.size() before:${allPdfs.size()} '${allPdfs.first()}'")
            int count = 0

            def x = ['nnn', 'E:\\bngl2\\85685712 Jennings Vedantic Buddhism Of The Buddha.pdf']
            def y = "85685712 Jennings Vedantic Buddhism Of The Buddha.pdf"
            println(x.findIndexOf { it =~ /($y)$/ })
            filterables.each { String filterable ->
                int count2 = 0
                try {
                    int idx = allPdfs.findIndexOf { pdf ->
                        String y2 = pdf.substring(src.length() + 1)

                        if(count2++ <10){
                            println( "$filterable == ${y2} ($pdf) " +  filterable.equals(y2))
                        }
                        filterable.equals(y2)
                    }

                    if (idx >= 0) {
                        println("${count++}).idx:$idx, $filterable == ${allPdfs.get(idx)}")
                        allPdfs.remove(idx)
                    }
                }

                catch (Exception e) {
                    log.error(e.message)
                }

            }
            log.info("\nallPdfs2.size() after:${allPdfs.size()} '${allPdfs.first()}'")
        }
        if (allPdfs?.size() > 100) {
            return allPdfs[0..100]
        } else {
            return allPdfs[0..allPdfs?.size()]
        }

    }

    static List<String> getFilterables() {
        String fileName = System.getProperty("user.home") + "${File.separator}eGangotri${File.separator}archiveFileName.txt"
        List<String> list = new File(fileName).readLines()
        List<String> filterables = []
        if (list) {
            filterables = list[0].split(",").collect { "${it}.pdf" }
        }
        return filterables
    }


    static def partition(List<String> partitionableList, int size) {
        def partitions = []
        int partitionCount = partitionableList.size() / size

        partitionCount.times { partitionNumber ->
            def start = partitionNumber * size
            def end = start + size - 1
            partitions << partitionableList[start..end]
        }

        if (partitionableList.size() % size) partitions << partitionableList[partitionCount * size..-1]
        return partitions
    }

    static void throwNoCreatorSpecifiedErrorIfNoRandomCreatorFlagAndQuit(){
        if (!EGangotriUtil.GENERATE_RANDOM_CREATOR) {
            throw new Exception("No Creator. Pls provide Creator in archiveMetadata.properties file")
        }
    }
    static String generateCreatorsForProfileAndPickARandomOne(String archiveProfile) {
        throwNoCreatorSpecifiedErrorIfNoRandomCreatorFlagAndQuit()
        if (!RANDOM_CREATOR_BY_PROFILE_MAP || !RANDOM_CREATOR_BY_PROFILE_MAP.containsKey(archiveProfile)) {
            RANDOM_CREATOR_BY_PROFILE_MAP.put(archiveProfile, null)
        }
        if (!RANDOM_CREATOR_BY_PROFILE_MAP["${archiveProfile}"]) {
            RANDOM_CREATOR_BY_PROFILE_MAP["${archiveProfile}"] = randomCreators()
        }
        List randomCreators =  RANDOM_CREATOR_BY_PROFILE_MAP["${archiveProfile}"]
        String randomPick =  randomCreators[new Random().nextInt(randomCreators.size)]
        return "creator=${randomPick}"
    }

    static List randomCreators() {
        List firstNames = UploadUtils.readTextFileAndDumpToList(EGangotriUtil.FIRST_NAME_FILE)
        List lastNames = UploadUtils.readTextFileAndDumpToList(EGangotriUtil.LAST_NAME_FILE)
        Random rnd = new Random()
        List creators = []
        int MAX_CREATORS = 50
        int max = firstNames.size() > lastNames.size() ? (firstNames.size() > MAX_CREATORS ? MAX_CREATORS : firstNames.size()) : (lastNames.size() > MAX_CREATORS ? MAX_CREATORS : lastNames.size())
        (1..max).each {
            int idx1 = rnd.nextInt(firstNames.size)
            int idx2 = rnd.nextInt(lastNames.size)
            creators << "${firstNames[idx1].trim().capitalize()} ${lastNames[idx2].trim().capitalize()}"
        }
        return creators
    }

    static String getOrGenerateSupplementaryURL(String archiveProfile) {
        if (!SUPPLEMENTARY_URL_FOR_EACH_PROFILE_MAP || !SUPPLEMENTARY_URL_FOR_EACH_PROFILE_MAP.containsKey(archiveProfile)) {
            SUPPLEMENTARY_URL_FOR_EACH_PROFILE_MAP.put(archiveProfile, null)
        }
        if (!SUPPLEMENTARY_URL_FOR_EACH_PROFILE_MAP["${archiveProfile}"]) {
            def metaDataMap = UploadUtils.loadProperties(EGangotriUtil.ARCHIVE_METADATA_PROPERTIES_FILE)
            String _creator = metaDataMap."${archiveProfile}.creator"
            if(!_creator){
                throwNoCreatorSpecifiedErrorIfNoRandomCreatorFlagAndQuit()
            }

            String _subjects = metaDataMap."${archiveProfile}.subjects"
            if(!_subjects){
                _subjects = !EGangotriUtil.GENERATE_RANDOM_CREATOR ? "subject=" + _creator.replaceAll("creator=", ""): null
            }

            String _lang = metaDataMap."${archiveProfile}.language" ?: "language=eng"
            String _fileNameAsDesc = '{0}'
            String _desc = metaDataMap."${archiveProfile}.description"
            String desc_and_file_name = _desc ? "${_desc}, ${_fileNameAsDesc}" : "description=" + _fileNameAsDesc
            String supplementary_url = desc_and_file_name + AMPERSAND +  _lang
            if (metaDataMap."${archiveProfile}.collection") {
                supplementary_url += AMPERSAND + metaDataMap."${archiveProfile}.collection"
            }
            if(_subjects){
                supplementary_url += AMPERSAND + _subjects
            }
            if(!EGangotriUtil.GENERATE_RANDOM_CREATOR){
                supplementary_url += AMPERSAND +  _creator
            }
            SUPPLEMENTARY_URL_FOR_EACH_PROFILE_MAP["${archiveProfile}"] = supplementary_url
        }
        String url = SUPPLEMENTARY_URL_FOR_EACH_PROFILE_MAP["${archiveProfile}"]
        if((EGangotriUtil.GENERATE_RANDOM_CREATOR)){
            String _creator = generateCreatorsForProfileAndPickARandomOne(archiveProfile)
            url += AMPERSAND +  _creator

            if(!url.contains("subject=")){
                String _subjects = "subject=" + _creator.replaceAll("creator=", "")
                url += AMPERSAND + _subjects
            }
        }
        return url
    }


    static String generateURL(String archiveProfile, String fileNameToBeUsedAsUniqueDescription = "") {
        boolean isPDF = fileNameToBeUsedAsUniqueDescription.endsWith(EGangotriUtil.PDF)
        if (isPDF) {
            fileNameToBeUsedAsUniqueDescription = fileNameToBeUsedAsUniqueDescription.replace(EGangotriUtil.PDF, "")
        }
        log.info "uniqueDescription:$fileNameToBeUsedAsUniqueDescription"

        String supplementary_url = getOrGenerateSupplementaryURL(archiveProfile)
        supplementary_url = supplementary_url.replace('{0}', "'${removeAmpersand(fileNameToBeUsedAsUniqueDescription)}'")
        String fullURL = ARCHIVE_UPLOAD_URL + supplementary_url
        log.info "generateURL($archiveProfile):  \n$fullURL"
        return fullURL
    }

    static String removeAmpersand(String title) {
        title = title.replaceAll(AMPERSAND, "")
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

    static void openNewTab(float sleepTimeInSeconds = 0) {
        Robot r = new Robot();
        r.keyPress(KeyEvent.VK_CONTROL);
        r.keyPress(KeyEvent.VK_T);
        r.keyRelease(KeyEvent.VK_T);
        r.keyRelease(KeyEvent.VK_CONTROL);
        if (sleepTimeInSeconds > 0) {
            EGangotriUtil.sleepTimeInSeconds(sleepTimeInSeconds)
        }
    }
}
