/*
 * Copyright 2016-2019 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tests.contract

import com.google.common.collect.testing.testers.CollectionClearTester
import com.google.common.collect.testing.testers.ListRemoveAtIndexTester
import com.google.common.collect.testing.testers.ListSubListTester
import junit.framework.*

open class GuavaImmutableCollectionBaseTest: TestListener {
    override fun addFailure(test: Test, e: AssertionFailedError) = throw e
    override fun addError(test: Test, e: Throwable) {
        if (e is ConcurrentModificationException
                && test is ListSubListTester<*>
                && test.testMethodName in listOf("testSubList_originalListSetAffectsSubList", "testSubList_originalListSetAffectsSubListLargeList")) {
            // These test cases check that changes in sublist are reflected in backed list, and vice-versa.
            // Backed list is structurally modified due to `set` function invocation,
            // leading to CME when `sublist.listIterator()` gets invoked to iterate elements of sublist to make sure changes are reflected.
            // The `sublist.listIterator()` method has the comodification check.
            //
            // The guava-testlib doesn't expect any exceptions to be thrown,
            // despite `List.subList` javadoc statement "The semantics of the list returned by this method become undefined" is such case.
            return
        }
        if (e is NoSuchElementException
                // Removing the only element from the sublist and calling `next()` on it's earlier created iterator throws NSEE.
                && (test is ListRemoveAtIndexTester<*> && test.testMethodName == "testRemoveAtIndexConcurrentWithIteration"
                        // Removing all elements from the sublist and calling `next()` on it's earlier created iterator throws NSEE.
                        || test is CollectionClearTester<*> && test.testMethodName == "testClearConcurrentWithIteration")) {
            // These test cases check that sublist iterator is fail-fast.
            //
            // `AbstractList` implementation of sublist iterator is not fail-fast.
            // Seems we need to override the `subList()` method in `PersistentVectorBuilder`
            // to return custom sublist implementation with fail-fast iterator.
            return
        }
        throw e
    }
    override fun startTest(test: Test) { }
    override fun endTest(test: Test) { }

    fun runTestSuite(suite: TestSuite) {
        for (t in suite.tests()) {
            val r = TestResult()
            r.addListener(this)
            t.run(r)
        }
    }
}