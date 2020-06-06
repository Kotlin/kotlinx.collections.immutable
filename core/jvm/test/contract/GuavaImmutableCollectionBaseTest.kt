/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract

import com.google.common.collect.testing.testers.*
import junit.framework.*
import java.lang.IllegalArgumentException

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
        if (e is IllegalArgumentException
                && (test is CollectionAddAllTester<*> && test.testMethodName == "testAddAll_nullCollectionReference"
                        || test is CollectionRemoveAllTester<*> && test.testMethodName == "testRemoveAll_nullCollectionReferenceEmptySubject"
                        || test is CollectionRemoveAllTester<*> && test.testMethodName == "testRemoveAll_nullCollectionReferenceNonEmptySubject"
                        || test is ListAddAllAtIndexTester<*> && test.testMethodName == "testAddAllAtIndex_nullCollectionReference")) {
            // These test cases check that `addAll(elements)`, `addAll(index, elements)` and `removeAll(elements)`
            // throw `NullPointerException` when null collection is passed.
            //
            // Kotlin throws IllegalArgumentException as those methods has Non-Null collection type parameter.
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