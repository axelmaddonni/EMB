#!/usr/bin/env python3

EVOMASTER_VERSION = "1.4.0"


import sys
import os
import shutil
import platform
from shutil import copy
from shutil import copytree
from subprocess import run
from os.path import expanduser


MAKE_ZIP = False

if len(sys.argv) > 1:
    MAKE_ZIP = sys.argv[1] == "true" or sys.argv[1] == "True"


### Environment variables ###

HOME = expanduser("~")
SCRIPT_LOCATION = os.path.dirname(os.path.realpath(__file__))
PROJ_LOCATION = os.path.abspath(os.path.join(SCRIPT_LOCATION, os.pardir))

JAVA_HOME_8 = os.environ.get('JAVA_HOME_8', '')
JAVA_HOME_11 = os.environ.get('JAVA_HOME_11', '')

SHELL = platform.system() == 'Windows'

DIST = os.path.join(PROJ_LOCATION, "dist")


##################################################
def checkJavaVersions():
    if JAVA_HOME_8 == '':
        print("\nERROR: JAVA_HOME_8 environment variable is not defined")
        exit(1)

    if JAVA_HOME_11 == '':
        print("\nERROR: JAVA_HOME_11 environment variable is not defined")
        exit(1)



######################################
### Prepare "dist" folder ###
def prepareDistFolder():

    if os.path.exists(DIST):
        shutil.rmtree(DIST)

    os.mkdir(DIST)


def callMaven(folder, jdk_home):
    env_vars = os.environ.copy()
    env_vars["JAVA_HOME"] = jdk_home

    mvnres = run(["mvn", "clean", "install", "-DskipTests"], shell=SHELL, cwd=os.path.join(PROJ_LOCATION,folder), env=env_vars)
    mvnres = mvnres.returncode

    if mvnres != 0:
        print("\nERROR: Maven command failed")
        exit(1)

### Building Maven JDK 8 projects ###
def build_jdk_8_maven() :

    folder = "jdk_8_maven"
    callMaven(folder, JAVA_HOME_8)

    # Copy JAR files
    copy(folder +"/cs/rest/original/features-service/target/features-service-sut.jar", DIST)
    copy(folder +"/em/external/rest/features-service/target/features-service-evomaster-runner.jar", DIST)

    copy(folder +"/cs/rest/original/scout-api/api/target/scout-api-sut.jar", DIST)
    copy(folder +"/em/external/rest/scout-api/target/scout-api-evomaster-runner.jar", DIST)

    copy(folder +"/cs/rest/original/proxyprint/target/proxyprint-sut.jar", DIST)
    copy(folder +"/em/external/rest/proxyprint/target/proxyprint-evomaster-runner.jar", DIST)

    copy(folder +"/cs/rest/original/catwatch/catwatch-backend/target/catwatch-sut.jar", DIST)
    copy(folder +"/em/external/rest/catwatch/target/catwatch-evomaster-runner.jar", DIST)

    copy(folder +"/cs/rest/artificial/ncs/target/rest-ncs-sut.jar", DIST)
    copy(folder +"/em/external/rest/ncs/target/rest-ncs-evomaster-runner.jar", DIST)

    copy(folder +"/cs/rest/artificial/scs/target/rest-scs-sut.jar", DIST)
    copy(folder +"/em/external/rest/scs/target/rest-scs-evomaster-runner.jar", DIST)

    copy(folder +"/cs/rest/artificial/news/target/rest-news-sut.jar", DIST)
    copy(folder +"/em/external/rest/news/target/rest-news-evomaster-runner.jar", DIST)

    copy(folder +"/cs/rest-gui/ocvn/web/target/ocvn-rest-sut.jar", DIST)
    copy(folder +"/em/external/rest/ocvn/target/ocvn-rest-evomaster-runner.jar", DIST)

    copy(folder +"/cs/rest/original/languagetool/languagetool-server/target/languagetool-sut.jar", DIST)
    copy(folder +"/em/external/rest/languagetool/target/languagetool-evomaster-runner.jar", DIST)

    copy(folder +"/cs/rest/original/restcountries/target/restcountries-sut.jar", DIST)
    copy(folder +"/em/external/rest/restcountries/target/restcountries-evomaster-runner.jar", DIST)

    copy(folder +"/cs/graphql/spring-petclinic-graphql/target/petclinic-sut.jar", DIST)
    copy(folder +"/em/external/graphql/spring-petclinic-graphql/target/petclinic-evomaster-runner.jar", DIST)

    copy(folder +"/cs/graphql/graphql-ncs/target/graphql-ncs-sut.jar", DIST)
    copy(folder +"/em/external/graphql/graphql-ncs/target/graphql-ncs-evomaster-runner.jar", DIST)

    copy(folder +"/cs/graphql/graphql-scs/target/graphql-scs-sut.jar", DIST)
    copy(folder +"/em/external/graphql/graphql-scs/target/graphql-scs-evomaster-runner.jar", DIST)



    ind0 = os.environ.get('SUT_LOCATION_IND0', '')
    if ind0 == '':
        print("\nWARN: SUT_LOCATION_IND0 env variable is not defined")
    else:
        copy(ind0, os.path.join(DIST, "ind0-sut.jar"))
        copy(folder +"/em/external/rest/ind0/target/ind0-evomaster-runner.jar", DIST)



####################
def build_jdk_11_maven() :

    folder = "jdk_11_maven"
    callMaven(folder, JAVA_HOME_11)

    copy(folder +"/cs/rest/cwa-verification-server/target/cwa-verification-sut.jar", DIST)
    copy(folder +"/em/external/rest/cwa-verification/target/cwa-verification-evomaster-runner.jar", DIST)

    copy(folder +"/cs/graphql/timbuctoo/timbuctoo-instancev4/target/timbuctoo-sut.jar", DIST)
    copy(folder +"/em/external/graphql/timbuctoo/target/timbuctoo-evomaster-runner.jar", DIST)

####################
def build_jdk_11_gradle() :

    env_vars = os.environ.copy()
    env_vars["JAVA_HOME"] = JAVA_HOME_11
    folder = "jdk_11_gradle"

    gradleres = run(["gradlew", "build", "-x", "test"], shell=SHELL, cwd=os.path.join(PROJ_LOCATION,folder), env=env_vars)
    gradleres = gradleres.returncode

    if gradleres != 0:
        print("\nERROR: Gradle command failed")
        exit(1)


    # Copy JAR files
    copy(folder +"/cs/graphql/patio-api/build/libs/patio-api-sut.jar", DIST)
    copy(folder +"/em/external/graphql/patio-api/build/libs/patio-api-evomaster-runner.jar", DIST)




# Building JavaScript projects
def buildJS(path, name):
    print("Building '"+name+"' from " + path)
    # we use "ci" instead of "install" due to major flaws in NPM
    res = run(["npm", "ci"], shell=SHELL, cwd=path).returncode
    if res != 0:
        print("\nERROR installing packages with NPM in " + path)
        exit(1)
    res = run(["npm", "run", "build"], shell=SHELL, cwd=path).returncode
    if res != 0:
        print("\nERROR when building " + path)
        exit(1)

    target = os.path.join(DIST, name)
    # shutil.make_archive(base_name=target, format='zip', root_dir=path+"/..", base_dir=name)
    copytree(path, target)



####################
def build_js_npm():
    buildJS(os.path.abspath(os.path.join(PROJ_LOCATION, "js_npm","rest","ncs")), "js-rest-ncs")
    buildJS(os.path.abspath(os.path.join(PROJ_LOCATION, "js_npm","rest","scs")), "js-rest-scs")
    buildJS(os.path.abspath(os.path.join(PROJ_LOCATION, "js_npm","rest","cyclotron")), "cyclotron")
    buildJS(os.path.abspath(os.path.join(PROJ_LOCATION, "js_npm","rest","disease-sh-api")), "disease-sh-api")
    buildJS(os.path.abspath(os.path.join(PROJ_LOCATION, "js_npm","rest","realworld-app")), "realworld-app")
    buildJS(os.path.abspath(os.path.join(PROJ_LOCATION, "js_npm","rest","spacex-api")), "spacex-api")


####################
def build_dotnet_3():

    env_vars = os.environ.copy()
    folder = "dotnet_3"

    dotnet = run(["dotnet", "build"], shell=SHELL, cwd=os.path.join(PROJ_LOCATION,folder), env=env_vars)

    if dotnet.returncode != 0:
        print("\nERROR: .Net command failed")
        exit(1)

    rest = os.path.join(PROJ_LOCATION, "dotnet_3","em","embedded","rest")

    ncs = os.path.abspath(os.path.join(rest,"NcsDriver","bin","Debug","netcoreapp3.1"))
    scs = os.path.abspath(os.path.join(rest,"ScsDriver","bin","Debug","netcoreapp3.1"))
    sampleproject = os.path.abspath(os.path.join(rest,"SampleProjectDriver","bin","Debug","netcoreapp3.1"))
    menuapi = os.path.abspath(os.path.join(rest,"MenuAPIDriver","bin","Debug","netcoreapp3.1"))


    copytree(ncs, os.path.join(DIST, "cs-rest-ncs"))
    copytree(scs, os.path.join(DIST, "cs-rest-scs"))
    copytree(sampleproject, os.path.join(DIST, "sampleproject"))
    copytree(menuapi, os.path.join(DIST, "menu-api"))



######################################################################################
### Copy JavaAgent library ###
## This requires EvoMaster to be "mvn install"ed on your machine
def copyEvoMasterAgent():
    copy(HOME + "/.m2/repository/org/evomaster/evomaster-client-java-instrumentation/"
         + EVOMASTER_VERSION + "/evomaster-client-java-instrumentation-"
         + EVOMASTER_VERSION + ".jar",
         os.path.join(DIST, "evomaster-agent.jar"))



######################################################################################
### Create Zip file with all the SUTs and Drivers ###
def makeZip():
    zipName = "dist.zip"
    if os.path.exists(zipName):
        os.remove(zipName)

    print("Creating " + zipName)
    shutil.make_archive(base_name=DIST, format='zip', root_dir=DIST + "/..", base_dir='dist')


#####################################################################################
### Build the different modules ###

checkJavaVersions()

prepareDistFolder()

build_jdk_8_maven()
build_jdk_11_maven()
build_jdk_11_gradle()
build_js_npm()

build_dotnet_3()


copyEvoMasterAgent()

if MAKE_ZIP:
    makeZip()

######################################################################################
## If we arrive here, it means everything worked fine, with no exception
print("\n\nSUCCESS\n\n")
