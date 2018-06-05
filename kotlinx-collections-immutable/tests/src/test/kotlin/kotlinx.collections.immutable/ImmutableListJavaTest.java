package kotlinx.collections.immutable;

import org.junit.Assert;
import org.junit.Test;


import static kotlinx.collections.immutable.ExtensionsKt.persistentListOf;
import static kotlin.collections.CollectionsKt.listOf;

public class ImmutableListJavaTest {

    @Test
    public void immutableList() {
        PersistentList<String> list = ExtensionsKt.persistentListOf("x");

        Assert.assertEquals(listOf("x"), list);

        list = list.clear();
        Assert.assertEquals(persistentListOf(), list);

        list = list.add("x").add("z").set(0, "y");
        Assert.assertEquals(listOf("y", "z"), list);

        list = list.remove("y").addAll(list);
        Assert.assertEquals(listOf("z", "y", "z"), list);
    }
}
