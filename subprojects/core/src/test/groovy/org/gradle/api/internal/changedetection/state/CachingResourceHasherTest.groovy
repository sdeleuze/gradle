/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.changedetection.state

import org.gradle.internal.hash.HashCode
import org.gradle.internal.serialize.HashCodeSerializer
import org.gradle.testfixtures.internal.InMemoryIndexedCache
import spock.lang.Specification

import java.nio.file.Paths
import java.util.zip.ZipEntry

class CachingResourceHasherTest extends Specification {
    def delegate = Mock(ResourceHasher)
    def path = Paths.get("some")
    def relativePath = ["relative", "path"]
    private FileHashSnapshot content = new FileHashSnapshot(HashCode.fromInt(456))
    def cachingHasher = new CachingResourceHasher(delegate, new DefaultResourceSnapshotterCacheService(new InMemoryIndexedCache(new HashCodeSerializer())))

    def "returns result from delegate"() {
        def expectedHash = HashCode.fromInt(123)
        when:
        def actualHash = cachingHasher.hash(path, relativePath, content)
        then:
        1 * delegate.hash(path, relativePath, content) >> expectedHash
        actualHash == expectedHash
        0 * _
    }

    def "caches the result"() {
        def expectedHash = HashCode.fromInt(123)
        when:
        def actualHash = cachingHasher.hash(path, relativePath, content)
        then:
        1 * delegate.hash(path, relativePath, content) >> expectedHash
        actualHash == expectedHash
        0 * _

        when:
        actualHash = cachingHasher.hash(path, relativePath, content)
        then:
        actualHash == expectedHash
        0 * _
    }

    def "caches 'no signature' results too"() {
        def noSignature = null
        when:
        def actualHash = cachingHasher.hash(path, relativePath, content)
        then:
        1 * delegate.hash(path, relativePath, content) >> noSignature
        actualHash == noSignature
        0 * _

        when:
        actualHash = cachingHasher.hash(path, relativePath, content)
        then:
        actualHash == noSignature
        0 * _
    }

    def "does not cache zip entries"() {
        def expectedHash = HashCode.fromInt(123)
        def inputStream = Mock(InputStream)
        def zipEntry = Mock(ZipEntry)

        when:
        def actualHash = cachingHasher.hash(zipEntry, inputStream)

        then:
        1 * delegate.hash(zipEntry, inputStream) >> expectedHash
        0 * _

        actualHash == expectedHash

        when:
        actualHash = cachingHasher.hash(zipEntry, inputStream)

        then:
        1 * delegate.hash(zipEntry, inputStream) >> expectedHash
        0 * _

        actualHash == expectedHash
    }
}
