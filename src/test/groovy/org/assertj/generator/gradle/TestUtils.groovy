/*
 * Copyright 2017. assertj-generator-gradle-plugin contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.assertj.generator.gradle

/**
 * Utilities used in test scripts
 */
class TestUtils {

    /**
     * Cleans up the boiler-plate while creating a build.gradle file applying the plugin
     * @param file
     * @param content
     * @return
     */
    static def buildFile(File file, String content) {
        file << """
            // Add required plugins and source sets to the sub projects
            plugins { id "org.assertj.generator" } // Note must use this syntax
            
            ${content}
            
            repositories {
                mavenCentral()
            }
            
            dependencies {
                implementation group: 'javax.annotation', name: 'javax.annotation-api', version: '1.3.2'

                // https://mvnrepository.com/artifact/org.assertj/assertj-core
                testImplementation group: 'org.assertj', name: 'assertj-core', version: '3.22.0'
                
                testImplementation group: 'junit', name: 'junit', version: '4.13.1'
            }
        """.stripMargin()
    }

}
