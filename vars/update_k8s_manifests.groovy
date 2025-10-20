#!/usr/bin/env groovy

/**
 * Update Kubernetes manifests with new image tags and push to GitHub
 */
def call(Map config = [:]) {
    def imageTag = config.imageTag ?: error("Image tag is required")
    def manifestsPath = config.manifestsPath ?: 'kubernetes'
    def gitCredentials = config.gitCredentials ?: 'github-credentials'
    def gitUserName = config.gitUserName ?: 'Jenkins CI'
    def gitUserEmail = config.gitUserEmail ?: 'jenkins@example.com'
    def gitRepo = config.gitRepo ?: 'arnab-e-commerce-app'   // default repo

    echo "Updating Kubernetes manifests with image tag: ${imageTag}"

    withCredentials([usernamePassword(
        credentialsId: gitCredentials,
        usernameVariable: 'GIT_USERNAME',
        passwordVariable: 'GIT_PASSWORD'
    )]) {
        // Configure Git
        sh """
            git config user.name "${gitUserName}"
            git config user.email "${gitUserEmail}"
        """

        // Update deployment manifests
        sh """
            # Update main application deployment
            sed -i "s|image: trainwithshubham/easyshop-app:.*|image: arnab23/easyshop-app:${imageTag}|g" ${manifestsPath}/08-easyshop-deployment.yaml

            # Update migration job if it exists
            if [ -f "${manifestsPath}/12-migration-job.yaml" ]; then
                sed -i "s|image: arnab23/easyshop-migration:.*|image: arnab23/easyshop-migration:${imageTag}|g" ${manifestsPath}/12-migration-job.yaml
            fi

            # Update ingress domain if file exists
            if [ -f "${manifestsPath}/10-ingress.yaml" ]; then
                sed -i "s|host: .*|host: easyshop.letsdeployit.com|g" ${manifestsPath}/10-ingress.yaml
            fi
        """

        // Commit and push changes only if something changed
        sh """
            if git diff --quiet; then
                echo "No changes to commit"
            else
                git add ${manifestsPath}/*.yaml
                git commit -m "Update image tags to ${imageTag} and ensure correct domain [ci skip]"

                # Always push to the correct repo
                git remote set-url origin https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/Arnab2239/${gitRepo}.git
                git push origin HEAD:${GIT_BRANCH}
            fi
        """
    }
}
