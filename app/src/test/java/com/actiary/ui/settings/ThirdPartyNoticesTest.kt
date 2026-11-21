package com.actiary.ui.settings

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThirdPartyNoticesTest {
    private val bundled = File("src/main/res/raw/third_party_notices.txt").readText()

    @Test
    fun `bundled notices match the repository notices`() {
        val repository = File("../THIRD_PARTY_NOTICES.md").readText()
        val repositoryLines = repository.lineSequence()
            .filterNot { it == "```" }
            .map { it.replace("`", "").replace(Regex("<(https://[^>]+)>"), "$1") }
            .toList()
        assertEquals(repositoryLines, bundled.lines())
    }

    @Test
    fun `About projects match their bundled sections and links`() {
        val licenseSections = thirdPartyProjects.mapNotNull { it.licenseSection }.distinct()
        val headings = bundled.lineSequence().filter { it.startsWith("## ") }
            .map { it.removePrefix("## ") }.toList()
        assertEquals(thirdPartyProjects.map { it.name } + licenseSections, headings)

        val sections = readNoticeSections(bundled)
        for (project in thirdPartyProjects) {
            val lines = sections.getValue(project.name).lines()
            assertTrue(project.name, "- Project: ${project.url}" in lines)
            assertTrue(project.name, project.licenseSection != null || project.termsLinks.isNotEmpty())
            project.licenseSection?.let { assertTrue(it, sections.getValue(it).isNotBlank()) }
            for (link in project.termsLinks) {
                assertTrue(link.url, lines.any { it.endsWith(": ${link.url}") })
            }
        }
    }

    @Test
    fun `full license texts and Backdrop attribution are available offline`() {
        val sections = readNoticeSections(bundled)
        val apache = sections.getValue("Apache License 2.0")
        assertTrue(apache.contains("END OF TERMS AND CONDITIONS"))
        assertTrue(apache.contains("APPENDIX: How to apply the Apache License to your work."))
        assertFalse(apache.contains("Copyright 2025 Kyant"))
        assertTrue(sections.getValue("Backdrop").contains("Copyright 2025 Kyant"))
        assertTrue(sections.getValue("MIT License (Calendar)").endsWith("SOFTWARE."))
        assertEquals("Public Domain.", sections.getValue("JSON-java 20240303 License"))
    }

    @Test
    fun `license display reflows prose and preserves headings and all words`() {
        val sections = readNoticeSections(bundled)
        val apache = formatNoticeLicense(sections.getValue("Apache License 2.0"))
        assertTrue(apache.startsWith("Apache License\nVersion 2.0, January 2004\nhttp://www.apache.org/licenses/\n\n"))
        assertTrue(
            apache.contains(
                "1. Definitions.\n\n\"License\" shall mean the terms and conditions for use, " +
                    "reproduction, and distribution as defined by Sections 1 through 9 of this document.",
            ),
        )
        val whitespace = Regex("\\s+")
        for (heading in thirdPartyProjects.mapNotNull { it.licenseSection }.distinct()) {
            val original = sections.getValue(heading)
            assertEquals(
                heading,
                original.trim().replace(whitespace, " "),
                formatNoticeLicense(original).trim().replace(whitespace, " "),
            )
        }
    }
}
