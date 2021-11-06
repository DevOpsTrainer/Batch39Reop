
echo 'next job'
srcHome="/home/SVCJENBAT/ahmInfraAdmin"
BRANCH=${MD_ENV}
appName=${MD_APP}
egGroup=${MD_EXECUTIONGRP}
appEnv=${MD_ENVTO}

echo "last_successful_build=${last_successful_build}"
echo "DEPLOYED_BUILD=${VERSION_NUMBER}"
echo "RELEASE_NUMBER=${RELEASE_NUMBER}"
echo "Execution Group Name: ${egGroup}"
echo "Deployment Environment: ${appEnv}"
echo "Application Name: ${appName}"
echo "Artifactory Branch: ${BRANCH}"

echo ".........."

echo ".......Cleanup Process for Deployment......Start..."
sh ${srcHome}/cleanUp_process.sh ${appEnv} ${egGroup} ${appName} || exit 1
echo ".......Cleanup Process for Deployment......Completed..."

echo ".........."
 
echo ".......Preparation of required folders for Deployment......Start..."
sh ${srcHome}/PreparDeployEnv.sh ${appEnv} ${egGroup} ${appName} || exit 1
echo ".......Preparation of required folders for Deployment......Completed..."

echo ".........."

echo ".......Download Process for Deployment......Start..."
sh ${srcHome}/download_Artifacts.sh ${appEnv} ${egGroup} ${appName} ${BRANCH} ${VERSION_NUMBER} || exit 1
echo ".......Download Process for Deployment......Completed..."

echo ".........."

echo ".......Preparing Applicatio BAR with Environment Specific Property files......Start..."
sh ${srcHome}/BarOverrideWithEnv.sh ${appEnv} ${egGroup} ${appName} ${VERSION_NUMBER}|| exit 1
echo ".......Preparing Applicatio BAR with Environment Specific Property files......Completed..."

echo ".........."

echo ".......Application BAR Deployment on Environment 1......Start..."
sh ${srcHome}/BarDeploy.sh ${appEnv} ${egGroup} ${appName} 1 ${VERSION_NUMBER}|| exit 1
echo ".......Application BAR Deployment......Completed..."

echo ".........."

echo ".......Application BAR Deployment on Environment 2......Start..."
sh ${srcHome}/BarDeploy.sh ${appEnv} ${egGroup} ${appName} 2 ${VERSION_NUMBER}|| exit 1
echo ".......Application BAR Deployment......Completed..."

echo ".........."

echo ".......Application BAR Monitor on Environment 1......Start..."
sh ${srcHome}/BarMonitor.sh ${appEnv} ${egGroup} ${appName} 1 || exit 1
echo ".......Application BAR Deployment......Completed..."

echo ".........."

echo ".......Application BAR Monitor on Environment 2......Start..."
sh ${srcHome}/BarMonitor.sh ${appEnv} ${egGroup} ${appName} 2 || exit 1
echo ".......Application BAR Deployment......Completed..."