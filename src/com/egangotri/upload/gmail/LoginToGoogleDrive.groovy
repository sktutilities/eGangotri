package com.egangotri.upload.gmail

import com.egangotri.upload.util.UploadUtils
import com.egangotri.util.EGangotriUtil
import groovy.util.logging.Slf4j

@Slf4j
class LoginToGoogleDrive {

    static final List<String> LOGIN_PROFILES = ["jm", "lk", "sr"] // "BM", "MM", "jm" , "lk", "sr", "srCP" , "ij", "srb1", gb11

    static main(args) {
       log.info "start$args"
        List<String> loginProfiles = LOGIN_PROFILES
        if (args) {
           log.info "args $args"
            loginProfiles = args.toList()
        }

        loginProfiles.each { String profile ->
           log.info "profile: $profile"
            Hashtable<String, String> metaDataMap = UploadUtils.loadProperties(EGangotriUtil.GOOGLE_DRIVE_PROPERTIES_FILE)
            GoogleDriveHandler.login(metaDataMap, profile)
        }
    }
}
