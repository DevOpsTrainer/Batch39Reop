println ""
println "-------Starting Views Creation Process--------"
println ""

def srcGrp = "Applications"
println "Inside Main View: ${srcGrp}"

Properties serviceViews = new Properties()
              File confPropertiesFile = new File("$WORKSPACE/dsl-automation/appViews.properties")
              confPropertiesFile.withInputStream {
                  serviceViews.load(it)
              }
Properties confProp = new Properties()
              File confPropertiesEnvFile = new File("$WORKSPACE/dsl-automation/appConf.properties")
              confPropertiesEnvFile.withInputStream {
                  confProp.load(it)
              }

def sViews = evaluate("${serviceViews.serView}")
def subgroup = (sViews.get(srcGrp))
println "Services: ${subgroup}"
println ""
  
nestedView("${srcGrp}") {
    subgroup.each { serName ->
    println "-------Starting ${serName} Service Views--------------"
    views {
    nestedView(serName) {
      
  views {
      listView("Build") {
      jobs {
        name("${serName}")
        regex("${serName}_Builds")
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

 buildMonitorView("Deploy Dashboard") {
          jobs {
            name('bar')
            regex("${serName}_.*_Deploy")
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
 
  nestedView("Deploys") {
  views {
      
      def iibEnvvironments = "${confProp.appEnviews}"
      def allEnv = Eval.me(iibEnvvironments)
      
        for (def envT : allEnv) {
          println "Creating ${envT} Env View."
    listView(envT) {
      jobs {
        name("${serName}")
        regex("${serName}_${envT}_.*")
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
    listView("All") {
      jobs {
        name("${serName}")
        regex("${serName}_.*_Deploy")
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
   
  }
      configure { view ->
    view / defaultView('Sandbox')
    println "Configuring Deployes default View to Sandbox ."
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
println "-------End Of Applications Views Creation Process--------"