// Copyright (C) 2017 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.gerrit.server.query.change;

import com.google.gerrit.exceptions.StorageException;
import com.google.gerrit.server.index.change.ChangeField;

public class RevertOfPredicate extends ChangeIndexPredicate {
  public RevertOfPredicate(String revertOf) {
    super(ChangeField.REVERT_OF, revertOf);
  }

  @Override
  public boolean match(ChangeData cd) throws StorageException {
    if (cd.change().getRevertOf() == null) {
      return false;
    }
    return cd.change().getRevertOf().toString().equals(value);
  }

  @Override
  public int getCost() {
    return 1;
  }
}
