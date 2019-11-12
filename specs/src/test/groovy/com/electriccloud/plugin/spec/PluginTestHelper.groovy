package com.electriccloud.plugin.spec

import com.electriccloud.spec.PluginSpockTestSupport

class PluginTestHelper extends PluginSpockTestSupport {

    static String PLUGIN_NAME = "CB-JIRA-Groovy"
    static String CONFIG_NAME = "specConfig"

    static String getResourceName() {
        return 'local'
    }

    def createConfiguration(String configName, Map props = [:]) {
        assert configName : 'no configName specified to the createConfiguration()'

        String authType = getAuthType()
        String oauthConsumerKey = getOAuthConsumerKey()

        String username = getJiraUsername()
        String password = getJiraPassword()

        def url = System.getenv('JIRA_URL') ?: 'http://jira.electric-cloud.com'

        def efProxyUrl = System.getenv('EF_PROXY_URL') ?: ''
        def efProxyUsername = System.getenv('EF_PROXY_USERNAME') ?: ''
        def efProxyPassword = System.getenv('EF_PROXY_PASSWORD') ?: ''

        if (System.getenv('RECREATE_CONFIG')) {
//            if (doesConfExist(PLUGIN_NAME, configName)){
                deleteConfiguration(PLUGIN_NAME, configName)
//            }
        }

        def result = dsl """
            runProcedure(
                projectName: '/plugins/${PLUGIN_NAME}/project',
                procedureName: 'CreateConfiguration',
                credential: [
                    [
                        credentialName: 'proxy_credential',
                        userName: '$efProxyUsername',
                        password: '$efProxyPassword'
                    ],
                    [
                        credentialName: 'oauth1_credential',
                        userName: '$username',
                        password: '$password'
                    ],
                    [
                        credentialName: 'basic_credential',
                        userName: '$username',
                        password: '$password'
                    ],
                ],
                actualParameter: [
                    endpoint          : '$url',
                    config            : '$configName',
                    oauth1_credential : 'oauth1_credential',
                    authScheme        : '$authType',
                    oauth1ConsumerKey : '$oauthConsumerKey',
                    httpProxyUrl      : '$efProxyUrl',
                    proxy_credential  : 'proxy_credential',
                    basic_credential  : 'basic_credential'
                ]
            )
            """
        assert result?.jobId
        waitUntil {
            jobCompleted(result)
        }
        assert jobStatus(result.jobId).outcome == 'success'
    }

    static String getOAuthConsumerKey() {
        return System.getenv('JIRA_CONSUMER_KEY')
    }

    static String getAuthType() {
        return getOAuthConsumerKey() ? "oauth1" : "basic"
    }

    static String getJiraUsername() {
        String username

        if (getOAuthConsumerKey()) {
            username = System.getenv('JIRA_OAUTH_TOKEN')
        } else {
            username = System.getenv('JIRA_USERNAME') ?: 'admin'
        }

        assert username
        return username
    }

    static String getJiraPassword() {
        String password

        if (getOAuthConsumerKey()) {
            password = System.getenv('JIRA_PRIVATE_KEY')
        } else {
            password = System.getenv('JIRA_PASSWORD') ?: 'changeme'
        }

        assert password
        return password
    }

}
