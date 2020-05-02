package com.egangotri.upload.vo

import com.egangotri.upload.util.ArchiveUtil

class LinksVO extends UploadVO{
    String archiveLink

    LinksVO(List<String> fields){
        super()
        archiveProfile = fields[0]
        uploadLink = fields[1]?.replaceAll("\"", "'")
        path = fields[2]
        title = fields[3]
        archiveLink = ArchiveUtil.ARCHIVE_DOCUMENT_DETAIL_URL + fields[4]
    }
    @Override
    public String toString(){
        return super.toString() + " \n" + archiveLink
    }
}
