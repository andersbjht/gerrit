// Copyright (C) 2014 The Android Open Source Project
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

package com.google.gerrit.server;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import com.google.gerrit.common.Nullable;
import com.google.gerrit.exceptions.StorageException;
import com.google.gerrit.extensions.common.ChangeMessageInfo;
import com.google.gerrit.reviewdb.client.Account;
import com.google.gerrit.reviewdb.client.ChangeMessage;
import com.google.gerrit.reviewdb.client.PatchSet;
import com.google.gerrit.server.account.AccountLoader;
import com.google.gerrit.server.notedb.ChangeNotes;
import com.google.gerrit.server.notedb.ChangeUpdate;
import com.google.gerrit.server.update.ChangeContext;
import com.google.inject.Singleton;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

/** Utility functions to manipulate ChangeMessages. */
@Singleton
public class ChangeMessagesUtil {
  public static final String AUTOGENERATED_TAG_PREFIX = "autogenerated:";

  public static final String TAG_ABANDON = AUTOGENERATED_TAG_PREFIX + "gerrit:abandon";
  public static final String TAG_CHERRY_PICK_CHANGE =
      AUTOGENERATED_TAG_PREFIX + "gerrit:cherryPickChange";
  public static final String TAG_DELETE_ASSIGNEE =
      AUTOGENERATED_TAG_PREFIX + "gerrit:deleteAssignee";
  public static final String TAG_DELETE_REVIEWER =
      AUTOGENERATED_TAG_PREFIX + "gerrit:deleteReviewer";
  public static final String TAG_DELETE_VOTE = AUTOGENERATED_TAG_PREFIX + "gerrit:deleteVote";
  public static final String TAG_MERGED = AUTOGENERATED_TAG_PREFIX + "gerrit:merged";
  public static final String TAG_MOVE = AUTOGENERATED_TAG_PREFIX + "gerrit:move";
  public static final String TAG_RESTORE = AUTOGENERATED_TAG_PREFIX + "gerrit:restore";
  public static final String TAG_REVERT = AUTOGENERATED_TAG_PREFIX + "gerrit:revert";
  public static final String TAG_SET_ASSIGNEE = AUTOGENERATED_TAG_PREFIX + "gerrit:setAssignee";
  public static final String TAG_SET_DESCRIPTION =
      AUTOGENERATED_TAG_PREFIX + "gerrit:setPsDescription";
  public static final String TAG_SET_HASHTAGS = AUTOGENERATED_TAG_PREFIX + "gerrit:setHashtag";
  public static final String TAG_SET_PRIVATE = AUTOGENERATED_TAG_PREFIX + "gerrit:setPrivate";
  public static final String TAG_SET_READY = AUTOGENERATED_TAG_PREFIX + "gerrit:setReadyForReview";
  public static final String TAG_SET_TOPIC = AUTOGENERATED_TAG_PREFIX + "gerrit:setTopic";
  public static final String TAG_SET_WIP = AUTOGENERATED_TAG_PREFIX + "gerrit:setWorkInProgress";
  public static final String TAG_UNSET_PRIVATE = AUTOGENERATED_TAG_PREFIX + "gerrit:unsetPrivate";
  public static final String TAG_UPLOADED_PATCH_SET =
      AUTOGENERATED_TAG_PREFIX + "gerrit:newPatchSet";
  public static final String TAG_UPLOADED_WIP_PATCH_SET =
      AUTOGENERATED_TAG_PREFIX + "gerrit:newWipPatchSet";

  public static ChangeMessage newMessage(ChangeContext ctx, String body, @Nullable String tag) {
    return newMessage(ctx.getChange().currentPatchSetId(), ctx.getUser(), ctx.getWhen(), body, tag);
  }

  public static ChangeMessage newMessage(
      PatchSet.Id psId, CurrentUser user, Timestamp when, String body, @Nullable String tag) {
    requireNonNull(psId);
    Account.Id accountId = user.isInternalUser() ? null : user.getAccountId();
    ChangeMessage m =
        new ChangeMessage(
            new ChangeMessage.Key(psId.getParentKey(), ChangeUtil.messageUuid()),
            accountId,
            when,
            psId);
    m.setMessage(body);
    m.setTag(tag);
    user.updateRealAccountId(m::setRealAuthor);
    return m;
  }

  public static String uploadedPatchSetTag(boolean workInProgress) {
    return workInProgress ? TAG_UPLOADED_WIP_PATCH_SET : TAG_UPLOADED_PATCH_SET;
  }

  public List<ChangeMessage> byChange(ChangeNotes notes) throws StorageException {
    return notes.load().getChangeMessages();
  }

  public void addChangeMessage(ChangeUpdate update, ChangeMessage changeMessage) {
    checkState(
        Objects.equals(changeMessage.getAuthor(), update.getNullableAccountId()),
        "cannot store change message by %s in update by %s",
        changeMessage.getAuthor(),
        update.getNullableAccountId());
    update.setChangeMessage(changeMessage.getMessage());
    update.setTag(changeMessage.getTag());
  }

  /**
   * Replace an existing change message with the provided new message.
   *
   * <p>The ID of a change message is different between NoteDb and ReviewDb. In NoteDb, it's the
   * commit SHA-1, but in ReviewDb it was generated randomly. Taking the target message as an index
   * rather than an ID allowed us to delete the message from both NoteDb and ReviewDb.
   *
   * @param update change update.
   * @param targetMessageId the id of the target change message.
   * @param newMessage the new message which is going to replace the old.
   */
  public void replaceChangeMessage(ChangeUpdate update, String targetMessageId, String newMessage) {
    update.deleteChangeMessageByRewritingHistory(targetMessageId, newMessage);
  }

  /**
   * @param tag value of a tag, or null.
   * @return whether the tag starts with the autogenerated prefix.
   */
  public static boolean isAutogenerated(@Nullable String tag) {
    return tag != null && tag.startsWith(AUTOGENERATED_TAG_PREFIX);
  }

  public static ChangeMessageInfo createChangeMessageInfo(
      ChangeMessage message, AccountLoader accountLoader) {
    PatchSet.Id patchNum = message.getPatchSetId();
    ChangeMessageInfo cmi = new ChangeMessageInfo();
    cmi.id = message.getKey().get();
    cmi.author = accountLoader.get(message.getAuthor());
    cmi.date = message.getWrittenOn();
    cmi.message = message.getMessage();
    cmi.tag = message.getTag();
    cmi._revisionNumber = patchNum != null ? patchNum.get() : null;
    Account.Id realAuthor = message.getRealAuthor();
    if (realAuthor != null) {
      cmi.realAuthor = accountLoader.get(realAuthor);
    }
    return cmi;
  }
}
