package com.egangotri.upload.archive

import com.egangotri.upload.util.ArchiveUtil
import com.egangotri.upload.util.SettingsUtil
import com.egangotri.upload.util.UploadUtils
import com.egangotri.upload.util.ValidateUtil
import com.egangotri.upload.vo.ItemsVO
import com.egangotri.upload.vo.LinksVO
import com.egangotri.upload.vo.UploadVO
import com.egangotri.util.EGangotriUtil
import groovy.util.logging.Slf4j

import static com.egangotri.upload.util.ArchiveUtil.storeQueuedItemsInFile

@Slf4j
class ValidateUploadsAndReUploadFailedItems {
    static Set archiveProfiles = []
    static File usheredFile = null
    static File queuedFile = null
    static List<LinksVO> identifierLinksForTesting = []
    static List<ItemsVO> queuedItemsForTesting = []
    static List<LinksVO> failedLinks = []
    static List<ItemsVO> missedOutQueuedItems = []
    static List<? extends UploadVO> allFailedItems =  []


    static main(args) {
        SettingsUtil.applySettingsWithReuploaderFlags()
        execute(args)
        System.exit(0)
    }

    static void execute(def args = [] ){
        setCSVsForValidation(args)
        UploadUtils.resetGlobalUploadCounter()
        processUsheredCSV()
        processQueuedCSV()
        findQueueItemsNotInUsheredCSV()
        //filterFailedUsheredItems()
        //(generateFailedLinksFromStaticList) for use in special cases only
        generateFailedLinksFromStaticList()
        combineAllFailedItems()
        startReuploadOfFailedItems()
    }

    static void findMissedQueueItemsOnlyAndReupload(boolean reupload = true){
        SettingsUtil.applySettingsWithReuploaderFlags([false,true,!reupload])
        execute()
    }

    static void findMissedUsheredItemsOnlyAndReupload(boolean reupload = false){
        SettingsUtil.applySettingsWithReuploaderFlags([true,false,reupload])
        execute()
    }

    static void processUsheredCSV() {
        identifierLinksForTesting = ValidateUtil.csvToUsheredItemsVO(usheredFile)
        archiveProfiles = identifierLinksForTesting*.archiveProfile as Set
        log.info("Converted " + identifierLinksForTesting.size() + " links of upload-ushered Item(s) from CSV in " + "Profiles ${archiveProfiles.toString()}")
    }

    static void processQueuedCSV() {
        queuedItemsForTesting = ValidateUtil.csvToItemsVO(queuedFile)
        Set queuedProfiles = queuedItemsForTesting*.archiveProfile as Set
        log.info("Converted " + queuedItemsForTesting.size() + " Queued Item(s) from CSV in " + "Profiles ${queuedProfiles.toString()}")
    }

    static void setCSVsForValidation(def args) {
        usheredFile = new File(EGangotriUtil.ARCHIVE_ITEMS_USHERED_FOLDER).listFiles()?.sort { -it.lastModified() }?.head()
        queuedFile = new File(EGangotriUtil.ARCHIVE_ITEMS_QUEUED_FOLDER).listFiles()?.sort { -it.lastModified() }?.head()

        if (!usheredFile) {
            log.error("No Files in ${EGangotriUtil.ARCHIVE_ITEMS_USHERED_FOLDER}.Cannot proceed. Quitting")
            System.exit(0)
        }

        if (!queuedFile) {
            log.error("No Files in ${EGangotriUtil.ARCHIVE_ITEMS_QUEUED_FOLDER}.Cannot proceed. Quitting")
            System.exit(0)
        }
        if (args) {
            println "args $args"
            if (args?.size() > 2) {
                log.error("Only 2 File Name(s) can be accepted.Cannot proceed. Quitting")
                System.exit(0)
            }
            String _file_1 = args.first().endsWith(".csv") ? args.first() : args.first() + ".csv"
            String _file_2 = args.last().endsWith(".csv") ? args.last() : args.last() + ".csv"
            usheredFile = new File(EGangotriUtil.ARCHIVE_ITEMS_USHERED_FOLDER + File.separator + _file_1)
            queuedFile = new File(EGangotriUtil.ARCHIVE_ITEMS_QUEUED_FOLDER + File.separator + _file_2)
            if (!usheredFile) {
                log.error("No such File ${usheredFile} in ${EGangotriUtil.ARCHIVE_ITEMS_USHERED_FOLDER}.Cannot proceed. Quitting")
                System.exit(0)
            }
            if (!queuedFile) {
                log.error("No such File ${queuedFile} in ${EGangotriUtil.ARCHIVE_ITEMS_QUEUED_FOLDER}.Cannot proceed. Quitting")
                System.exit(0)
            }
        }
        println("Identifier File for processing: ${usheredFile.name}")
        println("Queue File for processing: ${queuedFile.name}")
    }

    // Thsi function produces QueuedItem - IdentifierGeneratedItem
    //Queued Item is a superset of IdentifierGeneratedItem
    static void findQueueItemsNotInUsheredCSV() {
        if(SettingsUtil.IGNORE_QUEUED_ITEMS_IN_REUPLOAD_FAILED_ITEMS){
            log.info("Queued Items will be ignored for upload")
            return
        }
        List allFilePaths = identifierLinksForTesting*.path
        log.info("Searching from ${queuedItemsForTesting?.size()} Queued Item(s) that were never upload-ushered in ${allFilePaths.size()} identifiers")

        queuedItemsForTesting.eachWithIndex { queuedItem, index ->
            if (!allFilePaths.contains(queuedItem.path)) {
                missedOutQueuedItems << queuedItem
                log.info("\tFound missing Item [ (# $index). ${queuedItem.archiveProfile}] ${queuedItem.title} ")
            }
        }
        log.info("${missedOutQueuedItems.size()}/${queuedItemsForTesting.size()} Items found in Queued List that missed upload. Affected Profies "  +  (missedOutQueuedItems*.archiveProfile as Set).toString())
    }

    static void filterFailedUsheredItems() {
        if(SettingsUtil.IGNORE_USHERED_ITEMS_IN_REUPLOAD_FAILED_ITEMS){
            log.info("Ushered Items will be ignored for upload")
            return
        }
        int testableLinksCount = identifierLinksForTesting.size()
        log.info("Testing ${testableLinksCount} Links in archive for upload-success-confirmation")

        identifierLinksForTesting.eachWithIndex { LinksVO entry, int i ->
            try {
                entry.archiveLink.toURL().text
                print("${i},")
            }
            catch (FileNotFoundException e) {
                entry.uploadLink = entry.uploadLink.replace("=eng", "=san")
                failedLinks << entry
                println("\nFailed Link: \"${entry.archiveLink}\"(${failedLinks.size()} of $testableLinksCount) !!! @ ${i}..")
            }
            catch (Exception e) {
                log.error("This is an Unsual Error. ${entry.archiveLink} Check Manually" + e.message)
                e.printStackTrace()
                failedLinks << entry
            }
            if(i%35 == 0){
                //Thread.sleep(5000)
                System.gc()
                println("")
            }
        }
        log.info("\n${failedLinks.size()} failedLink" + " Item(s) found in Ushered List that were missing." +
                " Affected Profie(s)" +  (failedLinks*.archiveProfile as Set).toString())
        log.info("Failed Links: " + (failedLinks*.archiveLink.collect{ _link-> "'" + _link + "'"}))
    }


    //This static variable can only be used with generateFailedLinksFromStaticList()
    static  List<String> _staticListOfBadLinks =['https://archive.org/details/weorournationhooddefinedshrim.s.golwalkarguruji1939_143_a', 'https://archive.org/details/gurujivyaktiaurkaryapalkarn.h1m.s.golwalkar_601_O', 'https://archive.org/details/gurujivyaktiaurkaryapalkarn.h2m.s.golwalkar_550_v', 'https://archive.org/details/gurujiindianmuslimsm.s.golwalkar_792_G', 'https://archive.org/details/shrigurujisamagravol01m.s.golwalkar_785_A', 'https://archive.org/details/shrigurujisamagravol02m.s.golwalkar_673_H', 'https://archive.org/details/shrigurujisamagravol03m.s.golwalkar_177_O', 'https://archive.org/details/shrigurujisamagravol04m.s.golwalkar_180_H', 'https://archive.org/details/shrigurujisamagravol05m.s.golwalkar_356_Y', 'https://archive.org/details/shrigurujisamagravol06m.s.golwalkar_698_F', 'https://archive.org/details/shrigurujisamagravol07m.s.golwalkar_301_p', 'https://archive.org/details/shrigurujisamagravol08m.s.golwalkar_359_l', 'https://archive.org/details/shrigurujisamagravol09m.s.golwalkar_361_K', 'https://archive.org/details/shrigurujisamagravol10m.s.golwalkar_753_M', 'https://archive.org/details/shrigurujisamagravol11m.s.golwalkar_863_J', 'https://archive.org/details/shrigurujisamagravol12m.s.golwalkar_460_a', 'https://archive.org/details/gurujibiographybycpbhishikarenglishm.s.golwalkar_650_K', 'https://archive.org/details/gurujiavisionaryen001m.s.golwalkar_21_P', 'https://archive.org/details/gurujiekanaukhanetrutvav001m.s.golwalkar_98_n', 'https://archive.org/details/gurujivyaktitvakrutitvav004m.s.golwalkar_419_W', 'https://archive.org/details/gurujiaurrajnitim.s.golwalkar_242_K', 'https://archive.org/details/gurujiaurrashtraavadharanam.s.golwalkar_765_b', 'https://archive.org/details/gurujiaursamajiksamarastam.s.golwalkar_562_L', 'https://archive.org/details/gurujiauryuvam.s.golwalkar_611_S', 'https://archive.org/details/gurujibodhakatham.s.golwalkar_377_g', 'https://archive.org/details/gurujikaaarthikchintanm.s.golwalkar_537_k', 'https://archive.org/details/gurujikamargadarshanm.s.golwalkar_687_W', 'https://archive.org/details/gurujikaprabodhanm.s.golwalkar_792_A', 'https://archive.org/details/gurujikasamajikdarshanm.s.golwalkar_950_w', 'https://archive.org/details/gurujikepreraksansmaranm.s.golwalkar_679_e', 'https://archive.org/details/gurujiandmatrushaktim.s.golwalkar_293_t', 'https://archive.org/details/gurujiandthechristianmissionsm.s.golwalkar_290_o', 'https://archive.org/details/gurujionhinduviewoflifem.s.golwalkar_24_J', 'https://archive.org/details/poorvanchalaurshrigurujim.s.golwalkar_914_N', 'https://archive.org/details/shrigurujipioneerofanewera.englishm.s.golwalkar_744_I', 'https://archive.org/details/sanghatmasrigurujihhtdesaisgg61m.s.golwalkar_561_K', 'https://archive.org/details/shreegurujiekswayamsevaknarendramodim.s.golwalkar_877_L', 'https://archive.org/details/gurujihindibiobook003m.s.golwalkar_143_V', 'https://archive.org/details/yugdrasthasrigurujihi001m.s.golwalkar_364_i', 'https://archive.org/details/bharatiyasamskritisaneguruji1953_566_s']

    /** This method is used in unique cases.
     *  Where u have a list of failed Archive Urls and you want to use them to reupload them only
     * So u take the links copy paste to _staticListOfBadLinks ,
     * have following settings:
     * IGNORE_QUEUED_ITEMS_IN_REUPLOAD_FAILED_ITEMS=true
     * IGNORE_USHERED_ITEMS_IN_REUPLOAD_FAILED_ITEMS=false
     * ONLY_GENERATE_STATS_IN_REUPLOAD_FAILED_ITEMS=false
     *generating vos
     * comment out call to filterFailedUsheredItems()
     * uncomment call to generateFailedLinksFromStaticList() and execute the program
     *
     * .
     * Then upload the VOS
     */
    static void generateFailedLinksFromStaticList(){
        log.info("generating vos from static list of Links with size: " + _staticListOfBadLinks.size())
        SettingsUtil.IGNORE_QUEUED_ITEMS_IN_REUPLOAD_FAILED_ITEMS=true
        SettingsUtil.IGNORE_USHERED_ITEMS_IN_REUPLOAD_FAILED_ITEMS=false
        SettingsUtil.ONLY_GENERATE_STATS_IN_REUPLOAD_FAILED_ITEMS=false

        identifierLinksForTesting.eachWithIndex{ LinksVO entry, int i ->
            if(_staticListOfBadLinks*.trim().contains(entry.archiveLink)){
                entry.uploadLink = entry.uploadLink.replace("=eng", "=san")
                println("entry.uploadLink: " + entry.uploadLink)
                failedLinks << entry
            }
        }
    }

    static void combineAllFailedItems(){
        if (missedOutQueuedItems || failedLinks) {
            allFailedItems = missedOutQueuedItems

            failedLinks.each { failedLink ->
                allFailedItems.add(failedLink)
            }
            log.info("Combined figure for re-uploading(${missedOutQueuedItems.size()} + ${failedLinks.size()}) :" + allFailedItems.size() + " in Profiles: ${allFailedItems*.archiveProfile as Set}" )
        }
    }

    static void startReuploadOfFailedItems() {
        if(SettingsUtil.ONLY_GENERATE_STATS_IN_REUPLOAD_FAILED_ITEMS){
            log.info("Only stats generated. No Uploading due to Setting")
            return
        }
        Set<String> profilesWithFailedLinks = allFailedItems*.archiveProfile as Set
        Hashtable<String, String> metaDataMap = UploadUtils.loadProperties(EGangotriUtil.ARCHIVE_PROPERTIES_FILE)
        Set<String> validProfiles = ArchiveUtil.filterInvalidProfiles(profilesWithFailedLinks, metaDataMap)
        _execute(validProfiles, metaDataMap)
    }

    static _execute(Set<String> profiles, Hashtable<String, String> metaDataMap){
        Map<Integer, String> uploadSuccessCheckingMatrix = [:]
        EGangotriUtil.recordProgramStart("ValidateUploadsAndReUploadFailedItems")

        ArchiveUtil.GRAND_TOTAL_OF_ALL_UPLODABLES_IN_CURRENT_EXECUTION = allFailedItems.size()
        ValidateUtil.validateMaxUploadableLimit()

        int attemptedItemsTotal = 0

        profiles.eachWithIndex { archiveProfile, index ->
            List<UploadVO> failedItemsForProfile = allFailedItems.findAll { it.archiveProfile == archiveProfile }
            int countOfUploadableItems = failedItemsForProfile.size()
            if (countOfUploadableItems) {
                log.info "getUploadablesForProfile: $archiveProfile: ${countOfUploadableItems}"
                storeQueuedItemsInFile(failedItemsForProfile)
                List<Integer> uploadStats = ArchiveHandler.uploadAllItemsToArchiveByProfile(metaDataMap, failedItemsForProfile)
                String report = UploadUtils.generateStats([uploadStats], archiveProfile, countOfUploadableItems)
                uploadSuccessCheckingMatrix.put((index + 1), report)
                attemptedItemsTotal += countOfUploadableItems
            }
            else {
                log.info "No uploadable files for Profile $archiveProfile"
            }
            log.info "${index + 1}). Starting upload in archive.org for Profile $archiveProfile. Total Uplodables: ${countOfUploadableItems}/${ArchiveUtil.GRAND_TOTAL_OF_ALL_UPLODABLES_IN_CURRENT_EXECUTION}"
            EGangotriUtil.sleepTimeInSeconds(5)
        }

        EGangotriUtil.recordProgramEnd()
        ArchiveUtil.printFinalReport(uploadSuccessCheckingMatrix, attemptedItemsTotal)
    }
}
