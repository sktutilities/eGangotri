package com.egangotri.scheduler

import com.egangotri.util.EGangotriUtil
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException

class ExecuteBatchJob implements Job {
    static final String REMOTE_INSTRUCTIONS_FILE = EGangotriUtil.EGANGOTRI_BASE_DIR + File.separator + "google_drive" + File.separator + "cron.txt"
    static final String DEFAULT_INSTRUCTION = "echo Hi @"
    void execute(JobExecutionContext context)
            throws JobExecutionException {

        println("Cron Job Started");
        //we have to clear it each time so that no instuction is repeated
        File fileWithInstructions = new File(REMOTE_INSTRUCTIONS_FILE)
        if(!fileWithInstructions.exists()){
            fileWithInstructions.createNewFile()
        }
        String instructions = fileWithInstructions.getText('UTF-8')
        if(!instructions){
            instructions = DEFAULT_INSTRUCTION + new Date().format('YYYY-MM-DD HH:mm')
        }
        println "cmd /c ${instructions}".execute().text
        fileWithInstructions.write(instructions)
        //To reboot use
        //"shutdown /r
    }

}