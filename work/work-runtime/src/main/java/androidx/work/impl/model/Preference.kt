/*
 * Copyright 2019 The Android Open Source Project
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
package androidx.work.impl.model

import androidx.annotation.RestrictTo
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**  */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Entity
data class Preference(
    @ColumnInfo(name = "key") @PrimaryKey val key: String,
    @ColumnInfo(name = "long_value") val value: Long?
) {
    constructor(key: String, value: Boolean) : this(key, if (value) 1L else 0L)
}
