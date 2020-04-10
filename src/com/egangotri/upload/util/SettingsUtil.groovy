package com.egangotri.upload.util

import com.egangotri.upload.archive.ValidateUploadsAndReUploadFailedItems
import com.egangotri.util.EGangotriUtil
import com.egangotri.util.FileUtil
import groovy.util.logging.Slf4j

@Slf4j
class SettingsUtil {
    static boolean IGNORE_QUEUED_ITEMS_IN_REUPLOAD_FAILED_ITEMS = false
    static boolean IGNORE_USHERED_ITEMS_IN_REUPLOAD_FAILED_ITEMS = false
    static boolean ONLY_GENERATE_STATS_IN_REUPLOAD_FAILED_ITEMS = false
    static boolean MOVE_FILES_DUE_TO_CODE_503_SLOW_DOWN = false
    static boolean PREVIEW_FILES = true
    static String DEFAULT_LANGUAGE_ISO_CODE = "san"
    static List<String> IGNORE_EXTENSIONS = ["jpg","gif","bmp","png", "tif", "tiff","exe","jpeg","msi","ini","bat","jar","chm", "db"]
    static List<String> IGNORE_FILES_AND_FOLDERS_WITH_KEYWORDS=["freeze", "upload", "_dont"]

    static int MINIMUM_FILE_NAME_LENGTH = 1

    static void applySettings(boolean createVOSavingFiles = true) {
        UploadUtils.resetGlobalUploadCounter()
        if(createVOSavingFiles){
            ArchiveUtil.createVOSavingFiles()
        }
        Hashtable<String, String> settingsMetaDataMap = UploadUtils.loadProperties(EGangotriUtil.SETTINGS_PROPERTIES_FILE)
        if (settingsMetaDataMap) {
            if (settingsMetaDataMap.PARTITION_SIZE && settingsMetaDataMap.PARTITION_SIZE.isInteger() && settingsMetaDataMap.PARTITION_SIZE.toInteger() > 0) {
                try {
                    Integer partitionSize = Integer.parseInt(settingsMetaDataMap.PARTITION_SIZE)
                    if (partitionSize > 0) {
                        EGangotriUtil.PARTITION_SIZE = partitionSize
                        EGangotriUtil.PARTITIONING_ENABLED = true
                    }
                }
                catch (Exception e) {
                    log.info("PARTITION_SIZE : ${settingsMetaDataMap.PARTITION_SIZE} is not a valid mumber. will not be considered")
                }
                log.info "settingsMetaDataMap.PARTITION_SIZE ${settingsMetaDataMap.PARTITION_SIZE}"
            }
            //ADJUST_SLEEP_TIMES
            if (settingsMetaDataMap.ADJUST_SLEEP_TIMES) {
                try {
                    Float calibrateTimes = Float.parseFloat(settingsMetaDataMap.ADJUST_SLEEP_TIMES)
                    if (calibrateTimes >= 0.1 && calibrateTimes <= 5) {
                        EGangotriUtil.ARCHIVE_WAITING_PERIOD_ONE_SEC *= calibrateTimes
                    }
                }
                catch (Exception e) {
                    log.info("ADJUST_SLEEP_TIMES is not a valid decimal mumber. will not be considered")
                }
                log.info("ADJUST_SLEEP_TIMES: " + settingsMetaDataMap.ADJUST_SLEEP_TIMES)
            }

            if (settingsMetaDataMap.PRECUTOFF_PROFILE) {
                EGangotriUtil.PRECUTOFF_PROFILE = settingsMetaDataMap.PRECUTOFF_PROFILE
                log.info("PRECUTOFF_PROFILE: " + EGangotriUtil.PRECUTOFF_PROFILE)
            }

            if (settingsMetaDataMap.MAX_UPLODABLES) {
                try {
                    int maxUplodables = Integer.parseInt(settingsMetaDataMap.MAX_UPLODABLES)
                    if (maxUplodables >= 50 && maxUplodables <= 1500) {
                        EGangotriUtil.MAX_UPLODABLES = maxUplodables
                    }
                }
                catch (Exception e) {
                    log.info("MAX_UPLODABLES is not a valid Integer. will not be considered")
                }
                log.info("MAX_UPLODABLES: " + EGangotriUtil.MAX_UPLODABLES)
            }
            if (settingsMetaDataMap.PDF_ONLY && settingsMetaDataMap.PDF_ONLY == "true") {
                FileUtil.PDF_ONLY = settingsMetaDataMap.PDF_ONLY
                FileUtil.PDF_REGEX = FileUtil.PDF_ONLY ? /.*.pdf/ : /.*/
                log.info "settingsMetaDataMap.PDF_ONLY ${settingsMetaDataMap.PDF_ONLY}"
                log.info("PDF_REGEX: " + settingsMetaDataMap.PDF_ONLY.toBoolean() + " " + FileUtil.PDF_ONLY + " " + FileUtil.PDF_REGEX)
            }

            if (settingsMetaDataMap.CREATOR_FROM_DASH_SEPARATED_STRING) {
                EGangotriUtil.CREATOR_FROM_DASH_SEPARATED_STRING = settingsMetaDataMap.CREATOR_FROM_DASH_SEPARATED_STRING.toBoolean()
                log.info("CREATOR_FROM_DASH_SEPARATED_STRING: " + EGangotriUtil.CREATOR_FROM_DASH_SEPARATED_STRING)
            }

            if (settingsMetaDataMap.ADD_RANDOM_INTEGER_TO_PAGE_URL) {
                EGangotriUtil.ADD_RANDOM_INTEGER_TO_PAGE_URL = settingsMetaDataMap.ADD_RANDOM_INTEGER_TO_PAGE_URL.toBoolean()
                log.info("ADD_RANDOM_INTEGER_TO_PAGE_URL: " + EGangotriUtil.ADD_RANDOM_INTEGER_TO_PAGE_URL)
            }

            if (settingsMetaDataMap.GENERATE_ONLY_URLS) {
                EGangotriUtil.GENERATE_ONLY_URLS = settingsMetaDataMap.GENERATE_ONLY_URLS.toBoolean()
                log.info("GENERATE_ONLY_URLS: " + EGangotriUtil.GENERATE_ONLY_URLS)
            }

            if (settingsMetaDataMap.GENERATE_RANDOM_CREATOR) {
                EGangotriUtil.GENERATE_RANDOM_CREATOR = settingsMetaDataMap.GENERATE_RANDOM_CREATOR.toBoolean()
                log.info("GENERATE_RANDOM_CREATOR: " + EGangotriUtil.GENERATE_RANDOM_CREATOR)
            }

            if (settingsMetaDataMap.RANDOM_CREATOR_MAX_LIMIT && settingsMetaDataMap.RANDOM_CREATOR_MAX_LIMIT.isInteger()) {
                int randomCreatorMaxLimit = settingsMetaDataMap.RANDOM_CREATOR_MAX_LIMIT.toInteger()
                if (randomCreatorMaxLimit >= 10 && randomCreatorMaxLimit <= 1000) {
                    UploadUtils.RANDOM_CREATOR_MAX_LIMIT = randomCreatorMaxLimit
                }
                log.info("RANDOM_CREATOR_MAX_LIMIT: " + UploadUtils.RANDOM_CREATOR_MAX_LIMIT)
            }

            if (settingsMetaDataMap.IGNORE_CREATOR_SETTINGS_FOR_ACCOUNTS) {
                EGangotriUtil.IGNORE_CREATOR_SETTINGS_FOR_ACCOUNTS = csvToList(settingsMetaDataMap.IGNORE_CREATOR_SETTINGS_FOR_ACCOUNTS)
                log.info("IGNORE_CREATOR_SETTINGS_FOR_ACCOUNTS: " + EGangotriUtil.IGNORE_CREATOR_SETTINGS_FOR_ACCOUNTS)
            }

            if (settingsMetaDataMap.IGNORE_EXTENSIONS) {
                IGNORE_EXTENSIONS = csvToList(settingsMetaDataMap.IGNORE_EXTENSIONS)
                log.info("IGNORE_EXTENSIONS: " + IGNORE_EXTENSIONS)
            }

            if (settingsMetaDataMap.IGNORE_FILES_AND_FOLDERS_WITH_KEYWORDS) {
                IGNORE_FILES_AND_FOLDERS_WITH_KEYWORDS = csvToList(settingsMetaDataMap.IGNORE_FILES_AND_FOLDERS_WITH_KEYWORDS)
                log.info("IGNORE_FILES_AND_FOLDERS_WITH_KEYWORDS: " + IGNORE_FILES_AND_FOLDERS_WITH_KEYWORDS)
            }

            if (settingsMetaDataMap.DEFAULT_LANGUAGE_ISO_CODE) {
                DEFAULT_LANGUAGE_ISO_CODE = settingsMetaDataMap.DEFAULT_LANGUAGE_ISO_CODE
                log.info("DEFAULT_LANGUAGE_ISO_CODE: " + DEFAULT_LANGUAGE_ISO_CODE)
            }

            if (ArchiveUtil.ValidateUploadsAndReUploadFailedItems) {
                if (settingsMetaDataMap.IGNORE_QUEUED_ITEMS_IN_REUPLOAD_FAILED_ITEMS) {
                    IGNORE_QUEUED_ITEMS_IN_REUPLOAD_FAILED_ITEMS = settingsMetaDataMap.IGNORE_QUEUED_ITEMS_IN_REUPLOAD_FAILED_ITEMS.toBoolean()
                    log.info("IGNORE_QUEUED_ITEMS_IN_REUPLOAD_FAILED_ITEMS: " + IGNORE_QUEUED_ITEMS_IN_REUPLOAD_FAILED_ITEMS)
                }

                if (settingsMetaDataMap.IGNORE_USHERED_ITEMS_IN_REUPLOAD_FAILED_ITEMS) {
                    IGNORE_USHERED_ITEMS_IN_REUPLOAD_FAILED_ITEMS = settingsMetaDataMap.IGNORE_USHERED_ITEMS_IN_REUPLOAD_FAILED_ITEMS.toBoolean()
                    log.info("IGNORE_USHERED_ITEMS_IN_REUPLOAD_FAILED_ITEMS: " + IGNORE_USHERED_ITEMS_IN_REUPLOAD_FAILED_ITEMS)
                }

                if (settingsMetaDataMap.ONLY_GENERATE_STATS_IN_REUPLOAD_FAILED_ITEMS) {
                    ONLY_GENERATE_STATS_IN_REUPLOAD_FAILED_ITEMS = settingsMetaDataMap.ONLY_GENERATE_STATS_IN_REUPLOAD_FAILED_ITEMS.toBoolean()
                    log.info("ONLY_GENERATE_STATS_IN_REUPLOAD_FAILED_ITEMS: " + ONLY_GENERATE_STATS_IN_REUPLOAD_FAILED_ITEMS)
                }

                if (settingsMetaDataMap.MOVE_FILES_DUE_TO_CODE_503_SLOW_DOWN) {
                    MOVE_FILES_DUE_TO_CODE_503_SLOW_DOWN = settingsMetaDataMap.MOVE_FILES_DUE_TO_CODE_503_SLOW_DOWN.toBoolean()
                    log.info("MOVE_FILES_DUE_TO_CODE_503_SLOW_DOWN: " + MOVE_FILES_DUE_TO_CODE_503_SLOW_DOWN)
                }
            }

            if (settingsMetaDataMap.PREVIEW_FILES) {
                PREVIEW_FILES = settingsMetaDataMap.PREVIEW_FILES.toBoolean()
                log.info("PREVIEW_FILES: " + PREVIEW_FILES)
            }

            if (settingsMetaDataMap.MINIMUM_FILE_NAME_LENGTH && settingsMetaDataMap.MINIMUM_FILE_NAME_LENGTH.isInteger()) {
                MINIMUM_FILE_NAME_LENGTH = settingsMetaDataMap.MINIMUM_FILE_NAME_LENGTH.toInteger()
                log.info("MINIMUM_FILE_NAME_LENGTH: " + MINIMUM_FILE_NAME_LENGTH)
            }

        }
    }

    static void applySettingsWithReuploaderFlags(List<Boolean> reuploaderFlags = []) {
        resetValues()
        ArchiveUtil.ValidateUploadsAndReUploadFailedItems = true
        applySettings()
        if(reuploaderFlags?.size() >= 3){
            SettingsUtil.IGNORE_QUEUED_ITEMS_IN_REUPLOAD_FAILED_ITEMS=reuploaderFlags[0]
            SettingsUtil.IGNORE_USHERED_ITEMS_IN_REUPLOAD_FAILED_ITEMS=reuploaderFlags[1]
            SettingsUtil.ONLY_GENERATE_STATS_IN_REUPLOAD_FAILED_ITEMS=reuploaderFlags[2]
            if(reuploaderFlags.size() > 3){
                SettingsUtil.MOVE_FILES_DUE_TO_CODE_503_SLOW_DOWN = reuploaderFlags[3]
            }
        }
    }

    static List csvToList(String csv){
        csv = csv.replaceAll(/["|\[|\]|']/, "")
        //csv = csv.replace("[","").replace("]","")
        return csv.split(",")*.trim()
    }

    static void resetValues(){
        ValidateUploadsAndReUploadFailedItems.archiveProfiles = []
        ValidateUploadsAndReUploadFailedItems.USHERED_ITEMS_FILE = null
        ValidateUploadsAndReUploadFailedItems.QUEUED_ITEMS_FILE = null
        ValidateUploadsAndReUploadFailedItems.USHERED_LINKS_FOR_TESTING = []
        ValidateUploadsAndReUploadFailedItems.QUEUED_ITEMS_FOR_TESTING = []
        ValidateUploadsAndReUploadFailedItems.MISSED_OUT_USHERED_ITEMS = []
        ValidateUploadsAndReUploadFailedItems.MISSED_OUT_QUEUED_ITEMS = []
        ValidateUploadsAndReUploadFailedItems.ALL_FAILED_ITEMS =  []
        ValidateUploadsAndReUploadFailedItems.ITEMS_WITH_CODE_404_BAD_DATA =  []
        ValidateUploadsAndReUploadFailedItems.ITEMS_WITH_CODE_503_SLOW_DOWN =  []
        ArchiveUtil.generateFolder(EGangotriUtil.CODE_404_BAD_DATA_FOLDER)
        ArchiveUtil.generateFolder(EGangotriUtil.CODE_503_SLOW_DOWN_FOLDER)
    }
}
