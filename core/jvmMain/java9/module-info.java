/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

module kotlinx.collections.immutable {
    requires transitive kotlin.stdlib;

    exports kotlinx.collections.immutable;
    exports kotlinx.collections.immutable.adapters;
    exports kotlinx.collections.immutable.implementations.immutableList;
}
