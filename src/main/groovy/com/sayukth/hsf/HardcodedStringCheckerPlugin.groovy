package com.sayukth.hsf

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import org.gradle.api.GradleException
import org.xml.sax.InputSource
import javax.xml.parsers.DocumentBuilderFactory
import java.io.StringReader


class HardcodedStringCheckerPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        // Register the custom task
        project.tasks.register('checkForHardcodedStrings', CheckForHardcodedStringsTask)

        // Ensure it runs before the build
        project.tasks.named("preBuild").configure { task ->
            task.dependsOn 'checkForHardcodedStrings'
        }
    }

    static class CheckForHardcodedStringsTask extends org.gradle.api.DefaultTask {
        @TaskAction
        void checkForStrings() {
            def xmlDirectoryPath = project.file("${project.projectDir}/src/main/res/layout")

//            def xmlDirectoryPath = file("${project.projectDir}/src/main/res/layout")
            def hardcodedStringsFound = false

            xmlDirectoryPath.eachFileRecurse { file ->
                if (file.name.endsWith('.xml')) {
                    def factory = DocumentBuilderFactory.newInstance()
                    def builder = factory.newDocumentBuilder()

                    def lines = file.readLines()
                    def xmlContent = new StringBuilder()

                    lines.each { line ->
                        xmlContent.append(line).append("\n")
                    }

                    def inputSource = new InputSource(new StringReader(xmlContent.toString()))
                    def document = builder.parse(inputSource)
                    document.getDocumentElement().normalize()

                    hardcodedStringsFound |= checkForHardcodedStrings(file, document, 'android:text', lines)
                    hardcodedStringsFound |= checkForHardcodedStrings(file, document, 'android:hint', lines)
                    hardcodedStringsFound |= checkForHardcodedStrings(file, document, 'android:contentDescription', lines)
                }
            }

            if (hardcodedStringsFound) {
                throw new GradleException("Hardcoded strings found in XML files. Aborting build.")
            }
        }

        // Check for hardcoded strings in the XML attributes
        def checkForHardcodedStrings(File file, org.w3c.dom.Document document, String attribute, List<String> lines) {
            boolean found = false
            def elements = document.getElementsByTagName("*")

            for (int i = 0; i < elements.getLength(); i++) {
                def element = elements.item(i)
                if (element.hasAttribute(attribute)) {
                    def value = element.getAttribute(attribute)
                    if (!value.startsWith("@string/")) {
                        int lineNumber = findLineNumber(lines, value)
                        println "File: ${file.name}, Line: ${lineNumber}, Element: ${element.getTagName()}, Attribute: ${attribute}, Hardcoded String: ${value}"
                        println "==========\n"
                        found = true
                    }
                }
            }
            return found
        }

        def findLineNumber(List<String> lines, String searchString) {
            int lineNumber = 0
            lines.eachWithIndex { line, index ->
                if (line.contains(searchString)) {
                    lineNumber = index + 1
                    return
                }
            }
            return lineNumber
        }
    }
}