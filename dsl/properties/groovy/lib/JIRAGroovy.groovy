import com.electriccloud.flowpdf.Context
import com.electriccloud.flowpdf.FlowPlugin
import com.electriccloud.flowpdf.StepParameters
import com.electriccloud.flowpdf.StepResult
import com.electriccloud.flowpdf.client.REST
import groovy.json.JsonOutput

/**
 * JIRAGroovy
 */
class JIRAGroovy extends FlowPlugin {

    @Override
    Map<String, Object> pluginInfo() {
        return [
                pluginName         : '@PLUGIN_KEY@',
                pluginVersion      : '@PLUGIN_VERSION@',
                configFields       : ['config'],
                configLocations    : ['ec_plugin_cfgs'],
                defaultConfigValues: [:]
        ]
    }

/**
 * getIssues - GetIssues/GetIssues
 * Add your code into this method and it will be called when the step runs
 * @param config (required: true)
 * @param jiraIdentifier (required: true)
 */
    def getIssues(StepParameters runtimeParameters, StepResult sr) {

        /* Log is automatically available from the parent class */
        log.info(
                "getIssues was invoked with StepParameters",
                /* runtimeParameters contains both configuration and procedure parameters */
                runtimeParameters.toString()
        )

        Context context = getContext()
        REST rest = context.newRESTClient()

        String issueIdentifier = runtimeParameters.getRequiredParameter('jiraIdentifier').getValue()
        String jql
        // issue ID(s)
        if (issueIdentifier =~ /^[A-Za-z]+-[0-9]$/
                || issueIdentifier =~ /^((?:[A-Za-z]+-[0-9]), ?)+(?:[A-Za-z]+-[0-9])$/) {
            jql = "ID IN (${issueIdentifier})"
        } else { //JQL
            jql = issueIdentifier
        }

        def requestParams = [
                method: 'GET',
                path  : '/rest/api/2/search',
                query : ['search': jql]
        ]

        def issues = retrieveChunkedResults(requestParams)

        log.debug("Search result", issues.toString())

        String issueKeys = issues.collect({ it -> return it.key }).join(',')

        // Save JSON
        def jsonIssuesStr = JsonOutput.toJson(issues)
        sr.setOutcomeProperty('/myCall/issues', jsonIssuesStr)
        sr.setOutcomeProperty('/myCall/issuesCount', issues.size() as String)
        sr.setOutcomeProperty('/myCall/issueKeys', issueKeys)

        // Setting job step summary
        sr.setJobStepSummary("Retrieved ${issues.size()} issues")
        sr.setOutputParameter('issueIds', issueKeys)

        sr.apply()
        log.info("step GetIssues has been finished")
    }

// === step ends ===

    def retrieveChunkedResults(Map<String, Object> requestParameters, Map options = [:]) {

        // Defaults
        options['startAt'] = options['startAt'] ?: 0
        options['maxResults'] = options['maxResults'] ?: 50

        // Initializing the client and the request
        def restClient = context.newRESTClient()
        def request = restClient.newRequest(requestParameters)

        // Applying options to the request
        if (options['startAt'] != 0) {
            request.setQueryParameter('startAt', options['startAt'] as String)
        }
        request.setQueryParameter('maxResults', options['maxResults'] as String)

        log.debug("Retrieving starting from ${options['startAt']}.")

        // Retrieving issues
        def retrievedIssues = []
        def currentRequestResult = restClient.doRequest(request)
        if (currentRequestResult['total'] > 0) {
            for (def it : currentRequestResult['issues']) retrievedIssues.push(it)

            // Recursively retrieving next issues
            int lastIssueNum = currentRequestResult['startAt'] + currentRequestResult['maxResults']
            if (currentRequestResult['total'] > lastIssueNum) {
                def nextIssues = retrieveChunkedResults(requestParameters, [startAt: lastIssueNum + 1])
                for (def it : nextIssues) retrievedIssues.push(it)
            }
        }

        // Return result
        return retrievedIssues
    }
}