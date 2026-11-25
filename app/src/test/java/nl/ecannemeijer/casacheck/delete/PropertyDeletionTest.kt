package nl.ecannemeijer.casacheck.delete

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import nl.ecannemeijer.casacheck.data.repo.PropertyDeletion
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PropertyDeletionTest {
    private val hidden = mutableSetOf<Long>()
    private val deleted = mutableListOf<Long>()

    @Test fun deletesAfterUndoWindow() = runTest {
        val d = PropertyDeletion(this, { hidden += it }, { hidden -= it }, { deleted += it; hidden -= it }, 8_000)
        d.request(7, "Casa Cengio")
        assertThat(hidden).contains(7L)
        assertThat(d.current.value?.title).isEqualTo("Casa Cengio")
        advanceTimeBy(7_999); runCurrent()
        assertThat(deleted).isEmpty()
        advanceTimeBy(2); runCurrent()
        assertThat(deleted).containsExactly(7L)
        assertThat(d.current.value).isNull()
        assertThat(d.undo(7)).isFalse()
    }

    @Test fun undoKeepsProperty() = runTest {
        val d = PropertyDeletion(this, { hidden += it }, { hidden -= it }, { deleted += it }, 8_000)
        d.request(3, "A")
        advanceTimeBy(3_000); runCurrent()
        assertThat(d.undo(3)).isTrue()
        advanceTimeBy(20_000); runCurrent()
        assertThat(deleted).isEmpty()
        assertThat(hidden).isEmpty()
        assertThat(d.current.value).isNull()
    }

    @Test fun twoDeletionsAreIndependent() = runTest {
        val d = PropertyDeletion(this, { hidden += it }, { hidden -= it }, { deleted += it }, 8_000)
        d.request(1, "A"); advanceTimeBy(1_000)
        d.request(2, "B")
        assertThat(d.current.value?.id).isEqualTo(2L)
        assertThat(d.undo(2)).isTrue()
        advanceTimeBy(10_000); runCurrent()
        assertThat(deleted).containsExactly(1L)
        assertThat(hidden).doesNotContain(2L)
    }

    @Test fun failedDeleteMakesPropertyVisibleAgain() = runTest {
        val d = PropertyDeletion(this, { hidden += it }, { hidden -= it }, { throw IllegalStateException("disk") }, 1_000)
        d.request(5, "X")
        advanceTimeBy(2_000); runCurrent()
        assertThat(hidden).isEmpty()
        assertThat(d.current.value).isNull()
    }
}