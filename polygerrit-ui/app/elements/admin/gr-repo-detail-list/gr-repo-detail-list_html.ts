/**
 * @license
 * Copyright (C) 2020 The Android Open Source Project
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
import {html} from '@polymer/polymer/lib/utils/html-tag';

export const htmlTemplate = html`
  <style include="gr-form-styles">
    /* Workaround for empty style block - see https://github.com/Polymer/tools/issues/408 */
  </style>
  <style include="gr-table-styles">
    /* Workaround for empty style block - see https://github.com/Polymer/tools/issues/408 */
  </style>
  <style include="shared-styles">
    .tags td.name {
      min-width: 25em;
    }
    td.name,
    td.revision,
    td.message {
      word-break: break-word;
    }
    td.revision.tags {
      width: 27em;
    }
    td.message,
    td.tagger {
      max-width: 15em;
    }
    .editing .editItem {
      display: inherit;
    }
    .editItem,
    .editing .editBtn,
    .canEdit .revisionNoEditing,
    .editing .revisionWithEditing,
    .revisionEdit,
    .hideItem {
      display: none;
    }
    .revisionEdit gr-button {
      margin-left: var(--spacing-m);
    }
    .editBtn {
      margin-left: var(--spacing-l);
    }
    .canEdit .revisionEdit {
      align-items: center;
      display: flex;
    }
    .deleteButton:not(.show) {
      display: none;
    }
    .tagger.hide {
      display: none;
    }
  </style>
  <style include="gr-table-styles">
    /* Workaround for empty style block - see https://github.com/Polymer/tools/issues/408 */
  </style>
  <gr-list-view
    create-new="[[_loggedIn]]"
    filter="[[_filter]]"
    items-per-page="[[_itemsPerPage]]"
    items="[[_items]]"
    loading="[[_loading]]"
    offset="[[_offset]]"
    on-create-clicked="_handleCreateClicked"
    path="[[_getPath(_repo, detailType)]]"
  >
    <table id="list" class="genericList gr-form-styles">
      <tbody>
        <tr class="headerRow">
          <th class="name topHeader">Name</th>
          <th class="revision topHeader">Revision</th>
          <th class$="message topHeader [[_hideIfBranch(detailType)]]">
            Message
          </th>
          <th class$="tagger topHeader [[_hideIfBranch(detailType)]]">
            Tagger
          </th>
          <th class="repositoryBrowser topHeader">Repository Browser</th>
          <th class="delete topHeader"></th>
        </tr>
        <tr id="loading" class$="loadingMsg [[computeLoadingClass(_loading)]]">
          <td>Loading...</td>
        </tr>
      </tbody>
      <tbody class$="[[computeLoadingClass(_loading)]]">
        <template is="dom-repeat" items="[[_shownItems]]">
          <tr class="table">
            <td class$="[[detailType]] name">
              <a href$="[[_computeFirstWebLink(item)]]">
                [[_stripRefs(item.ref, detailType)]]
              </a>
            </td>
            <td
              class$="[[detailType]] revision [[_computeCanEditClass(item.ref, detailType, _isOwner)]]"
            >
              <span class="revisionNoEditing"> [[item.revision]] </span>
              <span class$="revisionEdit [[_computeEditingClass(_isEditing)]]">
                <span class="revisionWithEditing"> [[item.revision]] </span>
                <gr-button
                  link=""
                  on-click="_handleEditRevision"
                  class="editBtn"
                >
                  edit
                </gr-button>
                <iron-input bind-value="{{_revisedRef}}" class="editItem">
                  <input is="iron-input" bind-value="{{_revisedRef}}" />
                </iron-input>
                <gr-button
                  link=""
                  on-click="_handleCancelRevision"
                  class="cancelBtn editItem"
                >
                  Cancel
                </gr-button>
                <gr-button
                  link=""
                  on-click="_handleSaveRevision"
                  class="saveBtn editItem"
                  disabled="[[!_revisedRef]]"
                >
                  Save
                </gr-button>
              </span>
            </td>
            <td class$="message [[_hideIfBranch(detailType)]]">
              [[_computeMessage(item.message)]]
            </td>
            <td class$="tagger [[_hideIfBranch(detailType)]]">
              <div class$="tagger [[_computeHideTagger(item.tagger)]]">
                <gr-account-link account="[[item.tagger]]"> </gr-account-link>
                (<gr-date-formatter withTooltip date-str="[[item.tagger.date]]">
                </gr-date-formatter
                >)
              </div>
            </td>
            <td class="repositoryBrowser">
              <template
                is="dom-repeat"
                items="[[_computeWeblink(item)]]"
                as="link"
              >
                <a
                  href$="[[link.url]]"
                  class="webLink"
                  rel="noopener"
                  target="_blank"
                >
                  ([[link.name]])
                </a>
              </template>
            </td>
            <td class="delete">
              <gr-button
                link=""
                class$="deleteButton [[_computeHideDeleteClass(_isOwner, item.can_delete)]]"
                on-click="_handleDeleteItem"
              >
                Delete
              </gr-button>
            </td>
          </tr>
        </template>
      </tbody>
    </table>
    <gr-overlay id="overlay" with-backdrop="">
      <gr-confirm-delete-item-dialog
        class="confirmDialog"
        on-confirm="_handleDeleteItemConfirm"
        on-cancel="_handleConfirmDialogCancel"
        item="[[_refName]]"
        item-type-name="[[_computeItemName(detailType)]]"
      ></gr-confirm-delete-item-dialog>
    </gr-overlay>
  </gr-list-view>
  <gr-overlay id="createOverlay" with-backdrop="">
    <gr-dialog
      id="createDialog"
      disabled="[[!_hasNewItemName]]"
      confirm-label="Create"
      on-confirm="_handleCreateItem"
      on-cancel="_handleCloseCreate"
    >
      <div class="header" slot="header">
        Create [[_computeItemName(detailType)]]
      </div>
      <div class="main" slot="main">
        <gr-create-pointer-dialog
          id="createNewModal"
          detail-type="[[_computeItemName(detailType)]]"
          has-new-item-name="{{_hasNewItemName}}"
          item-detail="[[detailType]]"
          repo-name="[[_repo]]"
        ></gr-create-pointer-dialog>
      </div>
    </gr-dialog>
  </gr-overlay>
`;
