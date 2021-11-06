
File file = new File("${WORKSPACE}/iib/iib_${ServiceGroup}_Service.properties")
      println "IIB Services file path:${file.absolutePath}"
       lineNo = 1
       srcProp = [:]
      
      file.withReader { reader ->
         while ((srcProp = reader.readLine())!=null) {
             srcDetails = evaluate("$srcProp")
            
            
            println "(${lineNo}): Starting ${srcDetails.serviceName}: Service Build and Deploy Jobs-------"
            println ""
           
            def folderName = "${srcDetails.folderName}"
            println "FolderName: ${folderName}"
            
             serviceGrp = "${srcDetails.serviceGrp}".toString()
            println "ServiceGrp: ${serviceGrp}"
            
            def serviceName = "${srcDetails.serviceName}"
            println "ServiceName: ${serviceName}"
            
             def appName = "${srcDetails.appName}"
            println "AppName: ${appName}"
            
             gitHubRepo = "${srcDetails.gitHubRepo}"
            println "GitHubRepo: ${gitHubRepo}"
            
                     
            Properties confProp = new Properties()
              File confPropertiesFile = new File("$WORKSPACE/iib/iib_Conf.properties")
              confPropertiesFile.withInputStream {
                  confProp.load(it)
              }
             
             println "MD_YEAR: ${srcDetails.MD_YEAR}"
             println "MD_MONTH: ${srcDetails.MD_MONTH}"
                         
              accPermissions = evaluate("${confProp.accessPermissions}")
              accgrp = (accPermissions.get(serviceGrp))
              println "Build Access Granted To: ${accgrp}"
             
              emailList = evaluate("${confProp.emailRecipientList}")
              grpEmailList = (emailList.get(serviceGrp)).toString().replaceAll('[\\[\\]]',' ')
              println "Email Distribution List: ${grpEmailList}"
             
              depEnv = evaluate("${confProp.depEnv}")
             
              
              buildPa = "${confProp.artifactBranches}"
              buildPaths = Eval.me(buildPa)
             
             
    		  buildPaths.each { bpkey, bpval ->
				freeStyleJob("${srcDetails.folderName}/${srcDetails.serviceName}_${bpkey}") {
         	    description("--${srcDetails.serviceName} ${bpkey} Build Job---")
                    def gitCred = evaluate("${confProp.gitCredentials}")
             		def jHost = "hostname -s".execute().text
                    def jenHostName = "${jHost}".trim()
                 
                    triggers {
                          scm('H */1 * * *')
                            }
                    label("${confProp.buildServer}")
                    logRotator {
                                  daysToKeep(20)
                                  numToKeep(15)
                              }
                    jdk("${confProp.jdkVersion}")
                         authorization {
                           accgrp.eachWithIndex { agrp, idy ->
                             permission('hudson.model.Item.Build', "${agrp}")
                             permission('hudson.model.Item.Cancel', "${agrp}")
                             permission('hudson.model.Item.Discover', "${agrp}")
                             permission('hudson.model.Item.Read', "${agrp}")
                             permission('hudson.model.Item.Workspace', "${agrp}")
                                 }
                            }
                          
                    wrappers {
                         xvfb('Xvfb1') {
                           assignedLabels("${confProp.buildServer}")
                           parallelBuild(true)
                           shutdownWithBuild(true)
                           autoDisplayName(true)
                           shutdownWithBuild(true)
                           displayNameOffset(1)
                           debug(false)
                            timeout(0)
                               }
                           preBuildCleanup()
                                }
                    scm { 
                            git { 
                               remote { 
                                  github("ActiveHealth/${srcDetails.gitHubRepo}", 'https', 'github.com') 
                                 credentials(gitCred."${jenHostName}") 
                                    } 
                                     branch("${bpkey}") //adding regex to make sure it picks correct branch
                                 } 
                             } 
                    
                            steps {
                                ant {
                                    target('-Dbuild_parameter=${VERSION_NUMBER} run')
                                    prop('logging', 'info')
                                    buildFile('/${WORKSPACE}/build_devops.xml')
                                    javaOpt('-Xmx1g')
                                    antInstallation("${confProp.antVersion}")
                                    }
                                 
                                }
                           
                            configure {
                                concurrentBuild('false')
                                assignedNode("${confProp.buildServer}")
                                canRoam('false')
                                disabled('false')
                                it / 'buildWrappers' / 'org.jvnet.hudson.tools.versionnumber.VersionNumberBuilder'(plugin:'versionnumber@1.9') {
                                          'versionNumberString' ('${MD_YEAR}.${MD_MONTH}.$BUILD_NUMBER')
                                          'environmentVariableName' ('VERSION_NUMBER')
                                          'projectStartDate' ('1969-12-31 05:00:00.0 UTC')
                                          'worstResultForIncrement' ('NOT_BUILT')
                                          'skipFailedBuilds' (false)
                                          'useAsBuildDisplayName' (true)
                                           }
                        
                                it / 'properties' / 'job-metadata'(plugin:'metadata@1.1.0b') {'values'('class': 'linked-list'){
                                    'metadata-string'{
                                            'name'('YEAR')
                                            'parent'('class':"job-metadata", 'reference':"../../..")
                                            'generated'(false)
                                            'exposedToEnvironment'(true)
                                            'value'("${srcDetails.MD_YEAR}")
                                           }
                                    'metadata-string'{
                                            'name'('MONTH')
                                            'parent'('class':"job-metadata", 'reference':"../../..")
                                            'generated'(false)
                                            'exposedToEnvironment'(true)
                                            'value'("${srcDetails.MD_MONTH}")
                                            }
                                     'metadata-string'{
                                            'name'('APP')
                                            'parent'('class':"job-metadata", 'reference':"../../..")
                                            'generated'(false)
                                            'exposedToEnvironment'(true)
                                            'value'("${appName}")
                                              }
                                       }
                                                                                             
                                     }
                                 it / 'buildWrappers' / 'org.jfrog.hudson.generic.ArtifactoryGenericConfigurator'(plugin:'artifactory@2.15.1') {
                   
                                     'deployerDetails'{
                                     'artifactoryName' ('PDC 01')
                                     'artifactoryUrl' ("http://${confProp.artifactHost}.activehealth.loc:8081/artifactory")
                                     'deployReleaseRepository' ("libs-release-local") {
                                         keyFromText("libs-release-local")
                                         keyFromSelect("libs-release-local")
                                         dynamicMode(true)
                                           }
                                         }
                                      useSpecs(false)
                                      resolverDetails{
                                         artifactoryName('PDC 01')
                                         artifactoryUrl("http://${confProp.artifactHost}.activehealth.loc:8081/artifactory")
                                         resolveReleaseRepository("libs-release-local") {
					         keyFromText("libs-release-local")
					         keyFromSelect("libs-release-local")
					         dynamicMode(true)
    }
                                         }
                                       resolveReleaseRepository {
					  keyFromText("libs-release-local")
					  keyFromSelect("libs-release-local")
					  dynamicMode(true)
                                            }
                                       'deployPattern' ("deploy-artifacts-\${VERSION_NUMBER}-\${MD_APP}.tar.gz=>package/SDLC/IIB/\${MD_APP}/${bpval}/\${VERSION_NUMBER}")
                                        
                                       'envVarsPatterns'{
                                          'excludePatterns' ('*password*,*secret*,*key*')
                                                }
                                            }  
                                       
                                          }  
                                       
                                 publishers {
                                     extendedEmail {
                                       recipientList("${grpEmailList}")
                                        defaultSubject('$PROJECT_DEFAULT_SUBJECT')
                                        defaultContent('$PROJECT_DEFAULT_CONTENT')
                                        contentType('default')
                                        preSendScript ('$DEFAULT_PRESEND_SCRIPT')
                                        attachBuildLog (false)
                                        compressBuildLog (false)
                                        replyToList ('$DEFAULT_REPLYTO')
                                        saveToWorkspace (false)
                                        disabled (false)
                                        triggers {
                                  
                                   failure {
                                      subject('$PROJECT_DEFAULT_SUBJECT')
                                      content('$PROJECT_DEFAULT_CONTENT')
                                      sendTo {
                                          developers()
                                          }
                                    }
                                  success {
                                      subject('$PROJECT_DEFAULT_SUBJECT')
                                      content('$PROJECT_DEFAULT_CONTENT')
                                      sendTo {
                                          developers()
                                          }
                                        }
                                      }
                                    }
                                  }
                                println "${srcDetails.serviceName}_${bpkey} Build Job Successfully Created."
                      
                               }
    }; 

    def iibEnvvironments = "${confProp.iibEnv}"
              def iibEnv = Eval.me(iibEnvvironments)
              iibEnv.eachWithIndex { val, idx -> 
//------Starting Deploy job --------                
                 freeStyleJob("${srcDetails.folderName}/${idx+1}-${srcDetails.serviceName}_${val}_Deploy") {
                      description("${idx+1}-${srcDetails.serviceName}_${val}_Deploy job")
                      println "${srcDetails.folderName}/${idx+1}-${srcDetails.serviceName}_${val}_Deploy Job Successfully Created."
                      def egNameDetails = evaluate("${confProp.executionGrps}")
                      def egName = (egNameDetails.get(serviceGrp).get(val))
                     
                      label("${confProp.buildServer}")
                      logRotator {
                                  daysToKeep(20)
                                  numToKeep(15)
                              }
                  	  jdk("${confProp.jdkVersion}")
                      properties {
                                rebuild {
                                 autoRebuild (false)
                                 rebuildDisabled (false)
                               }
                            }
                   
                   
                   if (("${val}" == "AZDV1" || "${val}" == "AZDV2" || "${val}" == "AZDV3")){
                      authorization {
                           accgrp.eachWithIndex { agrp, idx1 ->
                             permission('hudson.model.Item.Build', "${agrp}")
                             permission('hudson.model.Item.Cancel', "${agrp}")
                             permission('hudson.model.Item.Discover', "${agrp}")
                             permission('hudson.model.Item.Read', "${agrp}")
                             permission('hudson.model.Item.Workspace', "${agrp}")
                                 }
                            }
                   }
        		
                               
                    publishers {
                                     extendedEmail {
                                       recipientList("${grpEmailList}")
                                        defaultSubject('$PROJECT_DEFAULT_SUBJECT')
                                        defaultContent('$PROJECT_DEFAULT_CONTENT')
                                        contentType('default')
                                        preSendScript ('$DEFAULT_PRESEND_SCRIPT')
                                        attachBuildLog (false)
                                        compressBuildLog (false)
                                        replyToList ('$DEFAULT_REPLYTO')
                                        saveToWorkspace (false)
                                        disabled (false)
                                        triggers {
                                  
                                   failure {
                                      subject('$PROJECT_DEFAULT_SUBJECT')
                                      content('$PROJECT_DEFAULT_CONTENT')
                                      sendTo {
                                          developers()
                                          }
                                    }
                                  success {
                                      subject('$PROJECT_DEFAULT_SUBJECT')
                                      content('$PROJECT_DEFAULT_CONTENT')
                                      sendTo {
                                          developers()
                                          }
                                        }
                                      }
                                    }
                                  
                     downstreamParameterized {
                          trigger("${idx+1}-${srcDetails.serviceName}_${val}_Migration") {
                              condition('UNSTABLE_OR_BETTER')
                              parameters {
                                      predefinedProp('DEPLOYED_VERSION','${VERSION_NUMBER}')
                                      predefinedProp('APP', '${MD_APP}')
                                      if (("${val}" == "AZSTRS" )){
                                      predefinedProp('ENV', '${MD_ENV}')
                                      }else{
                                       predefinedProp('ENV', '${MD_ENV}')
                                      }
                                      predefinedProp('ENVTO', '${MD_ENVTO}')
                                      predefinedProp('RELEASE_NUMBER', '${VERSION_NUMBER}')
                                         }
                                    }
                                 }
                   
                             } 
                   
                   steps {
                     if (("${val}" == "AZDV1" || "${val}" == "AZDV2" || "${val}" == "AZDV3" || "${val}" == "AZQA1" || "${val}" == "AZQA2" || "${val}" == "AZQA3")){
                          shell(readFileFromWorkspace('iib/iibDeploy_LowerEnv.sh'))
                     }else {
                           shell(readFileFromWorkspace('iib/iibDeploy_HigherEnv.sh'))
                     }
                   }
                   
                   configure {
                          concurrentBuild('false')
                          assignedNode("${confProp.buildServer}")
                          canRoam('false')
                          disabled('false')
                          it / 'buildWrappers' / 'org.jvnet.hudson.tools.versionnumber.VersionNumberBuilder'(plugin:'versionnumber@1.9') {
                                    'versionNumberString' ('${RELEASE_NUMBER}')
                                    'environmentVariableName' ('VERSION_NUMBER')
                                    'projectStartDate' ('1969-12-31 05:00:00.0 UTC')
                                    'worstResultForIncrement' ('NOT_BUILT')
                                    'skipFailedBuilds' (false)
                                    'useAsBuildDisplayName' (true)
                                  
                             }
                        
                            it / 'properties' / 'job-metadata'(plugin:'metadata@1.1.0b') {'values'('class': 'linked-list'){
                              'metadata-string'{
                                  'name'('APP')
                                  'parent'('class':"job-metadata", 'reference':"../../..")
                                  'generated'(false)
                                  'exposedToEnvironment'(true)
                                  'value'("${appName}")
                                  
                                    }
                                if (("${val}" == "AZSTRS" )){
                      
                                      }else {
                                                               
                                        'metadata-string'{
                                          'name'('ENV')
                                          'parent'('class':"job-metadata", 'reference':"../../..")
                                          'generated'(false)
                                          'exposedToEnvironment'(true)
                                          'value'(depEnv."${val}")
                                            }
                            		}
                                 'metadata-string'{
                                  'name'('ENVTO')
                                  'parent'('class':"job-metadata", 'reference':"../../..")
                                  'generated'(false)
                                  'exposedToEnvironment'(true)
                                  'value'("${val}")
                                    }
                                  'metadata-string'{
                                  'name'('EXECUTIONGRP')
                                  'parent'('class':"job-metadata", 'reference':"../../..")
                                  'generated'(false)
                                  'exposedToEnvironment'(true)
                                  'value'("${egName}")
                                    }
                                 
                                 }
                              }
                           }
                  parameters {
               			stringParam('RELEASE_NUMBER', '${VAR}', 'Provide Build Number To Start Deployment--- Example: 2018.01.317')
                        if (("${val}" == "AZSTRS" )){
                             choiceParam('MD_ENV', ['AZQA1', 'AZQA2', 'AZQA3'], 'Please select from Env: AZQA1/AZQA2/AZQA3')
                                      }
               				}
                  wrappers {
                         preBuildCleanup()
                         timestamps()
                         maskPasswords()
                        }
                   environmentVariables {
        keepBuildVariables(true)
        propertiesFile("\${MD_APP}_\${MD_ENV}.properties")
        script(readFileFromWorkspace('iib/downloadArtifacts.sh'))             
                   }
                  
                }
 
//-----End of Deploy Service  ----             

//----Starting Migration Job -----    
         	freeStyleJob("${srcDetails.folderName}/${idx+1}-${srcDetails.serviceName}_${val}_Migration") {
              	 description("${idx+1}-${srcDetails.serviceName}_${val}_Migration Job")
                  println "${srcDetails.folderName}/${idx+1}-${srcDetails.serviceName}_${val}_Deploy Job Successfully Created."
                     label("master")
                     logRotator {
		                     daysToKeep(20)
		                     numToKeep(15)
                              }
                      properties {
		                     rebuild {
		                     autoRebuild (false)
		                     rebuildDisabled (false)
		                             }
                            }
                      parameters {
		                     	stringParam('RELEASE_NUMBER', '${VAR}', 'Provide Build Number To Start Mitration--- Example: 2018.01.317')
		                      if (("${val}" == "AZSTRS" )){
		                            stringParam('ENV', depEnv."${val}", 'Please provide from Env: AZQA1/AZQA2/AZQA3')
		                           }else {
		                            stringParam('ENV', depEnv."${val}", 'Migrate From Env')
		                           }
		                      stringParam('ENVTO', "${val}", ' Migration To Env')
		                      stringParam('APP', "${appName}", 'Application Name')
     			        }
              
                      configure {
                          concurrentBuild('false')
                          assignedNode("master")
                          canRoam('false')
                          disabled('false')
                          it / 'buildWrappers' / 'org.jvnet.hudson.tools.versionnumber.VersionNumberBuilder'(plugin:'versionnumber@1.9') {
                                    'versionNumberString' ('${RELEASE_NUMBER}')
                                    'environmentVariableName' ('VERSION_NUMBER')
                                    'projectStartDate' ('1969-12-31 05:00:00.0 UTC')
                                    'worstResultForIncrement' ('NOT_BUILT')
                                    'skipFailedBuilds' (false)
                                    'useAsBuildDisplayName' (true)
                             }
                        
                           it / 'buildWrappers' / 'org.jfrog.hudson.generic.ArtifactoryGenericConfigurator'(plugin:'artifactory@2.15.1') {
                   
                                         'deployerDetails'{
				            'artifactoryName' ('PDC 01')
				            'artifactoryUrl' ("http://${confProp.artifactHost}.activehealth.loc:8081/artifactory")
				            'deployReleaseRepository' ("libs-release-local") {
				                keyFromText("libs-release-local")
				                keyFromSelect("libs-release-local")
				                dynamicMode(true)
				                  }
				                }
				           useSpecs(false)
				           resolverDetails{
				           artifactoryName('PDC 01')
				           artifactoryUrl("http://${confProp.artifactHost}.activehealth.loc:8081/artifactory")
				           resolveReleaseRepository("libs-release-local") {
				     	      keyFromText("libs-release-local")
				     	      keyFromSelect("libs-release-local")
				     	      dynamicMode(true)
				             }
				           }
				          resolveReleaseRepository {
				     	  keyFromText("libs-release-local")
				     	  keyFromSelect("libs-release-local")
				     	  dynamicMode(true)
                                            }
                                'deployPattern' ("*=>package/SDLC/${folderName}/\${APP}/\${ENVTO}/\${VERSION_NUMBER}")
                                        
                                       'envVarsPatterns'{
                                          'excludePatterns' ('*password*,*secret*,*key*')
                                                }
                                  }
                         }
              
                         steps {
                                shell("""
env
                                 
echo "#####################################################################################################"
echo ""
echo "RELEASE=\${RELEASE}"
echo "RELEASE_JOBNAME=\${RELEASE_JOBNAME}"
echo "RELEASE_NUMBER=\${RELEASE_NUMBER}"
echo "RELEASE_NAME=\${RELEASE_NAME}"
echo "RELEASE_RESULT=\${RELEASE_RESULT}"
echo ""
echo "#####################################################################################################"
                                
rm -f * 
                                 
wget http://azcuvctbartft01.activehealth.loc:8081/artifactory/libs-release-local/package/SDLC/${folderName}/\${APP}/\${ENV}/\${VERSION_NUMBER}/deploy-artifacts-\${VERSION_NUMBER}-\${APP}.tar.gz
                             
                                
""")
            }
              		 
         }         
              
// -----End of Migrarion Job  -----              
               }
//----End of Service loop  ----           
                       
        	   
              
              
              
            println "---End of ${srcDetails.serviceName}: process-------"
            println ""
            	         
            lineNo++
         }
println "------Starting to create views -----"         
      }