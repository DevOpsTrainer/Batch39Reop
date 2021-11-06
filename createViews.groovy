println ""
println "-------Starting Views Creation Process--------"
println ""
def fdrName = "${folderName}".trim()
def srcGrp = "${srcGroup}".trim()

println "Folder Name: ${fdrName}"
println "Service Group Name: ${srcGrp}"

Properties serviceViews = new Properties()
              File confPropertiesFile = new File("$WORKSPACE/iib/iib_ServiceViews.properties")
              confPropertiesFile.withInputStream {
                  serviceViews.load(it)
              }
Properties confProp = new Properties()
              File confPropertiesEnvFile = new File("$WORKSPACE/iib/iib_Conf.properties")
              confPropertiesEnvFile.withInputStream {
                  confProp.load(it)
              }

def sViews = evaluate("${serviceViews.serView}")
def subgroup = (sViews.get(srcGrp))
println "Services: ${subgroup}"
println ""
  
nestedView("${fdrName}/${srcGrp}") {
    subgroup.each { serName ->
    println "-------Starting ${serName} Service Views--------------"
    views {
    nestedView(serName) {
      
  views {
      listView("Build") {
      jobs {
        name("${serName}")
        regex("${serName}_Path*.*")
      }
      columns {
        status()
	name()
	lastSuccess()
	lastFailure()
	lastDuration()
        buildButton()
      }
    println "Creating Build View."
    } 
      listView("All") {
      jobs {
        name("${serName}")
        regex(".*${serName}*.*")
      }
      columns {
        status()
	name()
	lastSuccess()
	lastFailure()
	lastDuration()
        buildButton()
      }
    println "Creating All View."
    } 
    
    buildMonitorView("Dashboard") {
          jobs {
            name('bar')
            regex(".*-${serName}_.*_Deploy")
          }
          columns {
            status()
    	name()
    	lastSuccess()
    	lastFailure()
    	lastDuration()
            buildButton()
          }
     println "Creating Dashboard View."
    }
    
    
    
    nestedView('Deploys') {
  views {
      
      def iibEnvvironments = "${confProp.iibEnviews}"
      def allEnv = Eval.me(iibEnvvironments)
      
        for (def envT : allEnv) {
          println "Creating ${envT} Environment View."
    listView(envT) {
      jobs {
        name("${serName}")
          regex(".*-${serName}_${envT}_.*")
      }
      columns {
        status()
	    name()
	    lastSuccess()
	    lastFailure()
	    lastDuration()
        buildButton()
      }
    }
        }
   
  }
      configure { view ->
    view / defaultView('AZDV1')
    println "Configuring Deployes default View to AZDV1 ."
      }
    }
  }
  configure { view ->
    view / defaultView('Build')
    println "Configuring Main View defaults to Build."
       }
     }
    }
      println "-------End Of ${serName} Service Views--------------"
  } 
  
}
println ""
println "-------End Of IIB Views Creation Process--------"