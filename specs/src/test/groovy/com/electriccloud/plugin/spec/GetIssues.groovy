package com.electriccloud.plugin.spec

import com.electriccloud.plugins.annotations.*
import spock.lang.*

class GetIssues extends PluginTestHelper {
    static procedureName = 'GetIssues'
    static projectName = "EC-Specs GetIssues"

    static def procedureParams = [
            config        : '',
            jiraIdentifier: '',
//            projectKey : '',
//            resultFormat: '',
//            resultPropertySheet: ''
    ]


    @Shared
    String config = CONFIG_NAME

    @Shared
    String issueId

    @Shared
    String caseId

    def doSetupSpec() {
        createConfiguration(CONFIG_NAME)

        // Import procedure project
        importProject(projectName, 'dsl/procedure.dsl', [
                projectName  : projectName,
                pluginName   : PLUGIN_NAME,
                procedureName: procedureName,
                resourceName : getResourceName(),
                params       : procedureParams,
        ])
    }

    def doCleanupSpec() {
        deleteConfiguration(PLUGIN_NAME, CONFIG_NAME)
//        conditionallyDeleteProject(projectName)
    }


    @Sanity
    def "#caseId. GetIssues simple"() {
        given:

        def procedureParams = [
                config        : config,
                jiraIdentifier: issueId
        ]

        when:
        def result = runProcedure(projectName, procedureName, procedureParams)

        then:
        println("JOB LINK: " + getJobLink(result.jobId))
        assert result.outcome == 'success'

        // Check logs

        // Check properties
        String resultProperty = getJobProperty('/myJob/steps/RunProcedure/issueKeys', result.jobId)
        assert resultProperty
        def resultKeys = resultProperty.split(/, ?/)

        for (String key : keys){
            assert resultKeys.contains(key)
        }


        where:
        caseId       | config      | issueId                      | keys
        'CHANGEME_1' | CONFIG_NAME | 'TEST-6'                     | ['TEST-6']
        'CHANGEME_2' | CONFIG_NAME | 'TEST-6, TEST-8'             | ['TEST-6', 'TEST-8']
        'CHANGEME_3' | CONFIG_NAME | 'ID IN ("TEST-6", "TEST-8")' | ['TEST-6', 'TEST-8']
    }
}