package nl.ecannemeijer.casacheck.update

import com.google.common.truth.Truth.assertThat
import nl.ecannemeijer.casacheck.data.update.ReleaseAsset
import nl.ecannemeijer.casacheck.data.update.UpdateLogic
import org.junit.Test

class UpdateLogicTest {
    @Test fun parsesVersions() {
        assertThat(UpdateLogic.parseVersion("v1.0.1")).containsExactly(1, 0, 1).inOrder()
        assertThat(UpdateLogic.parseVersion("1.2")).containsExactly(1, 2, 0).inOrder()
        assertThat(UpdateLogic.parseVersion("1.0.1-debug")).containsExactly(1, 0, 1).inOrder()
        assertThat(UpdateLogic.parseVersion("latest")).isNull()
        assertThat(UpdateLogic.parseVersion("")).isNull()
    }

    @Test fun comparesSemver() {
        assertThat(UpdateLogic.isNewer("v1.0.1", "1.0.0")).isTrue()
        assertThat(UpdateLogic.isNewer("v1.0.10", "1.0.9")).isTrue()
        assertThat(UpdateLogic.isNewer("v2.0", "1.9.9")).isTrue()
        assertThat(UpdateLogic.isNewer("v1.0.0", "1.0.0")).isFalse()
        assertThat(UpdateLogic.isNewer("v1.0.0", "1.0.1")).isFalse()
        assertThat(UpdateLogic.isNewer("nightly", "1.0.0")).isFalse()
    }

    @Test fun selectsApkAsset() {
        val a = listOf(ReleaseAsset("notes.txt", "https://x/notes.txt", 1, null), ReleaseAsset("other.apk", "https://x/other.apk", 2, null),
            ReleaseAsset("CasaCheck-1.0.1.apk", "https://x/CasaCheck-1.0.1.apk", 3, null))
        assertThat(UpdateLogic.selectApk(a)!!.name).isEqualTo("CasaCheck-1.0.1.apk")
        assertThat(UpdateLogic.selectApk(a.take(2))!!.name).isEqualTo("other.apk")
        assertThat(UpdateLogic.selectApk(a.take(1))).isNull()
        assertThat(UpdateLogic.selectApk(listOf(ReleaseAsset("CasaCheck.apk", "http://insecure/x.apk", 1, null)))).isNull()
    }

    @Test fun parsesGithubJson() {
        val json = """{"tag_name":"v1.0.2","name":"CasaCheck 1.0.2","draft":false,"prerelease":false,"body":"Notities","html_url":"https://github.com/x",
            "assets":[{"name":"CasaCheck-1.0.2.apk","size":123,"content_type":"application/vnd.android.package-archive",
            "browser_download_url":"https://github.com/ecannemeijer/casacheck/releases/download/v1.0.2/CasaCheck-1.0.2.apk"}]}"""
        val r = UpdateLogic.parseRelease(json)!!
        assertThat(r.tag).isEqualTo("v1.0.2"); assertThat(r.notes).isEqualTo("Notities"); assertThat(r.apk.size).isEqualTo(123)
        assertThat(UpdateLogic.parseRelease(json.replace("\"prerelease\":false", "\"prerelease\":true"))).isNull()
        assertThat(UpdateLogic.parseRelease("""{"tag_name":"v1.0.2","assets":[]}""")).isNull()
        assertThat(UpdateLogic.parseRelease("{\"message\":\"API rate limit exceeded\"}")).isNull()
        assertThat(UpdateLogic.parseRelease("not json")).isNull()
    }
}