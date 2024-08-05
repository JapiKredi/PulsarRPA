package com.github.kklisura.cdt.protocol.v2023.types.indexeddb;

/*-
 * #%L
 * cdt-java-client
 * %%
 * Copyright (C) 2018 - 2023 Kenan Klisura
 * %%
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
 * #L%
 */

import java.util.List;

public class RequestData {

  private List<DataEntry> objectStoreDataEntries;

  private Boolean hasMore;

  /** Array of object store data entries. */
  public List<DataEntry> getObjectStoreDataEntries() {
    return objectStoreDataEntries;
  }

  /** Array of object store data entries. */
  public void setObjectStoreDataEntries(List<DataEntry> objectStoreDataEntries) {
    this.objectStoreDataEntries = objectStoreDataEntries;
  }

  /** If true, there are more entries to fetch in the given range. */
  public Boolean getHasMore() {
    return hasMore;
  }

  /** If true, there are more entries to fetch in the given range. */
  public void setHasMore(Boolean hasMore) {
    this.hasMore = hasMore;
  }
}