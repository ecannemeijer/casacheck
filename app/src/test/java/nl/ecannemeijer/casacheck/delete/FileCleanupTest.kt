package nl.ecannemeijer.casacheck.delete

import com.google.common.truth.Truth.assertThat
import nl.ecannemeijer.casacheck.data.files.FileCleanup
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FileCleanupTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun touch(root: File, rel: String) = File(root, rel).apply { parentFile!!.mkdirs(); writeText("x") }

    @Test fun planContainsPropertyFoldersAndReferencedFiles() {
        val plan = FileCleanup.planForProperty("0f3c2a1e-1111-2222-3333-444455556666",
            listOf("photos/a.jpg", null, " ", "reports/r1.pdf", "photos/a.jpg"))
        assertThat(plan.files).containsExactly("photos/a.jpg", "reports/r1.pdf")
        assertThat(plan.dirs).containsExactly(
            "photos/0f3c2a1e-1111-2222-3333-444455556666", "thumbs/0f3c2a1e-1111-2222-3333-444455556666",
            "docs/0f3c2a1e-1111-2222-3333-444455556666", "edited/0f3c2a1e-1111-2222-3333-444455556666",
            "recordings/0f3c2a1e-1111-2222-3333-444455556666")
    }

    @Test fun unsafeUuidNeverTargetsWholeFolders() {
        listOf("", "..", "../x", "a/b", "abc", "*").forEach { bad ->
            assertThat(FileCleanup.planForProperty(bad, emptyList()).dirs).isEmpty()
        }
    }

    @Test fun deletesOnlyThisPropertyAndNothingOutsideRoot() {
        val root = tmp.newFolder("files")
        val outside = touch(tmp.root, "outside.txt")
        val mine = "aaaaaaaa-0000-0000-0000-000000000001"; val other = "bbbbbbbb-0000-0000-0000-000000000002"
        listOf("photos", "thumbs", "docs", "edited", "recordings").forEach { touch(root, "$it/$mine/f.jpg"); touch(root, "$it/$other/f.jpg") }
        val report = touch(root, "reports/mine.pdf"); val otherReport = touch(root, "reports/other.pdf")

        val plan = FileCleanup.planForProperty(mine, listOf("reports/mine.pdf", "../outside.txt", "photos/$mine/f.jpg"))
        val removed = FileCleanup.execute(root, plan)

        listOf("photos", "thumbs", "docs", "edited", "recordings").forEach {
            assertThat(File(root, "$it/$mine").exists()).isFalse()
            assertThat(File(root, "$it/$other/f.jpg").exists()).isTrue()
        }
        assertThat(report.exists()).isFalse()
        assertThat(otherReport.exists()).isTrue()
        assertThat(outside.exists()).isTrue()
        assertThat(removed).isEqualTo(7) // 2 files + 5 folders
    }

    @Test fun deleteFileRefusesTraversalFoldersAndMissingFiles() {
        val root = tmp.newFolder("files2")
        val outside = touch(tmp.root, "secret.txt")
        touch(root, "photos/x/keep.jpg")
        assertThat(FileCleanup.deleteFile(root, "../secret.txt")).isFalse()
        assertThat(FileCleanup.deleteFile(root, "photos/x")).isFalse()
        assertThat(FileCleanup.deleteFile(root, "photos/x/missing.jpg")).isFalse()
        assertThat(FileCleanup.deleteFile(root, null)).isFalse()
        assertThat(FileCleanup.deleteFile(root, "photos/x/keep.jpg")).isTrue()
        assertThat(outside.exists()).isTrue()
    }
}