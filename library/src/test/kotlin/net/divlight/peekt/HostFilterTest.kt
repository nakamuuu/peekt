package net.divlight.peekt

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HostFilterTest {
    @Test
    fun emptyAllowlistRecordsEveryHost() {
        val filter = HostFilter(emptySet())
        assertThat(filter.shouldRecord("api.example.com")).isTrue()
        assertThat(filter.shouldRecord("127.0.0.1")).isTrue()
    }

    @Test
    fun exactHostMatches() {
        val filter = HostFilter(setOf("example.com"))
        assertThat(filter.shouldRecord("example.com")).isTrue()
        assertThat(filter.shouldRecord("example.org")).isFalse()
    }

    @Test
    fun subdomainMatches() {
        val filter = HostFilter(setOf("example.com"))
        assertThat(filter.shouldRecord("api.example.com")).isTrue()
        assertThat(filter.shouldRecord("foo.api.example.com")).isTrue()
    }

    @Test
    fun falseSuffixDoesNotMatch() {
        val filter = HostFilter(setOf("example.com"))
        assertThat(filter.shouldRecord("evil-example.com")).isFalse()
        assertThat(filter.shouldRecord("notexample.com")).isFalse()
        assertThat(filter.shouldRecord("example.com.evil.net")).isFalse()
    }

    @Test
    fun leadingDotIsIgnored() {
        val filter = HostFilter(setOf(".example.com"))
        assertThat(filter.shouldRecord("example.com")).isTrue()
        assertThat(filter.shouldRecord("api.example.com")).isTrue()
    }

    @Test
    fun comparisonIsCaseInsensitive() {
        val filter = HostFilter(setOf("EXAMPLE.COM"))
        assertThat(filter.shouldRecord("Api.Example.Com")).isTrue()
    }

    @Test
    fun blankEntriesAreSkipped() {
        val filter = HostFilter(setOf(" ", ".", ""))
        assertThat(filter.shouldRecord("api.example.com")).isFalse()
    }
}
