package com.egangotri.util

import com.egangotri.upload.util.UploadUtils
import groovy.util.logging.Slf4j

@Slf4j
class EGangotriUtil {
    static final String HOME = System.getProperty('user.home')
    static final String PDF = ".pdf"
    static final String PROPERTIES = ".properties"
    static String PRECUTOFF_PROFILE = ""
    static final int UPLOAD_FAILURE_THRESHOLD = 5
    static long PROGRAM_START_TIME_IN_SECONDS = 0
    static long PROGRAM_END_TIME_IN_SECONDS = 0
    static int ARCHIVE_WAITING_PERIOD_ONE_SEC = 1000

    static final String EGANGOTRI_BASE_DIR = HOME + File.separator + "eGangotri"

    static final String ARCHIVE_PROPERTIES_FILE = EGANGOTRI_BASE_DIR + File.separator + "archiveLogins" + PROPERTIES
    static final String SETTINGS_PROPERTIES_FILE = EGANGOTRI_BASE_DIR + File.separator + "settings" + PROPERTIES

    static final String GOOGLE_DRIVE_PROPERTIES_FILE = EGANGOTRI_BASE_DIR + File.separator + "googleDriveLogins" + PROPERTIES
    static final String LOCAL_FOLDERS_PROPERTIES_FILE = EGANGOTRI_BASE_DIR + File.separator + "localFolders" + PROPERTIES
    static final String ARCHIVE_METADATA_PROPERTIES_FILE = EGANGOTRI_BASE_DIR + File.separator + "archiveMetadata" + PROPERTIES

    static final String ARCHIVE_ITEMS_QUEUED_FOLDER = EGANGOTRI_BASE_DIR + File.separator + "items_queued"
    static final String ARCHIVE_ITEMS_USHERED_FOLDER = EGANGOTRI_BASE_DIR + File.separator + "items_ushered"
    static final String ARCHIVE_ITEMS_POST_VALIDATIONS_FOLDER = EGANGOTRI_BASE_DIR + File.separator + "items_post_validation"

    static String ARCHIVE_ITEMS_QUEUED_FILE = EGangotriUtil.ARCHIVE_ITEMS_QUEUED_FOLDER + File.separator + "item_{0}.csv"
    static String ARCHIVE_ITEMS_USHERED_FOR_UPLOAD_FILE = EGangotriUtil.ARCHIVE_ITEMS_USHERED_FOLDER + File.separator + "item_{0}.csv"
    static String ARCHIVE_ITEMS_QUEUED_POST_VALIDATION_FILE = EGangotriUtil.ARCHIVE_ITEMS_POST_VALIDATIONS_FOLDER + File.separator + "queued_item_{0}.csv"
    static String ARCHIVE_ITEMS_USHERED_POST_VALIDATION_FILE = EGangotriUtil.ARCHIVE_ITEMS_POST_VALIDATIONS_FOLDER + File.separator + "ushered_item_{0}.csv"
//ARCHIVE_VALIDATION_FILE

    static final String FIRST_NAME_FILE = EGANGOTRI_BASE_DIR + File.separator + "first_names.txt"
    static final String LAST_NAME_FILE = EGANGOTRI_BASE_DIR + File.separator + "last_names.txt"

    static Boolean GENERATE_ONLY_URLS = false
    static int PARTITION_SIZE = 250
    static boolean PARTITIONING_ENABLED = false
    static boolean GENERATE_RANDOM_CREATOR = false
    //This not implemented
    static List ACCOUNTS_WITH_RANDOMIZABLE_CREATORS = []

    static List IGNORE_CREATOR_SETTINGS_FOR_ACCOUNTS = []
    static boolean CREATOR_FROM_DASH_SEPARATED_STRING = false

    static boolean ADD_RANDOM_INTEGER_TO_PAGE_URL = false
    static final Range ASCII_CHARS =  "A".."z"
    static final def ASCII_CHARS_SIZE =  ASCII_CHARS.size()

    static
    final String UPLOAD_PROFILES_PROPERTIES_FILE = EGANGOTRI_BASE_DIR + File.separator + "uploadProfiles" + PROPERTIES
    static final int TIMEOUT_IN_TWO_SECONDS = 5

    static final int TIMEOUT_IN_FIVE_SECONDS = 5
    static final int TEN_TIMES_TIMEOUT_IN_SECONDS = TIMEOUT_IN_FIVE_SECONDS * 10

    static final String USER_ID = "userId"
    static final String USER_NAME = "username"

    public static List<String> ARCHIVE_PROFILES = getAllArchiveProfiles()
    static List GOOGLE_PROFILES = getAllGoogleDriveProfiles()

    static List getAllProfiles(String filePath, String textDiscarder) {
        Properties properties = new Properties()
        File propertiesFile = new File(filePath)

        if(!propertiesFile.exists()){
            log.info("$filePath not found.")
            return []
        }

       List profiles = []

        properties.load(propertiesFile.newDataInputStream())
        properties.each{ key, v -> if (key.contains(textDiscarder)) {
            profiles << (key - textDiscarder)
        } }
        return profiles
    }

    static List getAllGoogleDriveProfiles() {
        return getAllProfiles(GOOGLE_DRIVE_PROPERTIES_FILE, ".$USER_ID")
    }

    static List getAllArchiveProfiles() {
        return getAllProfiles(ARCHIVE_PROPERTIES_FILE, ".$USER_NAME")
    }

    static void sleepTimeInSeconds(Float sleepTimeInSeconds, boolean overRideToOneSec = false){
        Thread.sleep(overRideToOneSec ? 1000 : (EGangotriUtil.ARCHIVE_WAITING_PERIOD_ONE_SEC * sleepTimeInSeconds).toInteger())
    }

    static List getAllUploadProfiles() {
        Properties properties = new Properties()
        File propertiesFile = new File(UPLOAD_PROFILES_PROPERTIES_FILE)
        propertiesFile.withInputStream { stream ->
            properties.load(stream)
        }
        return properties.values().toList()
    }

    static boolean isAPreCutOffProfile(String archiveProfile) {
        return (archiveProfile == PRECUTOFF_PROFILE)
    }

    static void recordProgramStart(String program = ""){
        EGangotriUtil.PROGRAM_START_TIME_IN_SECONDS = System.currentTimeSeconds()
        log.info "Program $program started @ " + UploadUtils.getFormattedDateString()
    }

    static void recordProgramEnd(){
        EGangotriUtil.PROGRAM_END_TIME_IN_SECONDS = System.currentTimeSeconds()
        log.info "Program Execution ended @ " + UploadUtils.getFormattedDateString()
    }
}
