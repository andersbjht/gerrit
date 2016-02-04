// Copyright (C) 2011 The Android Open Source Project
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

package com.google.gerrit.sshd.commands;

import com.google.gerrit.extensions.api.changes.AddReviewerInput;
import com.google.gerrit.extensions.restapi.ResourceNotFoundException;
import com.google.gerrit.reviewdb.client.Account;
import com.google.gerrit.reviewdb.client.Change;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.gerrit.server.ChangeFinder;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.change.ChangeResource;
import com.google.gerrit.server.change.ChangesCollection;
import com.google.gerrit.server.change.DeleteReviewer;
import com.google.gerrit.server.change.PostReviewers;
import com.google.gerrit.server.change.ReviewerResource;
import com.google.gerrit.server.project.ChangeControl;
import com.google.gerrit.server.project.ProjectControl;
import com.google.gerrit.sshd.CommandMetaData;
import com.google.gerrit.sshd.SshCommand;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Inject;
import com.google.inject.Provider;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@CommandMetaData(name = "set-reviewers", description = "Add or remove reviewers on a change")
public class SetReviewersCommand extends SshCommand {
  private static final Logger log =
      LoggerFactory.getLogger(SetReviewersCommand.class);

  @Option(name = "--project", aliases = "-p", usage = "project containing the change")
  private ProjectControl projectControl;

  @Option(name = "--add", aliases = {"-a"}, metaVar = "REVIEWER", usage = "user or group that should be added as reviewer")
  private List<String> toAdd = new ArrayList<>();

  @Option(name = "--remove", aliases = {"-r"}, metaVar = "REVIEWER", usage = "user that should be removed from the reviewer list")
  void optionRemove(Account.Id who) {
    toRemove.add(who);
  }

  @Argument(index = 0, required = true, multiValued = true, metaVar = "COMMIT", usage = "changes to modify")
  void addChange(String token) {
    try {
      addChangeImpl(token);
    } catch (UnloggedFailure e) {
      throw new IllegalArgumentException(e.getMessage(), e);
    } catch (OrmException e) {
      throw new IllegalArgumentException("database is down", e);
    }
  }

  @Inject
  private ReviewDb db;

  @Inject
  private ReviewerResource.Factory reviewerFactory;

  @Inject
  private Provider<PostReviewers> postReviewersProvider;

  @Inject
  private Provider<DeleteReviewer> deleteReviewerProvider;

  @Inject
  private Provider<CurrentUser> userProvider;

  @Inject
  private ChangesCollection changesCollection;

  @Inject
  private ChangeFinder changeFinder;

  private Set<Account.Id> toRemove = new HashSet<>();

  private Map<Change.Id, ChangeResource> changes = new LinkedHashMap<>();

  @Override
  protected void run() throws UnloggedFailure {
    boolean ok = true;
    for (ChangeResource rsrc : changes.values()) {
      try {
        ok &= modifyOne(rsrc);
      } catch (Exception err) {
        ok = false;
        log.error("Error updating reviewers on change " + rsrc.getId(), err);
        writeError("fatal", "internal error while updating " + rsrc.getId());
      }
    }

    if (!ok) {
      throw error("fatal: one or more updates failed; review output above");
    }
  }

  private boolean modifyOne(ChangeResource changeRsrc) throws Exception {
    boolean ok = true;

    // Remove reviewers
    //
    DeleteReviewer delete = deleteReviewerProvider.get();
    for (Account.Id reviewer : toRemove) {
      ReviewerResource rsrc = reviewerFactory.create(changeRsrc, reviewer);
      String error = null;
      try {
        delete.apply(rsrc, new DeleteReviewer.Input());
      } catch (ResourceNotFoundException e) {
        error = String.format("could not remove %s: not found", reviewer);
      } catch (Exception e) {
        error = String.format("could not remove %s: %s",
            reviewer, e.getMessage());
      }
      if (error != null) {
        ok = false;
        writeError("error", error);
      }
    }

    // Add reviewers
    //
    PostReviewers post = postReviewersProvider.get();
    for (String reviewer : toAdd) {
      AddReviewerInput input = new AddReviewerInput();
      input.reviewer = reviewer;
      input.confirmed = true;
      String error;
      try {
        error = post.apply(changeRsrc, input).error;
      } catch (Exception e) {
        error = String.format("could not add %s: %s", reviewer, e.getMessage());
      }
      if (error != null) {
        ok = false;
        writeError("error", error);
      }
    }

    return ok;
  }

  private void addChangeImpl(String id) throws UnloggedFailure, OrmException {
    List<ChangeControl> matched =
        changeFinder.find(id, userProvider.get());
    List<ChangeControl> toAdd = new ArrayList<>(changes.size());
    for (ChangeControl ctl : matched) {
      if (!changes.containsKey(ctl.getId()) && inProject(ctl.getProject())
          && ctl.isVisible(db)) {
        toAdd.add(ctl);
      }
    }
    switch (toAdd.size()) {
      case 0:
        throw error("\"" + id + "\" no such change");

      case 1:
        ChangeControl ctl = toAdd.get(0);
        changes.put(ctl.getId(), changesCollection.parse(ctl));
        break;

      default:
        throw error("\"" + id + "\" matches multiple changes");
    }
  }

  private boolean inProject(Project project) {
    if (projectControl != null) {
      return projectControl.getProject().getNameKey().equals(project.getNameKey());
    } else {
      // No --project option, so they want every project.
      return true;
    }
  }

  private void writeError(String type, String msg) {
    try {
      err.write((type + ": " + msg + "\n").getBytes(ENC));
    } catch (IOException e) {
      // Ignored
    }
  }

  private static UnloggedFailure error(String msg) {
    return new UnloggedFailure(1, msg);
  }
}
