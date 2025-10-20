#!/usr/bin/env groovy

def call(Map config = [:]) {
    def imageTag = config.imageTag ?: error("Image tag is required")
    def manifestsPath = config.manifestsPath ?: 'kubernetes'
    def gitCredentials = config.gitCredentials ?: 'github-credentials'
    def gitUserName = config.gitUserName ?: 'Jenkins CI'
    def gitUserEmail = config.gitUserEmail ?: 'jenkins@example.com'
    def gitBranch = config.gitBranch ?: 'master'

    echo "Updating Kubernetes manifests with image tag: ${imageTag}"

    withCredentials([usernamePassword(
        credentialsId: gitCredentials,
        usernameVariable: 'GIT_USERNAME',
        passwordVariable: 'GIT_PASSWORD'
    )]) {

        sh """
            git config --global --add safe.directory "$PWD"
            git config user.name "${gitUserName}"
            git config user.email "${gitUserEmail}"

            # Update deployment manifests
            sed -i "s|image: trainwithshubham/easyshop-app:.*|image: arnab23/easyshop-app:${imageTag}|g" ${manifestsPath}/08-easyshop-deployment.yaml
            
            if [ -f "${manifestsPath}/12-migration-job.yaml" ]; then
                sed -i "s|image: arnab23/easyshop-migration:.*|image: arnab23/easyshop-migration:${imageTag}|g" ${manifestsPath}/12-migration-job.yaml
            fi

            if [ -f "${manifestsPath}/10-ingress.yaml" ]; then
                sed -i "s|host: .*|host: easyshop.letsdeployit.com|g" ${manifestsPath}/10-ingress.yaml
            fi

            # Commit changes if any
            if git diff --quiet; then
                echo "No changes to commit"
            else
                git add ${manifestsPath}/*.yaml
                git commit -m "Update image tags to ${imageTag} and ensure correct domain [ci skip]"

                # Set remote with credentials and push
                git remote set-url origin https://\${GIT_USERNAME}:\${GIT_PASSWORD}@github.com/Arnab2239/arnab-e-commerce-app.git
                git push origin HEAD:${gitBranch}
            fi
        """
    }
}
